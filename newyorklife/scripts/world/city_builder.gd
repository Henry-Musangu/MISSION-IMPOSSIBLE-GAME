class_name CityBuilder
extends Node3D
## Génère le secteur de Midtown : sol, chaussées, marquages, blocs d'immeubles,
## parc, Times Square, mobilier urbain, feux et bande d'immeubles périphérique.
## Chaque bloc est un nœud autonome (maillage fusionné + collisions) que le
## SectorManager active ou désactive selon la distance au joueur.

const L := CityLayout

const PALETTE := [
	Color(0.55, 0.27, 0.2), Color(0.42, 0.22, 0.17), Color(0.78, 0.72, 0.6),
	Color(0.7, 0.63, 0.5), Color(0.56, 0.57, 0.59), Color(0.33, 0.34, 0.36),
	Color(0.84, 0.82, 0.77), Color(0.62, 0.45, 0.33), Color(0.47, 0.5, 0.55),
]
const CURTAIN_COLOR := Color(0.26, 0.33, 0.42)
const SIDEWALK_COLOR := Color(0.62, 0.61, 0.58)
const CURB_COLOR := Color(0.72, 0.71, 0.68)
const ASPHALT := Color(0.2, 0.2, 0.22)
const ROOF := Color(0.24, 0.24, 0.25)

var blocks: Array[Node3D] = []
var block_centers: Array[Vector3] = []
var _reserved := {}   ## Vector2i bloc -> Array[Rect2] zones réservées (x, z)


func build() -> void:
	_reserve_sites()
	_build_ground()
	_build_markings()
	_build_traffic_lights()
	var bc := L.block_count()
	for i in bc.x:
		for j in bc.y:
			var b := Vector2i(i, j)
			var node := _build_block(b)
			blocks.append(node)
			block_centers.append(L.block_center(b))
	_build_backdrop()


# ------------------------------------------------------------------ sol

func _build_ground() -> void:
	var mn := L.city_min() - Vector2(L.BACKDROP_DEPTH + 60.0, L.BACKDROP_DEPTH + 60.0)
	var mx := L.city_max() + Vector2(L.BACKDROP_DEPTH + 60.0, L.BACKDROP_DEPTH + 60.0)
	var st := MeshUtil.begin()
	MeshUtil.add_ground_rect(st, Rect2(mn, mx - mn), 0.0, ASPHALT)
	var mi := MeshInstance3D.new()
	mi.name = "Ground"
	mi.mesh = MeshUtil.commit(st, Mats.ground())
	add_child(mi)
	var body := StaticBody3D.new()
	body.name = "GroundBody"
	var cs := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = Vector3(mx.x - mn.x, 2.0, mx.y - mn.y)
	cs.shape = shape
	cs.position = Vector3((mn.x + mx.x) * 0.5, -1.0, (mn.y + mx.y) * 0.5)
	body.add_child(cs)
	add_child(body)


func _build_markings() -> void:
	var st := MeshUtil.begin()
	var white := Color(0.88, 0.88, 0.86)
	var yellow := Color(0.92, 0.72, 0.1)
	var y := 0.02
	# Avenues (axe Z)
	for i in L.AVE_COUNT:
		var x := L.ave_x(i)
		for j in L.ST_COUNT - 1:
			var z0 := L.st_z(j) + L.ST_HALF + 3.6
			var z1 := L.st_z(j + 1) - L.ST_HALF - 3.6
			MeshUtil.add_ground_rect(st, Rect2(x - 0.3, z0, 0.12, z1 - z0), y, yellow)
			MeshUtil.add_ground_rect(st, Rect2(x + 0.18, z0, 0.12, z1 - z0), y, yellow)
			var z := z0
			while z < z1 - 3.0:
				MeshUtil.add_ground_rect(st, Rect2(x - 4.06, z, 0.12, 3.0), y, white)
				MeshUtil.add_ground_rect(st, Rect2(x + 3.94, z, 0.12, 3.0), y, white)
				z += 9.0
	# Rues (axe X)
	for j in L.ST_COUNT:
		var zc := L.st_z(j)
		for i in L.AVE_COUNT - 1:
			var x0 := L.ave_x(i) + L.AVE_HALF + 3.6
			var x1 := L.ave_x(i + 1) - L.AVE_HALF - 3.6
			MeshUtil.add_ground_rect(st, Rect2(x0, zc - 0.3, x1 - x0, 0.12), y, yellow)
			MeshUtil.add_ground_rect(st, Rect2(x0, zc + 0.18, x1 - x0, 0.12), y, yellow)
	# Passages piétons et lignes d'arrêt
	for i in L.AVE_COUNT:
		for j in L.ST_COUNT:
			var n := Vector2i(i, j)
			var c := L.node_pos(n)
			for d in [Vector2i(0, 1), Vector2i(0, -1)]:
				if not L.is_valid_node(n + d):
					continue
				var sgn := float(d.y)
				var zc2 := c.z + sgn * (L.ST_HALF + 1.6)
				var x := -L.AVE_HALF + 0.4
				while x < L.AVE_HALF - 0.4:
					MeshUtil.add_ground_rect(st, Rect2(c.x + x, zc2 - 1.2, 0.55, 2.4), y, white)
					x += 1.1
				# ligne d'arrêt sur la voie entrante (côté droit du conducteur qui arrive)
				var zs := c.z + sgn * (L.ST_HALF + 3.0)
				var xr := c.x if sgn > 0 else c.x - L.AVE_HALF
				MeshUtil.add_ground_rect(st, Rect2(xr, zs - 0.2, L.AVE_HALF, 0.4), y, white)
			for d in [Vector2i(1, 0), Vector2i(-1, 0)]:
				if not L.is_valid_node(n + d):
					continue
				var sgn := float(d.x)
				var xc := c.x + sgn * (L.AVE_HALF + 1.6)
				var z := -L.ST_HALF + 0.4
				while z < L.ST_HALF - 0.4:
					MeshUtil.add_ground_rect(st, Rect2(xc - 1.2, c.z + z, 2.4, 0.55), y, white)
					z += 1.1
				var xs := c.x + sgn * (L.AVE_HALF + 3.0)
				var zr := c.z - L.ST_HALF if sgn > 0 else c.z
				MeshUtil.add_ground_rect(st, Rect2(xs - 0.2, zr, 0.4, L.ST_HALF), y, white)
	var mi := MeshInstance3D.new()
	mi.name = "Markings"
	mi.mesh = MeshUtil.commit(st, Mats.markings())
	mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(mi)


