class_name Vehicle
extends VehicleBody3D
## Véhicule conduisible (joueur ou IA de police) basé sur VehicleBody3D.
## Les commandes passent par throttle / steer_input / handbrake, remplies par
## le joueur (InputState) ou par un contrôleur d'IA.

signal wrecked(vehicle: Vehicle)

var type := "sedan"
var spec: Dictionary
var color := Color.WHITE
var damage := 0.0               ## 0..100
var fuel := 1.0
var owned := false              ## appartient au joueur
var player_driving := false
var ai_driving := false
var mission_vehicle := false

# commandes
var throttle := 0.0             ## −1..1 (négatif = frein / marche arrière)
var steer_input := 0.0          ## −1 (droite) .. 1 (gauche)
var handbrake := false
var horn := false

var wheels: Array[VehicleWheel3D] = []
var _model: Dictionary
var _prev_velocity := Vector3.ZERO
var _steer := 0.0
var _flip_timer := 0.0
var _engine_player: AudioStreamPlayer3D
var _horn_player: AudioStreamPlayer3D
var _headlight: SpotLight3D
var _smoke: CPUParticles3D
var _siren_player: AudioStreamPlayer3D
var siren_on := false
var _siren_t := 0.0
var grip_factor := 1.0


func setup(p_type: String, p_color: Color) -> void:
	type = p_type
	spec = VehicleSpecs.get_spec(type)
	color = p_color
	mass = spec["mass"]
	collision_layer = 4          # couche 3 : véhicules
	collision_mask = 1 | 2 | 4
	contact_monitor = true
	max_contacts_reported = 4
	center_of_mass_mode = RigidBody3D.CENTER_OF_MASS_MODE_CUSTOM
	center_of_mass = Vector3(0, float(spec["wheel_r"]) + 0.15, 0.1)
	can_sleep = true
	linear_damp = 0.05
	angular_damp = 0.6
	add_to_group("vehicles")
	_model = VehicleModel.build(type, color, false)
	add_child(_model["node"])
	# Collision de la caisse
	var L: float = spec["length"]
	var W: float = spec["width"]
	var H: float = spec["height"]
	var r: float = spec["wheel_r"]
	var cab: float = spec["cabin"]
	var cs := CollisionShape3D.new()
	var box := BoxShape3D.new()
	var total_h := H + cab
	if spec.get("box", false):
		total_h = H * (1.15 if spec.get("truck", false) else 1.0) + 0.1
	box.size = Vector3(W, total_h - 0.1, L)
	cs.shape = box
	cs.position = Vector3(0, r * 0.55 + 0.1 + (total_h - 0.1) * 0.5, 0)
	add_child(cs)
	# Roues
	var positions := VehicleModel.wheel_positions(type)
	for i in positions.size():
		var w := VehicleWheel3D.new()
		w.name = "Wheel%d" % i
		w.position = positions[i] + Vector3(0, 0.1, 0)
		w.wheel_radius = r
		w.wheel_rest_length = 0.16
		w.suspension_travel = 0.2
		w.suspension_stiffness = 40.0
		w.suspension_max_force = mass * 16.0
		w.damping_compression = 2.2
		w.damping_relaxation = 3.2
		w.wheel_friction_slip = spec["grip"]
		w.wheel_roll_influence = 0.06
		var front := i < 2
		w.use_as_steering = front
		w.use_as_traction = (not front) or spec["drive"] == "awd"
		w.add_child(VehicleModel.wheel_mesh(r, W))
		add_child(w)
		wheels.append(w)
	# Sons
	_engine_player = AudioStreamPlayer3D.new()
	_engine_player.stream = Sfx.get_stream("engine")
	_engine_player.bus = "SFX"
	_engine_player.unit_size = 6.0
	_engine_player.max_distance = 70.0
	_engine_player.volume_db = -8.0
	add_child(_engine_player)
	_horn_player = AudioStreamPlayer3D.new()
	_horn_player.stream = Sfx.get_stream("horn")
	_horn_player.bus = "SFX"
	_horn_player.unit_size = 10.0
	_horn_player.max_distance = 120.0
	add_child(_horn_player)
	if spec.get("police", false):
		_siren_player = AudioStreamPlayer3D.new()
		_siren_player.stream = Sfx.get_stream("siren")
		_siren_player.bus = "SFX"
		_siren_player.unit_size = 14.0
		_siren_player.max_distance = 220.0
		_siren_player.volume_db = -4.0
		add_child(_siren_player)
	# Phare (un seul projecteur pour économiser le GPU), activé la nuit pour le joueur
	_headlight = SpotLight3D.new()
	_headlight.position = Vector3(0, r + float(spec["height"]) * 0.6, L * 0.5 + 0.2)
	_headlight.rotation = Vector3(deg_to_rad(-8.0), PI, 0)
	_headlight.spot_range = 32.0
	_headlight.spot_angle = 38.0
	_headlight.light_energy = 2.5
	_headlight.light_color = Color(1.0, 0.95, 0.85)
	_headlight.shadow_enabled = false
	_headlight.visible = false
	add_child(_headlight)
	_smoke = CPUParticles3D.new()
	_smoke.emitting = false
	_smoke.amount = 24
	_smoke.lifetime = 1.6
	_smoke.position = Vector3(0, r + H, L * 0.38)
	_smoke.direction = Vector3(0, 1, 0)
	_smoke.spread = 25.0
	_smoke.gravity = Vector3(0, 1.5, 0)
	_smoke.initial_velocity_min = 0.8
	_smoke.initial_velocity_max = 1.6
	_smoke.scale_amount_min = 0.6
	_smoke.scale_amount_max = 1.4
	var sm := SphereMesh.new()
	sm.radius = 0.35
	sm.height = 0.7
	sm.radial_segments = 6
	sm.rings = 3
	var smat := StandardMaterial3D.new()
	smat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	smat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	smat.albedo_color = Color(0.2, 0.2, 0.2, 0.5)
	sm.material = smat
	_smoke.mesh = sm
	add_child(_smoke)
	_apply_damage_visual()


