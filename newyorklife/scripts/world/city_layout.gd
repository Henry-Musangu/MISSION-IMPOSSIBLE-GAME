class_name CityLayout
extends RefCounted
## Plan du secteur de Midtown Manhattan : grille d'avenues (axe Z, nord = −Z)
## et de rues (axe X, est = +X). Toutes les coordonnées sont en mètres.

const AVE_COUNT := 5
const ST_COUNT := 7
const AVE_SPACING := 100.0
const ST_SPACING := 70.0
const AVE_HALF := 8.0          ## demi-largeur de chaussée d'une avenue
const ST_HALF := 6.0           ## demi-largeur de chaussée d'une rue
const SIDEWALK := 4.0
const CURB_H := 0.15
const AVE_LANE := 2.2          ## décalage de la voie (voie intérieure) depuis l'axe
const ST_LANE := 2.4
const BACKDROP_DEPTH := 36.0   ## bande d'immeubles qui ferme le secteur
const AVE_SPEED := 13.0        ## m/s (~47 km/h)
const ST_SPEED := 10.0

## Avenues d'ouest en est, rues du nord au sud.
const AVE_NAMES := ["8th", "7th", "Broadway", "6th", "5th"]
const AVE_NAMES_FR := ["8e Avenue", "7e Avenue", "Broadway", "6e Avenue", "5e Avenue"]
const ST_NUMBERS := [52, 50, 48, 46, 44, 42, 40]

## Blocs spéciaux (i, j)
const PARK_BLOCK := Vector2i(2, 3)
const TIMES_SQUARE := Vector2i(1, 4)   ## intersection (avenue, rue)

## Lieux : bloc, côté (0 = ouest, 1 = est, 2 = nord, 3 = sud), position le long du côté (0..1)
const LOCATIONS := {
	"terminal": {"block": Vector2i(0, 5), "side": 0, "t": 0.5, "name": "loc_terminal"},
	"garage": {"block": Vector2i(3, 1), "side": 3, "t": 0.35, "name": "loc_garage"},
	"deli": {"block": Vector2i(0, 2), "side": 0, "t": 0.5, "name": "loc_deli"},
	"times": {"block": Vector2i(1, 3), "side": 3, "t": 0.15, "name": "loc_times"},
	"park": {"block": Vector2i(2, 3), "side": 0, "t": 0.5, "name": "loc_park"},
	"dealer": {"block": Vector2i(3, 4), "side": 1, "t": 0.5, "name": "loc_dealer"},
	"clothes": {"block": Vector2i(2, 4), "side": 3, "t": 0.5, "name": "loc_clothes"},
	"apartment": {"block": Vector2i(1, 0), "side": 3, "t": 0.6, "name": "loc_apartment"},
	"police": {"block": Vector2i(0, 0), "side": 1, "t": 0.5, "name": "loc_police"},
	"hospital": {"block": Vector2i(3, 2), "side": 1, "t": 0.5, "name": "loc_hospital"},
	"metro_n": {"block": Vector2i(2, 0), "side": 3, "t": 0.2, "name": "loc_metro_n"},
	"metro_s": {"block": Vector2i(1, 5), "side": 2, "t": 0.8, "name": "loc_metro_s"},
	"gas": {"block": Vector2i(0, 4), "side": 3, "t": 0.5, "name": "loc_gas"},
	"office": {"block": Vector2i(2, 2), "side": 2, "t": 0.5, "name": "loc_office"},
	"hotel": {"block": Vector2i(1, 2), "side": 1, "t": 0.5, "name": "loc_hotel"},
}


static func ave_x(i: int) -> float:
	return i * AVE_SPACING


static func st_z(j: int) -> float:
	return j * ST_SPACING


static func node_pos(n: Vector2i) -> Vector3:
	return Vector3(ave_x(n.x), 0.0, st_z(n.y))


