class_name Mats
extends RefCounted
## Matériaux partagés (peu nombreux = peu de changements d'état GPU).

static var _cache := {}

const BUILDING_SHADER := """
shader_type spatial;
render_mode cull_back;

uniform float night = 0.0;
uniform float wet = 0.0;

float hash21(vec2 p) {
	p = fract(p * vec2(123.34, 456.21));
	p += dot(p, p + 45.32);
	return fract(p.x * p.y);
}

void fragment() {
	vec3 base = COLOR.rgb;
	float seed = COLOR.a;
	float facade = UV2.x;
	float curtain = UV2.y;
	vec2 cs = mix(vec2(3.0, 3.5), vec2(1.6, 3.5), curtain);
	vec2 cell = UV / cs;
	vec2 id = floor(cell);
	vec2 f = fract(cell);
	float mx = mix(0.2, 0.05, curtain);
	float my = mix(0.26, 0.1, curtain);
	float upper = step(4.4, UV.y);
	float win = step(mx, f.x) * step(f.x, 1.0 - mx) * step(my, f.y) * step(f.y, 1.0 - my * 0.5) * upper * facade;
	float sx = UV.x / 5.0;
	float sf = fract(sx);
	float shop = step(0.08, sf) * step(sf, 0.92) * step(0.35, UV.y) * step(UV.y, 3.3) * (1.0 - upper) * facade;
	float band = step(3.5, UV.y) * step(UV.y, 4.3) * (1.0 - upper) * facade;
	float rnd = hash21(id + vec2(seed * 97.0, seed * 31.0));
	float shop_rnd = hash21(vec2(floor(sx), seed * 53.0));
	vec3 glass = mix(vec3(0.16, 0.21, 0.29), vec3(0.42, 0.52, 0.63), rnd * 0.6 + curtain * 0.3);
	vec3 band_col = vec3(0.2 + shop_rnd * 0.6, 0.15 + fract(shop_rnd * 7.0) * 0.5, 0.12 + fract(shop_rnd * 13.0) * 0.45);
	float grain = hash21(floor(UV * 2.0) + vec2(seed)) * 0.07;
	vec3 col = base * (1.0 - grain);
	col = mix(col, glass, win);
	col = mix(col, vec3(0.1, 0.11, 0.13), shop);
	col = mix(col, band_col, band);
	ALBEDO = col;
	ROUGHNESS = clamp(mix(0.88, 0.18, win + shop) - wet * 0.25, 0.05, 1.0);
	METALLIC = win * 0.2;
	float lit = step(0.52 - night * 0.12, rnd) * win * night;
	vec3 warm = mix(vec3(1.0, 0.8, 0.52), vec3(0.72, 0.84, 1.0), step(0.86, rnd));
	EMISSION = warm * lit * 1.4 + vec3(1.0, 0.88, 0.68) * shop * (0.18 + night * 1.2) + band_col * band * night * 0.9;
}
"""

const SCREEN_SHADER := """
shader_type spatial;
render_mode unshaded, cull_back;

void fragment() {
	float seed = COLOR.a * 10.0;
	vec2 uv = UV;
	float t = TIME * 0.6 + seed;
	float stripes = 0.5 + 0.5 * sin(uv.x * 1.3 + t * 2.0 + sin(uv.y * 0.8 + t));
	float blocks = step(0.5, fract(sin(floor(uv.y * 0.5 + t * 0.5) * 12.7 + seed) * 43.1));
	vec3 a = COLOR.rgb;
	vec3 b = vec3(1.0) - COLOR.rgb * 0.6;
	vec3 col = mix(a, b, stripes * 0.6 + blocks * 0.25);
	float border = step(0.3, uv.x) * step(0.3, uv.y);
	ALBEDO = col * (0.65 + 0.35 * border);
}
"""


static func _has(key: String) -> bool:
	return _cache.has(key)


static func _put(key: String, m: Material) -> Material:
	_cache[key] = m
	return m


static func _shader_mat(code: String) -> ShaderMaterial:
	var sh := Shader.new()
	sh.code = code
	var m := ShaderMaterial.new()
	m.shader = sh
	return m