# ------------------------------------------------------------------ feux

func _build_traffic_lights() -> void:
	var housing := MeshUtil.begin()
	var lamps := {}
	for axis in ["ave", "st"]:
		for col in ["r", "y", "g"]:
			lamps[axis + col] = MeshUtil.begin()
	var dark := Color(0.13, 0.14, 0.15)
	var pole_col := Color(0.25, 0.27, 0.28)
	var body := StaticBody3D.new()
	body.name = "TrafficLightPoles"
	for i in L.AVE_COUNT:
		for j in L.ST_COUNT:
			var n := Vector2i(i, j)
			var c := L.node_pos(n)
			for dir in [Vector2i(1, 0), Vector2i(-1, 0), Vector2i(0, 1), Vector2i(0, -1)]:
				# Feu pour les véhicules arrivant dans la direction dir (venant de n - dir)
				if not L.is_valid_node(n - dir):
					continue
				var d := L.dir3(dir)
				var r := L.right_of(d)
				var along := L.ST_HALF if dir.y != 0 else L.AVE_HALF
				var across := L.AVE_HALF if dir.y != 0 else L.ST_HALF
				var pole := c + d * (along + 1.2) + r * (across + 1.2)
				MeshUtil.add_cylinder(housing, pole, 0.12, 6.0, pole_col, 6)
				# bras au-dessus de la chaussée
				var arm_len := across * 0.85
				var arm_c := pole + Vector3(0, 5.8, 0) - r * (arm_len * 0.5)
				var arm_size := Vector3(absf(r.x) * arm_len + 0.15, 0.15, absf(r.z) * arm_len + 0.15)
				MeshUtil.add_box(housing, arm_c, arm_size, pole_col)
				var head := pole + Vector3(0, 5.0, 0) - r * (arm_len * 0.75)
				MeshUtil.add_box(housing, head, Vector3(0.45, 1.3, 0.45), dark, Vector2.ZERO, true, true)
				var axis := "ave" if dir.y != 0 else "st"
				var face := head - d * 0.24
				var cols := ["r", "y", "g"]
				for k in 3:
					var p: Vector3 = face + Vector3(0, 0.4 - k * 0.4, 0)
					MeshUtil.add_box(lamps[axis + cols[k]], p, Vector3(0.28 if dir.y != 0 else 0.06, 0.28, 0.06 if dir.y != 0 else 0.28), Color.WHITE)
				var cs := CollisionShape3D.new()
				var sh := CylinderShape3D.new()
				sh.radius = 0.15
				sh.height = 6.0
				cs.shape = sh
				cs.position = pole + Vector3(0, 3.0, 0)
				body.add_child(cs)
	var mesh := MeshUtil.commit(housing, Mats.vcolor(0.6, 0.3))
	for axis in ["ave", "st"]:
		for col in ["r", "y", "g"]:
			mesh = MeshUtil.commit(lamps[axis + col], Mats.traffic_lamp(axis, col), mesh)
	var mi := MeshInstance3D.new()
	mi.name = "TrafficLights"
	mi.mesh = mesh
	add_child(mi)
	add_child(body)


# ------------------------------------------------------------------ blocs

