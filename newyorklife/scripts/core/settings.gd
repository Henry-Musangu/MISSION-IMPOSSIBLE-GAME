extends Node
## Paramètres du joueur (fichier séparé de la sauvegarde) et profils graphiques LOW / MEDIUM / HIGH.

const PATH := "user://settings.json"

enum Quality { LOW, MEDIUM, HIGH }

var quality: int = Quality.MEDIUM
var language := "fr"
var vol_master := 0.9
var vol_music := 0.6
var vol_sfx := 0.9
var vol_ambience := 0.6
var touch_scale := 1.0
var touch_opacity := 0.55
var camera_sensitivity := 1.0
var show_fps := false

## Valeurs dérivées du profil graphique, lues par les systèmes du monde.
const PROFILES := {
	Quality.LOW: {
		"render_scale": 0.7, "shadows": false, "shadow_size": 1024, "msaa": 0,
		"view_distance": 230.0, "fog_density": 0.006, "traffic": 7, "pedestrians": 10,
		"stream_radius": 190.0, "rain_particles": 250, "detail_range": 70.0,
	},
	Quality.MEDIUM: {
		"render_scale": 0.85, "shadows": true, "shadow_size": 1024, "msaa": 0,
		"view_distance": 320.0, "fog_density": 0.0042, "traffic": 12, "pedestrians": 18,
		"stream_radius": 260.0, "rain_particles": 500, "detail_range": 110.0,
	},
	Quality.HIGH: {
		"render_scale": 1.0, "shadows": true, "shadow_size": 2048, "msaa": 2,
		"view_distance": 450.0, "fog_density": 0.003, "traffic": 18, "pedestrians": 28,
		"stream_radius": 380.0, "rain_particles": 900, "detail_range": 160.0,
	},
}


func _ready() -> void:
	load_settings()
	Loc.lang = language
	apply_audio()
	apply_quality()


func profile() -> Dictionary:
	return PROFILES[quality]


func get_value(key: String):
	return PROFILES[quality][key]


func apply_quality() -> void:
	var vp := get_viewport()
	if vp:
		vp.scaling_3d_scale = get_value("render_scale")
		var msaa: int = get_value("msaa")
		vp.msaa_3d = Viewport.MSAA_2X if msaa == 2 else Viewport.MSAA_DISABLED
	Events.quality_changed.emit()


func apply_audio() -> void:
	_set_bus("Master", vol_master)
	_set_bus("Music", vol_music)
	_set_bus("SFX", vol_sfx)
	_set_bus("Ambience", vol_ambience)


func _set_bus(bus_name: String, v: float) -> void:
	var idx := AudioServer.get_bus_index(bus_name)
	if idx < 0:
		return
	AudioServer.set_bus_volume_db(idx, linear_to_db(maxf(v, 0.0001)))
	AudioServer.set_bus_mute(idx, v <= 0.001)


func to_dict() -> Dictionary:
	return {
		"version": 1, "quality": quality, "language": language,
		"vol_master": vol_master, "vol_music": vol_music, "vol_sfx": vol_sfx,
		"vol_ambience": vol_ambience, "touch_scale": touch_scale,
		"touch_opacity": touch_opacity, "camera_sensitivity": camera_sensitivity,
		"show_fps": show_fps,
	}


func from_dict(d: Dictionary) -> void:
	quality = clampi(int(d.get("quality", quality)), 0, 2)
	language = str(d.get("language", language))
	vol_master = clampf(float(d.get("vol_master", vol_master)), 0.0, 1.0)
	vol_music = clampf(float(d.get("vol_music", vol_music)), 0.0, 1.0)
	vol_sfx = clampf(float(d.get("vol_sfx", vol_sfx)), 0.0, 1.0)
	vol_ambience = clampf(float(d.get("vol_ambience", vol_ambience)), 0.0, 1.0)
	touch_scale = clampf(float(d.get("touch_scale", touch_scale)), 0.7, 1.5)
	touch_opacity = clampf(float(d.get("touch_opacity", touch_opacity)), 0.2, 1.0)
	camera_sensitivity = clampf(float(d.get("camera_sensitivity", camera_sensitivity)), 0.3, 2.5)
	show_fps = bool(d.get("show_fps", show_fps))


func save_settings() -> void:
	language = Loc.lang
	var f := FileAccess.open(PATH, FileAccess.WRITE)
	if f:
		f.store_string(JSON.stringify(to_dict(), "\t"))
		f.close()
	Events.settings_changed.emit()


func load_settings() -> void:
	if not FileAccess.file_exists(PATH):
		return
	var f := FileAccess.open(PATH, FileAccess.READ)
	if f == null:
		return
	var parsed = JSON.parse_string(f.get_as_text())
	f.close()
	if parsed is Dictionary:
		from_dict(parsed)
