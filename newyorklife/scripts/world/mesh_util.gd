class_name MeshUtil
extends RefCounted
## Construction de géométrie procédurale avec SurfaceTool.
## Les faces sont orientées automatiquement (Godot : faces avant dans le sens horaire).


static func begin() -> SurfaceTool:
	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	return st


static func _v(st: SurfaceTool, p: Vector3, n: Vector3, c: Color, uv: Vector2, uv2: Vector2) -> void:
	if not st.has_meta("used"):
		st.set_meta("used", true)
	st.set_normal(n)
	st.set_color(c)
	st.set_uv(uv)
	st.set_uv2(uv2)
	st.add_vertex(p)


static func add_tri(st: SurfaceTool, p0: Vector3, p1: Vector3, p2: Vector3, n: Vector3, c: Color,
		uv0 := Vector2.ZERO, uv1 := Vector2.ZERO, uv2v := Vector2.ZERO, flags := Vector2.ZERO) -> void:
	if (p1 - p0).cross(p2 - p0).dot(n) < 0.0:
		_v(st, p0, n, c, uv0, flags)
		_v(st, p1, n, c, uv1, flags)
		_v(st, p2, n, c, uv2v, flags)
	else:
		_v(st, p0, n, c, uv0, flags)
		_v(st, p2, n, c, uv2v, flags)
		_v(st, p1, n, c, uv1, flags)


## Quadrilatère a-b-c-d (contour dans l'ordre), normale n.
static func add_quad(st: SurfaceTool, a: Vector3, b: Vector3, c: Vector3, d: Vector3, n: Vector3, col: Color,
		ua := Vector2.ZERO, ub := Vector2.ZERO, uc := Vector2.ZERO, ud := Vector2.ZERO, flags := Vector2.ZERO) -> void:
	add_tri(st, a, b, c, n, col, ua, ub, uc, flags)
	add_tri(st, a, c, d, n, col, ua, uc, ud, flags)


## Boîte alignée. UV des côtés en mètres (u horizontal, v = hauteur depuis la base).
## flags (UV2) : x = façade avec fenêtres, y = mur-rideau vitré.
static func add_box(st: SurfaceTool, center: Vector3, size: Vector3, col: Color,
		flags := Vector2.ZERO, top := true, bottom := false, sides := true, top_col := Color(-1, 0, 0)) -> void:
	var h := size * 0.5
	var x0 := center.x - h.x
	var x1 := center.x + h.x
	var y0 := center.y - h.y
	var y1 := center.y + h.y
	var z0 := center.z - h.z
	var z1 := center.z + h.z
	var vh := size.y
	if sides:
		# +X
		add_quad(st, Vector3(x1, y0, z1), Vector3(x1, y0, z0), Vector3(x1, y1, z0), Vector3(x1, y1, z1), Vector3.RIGHT, col,
			Vector2(0, 0), Vector2(size.z, 0), Vector2(size.z, vh), Vector2(0, vh), flags)
		# −X
		add_quad(st, Vector3(x0, y0, z0), Vector3(x0, y0, z1), Vector3(x0, y1, z1), Vector3(x0, y1, z0), Vector3.LEFT, col,
			Vector2(0, 0), Vector2(size.z, 0), Vector2(size.z, vh), Vector2(0, vh), flags)
		# +Z
		add_quad(st, Vector3(x0, y0, z1), Vector3(x1, y0, z1), Vector3(x1, y1, z1), Vector3(x0, y1, z1), Vector3.BACK, col,
			Vector2(0, 0), Vector2(size.x, 0), Vector2(size.x, vh), Vector2(0, vh), flags)
		# −Z
		add_quad(st, Vector3(x1, y0, z0), Vector3(x0, y0, z0), Vector3(x0, y1, z0), Vector3(x1, y1, z0), Vector3.FORWARD, col,
			Vector2(0, 0), Vector2(size.x, 0), Vector2(size.x, vh), Vector2(0, vh), flags)
	var tc := col if top_col.r < 0.0 else top_col
	if top:
		add_quad(st, Vector3(x0, y1, z0), Vector3(x0, y1, z1), Vector3(x1, y1, z1), Vector3(x1, y1, z0), Vector3.UP, tc,
			Vector2(x0, z0), Vector2(x0, z1), Vector2(x1, z1), Vector2(x1, z0))
	if bottom:
		add_quad(st, Vector3(x0, y0, z0), Vector3(x1, y0, z0), Vector3(x1, y0, z1), Vector3(x0, y0, z1), Vector3.DOWN, col)


