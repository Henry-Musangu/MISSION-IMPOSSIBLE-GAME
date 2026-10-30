class_name FollowCamera
extends Node3D
## Caméra troisième personne : orbite au doigt, anti-collision (SpringArm3D),
## recentrage automatique derrière le véhicule.

var target: Node3D
var player: Player
var yaw := 0.0
var pitch := -0.32
var arm: SpringArm3D
var cam: Camera3D
var _idle_look := 10.0
var cinematic := false
var _shake := 0.0

const FOOT_LENGTH := 4.6
const FOOT_HEIGHT := 1.55
const CAR_LENGTH := 7.5
const CAR_HEIGHT := 1.6


func _ready() -> void:
	arm = SpringArm3D.new()
	arm.spring_length = FOOT_LENGTH
	arm.collision_mask = 1
	arm.margin = 0.3
	var shape := SphereShape3D.new()
	shape.radius = 0.25
	arm.shape = shape
	add_child(arm)
	cam = Camera3D.new()
	cam.fov = 70.0
	cam.near = 0.1
	arm.add_child(cam)
	cam.current = true
	Events.quality_changed.connect(_on_quality)
	_on_quality()


func _on_quality() -> void:
	cam.far = Settings.get_value("view_distance")


func shake(amount: float) -> void:
	_shake = maxf(_shake, amount)


func _process(delta: float) -> void:
	if cinematic or target == null or not is_instance_valid(target):
		return
	var look := InputState.take_look() * 0.005 * Settings.camera_sensitivity
	if look.length() > 0.0001:
		_idle_look = 0.0
	else:
		_idle_look += delta
	yaw -= look.x
	pitch = clampf(pitch - look.y, -1.2, 0.35)
	var driving := player != null and player.is_driving()
	var height := CAR_HEIGHT if driving else FOOT_HEIGHT
	var length := CAR_LENGTH if driving else FOOT_LENGTH
	if driving:
		var v := player.vehicle
		length += clampf(v.speed() / 30.0, 0.0, 1.0) * 2.0 + float(v.spec["length"]) * 0.25
		height += float(v.spec["height"]) * 0.5
		# Recentrage derrière le véhicule quand le joueur ne touche pas la caméra
		if _idle_look > 1.2 and v.speed() > 2.0:
			var car_yaw := v.global_rotation.y
			var desired := car_yaw + PI
			if v.forward_speed() < -2.0:
				desired = car_yaw
			yaw = lerp_angle(yaw, desired, delta * 2.2)
			pitch = lerpf(pitch, -0.22, delta * 1.5)
		cam.fov = lerpf(cam.fov, 70.0 + clampf(v.speed() / 40.0, 0.0, 1.0) * 12.0, delta * 2.0)
	else:
		cam.fov = lerpf(cam.fov, 70.0, delta * 3.0)
	arm.spring_length = lerpf(arm.spring_length, length, delta * 4.0)
	var goal := target.global_position + Vector3(0, height, 0)
	global_position = global_position.lerp(goal, clampf(delta * 14.0, 0.0, 1.0))
	rotation = Vector3(0, yaw, 0)
	arm.rotation = Vector3(pitch, 0, 0)
	if _shake > 0.0:
		_shake = maxf(0.0, _shake - delta * 2.5)
		cam.h_offset = randf_range(-1, 1) * _shake * 0.15
		cam.v_offset = randf_range(-1, 1) * _shake * 0.15
	else:
		cam.h_offset = 0.0
		cam.v_offset = 0.0
	if player:
		player.camera_yaw = yaw


func snap_to_target() -> void:
	if target:
		global_position = target.global_position + Vector3(0, FOOT_HEIGHT, 0)


## Vue cinématique : place la caméra à une position et vise un point.
func set_cinematic(pos: Vector3, look_at_pos: Vector3) -> void:
	cinematic = true
	global_position = pos
	rotation = Vector3.ZERO
	arm.rotation = Vector3.ZERO
	arm.spring_length = 0.0
	cam.position = Vector3.ZERO
	cam.h_offset = 0.0
	cam.v_offset = 0.0
	look_at_safe(look_at_pos)


func look_at_safe(p: Vector3) -> void:
	var dir := p - global_position
	if dir.length() < 0.01:
		return
	var y := atan2(-dir.x, -dir.z)
	var xz := Vector2(dir.x, dir.z).length()
	var x := atan2(dir.y, xz)
	rotation = Vector3(0, y, 0)
	arm.rotation = Vector3(x, 0, 0)


func end_cinematic() -> void:
	cinematic = false
	snap_to_target()
