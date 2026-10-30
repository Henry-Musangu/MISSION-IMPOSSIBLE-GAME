class_name Pedestrian
extends Node3D
## Habitant : marche sur les trottoirs autour de son bloc, s'arrête, discute,
## entre dans les commerces puis ressort, fuit le danger, peut être renversé.

const L = preload("res://scripts/world/city_layout.gd")

enum State { WALK, IDLE, INSIDE, FLEE, KNOCKED, PARK }

var rig: HumanoidRig
var state := State.WALK
var block := Vector2i.ZERO
var corner := 0
var clockwise := true
var speed := 1.3
var _timer := 0.0
var _target := Vector3.ZERO
var _flee_dir := Vector3.ZERO
var _rng := RandomNumberGenerator.new()
var _inset := 2.6
var in_park := false
var reserved := false        ## utilisé par une mission (passager)


func setup(seed_value: int) -> void:
	_rng.seed = seed_value
	rig = HumanoidRig.new()
	add_child(rig)
	var tops := [Color(0.7, 0.2, 0.15), Color(0.2, 0.3, 0.55), Color(0.85, 0.85, 0.8), Color(0.1, 0.1, 0.1),
		Color(0.35, 0.5, 0.3), Color(0.6, 0.45, 0.25), Color(0.5, 0.2, 0.45), Color(0.9, 0.65, 0.15)]
	var legs := [Color(0.12, 0.14, 0.2), Color(0.25, 0.25, 0.27), Color(0.4, 0.33, 0.25), Color(0.1, 0.1, 0.1)]
	var hair := [Color(0.05, 0.04, 0.04), Color(0.3, 0.2, 0.1), Color(0.6, 0.5, 0.3), Color(0.5, 0.5, 0.5)]
	rig.build(tops[_rng.randi() % tops.size()], legs[_rng.randi() % legs.size()],
		HumanoidRig.SKINS[_rng.randi() % HumanoidRig.SKINS.size()], hair[_rng.randi() % hair.size()])
	var s := _rng.randf_range(0.9, 1.08)
	rig.scale = Vector3(s, s, s)
	speed = _rng.randf_range(1.1, 1.6)
	rig.set_visibility_range(Settings.get_value("detail_range"))
	add_to_group("pedestrians")


func spawn_at_block(b: Vector2i) -> void:
	block = b
	in_park = b == L.PARK_BLOCK and _rng.randf() < 0.7
	clockwise = _rng.randf() < 0.5
	_inset = _rng.randf_range(2.2, 3.2)
	state = State.PARK if in_park else State.WALK
	corner = _rng.randi() % 4
	if in_park:
		global_position = _park_point()
		_target = _park_point()
	else:
		var a := _corner_pos(corner)
		var b2 := _corner_pos(_next_corner())
		global_position = a.lerp(b2, _rng.randf())
		_target = b2
		corner = _next_corner()
	visible = true
	rig.fallen = 0.0
	rig.rotation.x = 0.0


func _corner_pos(c: int) -> Vector3:
	var r := L.block_rect(block)
	var i := _inset
	match c:
		0: return Vector3(r.position.x + i, L.CURB_H, r.position.y + i)
		1: return Vector3(r.end.x - i, L.CURB_H, r.position.y + i)
		2: return Vector3(r.end.x - i, L.CURB_H, r.end.y - i)
		_: return Vector3(r.position.x + i, L.CURB_H, r.end.y - i)


func _next_corner() -> int:
	return (corner + (1 if clockwise else 3)) % 4


func _park_point() -> Vector3:
	var lot := L.block_rect(block).grow(-L.SIDEWALK - 3.0)
	var c := lot.get_center()
	# Points le long des allées en croix
	if _rng.randf() < 0.5:
		return Vector3(_rng.randf_range(lot.position.x, lot.end.x), L.CURB_H, c.y + _rng.randf_range(-1.0, 1.0))
	return Vector3(c.x + _rng.randf_range(-1.0, 1.0), L.CURB_H, _rng.randf_range(lot.position.y, lot.end.y))