static func city_min() -> Vector2:
	return Vector2(-AVE_HALF - SIDEWALK, -ST_HALF - SIDEWALK)


static func city_max() -> Vector2:
	return Vector2(ave_x(AVE_COUNT - 1) + AVE_HALF + SIDEWALK, st_z(ST_COUNT - 1) + ST_HALF + SIDEWALK)


static func is_inside_city(p: Vector3) -> bool:
	var mn := city_min()
	var mx := city_max()
	return p.x > mn.x and p.x < mx.x and p.z > mn.y and p.z < mx.y


static func block_count() -> Vector2i:
	return Vector2i(AVE_COUNT - 1, ST_COUNT - 1)


## Rectangle d'un bloc (trottoir compris) : Rect2 en (x, z).
static func block_rect(b: Vector2i) -> Rect2:
	var x0 := ave_x(b.x) + AVE_HALF
	var x1 := ave_x(b.x + 1) - AVE_HALF
	var z0 := st_z(b.y) + ST_HALF
	var z1 := st_z(b.y + 1) - ST_HALF
	return Rect2(x0, z0, x1 - x0, z1 - z0)


static func block_center(b: Vector2i) -> Vector3:
	var r := block_rect(b)
	return Vector3(r.get_center().x, 0.0, r.get_center().y)


## Point sur le trottoir d'un bloc (à 1,8 m de la bordure).
static func block_side_point(b: Vector2i, side: int, t: float, inset: float = 1.8) -> Vector3:
	var r := block_rect(b)
	match side:
		0:
			return Vector3(r.position.x + inset, CURB_H, lerpf(r.position.y + 6.0, r.end.y - 6.0, t))
		1:
			return Vector3(r.end.x - inset, CURB_H, lerpf(r.position.y + 6.0, r.end.y - 6.0, t))
		2:
			return Vector3(lerpf(r.position.x + 6.0, r.end.x - 6.0, t), CURB_H, r.position.y + inset)
		_:
			return Vector3(lerpf(r.position.x + 6.0, r.end.x - 6.0, t), CURB_H, r.end.y - inset)


## Direction extérieure (vers la chaussée) d'un côté de bloc.
static func side_normal(side: int) -> Vector3:
	match side:
		0: return Vector3.LEFT
		1: return Vector3.RIGHT
		2: return Vector3.FORWARD
		_: return Vector3.BACK


static func location_pos(id: String) -> Vector3:
	if not LOCATIONS.has(id):
		return Vector3(ave_x(0), CURB_H, st_z(3))
	var l: Dictionary = LOCATIONS[id]
	return block_side_point(l["block"], l["side"], l["t"])


## Point sur la chaussée devant un lieu (pour garer / faire apparaître un véhicule).
static func location_road_pos(id: String) -> Vector3:
	var l: Dictionary = LOCATIONS[id]
	var p := location_pos(id)
	var n := side_normal(l["side"])
	return Vector3(p.x, 0.0, p.z) + n * 3.1


## Orientation (yaw) d'un véhicule garé devant un lieu, dans le sens de la circulation.
static func location_road_yaw(id: String) -> float:
	var l: Dictionary = LOCATIONS[id]
	var side: int = l["side"]
	# Circulation à droite : le long d'un côté ouest on roule vers le sud (+Z), etc.
	var dir: Vector3
	match side:
		0: dir = Vector3.FORWARD
		1: dir = Vector3.BACK
		2: dir = Vector3.RIGHT
		_: dir = Vector3.LEFT
	return atan2(dir.x, dir.z)


static func location_name(id: String) -> String:
	if LOCATIONS.has(id):
		return Loc.t(LOCATIONS[id]["name"])
	return id


static func avenue_name(i: int) -> String:
	if Loc.lang == "en":
		var n: String = AVE_NAMES[i]
		return n if n == "Broadway" else n + " Avenue"
	return AVE_NAMES_FR[i]


