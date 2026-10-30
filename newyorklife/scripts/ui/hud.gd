class_name HUD
extends Control
## Interface de jeu : mini-carte, argent, santé, recherche, heure/météo, objectif,
## chrono de mission, vitesse/carburant, indications contextuelles, notifications.

var world: Node
var minimap: Minimap
var _money: Label
var _clock: Label
var _wanted: Label
var _health: ProgressBar
var _objective: Label
var _timer: Label
var _hint: Label
var _speed: Label
var _fuel: ProgressBar
var _road: Label
var _fps: Label
var _notes: VBoxContainer
var _big: Label
var _big_sub: Label
var _big_time := 0.0
var _shown_money := 0.0
var _weather_text := ""


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	theme = UITheme.get_theme()
	minimap = Minimap.new()
	minimap.world = world
	minimap.position = Vector2(16, 16)
	minimap.size = Vector2(210, 210)
	add_child(minimap)
	_health = ProgressBar.new()
	_health.position = Vector2(16, 232)
	_health.size = Vector2(210, 12)
	_health.max_value = 100
	_health.show_percentage = false
	_health.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(_health)
	_road = UITheme.label("", 16, UITheme.MUTED)
	_road.position = Vector2(16, 248)
	add_child(_road)
	var right := VBoxContainer.new()
	right.anchor_left = 1.0
	right.anchor_right = 1.0
	right.offset_left = -360
	right.offset_right = -90
	right.offset_top = 14
	right.alignment = BoxContainer.ALIGNMENT_BEGIN
	right.mouse_filter = Control.MOUSE_FILTER_IGNORE
	right.add_theme_constant_override("separation", 2)
	add_child(right)
	_money = UITheme.label("$0", 34, UITheme.MONEY)
	_money.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	right.add_child(_money)
	_clock = UITheme.label("", 18, UITheme.TEXT)
	_clock.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	right.add_child(_clock)
	_wanted = UITheme.label("", 30, UITheme.ACCENT)
	_wanted.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	right.add_child(_wanted)
	_objective = UITheme.label("", 22, UITheme.TEXT)
	_objective.anchor_left = 0.5
	_objective.anchor_right = 0.5
	_objective.offset_left = -330
	_objective.offset_right = 330
	_objective.offset_top = 16
	_objective.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_objective.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	add_child(_objective)
	_timer = UITheme.label("", 30, UITheme.ACCENT)
	_timer.anchor_left = 0.5
	_timer.anchor_right = 0.5
	_timer.offset_left = -150
	_timer.offset_right = 150
	_timer.offset_top = 76
	_timer.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	add_child(_timer)
	_hint = UITheme.label("", 22, UITheme.TEXT)
	_hint.anchor_left = 0.5
	_hint.anchor_right = 0.5
	_hint.anchor_top = 1.0
	_hint.anchor_bottom = 1.0
	_hint.offset_left = -300
	_hint.offset_right = 300
	_hint.offset_top = -150
	_hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_hint.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	add_child(_hint)
	_speed = UITheme.label("", 40, UITheme.TEXT)
	_speed.anchor_left = 0.5
	_speed.anchor_right = 0.5
	_speed.anchor_top = 1.0
	_speed.anchor_bottom = 1.0
	_speed.offset_left = -120
	_speed.offset_right = 120
	_speed.offset_top = -86
	_speed.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	add_child(_speed)
	_fuel = ProgressBar.new()
	_fuel.anchor_left = 0.5
	_fuel.anchor_right = 0.5
	_fuel.anchor_top = 1.0
	_fuel.anchor_bottom = 1.0
	_fuel.offset_left = -70
	_fuel.offset_right = 70
	_fuel.offset_top = -30
	_fuel.offset_bottom = -22
	_fuel.max_value = 1.0
	_fuel.step = 0.0
	_fuel.show_percentage = false
	_fuel.mouse_filter = Control.MOUSE_FILTER_IGNORE
	var fill := UITheme.box(UITheme.ACCENT, 4, 0, UITheme.ACCENT, 0)
	_fuel.add_theme_stylebox_override("fill", fill)
	add_child(_fuel)
	_notes = VBoxContainer.new()
	_notes.position = Vector2(16, 280)
	_notes.size = Vector2(380, 300)
	_notes.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(_notes)
	_big = UITheme.label("", 54, UITheme.ACCENT)
	_big.set_anchors_preset(Control.PRESET_CENTER)
	_big.anchor_top = 0.33
	_big.anchor_bottom = 0.33
	_big.offset_left = -500
	_big.offset_right = 500
	_big.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	add_child(_big)
	_big_sub = UITheme.label("", 26, UITheme.TEXT)
	_big_sub.set_anchors_preset(Control.PRESET_CENTER)
	_big_sub.anchor_top = 0.33
	_big_sub.anchor_bottom = 0.33
	_big_sub.offset_top = 70
	_big_sub.offset_left = -500
	_big_sub.offset_right = 500
	_big_sub.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	add_child(_big_sub)
	_fps = UITheme.label("", 16, UITheme.MUTED)
	_fps.position = Vector2(240, 16)
	add_child(_fps)
	Events.notify.connect(_on_notify)
	Events.objective_changed.connect(func(t): _objective.text = t)
	Events.hint_changed.connect(func(t): _hint.text = t)
	Events.money_changed.connect(_on_money)
	Events.mission_completed.connect(_on_mission_completed)
	Events.mission_failed.connect(_on_mission_failed)
	Events.weather_changed.connect(func(k): _weather_text = Loc.t(k))
	_shown_money = GameState.money
	_weather_text = Loc.t("weather_clear")


