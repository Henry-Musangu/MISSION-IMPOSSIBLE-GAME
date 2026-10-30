class_name DayNight
extends Node3D
## Cycle jour/nuit et météo : soleil, ciel, brouillard, éclairage public, pluie, orages.

var env: Environment
var sun: DirectionalLight3D
var sky_mat: ProceduralSkyMaterial
var rain: CPUParticles3D
var follow: Node3D            ## caméra suivie par la pluie
var weather := "weather_clear"
var _weather_timer := 0.0
var _target_cloud := 0.0
var _cloud := 0.0
var _rain_amount := 0.0
var _fog_extra := 0.0
var _flash := 0.0
var _thunder_timer := 8.0
var _last_phase := ""
var night_factor := 0.0
var wet := 0.0

const WEATHERS := ["weather_clear", "weather_clear", "weather_cloudy", "weather_rain", "weather_storm", "weather_fog"]


func _ready() -> void:
	sky_mat = ProceduralSkyMaterial.new()
	var sky := Sky.new()
	sky.sky_material = sky_mat
	env = Environment.new()
	env.background_mode = Environment.BG_SKY
	env.sky = sky
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
	env.fog_enabled = true
	env.fog_light_color = Color(0.6, 0.66, 0.74)
	env.fog_density = 0.004
	env.fog_sky_affect = 0.15
	env.glow_enabled = false
	var we := WorldEnvironment.new()
	we.environment = env
	add_child(we)
	sun = DirectionalLight3D.new()
	sun.name = "Sun"
	sun.shadow_enabled = true
	sun.directional_shadow_mode = DirectionalLight3D.SHADOW_ORTHOGONAL
	sun.directional_shadow_max_distance = 90.0
	sun.shadow_bias = 0.06
	add_child(sun)
	rain = CPUParticles3D.new()
	rain.name = "Rain"
	rain.emitting = false
	rain.amount = 400
	rain.lifetime = 0.9
	rain.local_coords = false
	rain.emission_shape = CPUParticles3D.EMISSION_SHAPE_BOX
	rain.emission_box_extents = Vector3(22, 1, 22)
	rain.direction = Vector3(0.05, -1, 0)
	rain.spread = 3.0
	rain.gravity = Vector3(0, -25, 0)
	rain.initial_velocity_min = 18.0
	rain.initial_velocity_max = 24.0
	var drop := BoxMesh.new()
	drop.size = Vector3(0.02, 0.55, 0.02)
	var dm := StandardMaterial3D.new()
	dm.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	dm.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	dm.albedo_color = Color(0.75, 0.82, 0.95, 0.45)
	drop.material = dm
	rain.mesh = drop
	add_child(rain)
	Events.quality_changed.connect(_on_quality)
	_on_quality()
	_weather_timer = randf_range(240.0, 480.0)


func _on_quality() -> void:
	sun.shadow_enabled = Settings.get_value("shadows")
	env.fog_density = Settings.get_value("fog_density")
	rain.amount = Settings.get_value("rain_particles")


func set_weather(kind: String) -> void:
	weather = kind
	match kind:
		"weather_clear":
			_target_cloud = 0.0
		"weather_cloudy":
			_target_cloud = 0.6
		"weather_rain":
			_target_cloud = 0.85
		"weather_storm":
			_target_cloud = 1.0
		"weather_fog":
			_target_cloud = 0.5
	Events.weather_changed.emit(kind)


