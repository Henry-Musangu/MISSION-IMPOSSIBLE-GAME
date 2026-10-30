class_name HumanoidRig
extends Node3D
## Personnage articulé en primitives (tête, torse, bras, jambes) animé de façon procédurale :
## marche, course, saut, arrêt, conduite, chute. Avant du personnage = +Z.

const OUTFITS := {
	"outfit_default": {"top": Color(0.16, 0.3, 0.6), "legs": Color(0.15, 0.17, 0.22), "price": 0},
	"outfit_red": {"top": Color(0.7, 0.12, 0.1), "legs": Color(0.12, 0.12, 0.14), "price": 90},
	"outfit_leather": {"top": Color(0.18, 0.12, 0.08), "legs": Color(0.2, 0.25, 0.35), "price": 220},
	"outfit_suit": {"top": Color(0.38, 0.39, 0.42), "legs": Color(0.32, 0.33, 0.36), "price": 450},
	"outfit_sport": {"top": Color(0.1, 0.45, 0.25), "legs": Color(0.08, 0.3, 0.17), "price": 120},
	"outfit_white": {"top": Color(0.92, 0.92, 0.9), "legs": Color(0.1, 0.1, 0.12), "price": 70},
}

const SKINS := [Color(0.36, 0.22, 0.14), Color(0.55, 0.38, 0.26), Color(0.8, 0.62, 0.48), Color(0.93, 0.78, 0.66), Color(0.25, 0.16, 0.1)]

static var _meshes := {}

var hips: Node3D
var torso: Node3D
var head: Node3D
var arm_l: Node3D
var arm_r: Node3D
var leg_l: Node3D
var leg_r: Node3D
var _phase := 0.0
var _top_mat: StandardMaterial3D
var _legs_mat: StandardMaterial3D
var _skin_mat: StandardMaterial3D
var scale_factor := 1.0
var fallen := 0.0         ## 0 debout, 1 allongé
var sitting := false


static func _mesh(key: String) -> Mesh:
	if _meshes.has(key):
		return _meshes[key]
	var m: Mesh
	match key:
		"torso":
			var b := BoxMesh.new()
			b.size = Vector3(0.46, 0.62, 0.26)
			m = b
		"head":
			var s := SphereMesh.new()
			s.radius = 0.14
			s.height = 0.3
			s.radial_segments = 10
			s.rings = 6
			m = s
		"hair":
			var s2 := SphereMesh.new()
			s2.radius = 0.15
			s2.height = 0.18
			s2.radial_segments = 10
			s2.rings = 4
			s2.is_hemisphere = true
			m = s2
		"limb":
			var c := CapsuleMesh.new()
			c.radius = 0.075
			c.height = 0.66
			c.radial_segments = 6
			c.rings = 2
			m = c
		"leg":
			var c2 := CapsuleMesh.new()
			c2.radius = 0.095
			c2.height = 0.86
			c2.radial_segments = 6
			c2.rings = 2
			m = c2
		"shoe":
			var b2 := BoxMesh.new()
			b2.size = Vector3(0.13, 0.08, 0.26)
			m = b2
	_meshes[key] = m
	return m


func build(top: Color, legs: Color, skin: Color, hair_col := Color(0.06, 0.05, 0.05)) -> void:
	_top_mat = StandardMaterial3D.new()
	_top_mat.albedo_color = top
	_top_mat.roughness = 0.9
	_legs_mat = StandardMaterial3D.new()
	_legs_mat.albedo_color = legs
	_legs_mat.roughness = 0.9
	_skin_mat = StandardMaterial3D.new()
	_skin_mat.albedo_color = skin
	_skin_mat.roughness = 0.7
	var hair_mat := Mats.solid(hair_col, 0.95)
	var shoe_mat := Mats.solid(Color(0.08, 0.08, 0.08), 0.6)
	hips = Node3D.new()
	hips.position = Vector3(0, 0.92, 0)
	add_child(hips)
	torso = Node3D.new()
	hips.add_child(torso)
	_part(torso, "torso", _top_mat, Vector3(0, 0.33, 0))
	head = Node3D.new()
	head.position = Vector3(0, 0.78, 0)
	torso.add_child(head)
	_part(head, "head", _skin_mat, Vector3.ZERO)
	_part(head, "hair", hair_mat, Vector3(0, 0.04, -0.01))
	arm_l = _limb(torso, Vector3(0.3, 0.6, 0), "limb", _top_mat, 0.32)
	arm_r = _limb(torso, Vector3(-0.3, 0.6, 0), "limb", _top_mat, 0.32)
	_part(arm_l, "head", _skin_mat, Vector3(0, -0.66, 0), 0.45)
	_part(arm_r, "head", _skin_mat, Vector3(0, -0.66, 0), 0.45)
	leg_l = _limb(hips, Vector3(0.11, 0.0, 0), "leg", _legs_mat, 0.43)
	leg_r = _limb(hips, Vector3(-0.11, 0.0, 0), "leg", _legs_mat, 0.43)
	_part(leg_l, "shoe", shoe_mat, Vector3(0, -0.88, 0.05))
	_part(leg_r, "shoe", shoe_mat, Vector3(0, -0.88, 0.05))


