class_name TrafficCar
extends AnimatableBody3D
## Véhicule de circulation piloté par l'IA (cinématique, peu coûteux) :
## suit les voies, respecte les feux, freine derrière les obstacles, tourne aux intersections.

const L = preload("res://scripts/world/city_layout.gd")

var type := "sedan"
var color := Color.WHITE
var spec: Dictionary
var is_police := false
var from_node := Vector2i.ZERO
var to_node := Vector2i(0, 1)
var speed := 0.0
var _dist := 0.0
var _seg_start := Vector3.ZERO
var _seg_end := Vector3.ZERO
var _seg_len := 1.0
var _turning := false
var _turn_pts: PackedVector3Array
var _turn_len := 1.0
var _turn_d := 0.0
var _next_node := Vector2i.ZERO
var _ray: RayCast3D
var _blocked_time := 0.0
var _rng := RandomNumberGenerator.new()
var _model: Dictionary
var _horn: AudioStreamPlayer3D
var active := true


func setup(p_type: String, p_color: Color, seed_value: int) -> void:
	type = p_type
	color = p_color
	spec = VehicleSpecs.get_spec(type)
	is_police = spec.get("police", false)
	_rng.seed = seed_value
	collision_layer = 4
	collision_mask = 0
	sync_to_physics = false
	add_to_group("traffic")
	_model = VehicleModel.build(type, color, true)
	add_child(_model["node"])
	var cs := CollisionShape3D.new()
	var box := BoxShape3D.new()
	var h: float = float(spec["height"]) + float(spec["cabin"])
	if spec.get("box", false):
		h = float(spec["height"]) * 1.1
	box.size = Vector3(spec["width"], h, spec["length"])
	cs.shape = box
	cs.position = Vector3(0, float(spec["wheel_r"]) * 0.55 + h * 0.5, 0)
	add_child(cs)
	_ray = RayCast3D.new()
	_ray.position = Vector3(0, 0.9, float(spec["length"]) * 0.5 + 0.2)
	_ray.target_position = Vector3(0, 0, 12.0)
	_ray.collision_mask = 2 | 4
	_ray.add_exception(self)
	add_child(_ray)
	_horn = AudioStreamPlayer3D.new()
	_horn.stream = Sfx.get_stream("horn")
	_horn.bus = "SFX"
	_horn.unit_size = 8.0
	_horn.max_distance = 90.0
	_horn.volume_db = -6.0
	add_child(_horn)
	set_visibility(Settings.get_value("view_distance") * 0.7)


func set_visibility(dist: float) -> void:
	for mi in find_children("*", "MeshInstance3D", true, false):
		(mi as MeshInstance3D).visibility_range_end = dist


func display_name() -> String:
	return Loc.t(spec["name"])


## Place le véhicule au début d'un segment de voie.
func place_on(a: Vector2i, b: Vector2i, progress: float = 0.0) -> void:
	from_node = a
	to_node = b
	_turning = false
	_set_segment()
	_dist = clampf(progress, 0.0, 1.0) * _seg_len
	speed = L.road_speed(b - a) * 0.6
	_apply_transform(_seg_start.lerp(_seg_end, _dist / _seg_len), (_seg_end - _seg_start).normalized())
	_ray.force_raycast_update()


func _set_segment() -> void:
	var pts := L.lane_points(from_node, to_node)
	_seg_start = pts[0]
	_seg_end = pts[1]
	_seg_len = maxf(_seg_start.distance_to(_seg_end), 0.5)


func _choose_next() -> Vector2i:
	var options := L.neighbors(to_node)
	options.erase(from_node)
	if options.is_empty():
		return from_node
	var dir := to_node - from_node
	var straight := to_node + dir
	if straight in options and _rng.randf() < 0.55:
		return straight
	return options[_rng.randi() % options.size()]