func _reserve_sites() -> void:
	_reserve("gas", Vector2(34.0, 22.0))
	_reserve("dealer", Vector2(30.0, 24.0))
	_reserve("garage", Vector2(22.0, 18.0))
	_reserve("terminal", Vector2(42.0, 28.0))


func _reserve(loc_id: String, size: Vector2) -> void:
	var l: Dictionary = L.LOCATIONS[loc_id]
	var b: Vector2i = l["block"]
	var lot := L.block_rect(b).grow(-L.SIDEWALK)
	var p := L.location_pos(loc_id)
	var side: int = l["side"]
	var r: Rect2
	match side:
		0:
			r = Rect2(lot.position.x, p.z - size.x * 0.5, size.y, size.x)
		1:
			r = Rect2(lot.end.x - size.y, p.z - size.x * 0.5, size.y, size.x)
		2:
			r = Rect2(p.x - size.x * 0.5, lot.position.y, size.x, size.y)
		_:
			r = Rect2(p.x - size.x * 0.5, lot.end.y - size.y, size.x, size.y)
	r = r.intersection(lot)
	if not _reserved.has(b):
		_reserved[b] = []
	_reserved[b].append({"rect": r, "id": loc_id, "side": side})


func _centrality(p: Vector3) -> float:
	var ts := L.node_pos(L.TIMES_SQUARE)
	return clampf(1.0 - p.distance_to(ts) / 300.0, 0.0, 1.0)


func _build_block(b: Vector2i) -> Node3D:
	var root := Node3D.new()
	root.name = "Block_%d_%d" % [b.x, b.y]
	add_child(root)
	var rng := RandomNumberGenerator.new()
	rng.seed = hash(b) + 1337
	var r := L.block_rect(b)
	var walls := MeshUtil.begin()      # immeubles (shader de façades)
	var props := MeshUtil.begin()      # trottoirs, mobilier (couleurs de sommets)
	var lampst := MeshUtil.begin()     # têtes de lampadaires (émissif)
	var screens := MeshUtil.begin()    # écrans de Times Square
	var glass := MeshUtil.begin()
	var body := StaticBody3D.new()
	body.name = "Collision"
	root.add_child(body)
	# Trottoir (dalle surélevée) + bordure
	MeshUtil.add_box(props, Vector3(r.get_center().x, L.CURB_H * 0.5, r.get_center().y),
		Vector3(r.size.x, L.CURB_H, r.size.y), CURB_COLOR, Vector2.ZERO, true, false, true, SIDEWALK_COLOR)
	_add_box_collision(body, Vector3(r.get_center().x, L.CURB_H * 0.5, r.get_center().y), Vector3(r.size.x, L.CURB_H, r.size.y))
	var lot := r.grow(-L.SIDEWALK)
	if b == L.PARK_BLOCK:
		_build_park(lot, props, body, rng)
	else:
		_build_buildings(b, lot, walls, props, glass, body, rng)
		for site in _reserved.get(b, []):
			_build_site(site, walls, props, glass, lampst, body)
	_build_street_furniture(b, r, props, lampst, body, rng)
	if _touches_times_square(b):
		_build_times_square(b, screens)
	var mesh := MeshUtil.commit(walls, Mats.building())
	mesh = MeshUtil.commit(props, Mats.vcolor(), mesh)
	mesh = MeshUtil.commit(lampst, Mats.lamp(), mesh)
	mesh = MeshUtil.commit(screens, Mats.screens(), mesh)
	mesh = MeshUtil.commit(glass, Mats.glass(), mesh)
	var mi := MeshInstance3D.new()
	mi.name = "Mesh"
	mi.mesh = mesh
	root.add_child(mi)
	return root


func _add_box_collision(body: StaticBody3D, center: Vector3, size: Vector3) -> void:
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = size
	cs.shape = sh
	cs.position = center
	body.add_child(cs)


func _intersects_reserved(b: Vector2i, rect: Rect2) -> bool:
	for site in _reserved.get(b, []):
		if (site["rect"] as Rect2).grow(-0.5).intersects(rect):
			return true
	return false


func _build_buildings(b: Vector2i, lot: Rect2, walls: SurfaceTool, props: SurfaceTool, glass: SurfaceTool,
		body: StaticBody3D, rng: RandomNumberGenerator) -> void:
	var cen := _centrality(L.block_center(b))
	var depth := lot.size.y * 0.5
	for row in 2:
		var z0 := lot.position.y + row * depth
		var x := lot.position.x
		while x < lot.end.x - 4.0:
			var w := rng.randf_range(12.0, 26.0)
			if lot.end.x - (x + w) < 10.0:
				w = lot.end.x - x
			var fp := Rect2(x, z0, w, depth)
			x += w
			if _intersects_reserved(b, fp):
				continue
			_add_building(fp, cen, walls, props, glass, body, rng)