static func building() -> ShaderMaterial:
	if not _has("building"):
		_put("building", _shader_mat(BUILDING_SHADER))
	return _cache["building"]


static func screens() -> ShaderMaterial:
	if not _has("screens"):
		_put("screens", _shader_mat(SCREEN_SHADER))
	return _cache["screens"]


## Matériau à couleurs de sommets (trottoirs, mobilier, véhicules simples).
static func vcolor(roughness := 0.85, metallic := 0.0) -> StandardMaterial3D:
	var key := "vcolor_%.2f_%.2f" % [roughness, metallic]
	if not _has(key):
		var m := StandardMaterial3D.new()
		m.vertex_color_use_as_albedo = true
		m.roughness = roughness
		m.metallic = metallic
		_put(key, m)
	return _cache[key]


static func ground() -> StandardMaterial3D:
	if not _has("ground"):
		var m := StandardMaterial3D.new()
		m.vertex_color_use_as_albedo = true
		m.roughness = 0.92
		_put("ground", m)
	return _cache["ground"]


static func markings() -> StandardMaterial3D:
	if not _has("markings"):
		var m := StandardMaterial3D.new()
		m.vertex_color_use_as_albedo = true
		m.roughness = 0.7
		_put("markings", m)
	return _cache["markings"]


static func lamp() -> StandardMaterial3D:
	if not _has("lamp"):
		var m := StandardMaterial3D.new()
		m.albedo_color = Color(0.9, 0.88, 0.8)
		m.emission_enabled = true
		m.emission = Color(1.0, 0.82, 0.55)
		m.emission_energy_multiplier = 0.0
		_put("lamp", m)
	return _cache["lamp"]


static func glass() -> StandardMaterial3D:
	if not _has("glass"):
		var m := StandardMaterial3D.new()
		m.albedo_color = Color(0.12, 0.16, 0.2)
		m.roughness = 0.08
		m.metallic = 0.6
		_put("glass", m)
	return _cache["glass"]


static func solid(c: Color, roughness := 0.8, metallic := 0.0) -> StandardMaterial3D:
	var key := "solid_%s_%.2f_%.2f" % [c.to_html(), roughness, metallic]
	if not _has(key):
		var m := StandardMaterial3D.new()
		m.albedo_color = c
		m.roughness = roughness
		m.metallic = metallic
		_put(key, m)
	return _cache[key]


static func emissive(c: Color, energy := 2.0) -> StandardMaterial3D:
	var key := "emi_%s_%.2f" % [c.to_html(), energy]
	if not _has(key):
		var m := StandardMaterial3D.new()
		m.albedo_color = c
		m.emission_enabled = true
		m.emission = c
		m.emission_energy_multiplier = energy
		_put(key, m)
	return _cache[key]


static func marker(c: Color) -> StandardMaterial3D:
	var key := "marker_%s" % c.to_html()
	if not _has(key):
		var m := StandardMaterial3D.new()
		m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
		m.albedo_color = c
		m.cull_mode = BaseMaterial3D.CULL_DISABLED
		_put(key, m)
	return _cache[key]


## Feux tricolores : 6 matériaux partagés (axe avenue/rue × rouge/orange/vert).
static func traffic_lamp(axis: String, color: String) -> StandardMaterial3D:
	var key := "tl_%s_%s" % [axis, color]
	if not _has(key):
		var m := StandardMaterial3D.new()
		var c := Color(0.95, 0.12, 0.08)
		if color == "y":
			c = Color(1.0, 0.68, 0.05)
		elif color == "g":
			c = Color(0.1, 0.95, 0.4)
		m.albedo_color = c * 0.2
		m.emission_enabled = true
		m.emission = c
		m.emission_energy_multiplier = 0.0
		m.set_meta("on_color", c)
		_put(key, m)
	return _cache[key]


static func set_traffic_lamp(axis: String, color: String, on: bool) -> void:
	var m := traffic_lamp(axis, color)
	var c: Color = m.get_meta("on_color")
	m.albedo_color = c if on else c * 0.18
	m.emission_energy_multiplier = 2.5 if on else 0.0