func big_message(title: String, sub: String, duration := 3.5) -> void:
	_big.text = title
	_big_sub.text = sub
	_big_time = duration
	_big.modulate.a = 1.0
	_big_sub.modulate.a = 1.0


func _on_mission_completed(_id: String, reward: int) -> void:
	big_message(Loc.t("mission_passed"), Loc.t("reward", [reward]) if reward > 0 else "")


func _on_mission_failed(_id: String, reason: String) -> void:
	big_message(Loc.t("mission_failed"), reason)


func _on_money(_amount: int, delta: int) -> void:
	if absi(delta) >= 1:
		_on_notify(("+$%d" % delta) if delta > 0 else ("-$%d" % -delta), "money" if delta > 0 else "bad")


func _on_notify(text: String, kind: String) -> void:
	var panel := PanelContainer.new()
	var col := UITheme.TEXT
	match kind:
		"money", "good":
			col = UITheme.GOOD
		"bad":
			col = UITheme.BAD
		"mission":
			col = UITheme.ACCENT
	panel.add_theme_stylebox_override("panel", UITheme.box(Color(0.06, 0.07, 0.09, 0.82), 8, 0, col, 10))
	var l := UITheme.label(text, 19, col)
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	l.custom_minimum_size = Vector2(300, 0)
	panel.add_child(l)
	panel.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_notes.add_child(panel)
	while _notes.get_child_count() > 4:
		_notes.get_child(0).queue_free()
		_notes.remove_child(_notes.get_child(0))
	var tw := panel.create_tween()
	tw.tween_interval(3.5)
	tw.tween_property(panel, "modulate:a", 0.0, 0.6)
	tw.tween_callback(panel.queue_free)


func _process(delta: float) -> void:
	if world == null or world.player == null:
		return
	var player: Player = world.player
	_shown_money = move_toward(_shown_money, GameState.money, maxf(4.0, absf(GameState.money - _shown_money) * 6.0) * delta)
	_money.text = "$%s" % _fmt(int(round(_shown_money)))
	_clock.text = "%s · %s · %s" % [GameState.clock_text(), Loc.t(GameState.time_phase()), _weather_text]
	var stars := ""
	for i in 5:
		stars += "★" if i < GameState.wanted_level else ("☆" if GameState.wanted_level > 0 else "")
	_wanted.text = stars
	_health.value = GameState.health
	_road.text = CityLayout.road_name_at(player.focus_position())
	var mm: MissionManager = world.missions
	if mm.is_active() and mm.timer_left > 0.0:
		var t := int(ceil(mm.timer_left))
		_timer.text = "%d:%02d" % [t / 60, t % 60]
		_timer.add_theme_color_override("font_color", UITheme.BAD if t <= 10 else UITheme.ACCENT)
	else:
		_timer.text = ""
	if player.vehicle:
		_speed.text = "%d %s" % [Loc.speed_value(player.vehicle.speed()), Loc.t("speed_unit")]
		_fuel.visible = true
		_fuel.value = player.vehicle.fuel
	else:
		_speed.text = ""
		_fuel.visible = false
	_fps.text = "%d FPS" % Engine.get_frames_per_second() if Settings.show_fps else ""
	if _big_time > 0.0:
		_big_time -= delta
		if _big_time < 0.8:
			_big.modulate.a = maxf(0.0, _big_time / 0.8)
			_big_sub.modulate.a = _big.modulate.a
		if _big_time <= 0.0:
			_big.text = ""
			_big_sub.text = ""


static func _fmt(n: int) -> String:
	var s := str(absi(n))
	var out := ""
	var c := 0
	for i in range(s.length() - 1, -1, -1):
		out = s[i] + out
		c += 1
		if c % 3 == 0 and i > 0:
			out = " " + out
	return ("-" if n < 0 else "") + out
