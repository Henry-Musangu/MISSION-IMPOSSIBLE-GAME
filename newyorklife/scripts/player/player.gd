class_name Player
extends CharacterBody3D
## Personnage joueur : marche, course, sprint, saut, interaction, entrée/sortie de véhicule.

const WALK_SPEED := 2.0
const RUN_SPEED := 4.6
const SPRINT_SPEED := 7.6
const JUMP_VELOCITY := 5.2
const GRAVITY := 15.0
const ACCEL := 14.0
const ENTER_RANGE := 3.6

var rig: HumanoidRig
var camera_yaw := 0.0                   ## fourni par la caméra
var vehicle: Vehicle = null             ## véhicule conduit
var controls_enabled := true
var knocked := 0.0
var _air_time := 0.0
var _fall_speed := 0.0
var _regen_delay := 0.0
var _invuln := 0.0


func _ready() -> void:
	add_to_group("player")
	collision_layer = 2
	collision_mask = 1 | 4
	floor_max_angle = deg_to_rad(55.0)
	floor_snap_length = 0.35
	var cs := CollisionShape3D.new()
	var cap := CapsuleShape3D.new()
	cap.radius = 0.32
	cap.height = 1.8
	cs.shape = cap
	cs.position = Vector3(0, 0.9, 0)
	add_child(cs)
	rig = HumanoidRig.new()
	rig.name = "Rig"
	add_child(rig)
	rig.build(Color(0.16, 0.3, 0.6), Color(0.15, 0.17, 0.22), HumanoidRig.SKINS[0])
	rig.set_outfit(GameState.outfit)


func is_driving() -> bool:
	return vehicle != null


func focus_node() -> Node3D:
	return vehicle if vehicle else self


func focus_position() -> Vector3:
	return focus_node().global_position


func _physics_process(delta: float) -> void:
	_invuln = maxf(0.0, _invuln - delta)
	if vehicle:
		global_position = vehicle.global_position
		return
	if knocked > 0.0:
		knocked -= delta
		rig.fallen = 1.0 if knocked > 0.6 else 0.0
		velocity.x = move_toward(velocity.x, 0.0, delta * 10.0)
		velocity.z = move_toward(velocity.z, 0.0, delta * 10.0)
		velocity.y -= GRAVITY * delta
		move_and_slide()
		rig.animate(delta, 0.0, false)
		return
	rig.fallen = 0.0
	var input := InputState.move if controls_enabled else Vector2.ZERO
	var fwd := Vector3(-sin(camera_yaw), 0, -cos(camera_yaw))
	var right := fwd.cross(Vector3.UP)
	var dir := (fwd * input.y + right * input.x)
	var mag := minf(dir.length(), 1.0)
	if mag > 0.01:
		dir = dir.normalized()
	var target_speed := 0.0
	if mag > 0.05:
		target_speed = WALK_SPEED if mag < 0.55 else RUN_SPEED
		if controls_enabled and InputState.is_held("sprint"):
			target_speed = SPRINT_SPEED
	var hv := Vector3(velocity.x, 0, velocity.z)
	hv = hv.move_toward(dir * target_speed, ACCEL * delta)
	velocity.x = hv.x
	velocity.z = hv.z
	if is_on_floor():
		if _air_time > 0.4 and _fall_speed > 13.0:
			take_damage((_fall_speed - 13.0) * 8.0)
		_air_time = 0.0
		_fall_speed = 0.0
		if controls_enabled and InputState.consume("jump"):
			velocity.y = JUMP_VELOCITY
			Sfx.play("jump", -10.0)
	else:
		_air_time += delta
		_fall_speed = maxf(_fall_speed, -velocity.y)
		velocity.y -= GRAVITY * delta
	_step_up(hv * delta)
	move_and_slide()
	if hv.length() > 0.2:
		var target_yaw := atan2(hv.x, hv.z)
		rig.rotation.y = lerp_angle(rig.rotation.y, target_yaw, delta * 12.0)
	rig.animate(delta, hv.length(), not is_on_floor() and _air_time > 0.12)
	# Régénération lente
	_regen_delay -= delta
	if _regen_delay <= 0.0 and GameState.health < 100.0:
		GameState.health = minf(100.0, GameState.health + delta * 1.5)


## Monte les marches / bordures de trottoir jusqu'à 35 cm.
func _step_up(motion: Vector3) -> void:
	if not is_on_floor() or motion.length() < 0.001:
		return
	var xf := global_transform
	if not test_move(xf, motion * 2.0):
		return
	var up_xf := xf.translated(Vector3(0, 0.36, 0))
	if test_move(xf, Vector3(0, 0.36, 0)):
		return
	if not test_move(up_xf, motion * 2.0):
		global_position.y += 0.36
		velocity.y = maxf(velocity.y, 0.0)


func take_damage(amount: float) -> void:
	if _invuln > 0.0 or amount <= 0.0:
		return
	GameState.health = maxf(0.0, GameState.health - amount)
	_regen_delay = 6.0
	Events.player_health_changed.emit(GameState.health)
	Input.vibrate_handheld(60)
	if GameState.health <= 0.0:
		Events.player_died.emit()


## Renversé par un véhicule.
func knock_down(push: Vector3, dmg: float) -> void:
	if vehicle or knocked > 0.0 or _invuln > 0.0:
		return
	knocked = 2.2
	velocity = push + Vector3(0, 3.0, 0)
	take_damage(dmg)
	_invuln = 2.5


func enter_vehicle(v: Vehicle) -> void:
	vehicle = v
	visible = false
	collision_layer = 0
	collision_mask = 0
	velocity = Vector3.ZERO
	v.set_player_driving(true)
	Sfx.play("door", -6.0)
	Events.player_entered_vehicle.emit(v)


func exit_vehicle() -> void:
	if vehicle == null:
		return
	var v := vehicle
	var xf := v.global_transform
	var w: float = v.spec["width"]
	var candidates := [
		xf * Vector3(w * 0.5 + 0.9, 0.3, 0.3),
		xf * Vector3(-w * 0.5 - 0.9, 0.3, 0.3),
		xf * Vector3(0, 0.3, float(v.spec["length"]) * 0.5 + 1.2),
		xf * Vector3(0, 0.3, -float(v.spec["length"]) * 0.5 - 1.2),
		v.global_position + Vector3(0, 3.0, 0),
	]
	vehicle = null
	v.set_player_driving(false)
	collision_layer = 2
	collision_mask = 1 | 4
	var chosen: Vector3 = candidates[-1]
	for c in candidates:
		var test := Transform3D(Basis(), c + Vector3(0, 0.05, 0))
		if not test_move(test, Vector3(0, 0.01, 0)):
			chosen = c
			break
	global_position = Vector3(chosen.x, maxf(chosen.y, 0.0), chosen.z)
	rig.rotation.y = v.global_rotation.y
	velocity = Vector3.ZERO
	visible = true
	Sfx.play("door", -6.0)
	Events.player_exited_vehicle.emit(v)


func nearest_vehicle() -> Vehicle:
	var best: Vehicle = null
	var best_d := ENTER_RANGE
	for v in get_tree().get_nodes_in_group("vehicles"):
		var veh := v as Vehicle
		if veh == null or veh.ai_driving:
			continue
		var d := veh.global_position.distance_to(global_position)
		var reach := ENTER_RANGE + float(veh.spec["length"]) * 0.3
		if d < reach and d < best_d + float(veh.spec["length"]) * 0.3:
			best = veh
			best_d = d
	return best


func apply_outfit() -> void:
	rig.set_outfit(GameState.outfit)
