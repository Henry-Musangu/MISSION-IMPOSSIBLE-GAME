class_name Menus
extends Control
## Écran d'accueil, menu pause (carte, missions, inventaire, garage, immobilier),
## paramètres et crédits. Construits par le code, textes FR/EN.

var world: Node
var _root: Control
var _screen := ""
var _from := ""
var _map: Minimap


func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	theme = UITheme.get_theme()
	process_mode = Node.PROCESS_MODE_ALWAYS
	mouse_filter = Control.MOUSE_FILTER_STOP
	Events.language_changed.connect(func(): show_screen(_screen))


func show_screen(screen: String) -> void:
	if _root:
		_root.queue_free()
		_root = null
	_screen = screen
	visible = screen != ""
	if screen == "":
		return
	_root = Control.new()
	_root.set_anchors_preset(Control.PRESET_FULL_RECT)
	_root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(_root)
	match screen:
		"main": _build_main()
		"pause": _build_pause()
		"settings": _build_settings()
		"credits": _build_credits()
		"map": _build_map()
		"missions": _build_missions()
		"inventory": _build_inventory()
		"garage": _build_garage()
		"housing": _build_housing()


func current() -> String:
	return _screen


func _dim(alpha := 0.55) -> void:
	var bg := ColorRect.new()
	bg.color = Color(0, 0, 0, alpha)
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	_root.add_child(bg)


func _panel(width := 560.0, center_x := 0.5) -> VBoxContainer:
	var p := PanelContainer.new()
	p.anchor_left = center_x
	p.anchor_right = center_x
	p.anchor_top = 0.5
	p.anchor_bottom = 0.5
	p.offset_left = -width * 0.5
	p.offset_right = width * 0.5
	p.grow_vertical = Control.GROW_DIRECTION_BOTH
	p.add_theme_stylebox_override("panel", UITheme.box(UITheme.BG_SOLID, 16, 0, UITheme.ACCENT, 26))
	_root.add_child(p)
	var v := VBoxContainer.new()
	v.add_theme_constant_override("separation", 12)
	p.add_child(v)
	return v


func _title(parent: Container, text: String, size := 34) -> void:
	var l := UITheme.label(text, size, UITheme.ACCENT)
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	parent.add_child(l)


func _back_button(parent: Container) -> void:
	parent.add_child(UITheme.button(Loc.t("menu_back"), func(): show_screen(_from if _from != "" else "pause")))


# ---------------------------------------------------------------- accueil

func _build_main() -> void:
	_from = "main"
	var shade := ColorRect.new()
	shade.color = Color(0, 0, 0, 0.35)
	shade.set_anchors_preset(Control.PRESET_FULL_RECT)
	_root.add_child(shade)
	var left := VBoxContainer.new()
	left.anchor_top = 0.0
	left.anchor_bottom = 1.0
	left.offset_left = 70
	left.offset_right = 520
	left.alignment = BoxContainer.ALIGNMENT_CENTER
	left.add_theme_constant_override("separation", 12)
	_root.add_child(left)
	var t := UITheme.label(Loc.t("game_title"), 64, UITheme.TEXT)
	left.add_child(t)
	var st := UITheme.label(Loc.t("game_subtitle"), 26, UITheme.ACCENT)
	left.add_child(st)
	var spacer := Control.new()
	spacer.custom_minimum_size = Vector2(0, 18)
	left.add_child(spacer)
	var cont := UITheme.button(Loc.t("menu_continue"), func(): world.continue_game())
	cont.disabled = not GameState.has_save()
	left.add_child(cont)
	left.add_child(UITheme.button(Loc.t("menu_new"), func(): world.new_game()))
	left.add_child(UITheme.button(Loc.t("menu_settings"), func():
		_from = "main"
		show_screen("settings")
	))
	left.add_child(UITheme.button(Loc.t("menu_language"), _toggle_language))
	left.add_child(UITheme.button(Loc.t("menu_credits"), func():
		_from = "main"
		show_screen("credits")
	))
	if OS.get_name() != "Web":
		left.add_child(UITheme.button(Loc.t("menu_quit"), func(): get_tree().quit()))
	var ver := UITheme.label("v" + str(ProjectSettings.get_setting("application/config/version", "0.1.0")), 16, UITheme.MUTED)
	ver.anchor_top = 1.0
	ver.anchor_bottom = 1.0
	ver.anchor_left = 1.0
	ver.anchor_right = 1.0
	ver.offset_left = -120
	ver.offset_top = -36
	_root.add_child(ver)


func _toggle_language() -> void:
	Loc.set_language("en" if Loc.lang == "fr" else "fr")
	Settings.save_settings()


