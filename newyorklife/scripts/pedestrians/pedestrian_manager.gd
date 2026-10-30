class_name PedestrianManager
extends Node3D
## Pool d'habitants autour du joueur, mise à jour à cadence réduite pour les plus lointains,
## détection des collisions avec le véhicule du joueur.

const L = preload("res://scripts/world/city_layout.gd")

var player: Player
var peds: Array[Pedestrian] = []
var _rng := RandomNumberGenerator.new()
var _check := 0.0
var _frame := 0


func _ready() -> void:
	_rng.randomize()
	Events.quality_changed.connect(_resize)


func populate() -> void:
	_resize()
	for p in peds:
		_respawn(p, true)


func _resize() -> void:
	var want: int = Settings.get_value("pedestrians")
	while peds.size() < want:
		var p := Pedestrian.new()
		add_child(p)
		p.setup(_rng.randi())
		peds.append(p)
		if player:
			_respawn(p, true)
	while peds.size() > want:
		var p: Pedestrian = peds.pop_back()
		p.queue_free()
	for p in peds:
		p.rig.set_visibility_range(Settings.get_value("detail_range"))


func _respawn(p: Pedestrian, anywhere: bool) -> void:
	if p.reserved:
		return
	var center := player.focus_position() if player else L.node_pos(Vector2i(2, 3))
	var cam := get_viewport().get_camera_3d()
	var bc := L.block_count()
	for attempt in 20:
		var b := Vector2i(_rng.randi_range(0, bc.x - 1), _rng.randi_range(0, bc.y - 1))
		var c := L.block_center(b)
		var d := c.distance_to(center)
		if d > 130.0:
			continue
		p.spawn_at_block(b)
		var pd := p.global_position.distance_to(center)
		if not anywhere and cam and not cam.is_position_behind(p.global_position) and pd < 60.0:
			continue
		return


func _physics_process(delta: float) -> void:
	if player == null:
		return
	_frame += 1
	var center := player.focus_position()
	var danger_speed := 0.0
	var danger_pos := center
	var v := player.vehicle
	if v:
		danger_speed = v.speed()
	for i in peds.size():
		var p := peds[i]
		var d := p.global_position.distance_to(center)
		# Les habitants lointains sont mis à jour un cadre sur deux
		if d > 40.0 and (i + _frame) % 2 == 0:
			continue
		var dt := delta if d <= 40.0 else delta * 2.0
		p.update_ai(dt, danger_pos, danger_speed, player.global_position if not v else Vector3(1e6, 0, 0))
		# Collision avec le véhicule du joueur
		if v and p.visible and not p.is_knocked() and not p.reserved and danger_speed > 3.5:
			var rel := p.global_position - v.global_position
			rel.y = 0.0
			var local := v.global_transform.basis.inverse() * rel
			var hw: float = float(v.spec["width"]) * 0.5 + 0.35
			var hl: float = float(v.spec["length"]) * 0.5 + 0.35
			if absf(local.x) < hw and absf(local.z) < hl:
				p.knock(v.global_position)
				Sfx.play("hit", -4.0)
				v.add_damage(1.5)
				Events.crime.emit(1, p.global_position, "pedestrian")
				for o in peds:
					if o != p and o.global_position.distance_to(p.global_position) < 18.0:
						o.panic(p.global_position)
	_check -= delta
	if _check <= 0.0:
		_check = 0.7
		for p in peds:
			if p.global_position.distance_to(center) > 150.0 and not p.reserved:
				_respawn(p, false)


## Fait fuir tous les habitants proches (klaxon, accident…).
func scare(at: Vector3, radius: float) -> void:
	for p in peds:
		if p.global_position.distance_to(at) < radius:
			p.panic(at)


## Réserve un habitant pour une mission (passager) et le place à un point donné.
func reserve_at(pos: Vector3) -> Pedestrian:
	var best: Pedestrian = null
	for p in peds:
		if not p.reserved:
			best = p
			break
	if best == null:
		return null
	best.reserved = true
	best.state = Pedestrian.State.IDLE
	best.visible = true
	best.rig.fallen = 0.0
	best.global_position = pos
	return best


func release(p: Pedestrian) -> void:
	if p and is_instance_valid(p):
		p.reserved = false
		p.visible = true
		_respawn(p, false)
