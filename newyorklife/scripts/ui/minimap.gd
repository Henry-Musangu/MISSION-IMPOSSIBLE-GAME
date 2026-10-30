class_name Minimap
extends Control
## Mini-carte (orientée selon la caméra) et carte complète (nord en haut).
## Dessinée à partir du plan de la ville : aucun rendu 3D supplémentaire.

const L = preload("res://scripts/world/city_layout.gd")

var world: Node
var full := false
var zoom := 0.55          ## pixels par mètre (mini-carte)
var _rot := 0.0


func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	clip_contents = true


func _process(_delta: float) -> void:
	if is_visible_in_tree():
		queue_redraw()


func _w2m(p: Vector3, center: Vector3, scale_px: float) -> Vector2:
	var d := Vector2(p.x - center.x, p.z - center.z)
	if not full:
		d = d.rotated(-_rot)
	return size * 0.5 + d * scale_px


func _draw() -> void:
	if world == null or world.player == null:
		return
	var player: Player = world.player
	var center := player.focus_position()
	var scale_px := zoom
	if full:
		var mn := L.city_min()
		var mx := L.city_max()
		scale_px = minf(size.x / (mx.x - mn.x + 40.0), size.y / (mx.y - mn.y + 40.0))
		center = Vector3((mn.x + mx.x) * 0.5, 0, (mn.y + mx.y) * 0.5)
		_rot = 0.0
	else:
		_rot = -world.camera.yaw
	# fond
	draw_rect(Rect2(Vector2.ZERO, size), Color(0.08, 0.09, 0.1, 0.85))
	# blocs
	var bc := L.block_count()
	for i in bc.x:
		for j in bc.y:
			var b := Vector2i(i, j)
			var r := L.block_rect(b)
			var col := Color(0.32, 0.33, 0.36) if b != L.PARK_BLOCK else Color(0.22, 0.42, 0.22)
			var pts := PackedVector2Array([
				_w2m(Vector3(r.position.x, 0, r.position.y), center, scale_px),
				_w2m(Vector3(r.end.x, 0, r.position.y), center, scale_px),
				_w2m(Vector3(r.end.x, 0, r.end.y), center, scale_px),
				_w2m(Vector3(r.position.x, 0, r.end.y), center, scale_px),
			])
			draw_colored_polygon(pts, col)
	# bordure de la ville
	var mn2 := L.city_min()
	var mx2 := L.city_max()
	var border := PackedVector2Array([
		_w2m(Vector3(mn2.x, 0, mn2.y), center, scale_px), _w2m(Vector3(mx2.x, 0, mn2.y), center, scale_px),
		_w2m(Vector3(mx2.x, 0, mx2.y), center, scale_px), _w2m(Vector3(mn2.x, 0, mx2.y), center, scale_px),
		_w2m(Vector3(mn2.x, 0, mn2.y), center, scale_px)])
	draw_polyline(border, Color(0.5, 0.5, 0.55), 2.0)
	# noms des voies (carte complète)
	var font := get_theme_default_font()
	if full:
		for i in L.AVE_COUNT:
			var p := _w2m(Vector3(L.ave_x(i), 0, L.city_min().y - 6.0), center, scale_px)
			var n := L.avenue_name(i)
			draw_string(font, p - Vector2(font.get_string_size(n, HORIZONTAL_ALIGNMENT_LEFT, -1, 14).x * 0.5, 0), n, HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color(0.85, 0.85, 0.8))
		for j in L.ST_COUNT:
			var p2 := _w2m(Vector3(L.city_max().x + 4.0, 0, L.st_z(j)), center, scale_px)
			draw_string(font, p2 + Vector2(0, 5), L.street_name(j), HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color(0.85, 0.85, 0.8))
	# points d'intérêt
	var points: Array = world.map_points()
	for pt in points:
		var mp := _w2m(pt["pos"], center, scale_px)
		var on_edge := false
		if not full:
			var half := size * 0.5 - Vector2(8, 8)
			var rel := mp - size * 0.5
			if absf(rel.x) > half.x or absf(rel.y) > half.y:
				var k := minf(half.x / maxf(absf(rel.x), 0.001), half.y / maxf(absf(rel.y), 0.001))
				mp = size * 0.5 + rel * k
				on_edge = true
		match str(pt["kind"]):
			"mission":
				_diamond(mp, 7.0, UITheme.ACCENT)
			"target":
				draw_circle(mp, 8.0 if not on_edge else 6.0, Color(0.3, 0.85, 1.0))
				draw_arc(mp, 11.0, 0, TAU, 20, Color(1, 1, 1, 0.8), 2.0)
			"shop":
				if full or not on_edge:
					draw_rect(Rect2(mp - Vector2(4, 4), Vector2(8, 8)), Color(0.3, 0.6, 1.0))
			"police":
				draw_circle(mp, 5.0, Color(1.0, 0.25, 0.25) if int(Time.get_ticks_msec() / 250) % 2 == 0 else Color(0.25, 0.4, 1.0))
	# joueur (flèche)
	var pp := _w2m(player.focus_position(), center, scale_px)
	var heading := player.rig.global_rotation.y if not player.vehicle else player.vehicle.global_rotation.y
	var fwd := Vector2(sin(heading), cos(heading))
	if not full:
		fwd = fwd.rotated(-_rot)
	var side := Vector2(-fwd.y, fwd.x)
	var tip := pp + fwd * 10.0
	draw_colored_polygon(PackedVector2Array([tip, pp - fwd * 6.0 + side * 6.0, pp - fwd * 3.0, pp - fwd * 6.0 - side * 6.0]), Color.WHITE)
	if not full:
		draw_rect(Rect2(Vector2.ZERO, size), Color(1, 1, 1, 0.35), false, 2.0)
		# indicateur du nord
		var north := size * 0.5 + Vector2(0, -1).rotated(-_rot) * (minf(size.x, size.y) * 0.5 - 12.0)
		draw_string(font, north - Vector2(5, -6), "N", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, UITheme.ACCENT)


func _diamond(p: Vector2, r: float, c: Color) -> void:
	draw_colored_polygon(PackedVector2Array([p + Vector2(0, -r), p + Vector2(r, 0), p + Vector2(0, r), p + Vector2(-r, 0)]), c)
	draw_polyline(PackedVector2Array([p + Vector2(0, -r), p + Vector2(r, 0), p + Vector2(0, r), p + Vector2(-r, 0), p + Vector2(0, -r)]), Color(0, 0, 0, 0.7), 1.5)