func display_name() -> String:
	return Loc.t(spec["name"])


func forward_speed() -> float:
	return linear_velocity.dot(global_transform.basis.z)


func speed() -> float:
	return linear_velocity.length()


func is_wrecked() -> bool:
	return damage >= 100.0


func is_upside_down() -> bool:
	return global_transform.basis.y.y < 0.35


func set_player_driving(on: bool) -> void:
	player_driving = on
	if on:
		sleeping = false
		_engine_player.play()
	else:
		throttle = 0.0
		steer_input = 0.0
		handbrake = true
		horn = false
		if not ai_driving:
			_engine_player.stop()


func _physics_process(delta: float) -> void:
	if player_driving:
		_read_player_input()
	var fs := forward_speed()
	var max_speed: float = spec["max_speed"]
	var engine: float = spec["engine"] * (1.0 - damage / 140.0)
	if is_wrecked() or fuel <= 0.0:
		engine = 0.0
	var engine_force_v := 0.0
	var brake_v := 0.0
	var decel_brake := mass * 0.034   # ≈ 8 m/s² répartis sur 4 roues
	if throttle > 0.05:
		if fs < -1.0:
			brake_v = decel_brake * throttle
		elif fs < max_speed:
			engine_force_v = engine * throttle * clampf(1.15 - fs / max_speed, 0.25, 1.0)
	elif throttle < -0.05:
		if fs > 1.0:
			brake_v = decel_brake * -throttle
		elif fs > -9.0:
			engine_force_v = engine * 0.6 * throttle
	else:
		brake_v = mass * 0.0025       # frottements au relâché
	# Direction : angle réduit à haute vitesse, retour progressif
	var max_steer: float = spec["steer"] * lerpf(1.0, 0.3, clampf(absf(fs) / 30.0, 0.0, 1.0))
	_steer = move_toward(_steer, steer_input * max_steer, delta * 2.6)
	var traction_count := 0
	for w in wheels:
		if w.use_as_traction:
			traction_count += 1
	var slip: float = spec["grip"] * grip_factor
	for w in wheels:
		w.steering = _steer if w.use_as_steering else 0.0
		var rear := not w.use_as_steering
		w.engine_force = engine_force_v * (2.0 / traction_count) if w.use_as_traction else 0.0
		w.brake = brake_v
		w.wheel_friction_slip = slip
		if handbrake:
			if rear:
				w.brake = mass * 0.05
				w.engine_force = 0.0
				w.wheel_friction_slip = slip * 0.45
	# Traînée aérodynamique
	var v := linear_velocity
	apply_central_force(-v * v.length() * mass * 0.0009)
	# Dégâts par choc
	var dv := (_prev_velocity - linear_velocity).length()
	if dv > 7.0 and _prev_velocity.length() > 5.0:
		add_damage((dv - 7.0) * 3.2)
	_prev_velocity = linear_velocity
	# Remise sur roues automatique après quelques secondes
	if is_upside_down() and speed() < 1.5:
		_flip_timer += delta
		if _flip_timer > 4.0:
			flip_upright()
	else:
		_flip_timer = 0.0
	if player_driving:
		fuel = maxf(0.0, fuel - absf(throttle) * delta * 0.0011)
	if player_driving or ai_driving:
		GameState.stats["distance_driven"] = float(GameState.stats["distance_driven"]) + absf(fs) * delta


