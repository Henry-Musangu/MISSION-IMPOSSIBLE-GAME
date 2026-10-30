class_name TrafficManager
extends Node3D
## Gère un groupe de véhicules IA autour du joueur (pool réutilisé).
## Les véhicules trop éloignés sont replacés sur une voie proche, hors de la vue.

const L := CityLayout

var player: Player
var cars: Array[TrafficCar] = []
var _rng := RandomNumberGenerator.new()
var _check := 0.0


func _ready() -> void:
	_rng.randomize()
	Events.quality_changed.connect(_resize_pool)


func populate() -> void:
	_resize_pool()
	for c in cars:
		_respawn(c, true)


func _resize_pool() -> void:
	var want: int = Settings.get_value("traffic")
	while cars.size() < want:
		var t := VehicleSpecs.random_traffic_type(_rng)
		if cars.size() % 6 == 5:
			t = "police"
		var c := TrafficCar.new()
		c.setup(t, VehicleSpecs.random_color(t, _rng), _rng.randi())
		add_child(c)
		cars.append(c)
		if player:
			_respawn(c, true)
	while cars.size() > want:
		var c: TrafficCar = cars.pop_back()
		c.queue_free()
	for c in cars:
		c.set_visibility(Settings.get_value("view_distance") * 0.7)


func _respawn(c: TrafficCar, anywhere: bool) -> void:
	var center := player.focus_position() if player else L.node_pos(Vector2i(2, 3))
	var cam := get_viewport().get_camera_3d()
	for attempt in 30:
		var a := Vector2i(_rng.randi_range(0, L.AVE_COUNT - 1), _rng.randi_range(0, L.ST_COUNT - 1))
		var nb := L.neighbors(a)
		var b: Vector2i = nb[_rng.randi() % nb.size()]
		var t := _rng.randf_range(0.05, 0.7)
		var pts := L.lane_points(a, b)
		var p := pts[0].lerp(pts[1], t)
		var d := p.distance_to(center)
		if d < (25.0 if anywhere else 70.0) or d > 170.0:
			continue
		if not anywhere and cam and not cam.is_position_behind(p) and d < 120.0:
			continue
		var free := true
		for o in cars:
			if o != c and o.global_position.distance_to(p) < 12.0:
				free = false
				break
		if not free:
			continue
		c.place_on(a, b, t)
		c.active = true
		c.visible = true
		return


func _physics_process(delta: float) -> void:
	if player == null:
		return
	_check -= delta
	if _check > 0.0:
		return
	_check = 0.5
	var center := player.focus_position()
	for c in cars:
		if not is_instance_valid(c):
			continue
		if c.global_position.distance_to(center) > 190.0:
			_respawn(c, false)


## Le joueur prend un véhicule de la circulation : on le retire du pool.
func take_car(c: TrafficCar) -> void:
	cars.erase(c)
	c.queue_free()
	call_deferred("_resize_pool")


func nearest_car(p: Vector3, max_d: float) -> TrafficCar:
	var best: TrafficCar = null
	var bd := max_d
	for c in cars:
		var d := c.global_position.distance_to(p) - float(c.spec["length"]) * 0.3
		if d < bd:
			bd = d
			best = c
	return best


func police_near(p: Vector3, radius: float) -> bool:
	for c in cars:
		if c.is_police and c.global_position.distance_to(p) < radius:
			return true
	return false
