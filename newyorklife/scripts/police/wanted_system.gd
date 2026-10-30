class_name WantedSystem
extends Node3D
## Niveaux de recherche 1 à 5 : 1 intervention locale, 2 poursuite, 3 renforts,
## 4 intervention renforcée (barrages), 5 recherche intensive.
## Le niveau retombe si le joueur reste hors de vue des policiers assez longtemps.

const L = preload("res://scripts/world/city_layout.gd")
const MAX_LEVEL := 5

var player: Player
var traffic: TrafficManager
var level := 0
var units: Array[Vehicle] = []
var _escape := 0.0
var _spawn_timer := 0.0
var _bust_timer := 0.0
var _roadblock_timer := 0.0
var heat := 0.0                  ## accumulation des délits mineurs


func _ready() -> void:
	Events.crime.connect(_on_crime)


func _on_crime(severity: int, pos: Vector3, kind: String) -> void:
	if player == null:
		return
	var witnessed := level > 0 or traffic.police_near(pos, 70.0) or _units_near(pos, 80.0)
	if not witnessed:
		# Un témoin appelle la police (plus probable pour les délits graves)
		heat += severity * 0.4
		if heat < 1.0:
			return
	heat = 0.0
	var new_level := mini(MAX_LEVEL, maxi(level, 1) + (severity - 1 if level == 0 else severity))
	if kind == "carjack" and level == 0:
		new_level = 1
	set_level(new_level)


func set_level(l: int) -> void:
	l = clampi(l, 0, MAX_LEVEL)
	if l == level:
		_escape = 0.0
		return
	var rising := l > level
	level = l
	GameState.wanted_level = level
	_escape = 0.0
	Events.wanted_changed.emit(level)
	if rising:
		Sfx.play("alert", -6.0)
	if level == 0:
		_clear_units()


func _units_near(p: Vector3, r: float) -> bool:
	for u in units:
		if is_instance_valid(u) and u.global_position.distance_to(p) < r:
			return true
	return false


func _desired_units() -> int:
	return [0, 1, 2, 3, 4, 5][level]


func _physics_process(delta: float) -> void:
	if player == null or level == 0:
		return
	var target := player.focus_position()
	# Nettoyage
	for u in units.duplicate():
		if not is_instance_valid(u):
			units.erase(u)
		elif u.global_position.distance_to(target) > 260.0 or u.is_wrecked() and u.global_position.distance_to(target) > 60.0:
			_remove_unit(u)
	# Renforts
	_spawn_timer -= delta
	if _spawn_timer <= 0.0 and units.filter(func(u): return not u.get_meta("roadblock", false)).size() < _desired_units():
		_spawn_timer = maxf(2.0, 7.0 - level)
		_spawn_unit(false)
	# Barrages à partir du niveau 4
	if level >= 4:
		_roadblock_timer -= delta
		if _roadblock_timer <= 0.0:
			_roadblock_timer = 25.0 - level * 2.0
			_spawn_roadblock()
	# Fuite : aucun policier à proximité avec ligne de vue
	var seen := false
	for u in units:
		if is_instance_valid(u) and not u.is_wrecked() and u.global_position.distance_to(target) < 55.0 + level * 8.0:
			seen = true
			break
	if seen:
		_escape = 0.0
	else:
		_escape += delta
		if _escape > 8.0 + level * 4.0:
			set_level(0)
			Events.notify.emit(Loc.t("wanted") + " : 0", "good")
			return
	# Arrestation
	var close := false
	for u in units:
		if is_instance_valid(u) and not u.is_wrecked() and u.global_position.distance_to(target) < (6.0 if player.vehicle else 4.5):
			close = true
			break
	var slow := player.vehicle == null or player.vehicle.speed() < 2.0
	if close and slow:
		_bust_timer += delta
		if _bust_timer > (1.5 if player.vehicle == null else 3.0):
			_bust_timer = 0.0
			Events.busted.emit()
	else:
		_bust_timer = maxf(0.0, _bust_timer - delta)


func _spawn_unit(_roadblock: bool) -> Vehicle:
	var target := player.focus_position()
	var spot := _find_spawn(target, 70.0, 130.0)
	if spot.is_empty():
		return null
	var car := Vehicle.new()
	car.setup("police", Color(0.92, 0.93, 0.95))
	add_child(car)
	car.global_transform = Transform3D(Basis(Vector3.UP, spot["yaw"]), spot["pos"] + Vector3(0, 0.6, 0))
	car.ai_driving = true
	car.siren_on = true
	var ai := PoliceAI.new()
	ai.car = car
	ai.player = player
	ai.aggressiveness = 0.75 + level * 0.12
	car.add_child(ai)
	car.set_meta("roadblock", false)
	units.append(car)
	car.set_player_driving(false)
	car.handbrake = false
	return car


func _spawn_roadblock() -> void:
	var p := player.focus_position()
	var fwd := Vector3.FORWARD
	if player.vehicle:
		fwd = player.vehicle.linear_velocity.normalized() if player.vehicle.speed() > 2.0 else player.vehicle.global_transform.basis.z
	var ahead := p + fwd * 70.0
	var n := L.nearest_node(ahead)
	var c := L.node_pos(n)
	if c.distance_to(p) < 35.0:
		return
	for k in 2:
		var car := Vehicle.new()
		car.setup("police", Color(0.92, 0.93, 0.95))
		add_child(car)
		var off := Vector3(k * 4.0 - 2.0, 0.6, 0) if absf(fwd.z) > absf(fwd.x) else Vector3(0, 0.6, k * 4.0 - 2.0)
		var yaw := PI * 0.5 if absf(fwd.z) > absf(fwd.x) else 0.0
		car.global_transform = Transform3D(Basis(Vector3.UP, yaw + k * 0.3), c + off)
		car.siren_on = true
		car.ai_driving = true
		car.handbrake = true
		var ai := PoliceAI.new()
		ai.car = car
		ai.player = player
		ai.roadblock = true
		car.add_child(ai)
		car.set_meta("roadblock", true)
		units.append(car)


func _find_spawn(target: Vector3, dmin: float, dmax: float) -> Dictionary:
	var cam := get_viewport().get_camera_3d()
	for attempt in 40:
		var a := Vector2i(randi_range(0, L.AVE_COUNT - 1), randi_range(0, L.ST_COUNT - 1))
		var nb := L.neighbors(a)
		var b: Vector2i = nb[randi() % nb.size()]
		var pts := L.lane_points(a, b)
		var p := pts[0].lerp(pts[1], randf_range(0.1, 0.6))
		var d := p.distance_to(target)
		if d < dmin or d > dmax:
			continue
		if cam and not cam.is_position_behind(p) and d < 90.0:
			continue
		var dir := pts[1] - pts[0]
		return {"pos": p, "yaw": atan2(dir.x, dir.z)}
	return {}


func _remove_unit(u: Vehicle) -> void:
	units.erase(u)
	if is_instance_valid(u):
		u.queue_free()


func _clear_units() -> void:
	for u in units:
		if is_instance_valid(u):
			u.queue_free()
	units.clear()
	_bust_timer = 0.0


func clear() -> void:
	set_level(0)
	_clear_units()
