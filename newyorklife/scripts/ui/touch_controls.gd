class_name TouchControls
extends Control
## Commandes tactiles multipoints : joystick flottant à gauche, zone caméra à droite,
## boutons contextuels (à pied / en voiture), bouton pause. Taille et opacité réglables.

var driving := false
var interact_label := ""
var _joy_index := -1
var _joy_origin := Vector2.ZERO
var _joy_pos := Vector2.ZERO
var _look_index := -1
var _look_last := Vector2.ZERO
var _button_touches := {}       ## index -> nom du bouton
var _buttons: Array = []        ## [{name, label, pos, radius}]
var pause_requested := false


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	Events.language_changed.connect(queue_redraw)
	resized.connect(_layout)
	_layout()


func set_driving(on: bool) -> void:
	if on == driving:
		return
	driving = on
	_release_all()
	_layout()


func _unit() -> float:
	return minf(size.x, size.y * 1.6) / 1280.0 * 1.0 * Settings.touch_scale


func _layout() -> void:
	var u := _unit()
	var w := size.x
	var h := size.y
	_buttons.clear()
	var r := 62.0 * u
	var pad := 36.0 * u
	if driving:
		_buttons.append({"name": "gas", "label": "btn_gas", "pos": Vector2(w - pad - r, h - pad - r), "radius": r * 1.15})
		_buttons.append({"name": "brake", "label": "btn_brake", "pos": Vector2(w - pad - r * 3.4, h - pad - r * 0.85), "radius": r})
		_buttons.append({"name": "handbrake", "label": "btn_handbrake", "pos": Vector2(w - pad - r * 1.1, h - pad - r * 3.4), "radius": r * 0.85})
		_buttons.append({"name": "horn", "label": "btn_horn", "pos": Vector2(w - pad - r * 3.3, h - pad - r * 3.0), "radius": r * 0.7})
		_buttons.append({"name": "interact", "label": "btn_exit", "pos": Vector2(w - pad - r * 0.8, h * 0.36), "radius": r * 0.7})
	else:
		_buttons.append({"name": "jump", "label": "btn_jump", "pos": Vector2(w - pad - r, h - pad - r), "radius": r * 1.05})
		_buttons.append({"name": "sprint", "label": "btn_sprint", "pos": Vector2(w - pad - r * 3.3, h - pad - r * 0.8), "radius": r * 0.9})
		_buttons.append({"name": "interact", "label": "btn_action", "pos": Vector2(w - pad - r * 1.0, h - pad - r * 3.3), "radius": r * 0.9})
	_buttons.append({"name": "pause", "label": "II", "pos": Vector2(w - 44.0 * u, 44.0 * u), "radius": 30.0 * u})
	queue_redraw()


func _release_all() -> void:
	for idx in _button_touches:
		InputState.set_touch_button(_button_touches[idx], false)
	_button_touches.clear()
	_joy_index = -1
	_look_index = -1
	InputState.touch_move = Vector2.ZERO
	queue_redraw()


func _hit_button(p: Vector2) -> String:
	for b in _buttons:
		if p.distance_to(b["pos"]) <= float(b["radius"]) * 1.15:
			return b["name"]
	return ""


func _input(event: InputEvent) -> void:
	if not visible or not is_visible_in_tree():
		return
	if event is InputEventScreenTouch:
		var p: Vector2 = event.position
		if event.pressed:
			var bn := _hit_button(p)
			if bn != "":
				if bn == "pause":
					pause_requested = true
				else:
					_button_touches[event.index] = bn
					InputState.set_touch_button(bn, true)
			elif p.x < size.x * 0.42 and p.y > size.y * 0.3:
				if _joy_index < 0:
					_joy_index = event.index
					_joy_origin = p
					_joy_pos = p
			else:
				if _look_index < 0:
					_look_index = event.index
					_look_last = p
			queue_redraw()
		else:
			if _button_touches.has(event.index):
				InputState.set_touch_button(_button_touches[event.index], false)
				_button_touches.erase(event.index)
			if event.index == _joy_index:
				_joy_index = -1
				InputState.touch_move = Vector2.ZERO
			if event.index == _look_index:
				_look_index = -1
			queue_redraw()
	elif event is InputEventScreenDrag:
		var p2: Vector2 = event.position
		if event.index == _joy_index:
			var radius := 80.0 * _unit()
			var d := p2 - _joy_origin
			if d.length() > radius:
				# le socle suit le doigt au-delà du rayon
				_joy_origin = p2 - d.normalized() * radius
				d = p2 - _joy_origin
			_joy_pos = p2
			var v := d / radius
			InputState.touch_move = Vector2(v.x, -v.y).limit_length(1.0)
			queue_redraw()
		elif event.index == _look_index:
			InputState.add_look((p2 - _look_last) * 1.4)
			_look_last = p2
		elif _button_touches.has(event.index):
			# glisser hors du bouton le relâche
			var bn: String = _button_touches[event.index]
			for b in _buttons:
				if b["name"] == bn and p2.distance_to(b["pos"]) > float(b["radius"]) * 1.6:
					InputState.set_touch_button(bn, false)
					_button_touches.erase(event.index)
					queue_redraw()
					break


func _draw() -> void:
	var a := Settings.touch_opacity
	var u := _unit()
	var font := get_theme_default_font()
	# Joystick
	var jr := 80.0 * u
	if _joy_index >= 0:
		draw_circle(_joy_origin, jr, Color(1, 1, 1, 0.12 * a * 2.0))
		draw_arc(_joy_origin, jr, 0, TAU, 40, Color(1, 1, 1, 0.5 * a), 3.0 * u)
		draw_circle(_joy_pos, jr * 0.42, Color(1, 1, 1, 0.55 * a))
	else:
		var home := Vector2(60.0 * u + jr, size.y - 60.0 * u - jr)
		draw_arc(home, jr, 0, TAU, 40, Color(1, 1, 1, 0.3 * a), 3.0 * u)
		draw_circle(home, jr * 0.42, Color(1, 1, 1, 0.25 * a))
	for b in _buttons:
		var pressed: bool = b["name"] in _button_touches.values()
		var r: float = b["radius"]
		var c := Color(0.96, 0.7, 0.2, 0.75 * a) if pressed else Color(0.08, 0.09, 0.11, 0.55 * a)
		draw_circle(b["pos"], r, c)
		draw_arc(b["pos"], r, 0, TAU, 36, Color(1, 1, 1, 0.6 * a), 2.5 * u)
		var text: String = b["label"] if b["name"] == "pause" else Loc.t(b["label"])
		var fs := int(clampf(r * 0.34, 12.0, 30.0))
		var lines := text.split("\n")
		var total_h := fs * lines.size()
		for li in lines.size():
			var line: String = lines[li]
			var ts := font.get_string_size(line, HORIZONTAL_ALIGNMENT_CENTER, -1, fs)
			var pos: Vector2 = b["pos"] + Vector2(-ts.x * 0.5, -total_h * 0.5 + fs * (li + 0.8))
			draw_string(font, pos, line, HORIZONTAL_ALIGNMENT_LEFT, -1, fs, Color(1, 1, 1, minf(1.0, a * 1.6)))
