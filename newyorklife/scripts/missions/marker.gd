class_name Marker
extends Node3D
## Marqueur 3D au sol : colonne lumineuse translucide + losange flottant.

var _diamond: MeshInstance3D
var _t := 0.0


static func create(color: Color, radius := 1.6, height := 2.2) -> Marker:
	var m := Marker.new()
	m._build(color, radius, height)
	return m


func _build(color: Color, radius: float, height: float) -> void:
	var tube := CylinderMesh.new()
	tube.top_radius = radius
	tube.bottom_radius = radius
	tube.height = height
	tube.cap_top = false
	tube.cap_bottom = false
	tube.radial_segments = 18
	tube.rings = 1
	var c := color
	c.a = 0.28
	tube.material = Mats.marker(c)
	var mi := MeshInstance3D.new()
	mi.mesh = tube
	mi.position = Vector3(0, height * 0.5, 0)
	mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(mi)
	var ring := CylinderMesh.new()
	ring.top_radius = radius
	ring.bottom_radius = radius
	ring.height = 0.05
	ring.radial_segments = 18
	var c2 := color
	c2.a = 0.5
	ring.material = Mats.marker(c2)
	var rmi := MeshInstance3D.new()
	rmi.mesh = ring
	rmi.position = Vector3(0, 0.04, 0)
	rmi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(rmi)
	var dm := PrismMesh.new()
	dm.size = Vector3(0.5, 0.6, 0.5)
	dm.material = Mats.emissive(Color(color.r, color.g, color.b), 1.5)
	_diamond = MeshInstance3D.new()
	_diamond.mesh = dm
	_diamond.rotation = Vector3(PI, 0, 0)
	_diamond.position = Vector3(0, height + 0.8, 0)
	add_child(_diamond)


func _process(delta: float) -> void:
	_t += delta
	_diamond.position.y = 3.0 + sin(_t * 2.5) * 0.2
	_diamond.rotation.y = _t * 1.5