func _part(parent: Node3D, key: String, mat: Material, pos: Vector3, s := 1.0) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	mi.mesh = _mesh(key)
	mi.material_override = mat
	mi.position = pos
	if s != 1.0:
		mi.scale = Vector3(s, s, s)
	parent.add_child(mi)
	return mi


func _limb(parent: Node3D, pivot: Vector3, key: String, mat: Material, half: float) -> Node3D:
	var p := Node3D.new()
	p.position = pivot
	parent.add_child(p)
	_part(p, key, mat, Vector3(0, -half, 0))
	return p


func set_outfit(id: String) -> void:
	var o: Dictionary = OUTFITS.get(id, OUTFITS["outfit_default"])
	_top_mat.albedo_color = o["top"]
	_legs_mat.albedo_color = o["legs"]


func set_visibility_range(dist: float) -> void:
	for mi in find_children("*", "MeshInstance3D", true, false):
		(mi as MeshInstance3D).visibility_range_end = dist


## speed en m/s ; airborne pour la pose de saut.
func animate(delta: float, speed: float, airborne: bool) -> void:
	if fallen > 0.0:
		rotation.x = lerpf(rotation.x, -PI / 2.0 * fallen, delta * 8.0)
		position.y = lerpf(position.y, 0.15 * fallen, delta * 8.0)
		_relax(delta)
		return
	rotation.x = lerpf(rotation.x, 0.0, delta * 8.0)
	position.y = lerpf(position.y, 0.0, delta * 8.0)
	if sitting:
		leg_l.rotation.x = -1.4
		leg_r.rotation.x = -1.4
		arm_l.rotation.x = -1.0
		arm_r.rotation.x = -1.0
		hips.position.y = 0.5
		return
	hips.position.y = 0.92
	if airborne:
		leg_l.rotation.x = lerpf(leg_l.rotation.x, -0.7, delta * 10.0)
		leg_r.rotation.x = lerpf(leg_r.rotation.x, 0.3, delta * 10.0)
		arm_l.rotation.x = lerpf(arm_l.rotation.x, -2.4, delta * 10.0)
		arm_r.rotation.x = lerpf(arm_r.rotation.x, -2.4, delta * 10.0)
		return
	if speed < 0.15:
		_relax(delta)
		_phase = 0.0
		# respiration
		torso.position.y = sin(Time.get_ticks_msec() * 0.003) * 0.008
		return
	var stride := clampf(speed / 7.0, 0.25, 1.0)
	_phase += delta * (4.0 + speed * 1.25)
	var s := sin(_phase)
	var amp := 0.45 + stride * 0.5
	leg_l.rotation.x = s * amp
	leg_r.rotation.x = -s * amp
	arm_l.rotation.x = -s * amp * 0.8
	arm_r.rotation.x = s * amp * 0.8
	arm_l.rotation.z = 0.08
	arm_r.rotation.z = -0.08
	torso.rotation.x = stride * 0.18
	torso.position.y = absf(cos(_phase)) * 0.05 * stride


func _relax(delta: float) -> void:
	for limb in [leg_l, leg_r, arm_l, arm_r]:
		limb.rotation.x = lerpf(limb.rotation.x, 0.0, delta * 8.0)
	arm_l.rotation.z = lerpf(arm_l.rotation.z, 0.06, delta * 8.0)
	arm_r.rotation.z = lerpf(arm_r.rotation.z, -0.06, delta * 8.0)
	torso.rotation.x = lerpf(torso.rotation.x, 0.0, delta * 8.0)


## Geste d'interaction (bras tendu).
func wave() -> void:
	var tw := create_tween()
	tw.tween_property(arm_r, "rotation:x", -1.6, 0.15)
	tw.tween_property(arm_r, "rotation:x", 0.0, 0.3)