static func street_name(j: int) -> String:
	var n: int = ST_NUMBERS[j]
	if Loc.lang == "en":
		return "%d%s Street" % [n, "nd" if n % 10 == 2 else "th"]
	return "%de Rue" % n


## Nom de la voie la plus proche d'un point.
static func road_name_at(p: Vector3) -> String:
	var i := clampi(int(round(p.x / AVE_SPACING)), 0, AVE_COUNT - 1)
	var j := clampi(int(round(p.z / ST_SPACING)), 0, ST_COUNT - 1)
	var dx := absf(p.x - ave_x(i))
	var dz := absf(p.z - st_z(j))
	if dx < dz:
		return avenue_name(i)
	return street_name(j)


static func nearest_node(p: Vector3) -> Vector2i:
	return Vector2i(clampi(int(round(p.x / AVE_SPACING)), 0, AVE_COUNT - 1),
		clampi(int(round(p.z / ST_SPACING)), 0, ST_COUNT - 1))


static func is_valid_node(n: Vector2i) -> bool:
	return n.x >= 0 and n.y >= 0 and n.x < AVE_COUNT and n.y < ST_COUNT


static func neighbors(n: Vector2i) -> Array[Vector2i]:
	var out: Array[Vector2i] = []
	for d in [Vector2i(1, 0), Vector2i(-1, 0), Vector2i(0, 1), Vector2i(0, -1)]:
		var m: Vector2i = n + d
		if is_valid_node(m):
			out.append(m)
	return out


## Distance entre l'axe de l'intersection et la ligne d'arrêt, selon l'axe de déplacement.
static func stop_offset(dir: Vector2i) -> float:
	if dir.x != 0:
		return AVE_HALF + 3.0   # on roule sur une rue (axe X), on croise une avenue
	return ST_HALF + 3.0


static func lane_offset(dir: Vector2i) -> float:
	return ST_LANE if dir.x != 0 else AVE_LANE


static func dir3(dir: Vector2i) -> Vector3:
	return Vector3(dir.x, 0.0, dir.y)


## Vecteur « droite du conducteur » pour une direction de déplacement.
static func right_of(d: Vector3) -> Vector3:
	return d.cross(Vector3.UP)


## Début et fin de la voie (côté droit) entre deux intersections voisines.
static func lane_points(a: Vector2i, b: Vector2i) -> Array[Vector3]:
	var dir := b - a
	var d := dir3(dir)
	var r := right_of(d) * lane_offset(dir)
	var off := stop_offset(dir)
	var start := node_pos(a) + d * off + r
	var end := node_pos(b) - d * off + r
	return [start, end]


static func road_speed(dir: Vector2i) -> float:
	return ST_SPEED if dir.x != 0 else AVE_SPEED


## Chemin le plus court (BFS) sur la grille d'intersections.
static func path_nodes(from: Vector2i, to: Vector2i) -> Array[Vector2i]:
	var prev := {}
	var queue: Array[Vector2i] = [from]
	prev[from] = from
	while not queue.is_empty():
		var n: Vector2i = queue.pop_front()
		if n == to:
			break
		for m in neighbors(n):
			if not prev.has(m):
				prev[m] = n
				queue.append(m)
	var path: Array[Vector2i] = []
	if not prev.has(to):
		return path
	var cur := to
	while cur != from:
		path.push_front(cur)
		cur = prev[cur]
	path.push_front(from)
	return path


## Vrai si le point est sur une chaussée (et pas sur un trottoir ou un bloc).
static func is_on_road(p: Vector3) -> bool:
	for i in AVE_COUNT:
		if absf(p.x - ave_x(i)) <= AVE_HALF:
			return p.z > -ST_HALF and p.z < st_z(ST_COUNT - 1) + ST_HALF
	for j in ST_COUNT:
		if absf(p.z - st_z(j)) <= ST_HALF:
			return p.x > -AVE_HALF and p.x < ave_x(AVE_COUNT - 1) + AVE_HALF
	return false