func _process(delta: float) -> void:
	# Sons moteur
	if _engine_player.playing:
		var ratio := clampf(absf(forward_speed()) / float(spec["max_speed"]), 0.0, 1.0)
		var gear_ratio := fmod(ratio * 4.0, 1.0) * 0.5 + ratio * 0.6
		_engine_player.pitch_scale = 0.55 + gear_ratio + absf(throttle) * 0.15
		_engine_player.volume_db = -14.0 + absf(throttle) * 5.0
	if horn and not _horn_player.playing:
		_horn_player.play()
	elif not horn and _horn_player.playing:
		_horn_player.stop()
	# Phares
	var night := false
	var dn := get_tree().get_first_node_in_group("day_night")
	if dn:
		night = dn.is_night()
		grip_factor = dn.grip_factor()
	var driven := player_driving or ai_driving
	_headlight.visible = night and player_driving
	(_model["head_mat"] as StandardMaterial3D).emission_energy_multiplier = 2.5 if (night and driven) else 0.1
	var braking := driven and throttle < -0.05 and forward_speed() > 0.5
	(_model["tail_mat"] as StandardMaterial3D).emission_energy_multiplier = 3.0 if braking else (1.2 if night and driven else 0.2)
	# Gyrophares
	if _model.has("siren_red"):
		if siren_on:
			_siren_t += delta * 6.0
			var on := fmod(_siren_t, 2.0) < 1.0
			(_model["siren_red"] as StandardMaterial3D).emission_energy_multiplier = 4.0 if on else 0.0
			(_model["siren_blue"] as StandardMaterial3D).emission_energy_multiplier = 0.0 if on else 4.0
			if _siren_player and not _siren_player.playing:
				_siren_player.play()
		else:
			(_model["siren_red"] as StandardMaterial3D).emission_energy_multiplier = 0.0
			(_model["siren_blue"] as StandardMaterial3D).emission_energy_multiplier = 0.0
			if _siren_player and _siren_player.playing:
				_siren_player.stop()


func _read_player_input() -> void:
	var t := InputState.throttle_axis()
	var joy_y := InputState.touch_move.y
	if absf(joy_y) > absf(t):
		t = joy_y
	throttle = clampf(t, -1.0, 1.0)
	steer_input = -InputState.move.x
	handbrake = InputState.is_held("handbrake")
	horn = InputState.is_held("horn")


func add_damage(amount: float) -> void:
	if amount <= 0.0:
		return
	var was_wrecked := is_wrecked()
	damage = clampf(damage + amount, 0.0, 100.0)
	_apply_damage_visual()
	if player_driving:
		GameState.health = maxf(0.0, GameState.health - amount * 0.25)
		Events.player_health_changed.emit(GameState.health)
		if GameState.health <= 0.0:
			Events.player_died.emit()
		if amount > 6.0:
			Sfx.play("crash", -6.0)
			Input.vibrate_handheld(80)
	if not was_wrecked and is_wrecked():
		wrecked.emit(self)
		if player_driving:
			Events.notify.emit(Loc.t("vehicle_wrecked"), "bad")


func repair() -> void:
	damage = 0.0
	_apply_damage_visual()


func _apply_damage_visual() -> void:
	if _model.is_empty():
		return
	var paint: StandardMaterial3D = _model["paint"]
	paint.albedo_color = color.lerp(Color(0.12, 0.11, 0.1), damage / 160.0)
	paint.roughness = lerpf(0.35, 0.9, damage / 100.0)
	if _smoke:
		_smoke.emitting = damage > 60.0
		var sm := _smoke.mesh as SphereMesh
		if sm and sm.material:
			(sm.material as StandardMaterial3D).albedo_color = Color(0.15, 0.15, 0.15, 0.55) if damage < 95.0 else Color(0.08, 0.06, 0.05, 0.75)


func repaint(c: Color) -> void:
	color = c
	_apply_damage_visual()


func flip_upright() -> void:
	var yaw := global_rotation.y
	global_transform = Transform3D(Basis(Vector3.UP, yaw), global_position + Vector3(0, 1.2, 0))
	linear_velocity = Vector3.ZERO
	angular_velocity = Vector3.ZERO
	_flip_timer = 0.0


func honk(duration: float) -> void:
	horn = true
	get_tree().create_timer(duration).timeout.connect(func(): horn = false)


func to_dict() -> Dictionary:
	return {"type": type, "color": [color.r, color.g, color.b], "damage": damage, "fuel": fuel}


static func color_from(arr) -> Color:
	if arr is Array and arr.size() >= 3:
		return Color(float(arr[0]), float(arr[1]), float(arr[2]))
	return Color(0.5, 0.5, 0.5)