func _build_credits() -> void:
	_dim(0.6)
	var v := _panel(720)
	_title(v, Loc.t("menu_credits"))
	var l := UITheme.label(Loc.t("credits_text"), 20)
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	l.custom_minimum_size = Vector2(640, 0)
	v.add_child(l)
	_back_button(v)


# ---------------------------------------------------------------- pause

func _build_pause() -> void:
	_from = "pause"
	_dim()
	var v := _panel(460, 0.5)
	_title(v, Loc.t("pause_title"))
	var grid := GridContainer.new()
	grid.columns = 2
	grid.add_theme_constant_override("h_separation", 10)
	grid.add_theme_constant_override("v_separation", 10)
	v.add_child(grid)
	var items := [
		["pause_resume", func(): world.resume()],
		["pause_map", func(): show_screen("map")],
		["pause_missions", func(): show_screen("missions")],
		["pause_inventory", func(): show_screen("inventory")],
		["pause_garage", func(): show_screen("garage")],
		["pause_housing", func(): show_screen("housing")],
		["pause_settings", func(): show_screen("settings")],
		["pause_save", func(): world.manual_save()],
	]
	for it in items:
		var b := UITheme.button(Loc.t(it[0]), it[1], 200, 21)
		grid.add_child(b)
	v.add_child(UITheme.button(Loc.t("pause_quit"), func(): world.quit_to_menu(), 410, 21))


func _build_map() -> void:
	_dim(0.75)
	var holder := PanelContainer.new()
	holder.set_anchors_preset(Control.PRESET_FULL_RECT)
	holder.offset_left = 40
	holder.offset_right = -40
	holder.offset_top = 30
	holder.offset_bottom = -30
	holder.add_theme_stylebox_override("panel", UITheme.box(UITheme.BG_SOLID, 16, 0, UITheme.ACCENT, 14))
	_root.add_child(holder)
	var v := VBoxContainer.new()
	holder.add_child(v)
	var top := HBoxContainer.new()
	v.add_child(top)
	var t := UITheme.label(Loc.t("map_title"), 28, UITheme.ACCENT)
	t.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	top.add_child(t)
	top.add_child(UITheme.label(Loc.t("map_legend"), 18, UITheme.MUTED))
	top.add_child(UITheme.button(Loc.t("menu_back"), func(): show_screen("pause"), 160, 20))
	_map = Minimap.new()
	_map.world = world
	_map.full = true
	_map.size_flags_vertical = Control.SIZE_EXPAND_FILL
	_map.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	_map.process_mode = Node.PROCESS_MODE_ALWAYS
	v.add_child(_map)


func _list_panel(title: String) -> VBoxContainer:
	_dim()
	var v := _panel(720)
	_title(v, title, 30)
	var scroll := ScrollContainer.new()
	scroll.custom_minimum_size = Vector2(660, 380)
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	v.add_child(scroll)
	var inner := VBoxContainer.new()
	inner.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.add_child(inner)
	_back_button(v)
	return inner


func _row(parent: Container, text: String, color := UITheme.TEXT, size := 20) -> void:
	var l := UITheme.label(text, size, color)
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	l.custom_minimum_size = Vector2(620, 0)
	parent.add_child(l)


func _build_missions() -> void:
	var inner := _list_panel(Loc.t("pause_missions"))
	var mm: MissionManager = world.missions
	_row(inner, Loc.t("missions_active"), UITheme.ACCENT, 24)
	_row(inner, mm.active_title() if mm.is_active() else Loc.t("none"))
	_row(inner, Loc.t("missions_available"), UITheme.ACCENT, 24)
	var any := false
	for id in MissionDefs.MISSIONS:
		if MissionDefs.is_available(id) and id != mm.active_id:
			var d: Dictionary = MissionDefs.MISSIONS[id]
			_row(inner, "◆ %s — %s" % [Loc.t(d["title"]), CityLayout.location_name(d["giver"])])
			_row(inner, "   " + Loc.t(d["desc"]), UITheme.MUTED, 17)
			any = true
	if not any:
		_row(inner, Loc.t("none"))
	_row(inner, Loc.t("missions_done"), UITheme.ACCENT, 24)
	if GameState.missions_done.is_empty():
		_row(inner, Loc.t("none"))
	for id in GameState.missions_done:
		var d2 := MissionDefs.get_def(id)
		if not d2.is_empty():
			_row(inner, "✓ " + Loc.t(d2["title"]), UITheme.GOOD)