func _add_building(fp: Rect2, cen: float, walls: SurfaceTool, props: SurfaceTool, glass: SurfaceTool,
		body: StaticBody3D, rng: RandomNumberGenerator) -> void:
	var tower := rng.randf() < 0.08 + cen * 0.3
	var h: float
	if tower:
		h = rng.randf_range(90.0, 120.0 + cen * 80.0)
	else:
		h = rng.randf_range(16.0, 32.0 + cen * 50.0)
	var col: Color = CURTAIN_COLOR if tower and rng.randf() < 0.7 else PALETTE[rng.randi() % PALETTE.size()]
	col.a = rng.randf()
	var curtain := 1.0 if col.is_equal_approx(CURTAIN_COLOR) or (tower and rng.randf() < 0.5) else 0.0
	var flags := Vector2(1.0, curtain)
	var gy := L.CURB_H
	var fc := Vector3(fp.get_center().x, 0.0, fp.get_center().y)
	var size := Vector3(fp.size.x - 0.2, h, fp.size.y - 0.2)
	if h > 50.0 and fp.size.x > 16.0 and rng.randf() < 0.7:
		# Retrait typique new-yorkais : socle puis tour en retrait
		var base_h := roundf(rng.randf_range(0.3, 0.5) * h / 3.5) * 3.5 + 4.4
		MeshUtil.add_box(walls, fc + Vector3(0, gy + base_h * 0.5, 0), Vector3(size.x, base_h, size.z), col, flags, true, false, true, ROOF)
		var inset := rng.randf_range(2.5, 4.5)
		var top_size := Vector3(maxf(size.x - inset * 2.0, 6.0), h - base_h, maxf(size.z - inset * 2.0, 6.0))
		var top_center := fc + Vector3(0, gy + base_h + top_size.y * 0.5, 0)
		_add_upper_box(walls, top_center, top_size, col, flags)
		if tower and rng.randf() < 0.6:
			MeshUtil.add_cylinder(props, top_center + Vector3(0, top_size.y * 0.5, 0), 0.25, rng.randf_range(8.0, 20.0), Color(0.5, 0.5, 0.52), 5)
	else:
		_add_upper_box(walls, fc + Vector3(0, gy + h * 0.5, 0), size, col, flags, true)
		if h < 45.0 and rng.randf() < 0.4:
			_add_water_tower(props, fc + Vector3(rng.randf_range(-size.x * 0.25, size.x * 0.25), gy + h, rng.randf_range(-size.z * 0.25, size.z * 0.25)))
	# Climatiseurs sur le toit des immeubles bas
	if h <= 50.0:
		for k in rng.randi_range(0, 3):
			var p := fc + Vector3(rng.randf_range(-size.x * 0.35, size.x * 0.35), gy + h + 0.6, rng.randf_range(-size.z * 0.35, size.z * 0.35))
			MeshUtil.add_box(props, p, Vector3(1.6, 1.2, 1.2), Color(0.6, 0.6, 0.6))
	_add_box_collision(body, fc + Vector3(0, gy + h * 0.5, 0), size)


func _add_upper_box(walls: SurfaceTool, center: Vector3, size: Vector3, col: Color, flags: Vector2, ground := false) -> void:
	# Les UV verticaux doivent partir du sol pour que le rez-de-chaussée ait des vitrines.
	var st := walls
	var h := size * 0.5
	var y0 := center.y - h.y
	var y1 := center.y + h.y
	var base := 0.0 if ground else y0 - L.CURB_H
	var x0 := center.x - h.x
	var x1 := center.x + h.x
	var z0 := center.z - h.z
	var z1 := center.z + h.z
	var v0 := base
	var v1 := base + size.y
	MeshUtil.add_quad(st, Vector3(x1, y0, z1), Vector3(x1, y0, z0), Vector3(x1, y1, z0), Vector3(x1, y1, z1), Vector3.RIGHT, col,
		Vector2(0, v0), Vector2(size.z, v0), Vector2(size.z, v1), Vector2(0, v1), flags)
	MeshUtil.add_quad(st, Vector3(x0, y0, z0), Vector3(x0, y0, z1), Vector3(x0, y1, z1), Vector3(x0, y1, z0), Vector3.LEFT, col,
		Vector2(0, v0), Vector2(size.z, v0), Vector2(size.z, v1), Vector2(0, v1), flags)
	MeshUtil.add_quad(st, Vector3(x0, y0, z1), Vector3(x1, y0, z1), Vector3(x1, y1, z1), Vector3(x0, y1, z1), Vector3.BACK, col,
		Vector2(0, v0), Vector2(size.x, v0), Vector2(size.x, v1), Vector2(0, v1), flags)
	MeshUtil.add_quad(st, Vector3(x1, y0, z0), Vector3(x0, y0, z0), Vector3(x0, y1, z0), Vector3(x1, y1, z0), Vector3.FORWARD, col,
		Vector2(0, v0), Vector2(size.x, v0), Vector2(size.x, v1), Vector2(0, v1), flags)
	MeshUtil.add_quad(st, Vector3(x0, y1, z0), Vector3(x0, y1, z1), Vector3(x1, y1, z1), Vector3(x1, y1, z0), Vector3.UP, Color(ROOF, col.a))