func _build_turn() -> void:
	_next_node = _choose_next()
	var p0 := _seg_end
	var nxt := L.lane_points(to_node, _next_node)
	var p2 := nxt[0]
	var d1 := L.dir3(to_node - from_node)
	var p1: Vector3
	if _next_node == from_node:
		# Demi-tour (impasse)
		p1 = p0 + d1 * 8.0
	elif absf(d1.dot(L.dir3(_next_node - to_node))) > 0.5:
		p1 = (p0 + p2) * 0.5
	else:
		p1 = p0 + d1 * (p2 - p0).dot(d1)
	_turn_pts = PackedVector3Array()
	var steps := 10
	for k in steps + 1:
		var t := float(k) / steps
		var a := p0.lerp(p1, t)
		var b := p1.lerp(p2, t)
		_turn_pts.append(a.lerp(b, t))
	_turn_len = 0.0
	for k in steps:
		_turn_len += _turn_pts[k].distance_to(_turn_pts[k + 1])
	_turn_d = 0.0


func _turn_sample(d: float) -> Array:
	var acc := 0.0
	for k in _turn_pts.size() - 1:
		var seg := _turn_pts[k].distance_to(_turn_pts[k + 1])
		if acc + seg >= d or k == _turn_pts.size() - 2:
			var t := clampf((d - acc) / maxf(seg, 0.001), 0.0, 1.0)
			return [_turn_pts[k].lerp(_turn_pts[k + 1], t), (_turn_pts[k + 1] - _turn_pts[k]).normalized()]
		acc += seg
	return [_turn_pts[_turn_pts.size() - 1], Vector3.FORWARD]


func _physics_process(delta: float) -> void:
	if not active:
		return
	var dir := to_node - from_node
	var max_speed := L.road_speed(dir) * (1.0 if not is_police else 1.1)
	if spec.get("bus", false) or spec.get("truck", false):
		max_speed *= 0.8
	var target := max_speed
	if _turning:
		target = minf(target, 6.5)
	else:
		# Feu tricolore à la fin du segment
		var to_line := _seg_len - _dist
		var light := TrafficSignals.state(TrafficSignals.axis_of(dir))
		if light != "g":
			var stop_dist := speed * speed / (2.0 * 5.0) + 1.0
			if light == "r" or to_line > stop_dist * 0.6:
				if to_line < 30.0:
					target = minf(target, maxf(0.0, (to_line - 0.5) * 0.9))
		if to_line < 12.0:
			target = minf(target, maxf(7.0, to_line))
	# Obstacle devant (joueur, autre véhicule)
	_ray.target_position = Vector3(0, 0, 6.0 + speed * 1.1)
	var blocked_by_player := false
	if _ray.is_colliding():
		var hit_d := _ray.global_position.distance_to(_ray.get_collision_point())
		target = minf(target, maxf(0.0, (hit_d - 2.5) * 1.2))
		var col := _ray.get_collider()
		if col and (col.is_in_group("player") or (col is Vehicle and (col as Vehicle).player_driving)):
			blocked_by_player = true
	if blocked_by_player and speed < 0.5:
		_blocked_time += delta
		if _blocked_time > 2.5 and not _horn.playing:
			_horn.play()
			_blocked_time = -1.5
	else:
		_blocked_time = maxf(0.0, _blocked_time - delta)
	var accel := 3.2 if target > speed else 8.0
	speed = move_toward(speed, target, accel * delta)
	var step := speed * delta
	if _turning:
		_turn_d += step
		if _turn_d >= _turn_len:
			from_node = to_node
			to_node = _next_node
			_turning = false
			_set_segment()
			_dist = _turn_d - _turn_len
		else:
			var s := _turn_sample(_turn_d)
			_apply_transform(s[0], s[1])
			return
	_dist += step
	if _dist >= _seg_len:
		_build_turn()
		_turning = true
		_turn_d = _dist - _seg_len
		var s2 := _turn_sample(_turn_d)
		_apply_transform(s2[0], s2[1])
		return
	_apply_transform(_seg_start.lerp(_seg_end, _dist / _seg_len), (_seg_end - _seg_start).normalized())


func _apply_transform(pos: Vector3, fwd: Vector3) -> void:
	if fwd.length() < 0.01:
		fwd = Vector3.BACK
	global_transform = Transform3D(Basis.looking_at(fwd, Vector3.UP, true), pos)


func forward_speed() -> float:
	return speed