## Boîte orientée (base + axes), pour les objets tournés.
static func add_obox(st: SurfaceTool, xf: Transform3D, size: Vector3, col: Color) -> void:
	var h := size * 0.5
	var corners := []
	for i in 8:
		var p := Vector3(h.x if i & 1 else -h.x, h.y if i & 2 else -h.y, h.z if i & 4 else -h.z)
		corners.append(xf * p)
	var bx := xf.basis.x.normalized()
	var by := xf.basis.y.normalized()
	var bz := xf.basis.z.normalized()
	add_quad(st, corners[1], corners[5], corners[7], corners[3], bx, col)
	add_quad(st, corners[0], corners[2], corners[6], corners[4], -bx, col)
	add_quad(st, corners[2], corners[3], corners[7], corners[6], by, col)
	add_quad(st, corners[0], corners[4], corners[5], corners[1], -by, col)
	add_quad(st, corners[4], corners[6], corners[7], corners[5], bz, col)
	add_quad(st, corners[0], corners[1], corners[3], corners[2], -bz, col)


## Cylindre vertical.
static func add_cylinder(st: SurfaceTool, base: Vector3, radius: float, height: float, col: Color,
		segments := 8, top := true) -> void:
	for s in segments:
		var a0 := TAU * s / segments
		var a1 := TAU * (s + 1) / segments
		var p0 := base + Vector3(cos(a0) * radius, 0, sin(a0) * radius)
		var p1 := base + Vector3(cos(a1) * radius, 0, sin(a1) * radius)
		var n := Vector3(cos((a0 + a1) * 0.5), 0, sin((a0 + a1) * 0.5))
		add_quad(st, p0, p1, p1 + Vector3(0, height, 0), p0 + Vector3(0, height, 0), n, col)
		if top:
			var c := base + Vector3(0, height, 0)
			add_tri(st, c, p0 + Vector3(0, height, 0), p1 + Vector3(0, height, 0), Vector3.UP, col)


## Cône (toits, sapins).
static func add_cone(st: SurfaceTool, base: Vector3, radius: float, height: float, col: Color, segments := 8) -> void:
	var tip := base + Vector3(0, height, 0)
	for s in segments:
		var a0 := TAU * s / segments
		var a1 := TAU * (s + 1) / segments
		var p0 := base + Vector3(cos(a0) * radius, 0, sin(a0) * radius)
		var p1 := base + Vector3(cos(a1) * radius, 0, sin(a1) * radius)
		var am := (a0 + a1) * 0.5
		var n := Vector3(cos(am) * height, radius, sin(am) * height).normalized()
		add_tri(st, p0, p1, tip, n, col)


## Sphère basse définition (feuillages).
static func add_sphere(st: SurfaceTool, center: Vector3, radius: float, col: Color, rings := 4, segments := 7,
		squash := 1.0) -> void:
	for r in rings:
		var t0 := PI * r / rings
		var t1 := PI * (r + 1) / rings
		for s in segments:
			var a0 := TAU * s / segments
			var a1 := TAU * (s + 1) / segments
			var pts := [
				_sph(t0, a0), _sph(t0, a1), _sph(t1, a1), _sph(t1, a0)
			]
			var ps := []
			for p in pts:
				ps.append(center + Vector3(p.x * radius, p.y * radius * squash, p.z * radius))
			var n: Vector3 = (pts[0] + pts[1] + pts[2] + pts[3]).normalized()
			if r == 0:
				add_tri(st, ps[0], ps[2], ps[3], n, col)
			elif r == rings - 1:
				add_tri(st, ps[0], ps[1], ps[2], n, col)
			else:
				add_quad(st, ps[0], ps[1], ps[2], ps[3], n, col)


static func _sph(theta: float, phi: float) -> Vector3:
	return Vector3(sin(theta) * cos(phi), cos(theta), sin(theta) * sin(phi))


## Quadrilatère plat au sol (marquages), légèrement surélevé.
static func add_ground_rect(st: SurfaceTool, rect: Rect2, y: float, col: Color) -> void:
	var a := Vector3(rect.position.x, y, rect.position.y)
	var b := Vector3(rect.end.x, y, rect.position.y)
	var c := Vector3(rect.end.x, y, rect.end.y)
	var d := Vector3(rect.position.x, y, rect.end.y)
	add_quad(st, a, b, c, d, Vector3.UP, col,
		Vector2(a.x, a.z), Vector2(b.x, b.z), Vector2(c.x, c.z), Vector2(d.x, d.z))


## Ajoute la surface au maillage (créé si besoin) ; ignore les SurfaceTool vides.
static func commit(st: SurfaceTool, mat: Material, mesh: ArrayMesh = null) -> ArrayMesh:
	if mesh == null:
		mesh = ArrayMesh.new()
	if not st.has_meta("used"):
		return mesh
	var m := st.commit(mesh)
	m.surface_set_material(m.get_surface_count() - 1, mat)
	return m