func _build_inventory() -> void:
	var inner := _list_panel(Loc.t("pause_inventory"))
	_row(inner, Loc.t("inv_wallet") + " : $%d" % GameState.money, UITheme.MONEY, 26)
	_row(inner, Loc.t("inv_outfit", [Loc.t(GameState.outfit)]))
	_row(inner, Loc.t("inv_transactions"), UITheme.ACCENT, 22)
	for tr in GameState.transactions.slice(0, 12):
		var amount := int(tr["amount"])
		_row(inner, "%s   %s" % [("+$%d" % amount) if amount > 0 else ("-$%d" % -amount), tr["label"]],
			UITheme.GOOD if amount > 0 else UITheme.BAD, 18)


func _build_garage() -> void:
	var inner := _list_panel(Loc.t("pause_garage"))
	if GameState.owned_vehicles.is_empty():
		_row(inner, Loc.t("garage_empty"), UITheme.MUTED)
		return
	_row(inner, Loc.t("garage_owned"), UITheme.ACCENT, 22)
	for rec in GameState.owned_vehicles:
		var s := VehicleSpecs.get_spec(rec["type"])
		_row(inner, "%s — %s" % [Loc.t(s["name"]), Loc.t("garage_condition", [int(100.0 - float(rec.get("damage", 0.0)))])])


func _build_housing() -> void:
	var inner := _list_panel(Loc.t("pause_housing"))
	if GameState.apartment == "":
		_row(inner, Loc.t("housing_none"))
	else:
		_row(inner, Loc.t("housing_rented", [Loc.t("loc_apartment"), GameState.RENT_PRICE, GameState.hours_until_rent()]))


# ---------------------------------------------------------------- paramètres

func _build_settings() -> void:
	_dim(0.6)
	var v := _panel(680)
	_title(v, Loc.t("menu_settings"))
	var q := HBoxContainer.new()
	q.add_child(UITheme.label(Loc.t("set_graphics"), 20))
	var spacer := Control.new()
	spacer.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	q.add_child(spacer)
	for lvl in 3:
		var key: String = ["set_low", "set_medium", "set_high"][lvl]
		var lv: int = lvl
		var b := UITheme.button(Loc.t(key), _set_quality.bind(lv), 120, 18)
		if Settings.quality == lvl:
			b.add_theme_stylebox_override("normal", UITheme.box(UITheme.ACCENT, 10, 0, UITheme.ACCENT, 12))
			b.add_theme_color_override("font_color", Color(0.1, 0.1, 0.1))
		q.add_child(b)
	v.add_child(q)
	_slider(v, "set_master", Settings.vol_master, 0.0, 1.0, _on_setting.bind("vol_master"))
	_slider(v, "set_music", Settings.vol_music, 0.0, 1.0, _on_setting.bind("vol_music"))
	_slider(v, "set_sfx", Settings.vol_sfx, 0.0, 1.0, _on_setting.bind("vol_sfx"))
	_slider(v, "set_ambience", Settings.vol_ambience, 0.0, 1.0, _on_setting.bind("vol_ambience"))
	_slider(v, "set_touch_size", Settings.touch_scale, 0.7, 1.5, _on_setting.bind("touch_scale"))
	_slider(v, "set_touch_opacity", Settings.touch_opacity, 0.2, 1.0, _on_setting.bind("touch_opacity"))
	_slider(v, "set_sensitivity", Settings.camera_sensitivity, 0.3, 2.5, _on_setting.bind("camera_sensitivity"))
	var row := HBoxContainer.new()
	var fps := CheckButton.new()
	fps.text = Loc.t("set_fps")
	fps.button_pressed = Settings.show_fps
	fps.add_theme_font_size_override("font_size", 20)
	fps.toggled.connect(_on_fps)
	row.add_child(fps)
	var lang := UITheme.button(Loc.t("menu_language"), _toggle_language, 260, 18)
	row.add_child(lang)
	v.add_child(row)
	v.add_child(UITheme.button(Loc.t("menu_back"), func():
		Settings.save_settings()
		show_screen(_from if _from != "" else "pause")
	))


func _on_fps(on: bool) -> void:
	Settings.show_fps = on


func _set_quality(lv: int) -> void:
	Settings.quality = lv
	Settings.apply_quality()
	Settings.save_settings()
	show_screen("settings")


func _on_setting(x: float, prop: String) -> void:
	Settings.set(prop, x)
	Settings.apply_audio()
	Events.settings_changed.emit()


func _slider(parent: Container, key: String, value: float, mn: float, mx: float, cb: Callable) -> void:
	var h := HBoxContainer.new()
	var l := UITheme.label(Loc.t(key), 19)
	l.custom_minimum_size = Vector2(260, 0)
	h.add_child(l)
	var s := HSlider.new()
	s.min_value = mn
	s.max_value = mx
	s.step = 0.01
	s.value = value
	s.custom_minimum_size = Vector2(320, 36)
	s.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	s.value_changed.connect(cb)
	h.add_child(s)
	parent.add_child(h)