func _process(delta: float) -> void:
	var minutes := GameState.time_minutes
	var day_t := minutes / GameState.DAY_MINUTES
	# Soleil : lever à 6 h, zénith à 13 h, coucher à 20 h
	var elev := sin((day_t - 0.25) * TAU) * 70.0 + 8.0
	var azimuth := day_t * 360.0 + 40.0
	sun.rotation_degrees = Vector3(-elev, azimuth, 0)
	var daylight := clampf((elev + 4.0) / 24.0, 0.0, 1.0)
	night_factor = 1.0 - smoothstep(0.0, 0.6, daylight)
	var sunset := clampf(1.0 - absf(elev - 6.0) / 14.0, 0.0, 1.0)

	# Météo
	if GameState.playing:
		_weather_timer -= delta * GameState.time_scale
		if _weather_timer <= 0.0:
			_weather_timer = randf_range(240.0, 600.0)
			set_weather(WEATHERS[randi() % WEATHERS.size()])
	_cloud = move_toward(_cloud, _target_cloud, delta * 0.05)
	var raining := weather == "weather_rain" or weather == "weather_storm"
	_rain_amount = move_toward(_rain_amount, 1.0 if raining else 0.0, delta * 0.08)
	_fog_extra = move_toward(_fog_extra, 1.0 if weather == "weather_fog" else 0.0, delta * 0.05)
	wet = move_toward(wet, 1.0 if raining else 0.0, delta * (0.05 if raining else 0.01))
	rain.emitting = _rain_amount > 0.3
	if follow and is_instance_valid(follow):
		rain.global_position = follow.global_position + Vector3(0, 14, 0)

	# Éclairs
	if weather == "weather_storm":
		_thunder_timer -= delta
		if _thunder_timer <= 0.0:
			_thunder_timer = randf_range(6.0, 16.0)
			_flash = 1.0
			Sfx.play("thunder", -4.0)
	_flash = maxf(0.0, _flash - delta * 3.0)

	# Ciel
	var day_top := Color(0.22, 0.45, 0.82)
	var day_hor := Color(0.68, 0.78, 0.9)
	var night_top := Color(0.01, 0.015, 0.04)
	var night_hor := Color(0.06, 0.07, 0.12)
	var dusk := Color(0.95, 0.5, 0.25)
	var grey := Color(0.45, 0.48, 0.52)
	var top := night_top.lerp(day_top, daylight)
	var hor := night_hor.lerp(day_hor, daylight).lerp(dusk, sunset * 0.6)
	top = top.lerp(grey * maxf(daylight, 0.1), _cloud * 0.8)
	hor = hor.lerp(grey * maxf(daylight, 0.12), _cloud * 0.7)
	sky_mat.sky_top_color = top
	sky_mat.sky_horizon_color = hor
	sky_mat.ground_horizon_color = hor
	sky_mat.ground_bottom_color = hor.darkened(0.5)
	sky_mat.sun_angle_max = 30.0
	var sun_col := Color(1.0, 0.95, 0.88).lerp(Color(1.0, 0.6, 0.35), sunset)
	sun.light_color = sun_col
	sun.light_energy = (daylight * 1.1) * (1.0 - _cloud * 0.5) + _flash * 2.0
	sun.visible = daylight > 0.01 or _flash > 0.0
	var amb_day := Color(0.78, 0.78, 0.8).lerp(Color(0.95, 0.75, 0.6), sunset * 0.5)
	env.ambient_light_color = Color(0.3, 0.34, 0.48).lerp(amb_day, daylight)
	env.ambient_light_energy = lerpf(0.55, 0.5, daylight) * (1.0 + _cloud * 0.25) + _flash
	env.fog_light_color = hor
	env.fog_density = Settings.get_value("fog_density") * (1.0 + _cloud * 0.6 + _rain_amount * 0.8 + _fog_extra * 6.0)
	env.fog_light_color = hor.lerp(Color(0.05, 0.06, 0.1), night_factor * 0.5)

	# Éclairage urbain et fenêtres
	var lamp_on := smoothstep(0.35, 0.75, night_factor + _cloud * 0.25)
	Mats.lamp().emission_energy_multiplier = lamp_on * 3.0
	Mats.building().set_shader_parameter("night", clampf(night_factor + _cloud * 0.2, 0.0, 1.0))
	Mats.building().set_shader_parameter("wet", wet)
	Mats.ground().roughness = lerpf(0.92, 0.35, wet)
	Mats.ground().metallic = wet * 0.25

	var phase := GameState.time_phase()
	if phase != _last_phase:
		_last_phase = phase
		Events.time_phase_changed.emit(phase)


func is_night() -> bool:
	return night_factor > 0.5


## Adhérence des pneus selon la chaussée mouillée (1 = sec).
func grip_factor() -> float:
	return lerpf(1.0, 0.78, wet)