func _add_water_tower(st: SurfaceTool, base: Vector3) -> void:
	var wood := Color(0.42, 0.3, 0.2)
	var steel := Color(0.25, 0.25, 0.26)
	for k in 4:
		var a := TAU * k / 4.0 + PI / 4.0
		MeshUtil.add_box(st, base + Vector3(cos(a) * 1.2, 1.2, sin(a) * 1.2), Vector3(0.15, 2.4, 0.15), steel)
	MeshUtil.add_cylinder(st, base + Vector3(0, 2.4, 0), 1.6, 3.2, wood, 10)
	MeshUtil.add_cone(st, base + Vector3(0, 5.6, 0), 1.75, 1.2, Color(0.3, 0.22, 0.16), 10)


# ------------------------------------------------------------------ sites spéciaux

func _build_site(site: Dictionary, walls: SurfaceTool, props: SurfaceTool, glass: SurfaceTool,
		lampst: SurfaceTool, body: StaticBody3D) -> void:
	var r: Rect2 = site["rect"]
	var side: int = site["side"]
	var c := Vector3(r.get_center().x, L.CURB_H, r.get_center().y)
	var n := L.side_normal(side)
	match str(site["id"]):
		"gas":
			# Auvent sur piliers, pompes, petite boutique au fond
			MeshUtil.add_box(props, c + Vector3(0, 5.2, 0) + n * 2.0, Vector3(r.size.x * 0.8, 0.6, r.size.y * 0.55), Color(0.85, 0.2, 0.15))
			MeshUtil.add_box(lampst, c + Vector3(0, 4.85, 0) + n * 2.0, Vector3(r.size.x * 0.7, 0.08, r.size.y * 0.45), Color.WHITE)
			for k in 4:
				var off := Vector3((k % 2 - 0.5) * r.size.x * 0.55, 0, (int(k / 2) - 0.5) * r.size.y * 0.35)
				MeshUtil.add_box(props, c + n * 2.0 + off + Vector3(0, 2.5, 0), Vector3(0.35, 5.0, 0.35), Color(0.8, 0.8, 0.8))
				MeshUtil.add_box(props, c + n * 2.0 + off * 0.6 + Vector3(0, 0.8, 0), Vector3(0.8, 1.6, 0.5), Color(0.9, 0.9, 0.9))
				_add_box_collision(body, c + n * 2.0 + off * 0.6 + Vector3(0, 0.8, 0), Vector3(0.8, 1.6, 0.5))
			var kiosk := c - n * (r.size.y * 0.32)
			MeshUtil.add_box(walls, kiosk + Vector3(0, 2.2, 0), Vector3(12.0, 4.4, 6.0), Color(0.8, 0.78, 0.72, 0.3), Vector2(1, 0))
			_add_box_collision(body, kiosk + Vector3(0, 2.2, 0), Vector3(12.0, 4.4, 6.0))
		"dealer":
			MeshUtil.add_box(glass, c + Vector3(0, 3.5, 0), Vector3(r.size.x - 2.0, 7.0, r.size.y - 2.0), Color.WHITE, Vector2.ZERO, false)
			MeshUtil.add_box(props, c + Vector3(0, 7.2, 0), Vector3(r.size.x - 1.0, 0.4, r.size.y - 1.0), Color(0.85, 0.85, 0.87))
			_add_box_collision(body, c + Vector3(0, 3.5, 0), Vector3(r.size.x - 2.0, 7.0, r.size.y - 2.0))
		"garage":
			MeshUtil.add_box(walls, c + Vector3(0, 3.6, 0), Vector3(r.size.x, 7.2, r.size.y), Color(0.5, 0.25, 0.18, 0.7), Vector2(0, 0), true, false, true, ROOF)
			var door := c + n * (r.size.y * 0.5 + 0.02) + Vector3(0, 2.4, 0)
			var dsize := Vector3(absf(n.z) * 8.0 + 0.05, 4.8, absf(n.x) * 8.0 + 0.05)
			MeshUtil.add_box(props, door, dsize, Color(0.32, 0.33, 0.35))
			_add_box_collision(body, c + Vector3(0, 3.6, 0), Vector3(r.size.x, 7.2, r.size.y))
		"terminal":
			MeshUtil.add_box(walls, c + Vector3(0, 8.0, 0), Vector3(r.size.x, 16.0, r.size.y), Color(0.7, 0.68, 0.64, 0.4), Vector2(1, 1), true, false, true, ROOF)
			_add_box_collision(body, c + Vector3(0, 8.0, 0), Vector3(r.size.x, 16.0, r.size.y))
			# Marquise d'entrée
			MeshUtil.add_box(props, c + n * (r.size.y * 0.5 + 1.5) + Vector3(0, 4.5, 0),
				Vector3(absf(n.z) * 20.0 + absf(n.x) * 3.0, 0.4, absf(n.x) * 20.0 + absf(n.z) * 3.0), Color(0.2, 0.22, 0.26))


