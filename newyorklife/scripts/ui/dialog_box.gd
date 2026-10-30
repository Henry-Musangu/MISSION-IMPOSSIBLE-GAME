class_name DialogBox
extends Control
## Dialogues (touchez pour continuer) et menus de choix (missions, commerces, décisions).

signal opened()
signal closed()

var _panel: PanelContainer
var _speaker: Label
var _text: Label
var _tap: Label
var _choices: VBoxContainer
var _lines: Array = []
var _line := 0
var _on_done: Callable
var _on_pick: Callable
var _mode := ""
var _reveal := 0.0
var _scroll: ScrollContainer
var auto_advance := false     ## test automatique : valide tout sans attendre


func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	theme = UITheme.get_theme()
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	visible = false
	process_mode = Node.PROCESS_MODE_ALWAYS
	_panel = PanelContainer.new()
	_panel.anchor_left = 0.5
	_panel.anchor_right = 0.5
	_panel.anchor_top = 1.0
	_panel.anchor_bottom = 1.0
	_panel.offset_left = -470
	_panel.offset_right = 470
	_panel.offset_top = -250
	_panel.offset_bottom = -24
	_panel.add_theme_stylebox_override("panel", UITheme.box(UITheme.BG_SOLID, 14, 2, UITheme.ACCENT, 22))
	add_child(_panel)
	var v := VBoxContainer.new()
	_panel.add_child(v)
	_speaker = UITheme.label("", 24, UITheme.ACCENT)
	v.add_child(_speaker)
	_text = UITheme.label("", 23)
	_text.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_text.size_flags_vertical = Control.SIZE_EXPAND_FILL
	v.add_child(_text)
	_scroll = ScrollContainer.new()
	_scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	_scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	v.add_child(_scroll)
	_choices = VBoxContainer.new()
	_choices.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	_scroll.add_child(_choices)
	_tap = UITheme.label(Loc.t("continue_tap"), 18, UITheme.MUTED)
	_tap.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	v.add_child(_tap)
	Events.dialog_requested.connect(show_dialog)
	Events.choice_requested.connect(show_choice)


func is_open() -> bool:
	return visible


func show_dialog(lines: Array, on_done: Callable) -> void:
	if auto_advance:
		on_done.call_deferred()
		return
	_mode = "dialog"
	_lines = lines
	_line = 0
	_on_done = on_done
	_clear_choices()
	_scroll.visible = false
	_text.visible = true
	_tap.visible = true
	_panel.offset_top = -230
	_show_line()
	_open()


func show_choice(title: String, options: Array, on_pick: Callable) -> void:
	if auto_advance:
		on_pick.call_deferred(0)
		return
	_mode = "choice"
	_on_pick = on_pick
	_speaker.text = ""
	_text.text = title
	_text.visible_ratio = 1.0
	_text.visible = true
	_tap.visible = false
	_clear_choices()
	_scroll.visible = true
	for i in options.size():
		var idx := i
		var b := UITheme.button(str(options[i]), func(): _pick(idx), 600, 22)
		b.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		# évite qu'un appui encore en cours ne valide immédiatement un choix
		b.disabled = true
		get_tree().create_timer(0.35, true).timeout.connect(_enable_button.bind(b))
		_choices.add_child(b)
	var rows := mini(options.size(), 5)
	var lines_h := 40 + int(title.length() / 60.0) * 28
	_panel.offset_top = -(lines_h + 110 + rows * 68)
	_panel.offset_top = maxf(_panel.offset_top, -get_viewport_rect().size.y + 30)
	_open()


func _enable_button(b: Button) -> void:
	if is_instance_valid(b):
		b.disabled = false


func _open() -> void:
	visible = true
	InputState.enabled = false
	InputState.clear()
	opened.emit()


func _close() -> void:
	visible = false
	InputState.enabled = true
	InputState.clear()
	closed.emit()


func _clear_choices() -> void:
	for c in _choices.get_children():
		c.queue_free()


func _show_line() -> void:
	var l: Array = _lines[_line]
	_speaker.text = str(l[0])
	_text.text = str(l[1])
	_reveal = 0.0
	_text.visible_ratio = 0.0


func _process(delta: float) -> void:
	if visible and _mode == "dialog" and _text.visible_ratio < 1.0:
		_reveal += delta * 60.0
		_text.visible_ratio = clampf(_reveal / maxf(_text.text.length(), 1.0), 0.0, 1.0)


func _gui_input(_e: InputEvent) -> void:
	pass


func _input(event: InputEvent) -> void:
	if not visible or _mode != "dialog":
		return
	var tapped := false
	if event is InputEventScreenTouch and event.pressed:
		tapped = true
	elif event is InputEventKey and event.pressed and not event.echo and event.physical_keycode in [KEY_SPACE, KEY_ENTER, KEY_E]:
		tapped = true
	elif event is InputEventJoypadButton and event.pressed:
		tapped = true
	if not tapped:
		return
	get_viewport().set_input_as_handled()
	if _text.visible_ratio < 1.0:
		_text.visible_ratio = 1.0
		_reveal = 9999.0
		return
	_line += 1
	if _line >= _lines.size():
		_close()
		if _on_done.is_valid():
			_on_done.call()
	else:
		Sfx.play("click", -12.0)
		_show_line()


func _pick(idx: int) -> void:
	_close()
	if _on_pick.is_valid():
		_on_pick.call(idx)
