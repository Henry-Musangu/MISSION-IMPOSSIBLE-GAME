class_name VehicleModel
extends RefCounted
## Construit le modèle 3D d'un véhicule à partir de ses caractéristiques.
## Repère : avant = +Z, sol à y = 0, roues dessinées à part (pour pouvoir tourner).

const GLASS := Color(0.1, 0.13, 0.17)
const TRIM := Color(0.08, 0.08, 0.09)


## Renvoie {"node": Node3D, "paint": StandardMaterial3D, "lights": {...}}
static func build(type: String, color: Color, with_wheels: bool) -> Dictionary:
	var s := VehicleSpecs.get_spec(type)
	var root := Node3D.new()
	root.name = "Model"
	var L: float = s["length"]
	var W: float = s["width"]
	var H: float = s["height"]
	var r: float = s["wheel_r"]
	var clearance := r * 0.55
	var paint := StandardMaterial3D.new()
	paint.albedo_color = color
	paint.roughness = 0.35
	paint.metallic = 0.35
	var body := MeshUtil.begin()
	var trim := MeshUtil.begin()
	var glass := MeshUtil.begin()
	var white := Color.WHITE
	var y0 := clearance
	if s.get("box", false):
		var cab_len := 1.9 if not s.get("bus", false) else 0.0
		if s.get("bus", false):
			MeshUtil.add_box(body, Vector3(0, y0 + H * 0.5, 0), Vector3(W, H, L), white)
			# bandeau de vitres
			MeshUtil.add_box(glass, Vector3(0, y0 + H * 0.62, 0.1), Vector3(W + 0.02, H * 0.35, L * 0.9), white, Vector2.ZERO, false)
			MeshUtil.add_box(glass, Vector3(0, y0 + H * 0.55, L * 0.5 + 0.01), Vector3(W * 0.9, H * 0.5, 0.02), white, Vector2.ZERO, false)
		else:
			var box_len := L - cab_len - 0.15
			var box_h := H * (1.15 if s.get("truck", false) else 1.0)
			MeshUtil.add_box(body if not s.get("truck", false) else trim, Vector3(0, y0 + box_h * 0.5 + 0.1, -L * 0.5 + box_len * 0.5), Vector3(W, box_h, box_len),
				white if not s.get("truck", false) else Color(0.9, 0.9, 0.88))
			var cab_h := H * 0.75
			MeshUtil.add_box(body, Vector3(0, y0 + cab_h * 0.5, L * 0.5 - cab_len * 0.5), Vector3(W * 0.98, cab_h, cab_len), white)
			MeshUtil.add_box(glass, Vector3(0, y0 + cab_h * 0.72, L * 0.5 + 0.005), Vector3(W * 0.85, cab_h * 0.35, 0.02), white, Vector2.ZERO, false)
			MeshUtil.add_box(glass, Vector3(0, y0 + cab_h * 0.72, L * 0.5 - cab_len * 0.45), Vector3(W + 0.01, cab_h * 0.3, cab_len * 0.6), white, Vector2.ZERO, false)
	else:
		# Caisse basse + habitacle vitré
		MeshUtil.add_box(body, Vector3(0, y0 + H * 0.5, 0), Vector3(W, H, L), white)
		var cab_len: float = L * s["cabin_len"]
		var cab_h: float = s["cabin"]
		var cz: float = L * s["cabin_offset"]
		var cab_c := Vector3(0, y0 + H + cab_h * 0.5, cz)
		MeshUtil.add_box(glass, cab_c, Vector3(W * 0.86, cab_h, cab_len), white)
		MeshUtil.add_box(body, cab_c + Vector3(0, cab_h * 0.5 + 0.03, 0), Vector3(W * 0.82, 0.06, cab_len * 0.8), white)
		# montants
		for sx in [-1.0, 1.0]:
			MeshUtil.add_box(body, cab_c + Vector3(sx * W * 0.43, 0, 0), Vector3(0.06, cab_h, 0.12), white)
		if s.get("taxi_sign", false):
			MeshUtil.add_box(trim, cab_c + Vector3(0, cab_h * 0.5 + 0.2, 0), Vector3(0.7, 0.25, 0.3), Color(1.0, 0.95, 0.75))
	# Pare-chocs
	MeshUtil.add_box(trim, Vector3(0, y0 + 0.18, L * 0.5 + 0.04), Vector3(W * 0.98, 0.22, 0.1), TRIM)
	MeshUtil.add_box(trim, Vector3(0, y0 + 0.18, -L * 0.5 - 0.04), Vector3(W * 0.98, 0.22, 0.1), TRIM)
	if s.get("police", false):
		# portières noires + rampe lumineuse
		MeshUtil.add_box(trim, Vector3(0, y0 + H * 0.45, -0.1), Vector3(W + 0.01, H * 0.5, L * 0.38), Color(0.06, 0.08, 0.15))
	var mesh := MeshUtil.commit(body, paint)
	mesh = MeshUtil.commit(trim, Mats.vcolor(0.5, 0.2), mesh)
	mesh = MeshUtil.commit(glass, Mats.glass(), mesh)
	var mi := MeshInstance3D.new()
	mi.name = "Body"
	mi.mesh = mesh
	root.add_child(mi)
	# Feux avant / arrière (matériaux propres au véhicule pour pouvoir les allumer)
	var head_mat := StandardMaterial3D.new()
	head_mat.albedo_color = Color(0.95, 0.95, 0.9)
	head_mat.emission_enabled = true
	head_mat.emission = Color(1.0, 0.95, 0.8)
	head_mat.emission_energy_multiplier = 0.2
	var tail_mat := StandardMaterial3D.new()
	tail_mat.albedo_color = Color(0.6, 0.05, 0.05)
	tail_mat.emission_enabled = true
	tail_mat.emission = Color(1.0, 0.08, 0.05)
	tail_mat.emission_energy_multiplier = 0.3
	var hl := MeshUtil.begin()
	var tl := MeshUtil.begin()
	for sx in [-1.0, 1.0]:
		MeshUtil.add_box(hl, Vector3(sx * W * 0.36, y0 + H * 0.62, L * 0.5 + 0.01), Vector3(W * 0.2, 0.14, 0.04), Color.WHITE)
		MeshUtil.add_box(tl, Vector3(sx * W * 0.38, y0 + H * 0.62, -L * 0.5 - 0.01), Vector3(W * 0.18, 0.14, 0.04), Color.WHITE)
	var lm := MeshUtil.commit(hl, head_mat)
	lm = MeshUtil.commit(tl, tail_mat, lm)
	var lights := MeshInstance3D.new()
	lights.name = "Lights"
	lights.mesh = lm
	lights.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	root.add_child(lights)
	var result := {"node": root, "paint": paint, "head_mat": head_mat, "tail_mat": tail_mat}
	if s.get("police", false):
		var bar_y: float = y0 + H + float(s["cabin"]) + 0.12
		var red := Mats.emissive(Color(1.0, 0.1, 0.1), 0.0).duplicate() as StandardMaterial3D
		var blue := Mats.emissive(Color(0.1, 0.3, 1.0), 0.0).duplicate() as StandardMaterial3D
		var rm := BoxMesh.new()
		rm.size = Vector3(0.45, 0.14, 0.25)
		var a := MeshInstance3D.new()
		a.mesh = rm
		a.material_override = red
		a.position = Vector3(-0.28, bar_y, L * float(s["cabin_offset"]))
		var b := MeshInstance3D.new()
		b.mesh = rm
		b.material_override = blue
		b.position = Vector3(0.28, bar_y, L * float(s["cabin_offset"]))
		root.add_child(a)
		root.add_child(b)
		result["siren_red"] = red
		result["siren_blue"] = blue
	if with_wheels:
		for pos in wheel_positions(type):
			var w := wheel_mesh(r, W)
			w.position = pos
			root.add_child(w)
	return result


static func wheel_positions(type: String) -> Array[Vector3]:
	var s := VehicleSpecs.get_spec(type)
	var L: float = s["length"]
	var W: float = s["width"]
	var r: float = s["wheel_r"]
	var zf := L * 0.5 - r - 0.35
	var zr := -L * 0.5 + r + 0.4
	var x := W * 0.5 - 0.2
	return [Vector3(x, r, zf), Vector3(-x, r, zf), Vector3(x, r, zr), Vector3(-x, r, zr)]


static var _wheel_cache := {}


static func wheel_mesh(radius: float, _width: float) -> MeshInstance3D:
	var key := "%.2f" % radius
	if not _wheel_cache.has(key):
		var cm := CylinderMesh.new()
		cm.top_radius = radius
		cm.bottom_radius = radius
		cm.height = 0.26
		cm.radial_segments = 12
		cm.rings = 1
		cm.material = Mats.solid(Color(0.07, 0.07, 0.07), 0.9)
		_wheel_cache[key] = cm
	var mi := MeshInstance3D.new()
	mi.name = "Wheel"
	mi.mesh = _wheel_cache[key]
	mi.rotation = Vector3(0, 0, PI / 2.0)
	return mi