# ------------------------------------------------------------------ parc

func _build_park(lot: Rect2, props: SurfaceTool, body: StaticBody3D, rng: RandomNumberGenerator) -> void:
	var grass := Color(0.27, 0.45, 0.2)
	var path := Color(0.72, 0.66, 0.52)
	var c := lot.get_center()
	MeshUtil.add_ground_rect(props, lot, L.CURB_H + 0.02, grass)
	# Allées en croix
	MeshUtil.add_ground_rect(props, Rect2(lot.position.x, c.y - 1.8, lot.size.x, 3.6), L.CURB_H + 0.04, path)
	MeshUtil.add_ground_rect(props, Rect2(c.x - 1.8, lot.position.y, 3.6, lot.size.y), L.CURB_H + 0.04, path)
	# Muret périphérique avec ouvertures
	var stone := Color(0.55, 0.53, 0.5)
	var wall_h := 0.6
	for seg in [
		Rect2(lot.position.x, lot.position.y, c.x - 3.0 - lot.position.x, 0.4),
		Rect2(c.x + 3.0, lot.position.y, lot.end.x - c.x - 3.0, 0.4),
		Rect2(lot.position.x, lot.end.y - 0.4, c.x - 3.0 - lot.position.x, 0.4),
		Rect2(c.x + 3.0, lot.end.y - 0.4, lot.end.x - c.x - 3.0, 0.4),
		Rect2(lot.position.x, lot.position.y, 0.4, c.y - 3.0 - lot.position.y),
		Rect2(lot.position.x, c.y + 3.0, 0.4, lot.end.y - c.y - 3.0),
		Rect2(lot.end.x - 0.4, lot.position.y, 0.4, c.y - 3.0 - lot.position.y),
		Rect2(lot.end.x - 0.4, c.y + 3.0, 0.4, lot.end.y - c.y - 3.0),
	]:
		var s: Rect2 = seg
		var center := Vector3(s.get_center().x, L.CURB_H + wall_h * 0.5, s.get_center().y)
		MeshUtil.add_box(props, center, Vector3(s.size.x, wall_h, s.size.y), stone)
		_add_box_collision(body, center, Vector3(s.size.x, wall_h, s.size.y))
	# Fontaine centrale
	var fc := Vector3(c.x, L.CURB_H, c.y)
	MeshUtil.add_cylinder(props, fc, 5.0, 0.7, stone, 16)
	MeshUtil.add_cylinder(props, fc + Vector3(0, 0.7, 0), 4.5, 0.02, Color(0.25, 0.42, 0.55), 16)
	MeshUtil.add_cylinder(props, fc, 0.6, 2.5, stone, 8)
	var csf := CollisionShape3D.new()
	var shf := CylinderShape3D.new()
	shf.radius = 5.0
	shf.height = 0.7
	csf.shape = shf
	csf.position = fc + Vector3(0, 0.35, 0)
	body.add_child(csf)
	# Arbres
	var placed := 0
	var tries := 0
	while placed < 34 and tries < 400:
		tries += 1
		var p := Vector2(rng.randf_range(lot.position.x + 2.5, lot.end.x - 2.5), rng.randf_range(lot.position.y + 2.5, lot.end.y - 2.5))
		if absf(p.x - c.x) < 3.5 or absf(p.y - c.y) < 3.5 or p.distance_to(c) < 8.0:
			continue
		_add_tree(props, body, Vector3(p.x, L.CURB_H, p.y), rng)
		placed += 1
	# Bancs le long des allées
	for k in 6:
		var bx := lerpf(lot.position.x + 6.0, lot.end.x - 6.0, k / 5.0)
		if absf(bx - c.x) < 7.0:
			continue
		MeshUtil.add_box(props, Vector3(bx, L.CURB_H + 0.45, c.y - 2.6), Vector3(1.8, 0.1, 0.5), Color(0.45, 0.3, 0.18))
		MeshUtil.add_box(props, Vector3(bx, L.CURB_H + 0.22, c.y - 2.6), Vector3(1.6, 0.44, 0.1), Color(0.2, 0.2, 0.2))


