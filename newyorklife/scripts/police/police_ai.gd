class_name PoliceAI
extends Node
## Pilote une voiture de police (Vehicle) : suit la grille de rues vers le joueur,
## puis le poursuit directement à courte distance ; se dégage s'il est bloqué.

const L = preload("res://scripts/world/city_layout.gd")

var car: Vehicle
var player: Player
var aggressiveness := 1.0
var _path: Array[Vector2i] = []
var _repath := 0.0
var _stuck := 0.0
var _reverse := 0.0
var roadblock := false


func _physics_process(delta: float) -> void:
	if car == null or not is_instance_valid(car) or player == null:
		return
	if roadblock:
		car.throttle = 0.0
		car.handbrake = true
		return
	var target := player.focus_position()
	var pos := car.global_position
	var dist := pos.distance_to(target)
	var aim := target
	if dist > 28.0:
		_repath -= delta
		if _repath <= 0.0 or _path.is_empty():
			_repath = 1.5
			_path = L.path_nodes(L.nearest_node(pos), L.nearest_node(target))
		while _path.size() > 1 and L.node_pos(_path[0]).distance_to(pos) < 12.0:
			_path.pop_front()
		if not _path.is_empty():
			aim = L.node_pos(_path[0])
	var local := car.global_transform.affine_inverse() * aim
	var angle := atan2(local.x, local.z)
	var fs := car.forward_speed()
	if _reverse > 0.0:
		_reverse -= delta
		car.throttle = -1.0
		car.steer_input = -signf(angle)
		car.handbrake = false
		return
	car.steer_input = clampf(angle * 1.8, -1.0, 1.0)
	var want_speed := 26.0 * aggressiveness
	if absf(angle) > 0.9:
		want_speed = 9.0
	if dist < 12.0:
		var tv := player.vehicle.speed() if player.vehicle else 0.0
		want_speed = maxf(tv + 2.0, 4.0)
	if dist < 5.0 and not player.vehicle:
		want_speed = 0.0
	car.throttle = 1.0 if fs < want_speed else (-0.6 if fs > want_speed + 6.0 else 0.0)
	car.handbrake = absf(angle) > 1.2 and fs > 12.0
	# Détection de blocage
	if car.throttle > 0.5 and absf(fs) < 1.0:
		_stuck += delta
		if _stuck > 1.6:
			_stuck = 0.0
			_reverse = 1.4
	else:
		_stuck = maxf(0.0, _stuck - delta)