func update_ai(delta: float, danger_pos: Vector3, danger_speed: float, player_pos: Vector3) -> void:
	if reserved:
		rig.animate(delta, 0.0, false)
		return
	match state:
		State.KNOCKED:
			_timer -= delta
			rig.fallen = 1.0
			if _timer <= 0.0:
				rig.fallen = 0.0
				state = State.FLEE
				_timer = 4.0
				_flee_dir = (global_position - player_pos).normalized()
			rig.animate(delta, 0.0, false)
			return
		State.INSIDE:
			_timer -= delta
			if _timer <= 0.0:
				visible = true
				state = State.WALK
			return
	# Réaction au danger (véhicule rapide proche, klaxon)
	var to_danger := danger_pos - global_position
	to_danger.y = 0.0
	if danger_speed > 9.0 and to_danger.length() < 9.0 and state != State.FLEE:
		state = State.FLEE
		_timer = 3.0
		_flee_dir = -to_danger.normalized()
	var move_speed := 0.0
	var dir := Vector3.ZERO
	match state:
		State.WALK, State.PARK:
			dir = _target - global_position
			dir.y = 0.0
			if dir.length() < 0.4:
				_arrived()
				dir = Vector3.ZERO
			else:
				dir = dir.normalized()
				move_speed = speed
			# Le joueur à pied trop proche : on s'écarte un peu
			var to_p := player_pos - global_position
			to_p.y = 0.0
			if to_p.length() < 0.9 and to_p.length() > 0.01:
				global_position -= to_p.normalized() * delta * 1.5
		State.IDLE:
			_timer -= delta
			if _timer <= 0.0:
				state = State.PARK if in_park else State.WALK
			# se tourne vers un voisin ou le joueur (« discute »)
			var look := player_pos - global_position
			if look.length() < 4.0:
				rig.rotation.y = lerp_angle(rig.rotation.y, atan2(look.x, look.z), delta * 4.0)
		State.FLEE:
			_timer -= delta
			dir = _flee_dir
			move_speed = 5.0
			if _timer <= 0.0:
				_resume_walk()
	if dir != Vector3.ZERO:
		var np := global_position + dir * move_speed * delta
		# reste sur le trottoir du bloc
		var r := L.block_rect(block).grow(-0.8)
		np.x = clampf(np.x, r.position.x, r.end.x)
		np.z = clampf(np.z, r.position.y, r.end.y)
		np.y = L.CURB_H
		global_position = np
		rig.rotation.y = lerp_angle(rig.rotation.y, atan2(dir.x, dir.z), delta * 8.0)
	rig.animate(delta, move_speed, false)


func _arrived() -> void:
	if in_park:
		if _rng.randf() < 0.3:
			state = State.IDLE
			_timer = _rng.randf_range(3.0, 9.0)
		_target = _park_point()
		return
	var roll := _rng.randf()
	if roll < 0.12:
		# entre dans un commerce, ressortira plus tard
		state = State.INSIDE
		_timer = _rng.randf_range(6.0, 18.0)
		visible = false
	elif roll < 0.3:
		state = State.IDLE
		_timer = _rng.randf_range(2.0, 7.0)
	corner = _next_corner()
	_target = _corner_pos(corner)
	if _rng.randf() < 0.15:
		clockwise = not clockwise


func _resume_walk() -> void:
	state = State.PARK if in_park else State.WALK
	if not in_park:
		# repart vers le coin le plus proche
		var best := 0
		var bd := 1e9
		for c in 4:
			var d := _corner_pos(c).distance_to(global_position)
			if d < bd:
				bd = d
				best = c
		corner = best
		_target = _corner_pos(corner)


func knock(from: Vector3) -> void:
	if state == State.KNOCKED or not visible:
		return
	state = State.KNOCKED
	_timer = 6.0
	var push := (global_position - from)
	push.y = 0
	if push.length() > 0.01:
		global_position += push.normalized() * 1.5
	global_position.y = L.CURB_H if not L.is_on_road(global_position) else 0.0


func is_knocked() -> bool:
	return state == State.KNOCKED


func panic(from: Vector3) -> void:
	if state == State.KNOCKED or state == State.INSIDE or reserved:
		return
	state = State.FLEE
	_timer = 3.5
	var d := global_position - from
	d.y = 0
	_flee_dir = d.normalized() if d.length() > 0.01 else Vector3.RIGHT