func _add_tree(st: SurfaceTool, body: StaticBody3D, base: Vector3, rng: RandomNumberGenerator) -> void:
	var h := rng.randf_range(3.5, 6.0)
	MeshUtil.add_cylinder(st, base, 0.18, h, Color(0.32, 0.22, 0.15), 6, false)
	var g := Color(0.2 + rng.randf() * 0.1, 0.38 + rng.randf() * 0.15, 0.15)
	MeshUtil.add_sphere(st, base + Vector3(0, h + 0.6, 0), rng.randf_range(1.8, 2.6), g, 4, 7, 0.85)
	MeshUtil.add_sphere(st, base + Vector3(0.6, h - 0.2, 0.4), 1.4, g.darkened(0.1), 3, 6)
	var cs := CollisionShape3D.new()
	var sh := CylinderShape3D.new()
	sh.radius = 0.25
	sh.height = h
	cs.shape = sh
	cs.position = base + Vector3(0, h * 0.5, 0)
	body.add_child(cs)


# ------------------------------------------------------------------ mobilier urbain

func _build_street_furniture(b: Vector2i, r: Rect2, props: SurfaceTool, lampst: SurfaceTool, body: StaticBody3D,
		rng: RandomNumberGenerator) -> void:
	var y := L.CURB_H
	var pole_col := Color(0.18, 0.2, 0.2)
	var trees_ok := b != L.PARK_BLOCK and not _touches_times_square(b)
	for side in 4:
		var horizontal := side >= 2
		var length := r.size.x if horizontal else r.size.y
		var count := int(length / 24.0)
		for k in count:
			var t := (k + 0.5) / count
			var p := L.block_side_point(b, side, t, 0.6)
			var n := L.side_normal(side)
			# Lampadaire : mât + bras vers la chaussée + tête lumineuse
			MeshUtil.add_cylinder(props, p, 0.1, 7.0, pole_col, 6, false)
			var arm := p + Vector3(0, 7.0, 0) + n * 0.9
			MeshUtil.add_box(props, arm, Vector3(absf(n.x) * 1.8 + 0.12, 0.12, absf(n.z) * 1.8 + 0.12), pole_col)
			MeshUtil.add_box(lampst, arm + n * 0.9 + Vector3(0, -0.15, 0), Vector3(0.6, 0.18, 0.6), Color.WHITE, Vector2.ZERO, true, true)
			var cs := CollisionShape3D.new()
			var sh := CylinderShape3D.new()
			sh.radius = 0.14
			sh.height = 7.0
			cs.shape = sh
			cs.position = p + Vector3(0, 3.5, 0)
			body.add_child(cs)
			# Arbre d'alignement entre deux lampadaires
			if trees_ok and rng.randf() < 0.55:
				var tp := L.block_side_point(b, side, clampf(t + 0.5 / count, 0.0, 1.0), 1.0)
				if not _near_location(tp):
					MeshUtil.add_box(props, tp + Vector3(0, 0.03, 0), Vector3(1.4, 0.06, 1.4), Color(0.3, 0.24, 0.18))
					_add_tree(props, body, tp, rng)
			elif rng.randf() < 0.35:
				# Borne d'incendie ou poubelle
				var q := L.block_side_point(b, side, clampf(t + 0.3 / count, 0.0, 1.0), 0.5)
				if rng.randf() < 0.5:
					MeshUtil.add_cylinder(props, q, 0.16, 0.7, Color(0.75, 0.15, 0.1), 6)
				else:
					MeshUtil.add_cylinder(props, q, 0.28, 0.95, Color(0.2, 0.32, 0.22), 7)


func _near_location(p: Vector3) -> bool:
	for id in L.LOCATIONS:
		if L.location_pos(id).distance_to(p) < 5.0:
			return true
	return false


# ------------------------------------------------------------------ Times Square

func _touches_times_square(b: Vector2i) -> bool:
	var n := L.TIMES_SQUARE
	return (b.x == n.x or b.x == n.x - 1) and (b.y == n.y or b.y == n.y - 1)


func _build_times_square(b: Vector2i, screens: SurfaceTool) -> void:
	# Grands écrans animés sur les façades qui donnent sur l'intersection
	var n := L.TIMES_SQUARE
	var corner := L.node_pos(n)
	var lot := L.block_rect(b).grow(-L.SIDEWALK)
	var rng := RandomNumberGenerator.new()
	rng.seed = hash(b) * 7 + 3
	var faces := []
	# façade côté avenue (x) et côté rue (z) la plus proche de l'intersection
	var fx := lot.end.x if b.x < n.x else lot.position.x
	var fz := lot.end.y if b.y < n.y else lot.position.y
	var nx := 1.0 if b.x < n.x else -1.0
	var nz := 1.0 if b.y < n.y else -1.0
	var zc := fz - nz * 10.0
	var xc := fx - nx * 12.0
	faces.append({"pos": Vector3(fx + nx * 0.15, 0, zc), "normal": Vector3(nx, 0, 0)})
	faces.append({"pos": Vector3(xc, 0, fz + nz * 0.15), "normal": Vector3(0, 0, nz)})
	var palette := [Color(0.95, 0.2, 0.35), Color(0.15, 0.6, 1.0), Color(1.0, 0.75, 0.1), Color(0.3, 1.0, 0.5), Color(0.8, 0.3, 1.0)]
	for f in faces:
		var y := 6.0
		var normal: Vector3 = f["normal"]
		var tangent := normal.cross(Vector3.UP)
		for k in 3:
			var h := rng.randf_range(4.0, 8.0)
			var w := rng.randf_range(8.0, 14.0)
			var c: Color = palette[rng.randi() % palette.size()]
			c.a = rng.randf()
			var center: Vector3 = f["pos"] + Vector3(0, y + h * 0.5, 0) + tangent * rng.randf_range(-3.0, 3.0)
			var a := center - tangent * w * 0.5 - Vector3(0, h * 0.5, 0)
			var bb := center + tangent * w * 0.5 - Vector3(0, h * 0.5, 0)
			var cc := center + tangent * w * 0.5 + Vector3(0, h * 0.5, 0)
			var d := center - tangent * w * 0.5 + Vector3(0, h * 0.5, 0)
			MeshUtil.add_quad(screens, a, bb, cc, d, normal, c, Vector2(0, 0), Vector2(w, 0), Vector2(w, h), Vector2(0, h))
			y += h + rng.randf_range(1.0, 3.0)


# ------------------------------------------------------------------ périphérie

func _build_backdrop() -> void:
	var mn := L.city_min()
	var mx := L.city_max()
	var dd := L.BACKDROP_DEPTH
	var strips := [
		Rect2(mn.x - dd, mn.y - dd, mx.x - mn.x + dd * 2.0, dd),   # nord
		Rect2(mn.x - dd, mx.y, mx.x - mn.x + dd * 2.0, dd),        # sud
		Rect2(mn.x - dd, mn.y, dd, mx.y - mn.y),                   # ouest
		Rect2(mx.x, mn.y, dd, mx.y - mn.y),                        # est
	]
	var rng := RandomNumberGenerator.new()
	rng.seed = 4242
	for si in strips.size():
		var s: Rect2 = strips[si]
		var root := Node3D.new()
		root.name = "Backdrop_%d" % si
		add_child(root)
		var walls := MeshUtil.begin()
		var props := MeshUtil.begin()
		var body := StaticBody3D.new()
		root.add_child(body)
		var horizontal := s.size.x > s.size.y
		var length := s.size.x if horizontal else s.size.y
		var pos := 0.0
		while pos < length - 1.0:
			var w := minf(rng.randf_range(14.0, 30.0), length - pos)
			var fp: Rect2
			if horizontal:
				fp = Rect2(s.position.x + pos, s.position.y, w, s.size.y)
			else:
				fp = Rect2(s.position.x, s.position.y + pos, s.size.x, w)
			pos += w
			var h := rng.randf_range(22.0, 95.0)
			var col: Color = PALETTE[rng.randi() % PALETTE.size()]
			col.a = rng.randf()
			var center := Vector3(fp.get_center().x, h * 0.5, fp.get_center().y)
			_add_upper_box(walls, center, Vector3(fp.size.x, h, fp.size.y), col, Vector2(1.0, 0.0 if rng.randf() < 0.8 else 1.0), true)
			_add_box_collision(body, center, Vector3(fp.size.x, h, fp.size.y))
		var mesh := MeshUtil.commit(walls, Mats.building())
		mesh = MeshUtil.commit(props, Mats.vcolor(), mesh)
		var mi := MeshInstance3D.new()
		mi.mesh = mesh
		mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
		root.add_child(mi)
	# Trottoirs extérieurs le long des avenues/rues de bordure
	var root2 := Node3D.new()
	root2.name = "OuterSidewalks"
	add_child(root2)
	var st := MeshUtil.begin()
	var body2 := StaticBody3D.new()
	root2.add_child(body2)
	var sw := L.SIDEWALK
	var outer := [
		Rect2(mn.x, mn.y, mx.x - mn.x, sw), Rect2(mn.x, mx.y - sw, mx.x - mn.x, sw),
		Rect2(mn.x, mn.y + sw, sw, mx.y - mn.y - sw * 2.0), Rect2(mx.x - sw, mn.y + sw, sw, mx.y - mn.y - sw * 2.0),
	]
	for o in outer:
		var rr: Rect2 = o
		var cc := Vector3(rr.get_center().x, L.CURB_H * 0.5, rr.get_center().y)
		MeshUtil.add_box(st, cc, Vector3(rr.size.x, L.CURB_H, rr.size.y), CURB_COLOR, Vector2.ZERO, true, false, true, SIDEWALK_COLOR)
		_add_box_collision(body2, cc, Vector3(rr.size.x, L.CURB_H, rr.size.y))
	var mi2 := MeshInstance3D.new()
	mi2.mesh = MeshUtil.commit(st, Mats.vcolor())
	root2.add_child(mi2)
