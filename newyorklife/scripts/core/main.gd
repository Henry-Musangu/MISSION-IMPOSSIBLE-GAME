extends Node
## Point d'entrée : construit le monde, gère les états (menu, cinématique, jeu, pause),
## les interactions du joueur, l'arrestation, l'hôpital, la sauvegarde automatique
## et le chargement progressif des secteurs de la ville.

const L = preload("res://scripts/world/city_layout.gd")
const AUTOSAVE_INTERVAL := 120.0

var world_root: Node3D
var city: CityBuilder
var day_night: DayNight
var signals: TrafficSignals
var traffic: TrafficManager
var peds: PedestrianManager
var wanted: WantedSystem
var missions: MissionManager
var shops: Shops
var player: Player
var camera: FollowCamera
var hud: HUD
var touch: TouchControls
var dialog: DialogBox
var menus: Menus
var _fade: ColorRect
var _cutscene_layer: Control
var _subtitle: Label
var _skip := false
var state := "boot"          ## boot, menu, cutscene, play, pause
var _autosave := AUTOSAVE_INTERVAL
var _stream_timer := 0.0
var _cleanup_timer := 0.0
var _menu_t := 0.0
var _busy := false
var smoke := false
var _shots_dir := ""


func _ready() -> void:
	for a in OS.get_cmdline_user_args():
		if a == "--smoke":
			smoke = true
		elif a.begins_with("--shots="):
			_shots_dir = a.substr(8)
	get_tree().set_auto_accept_quit(true)
	process_mode = Node.PROCESS_MODE_ALWAYS
	_build_world()
	_build_ui()
	Events.busted.connect(_on_busted)
	Events.player_died.connect(_on_died)
	Events.player_entered_vehicle.connect(func(_v): _on_vehicle_changed())
	Events.player_exited_vehicle.connect(func(_v): _on_vehicle_changed())
	Events.settings_changed.connect(func(): touch._layout())
	if smoke:
		_run_smoke_test()
	else:
		show_menu()


# ---------------------------------------------------------------- construction

func _build_world() -> void:
	world_root = Node3D.new()
	world_root.name = "World"
	world_root.process_mode = Node.PROCESS_MODE_PAUSABLE
	add_child(world_root)
	day_night = DayNight.new()
	day_night.add_to_group("day_night")
	world_root.add_child(day_night)
	signals = TrafficSignals.new()
	world_root.add_child(signals)
	city = CityBuilder.new()
	city.name = "City"
	world_root.add_child(city)
	city.build()
	player = Player.new()
	player.name = "Player"
	world_root.add_child(player)
	player.global_position = L.location_pos("terminal")
	camera = FollowCamera.new()
	camera.name = "Camera"
	world_root.add_child(camera)
	camera.target = player
	camera.player = player
	day_night.follow = camera
	traffic = TrafficManager.new()
	world_root.add_child(traffic)
	traffic.player = player
	traffic.populate()
	peds = PedestrianManager.new()
	world_root.add_child(peds)
	peds.player = player
	peds.populate()
	wanted = WantedSystem.new()
	world_root.add_child(wanted)
	wanted.player = player
	wanted.traffic = traffic
	missions = MissionManager.new()
	missions.world = self
	missions.player = player
	missions.peds = peds
	world_root.add_child(missions)
	shops = Shops.new()
	shops.world = self
	shops.player = player
	world_root.add_child(shops)


func _build_ui() -> void:
	var layer := CanvasLayer.new()
	layer.layer = 5
	add_child(layer)
	hud = HUD.new()
	hud.world = self
	layer.add_child(hud)
	touch = TouchControls.new()
	layer.add_child(touch)
	_cutscene_layer = Control.new()
	_cutscene_layer.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	_cutscene_layer.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_cutscene_layer.visible = false
	layer.add_child(_cutscene_layer)
	for top in [true, false]:
		var bar := ColorRect.new()
		bar.color = Color.BLACK
		bar.anchor_right = 1.0
		if top:
			bar.offset_bottom = 70
		else:
			bar.anchor_top = 1.0
			bar.anchor_bottom = 1.0
			bar.offset_top = -90
		bar.mouse_filter = Control.MOUSE_FILTER_IGNORE
		_cutscene_layer.add_child(bar)
	_subtitle = UITheme.label("", 28)
	_subtitle.anchor_left = 0.1
	_subtitle.anchor_right = 0.9
	_subtitle.anchor_top = 1.0
	_subtitle.anchor_bottom = 1.0
	_subtitle.offset_top = -82
	_subtitle.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_subtitle.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_cutscene_layer.add_child(_subtitle)
	var skip := UITheme.button(Loc.t("skip"), _on_skip, 160, 20)
	skip.anchor_left = 1.0
	skip.anchor_right = 1.0
	skip.offset_left = -190
	skip.offset_top = 8
	skip.offset_right = -20
	_cutscene_layer.add_child(skip)
	var top_layer := CanvasLayer.new()
	top_layer.layer = 10
	add_child(top_layer)
	dialog = DialogBox.new()
	top_layer.add_child(dialog)
	dialog.opened.connect(_on_dialog_opened)
	dialog.closed.connect(_on_dialog_closed)
	menus = Menus.new()
	menus.world = self
	top_layer.add_child(menus)
	_fade = ColorRect.new()
	_fade.color = Color(0, 0, 0, 0)
	_fade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	_fade.mouse_filter = Control.MOUSE_FILTER_IGNORE
	top_layer.add_child(_fade)


# ---------------------------------------------------------------- états

func show_menu() -> void:
	state = "menu"
	get_tree().paused = false
	GameState.playing = false
	hud.visible = false
	touch.visible = false
	menus.show_screen("main")
	Sfx.set_music(true)
	Sfx.set_ambience(true)
	camera.cinematic = true


func new_game() -> void:
	if _busy:
		return
	SaveSystem.delete()
	GameState.reset_new_game()
	_reset_world()
	player.global_position = GameState.player_position
	player.rig.rotation.y = 0.0
	player.apply_outfit()
	menus.show_screen("")
	await _play_intro()
	_start_play()
	missions.start_auto_missions()


func continue_game() -> void:
	if _busy:
		return
	if not GameState.load_game():
		Events.notify.emit(Loc.t("save_failed"), "bad")
		return
	_reset_world()
	for rec in GameState.owned_vehicles:
		rec["out"] = false
	player.global_position = GameState.player_position + Vector3(0, 0.2, 0)
	player.rig.rotation.y = GameState.player_yaw
	player.apply_outfit()
	var cv := GameState.current_vehicle
	if not cv.is_empty():
		var v := spawn_vehicle(str(cv.get("type", "sedan")), Vehicle.color_from(cv.get("color")),
			GameState.player_position, GameState.player_yaw)
		v.damage = float(cv.get("damage", 0.0))
		v.fuel = float(cv.get("fuel", 1.0))
		v.repaint(v.color)
		var idx := int(cv.get("record", -1))
		if idx >= 0 and idx < GameState.owned_vehicles.size():
			v.owned = true
			v.set_meta("record", idx)
			GameState.owned_vehicles[idx]["out"] = true
		player.enter_vehicle(v)
	menus.show_screen("")
	_start_play()
	missions.refresh_givers()
	Events.game_loaded.emit()


func _reset_world() -> void:
	missions.abort()
	wanted.clear()
	if player.vehicle:
		player.exit_vehicle()
	for v in get_tree().get_nodes_in_group("vehicles"):
		if v.get_parent() == world_root:
			v.queue_free()
	GameState.wanted_level = 0
	day_night.set_weather("weather_clear")


func _start_play() -> void:
	state = "play"
	GameState.playing = true
	camera.cinematic = false
	camera.target = player
	camera.yaw = player.rig.rotation.y + PI
	camera.snap_to_target()
	hud.visible = true
	touch.visible = true
	_cutscene_layer.visible = false
	Sfx.set_music(player.is_driving())
	Sfx.set_ambience(true)
	_autosave = AUTOSAVE_INTERVAL
	Events.game_started.emit()


func resume() -> void:
	if state != "pause":
		return
	state = "play"
	get_tree().paused = false
	menus.show_screen("")
	hud.visible = true
	touch.visible = true
	InputState.clear()


func pause() -> void:
	if state != "play" or dialog.is_open():
		return
	state = "pause"
	touch._release_all()
	touch.visible = false
	get_tree().paused = true
	menus.show_screen("pause")


func quit_to_menu() -> void:
	_store_state()
	GameState.save_game()
	missions.abort()
	wanted.clear()
	if player.vehicle:
		player.exit_vehicle()
	get_tree().paused = false
	show_menu()


func manual_save() -> void:
	_store_state()
	if GameState.save_game():
		Events.notify.emit(Loc.t("saved_ok"), "good")
		Sfx.play("checkpoint", -6.0)
	else:
		Events.notify.emit(Loc.t("save_failed"), "bad")
	resume()


func _store_state() -> void:
	shops.sync_all_records()
	GameState.player_position = player.global_position
	GameState.player_yaw = player.rig.global_rotation.y
	if player.vehicle:
		var d := player.vehicle.to_dict()
		d["record"] = int(player.vehicle.get_meta("record", -1)) if player.vehicle.owned else -1
		GameState.current_vehicle = d
		GameState.player_yaw = player.vehicle.global_rotation.y
	else:
		GameState.current_vehicle = {}


func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_GO_BACK_REQUEST:
		if state == "pause":
			if menus.current() == "pause":
				resume()
			else:
				menus.show_screen("pause")
		elif state == "play":
			pause()
	elif what == NOTIFICATION_APPLICATION_PAUSED or what == NOTIFICATION_WM_CLOSE_REQUEST:
		if state == "play" or state == "pause":
			_store_state()
			GameState.save_game()
		if what == NOTIFICATION_APPLICATION_PAUSED and state == "play":
			pause()


# ---------------------------------------------------------------- cinématique

func _play_intro() -> void:
	state = "cutscene"
	_skip = smoke
	hud.visible = false
	touch.visible = false
	_cutscene_layer.visible = true
	camera.cinematic = true
	Sfx.set_music(true)
	var ts := L.node_pos(L.TIMES_SQUARE)
	var term := L.location_pos("terminal")
	var shots := [
		[Vector3(-60, 120, 520), Vector3(80, 95, 460), ts + Vector3(0, 40, 0), "cutscene_1", 4.5],
		[ts + Vector3(-40, 12, 40), ts + Vector3(30, 18, 30), ts + Vector3(0, 14, 0), "cutscene_2", 4.5],
		[term + Vector3(25, 6, 18), term + Vector3(12, 4, 8), term + Vector3(0, 1.4, 0), "cutscene_3", 5.0],
		[term + Vector3(6, 2.5, 6), term + Vector3(4, 2.0, -3), term + Vector3(0, 1.5, 0), "cutscene_4", 3.0],
	]
	for shot in shots:
		if _skip:
			break
		_subtitle.text = Loc.t(shot[3])
		var t := 0.0
		var dur: float = shot[4]
		while t < dur and not _skip:
			t += get_process_delta_time()
			var k := smoothstep(0.0, 1.0, t / dur)
			camera.set_cinematic((shot[0] as Vector3).lerp(shot[1], k), shot[2])
			await get_tree().process_frame
	_cutscene_layer.visible = false
	Sfx.set_music(false)


# ---------------------------------------------------------------- boucle

func _process(delta: float) -> void:
	match state:
		"menu":
			_menu_t += delta * 0.05
			var c := L.node_pos(L.TIMES_SQUARE)
			camera.set_cinematic(c + Vector3(cos(_menu_t) * 140.0, 70.0, sin(_menu_t) * 140.0), c + Vector3(0, 20, 0))
			GameState.time_minutes = fmod(GameState.time_minutes + delta * 3.0, GameState.DAY_MINUTES)
		"play":
			_play_process(delta)
		"pause":
			if InputState.consume("pause"):
				resume()
	_stream_timer -= delta
	if _stream_timer <= 0.0:
		_stream_timer = 0.5
		_update_streaming()


func _play_process(delta: float) -> void:
	if touch.pause_requested or InputState.consume("pause"):
		touch.pause_requested = false
		pause()
		return
	touch.set_driving(player.is_driving())
	GameState.player_position = player.global_position
	if not dialog.is_open() and not _busy:
		_handle_interaction()
	# sécurité : chute hors du monde
	if player.focus_position().y < -20.0:
		teleport_player(L.location_pos("terminal"))
	# klaxon : les passants s'écartent
	if player.vehicle and player.vehicle.horn:
		peds.scare(player.vehicle.global_position + player.vehicle.global_transform.basis.z * 6.0, 7.0)
	# percuter un véhicule de police = délit
	_autosave -= delta
	if _autosave <= 0.0:
		_autosave = AUTOSAVE_INTERVAL
		_store_state()
		GameState.save_game()
	_cleanup_timer -= delta
	if _cleanup_timer <= 0.0:
		_cleanup_timer = 3.0
		_cleanup_vehicles()
	_check_traffic_hits()


func _handle_interaction() -> void:
	var hint := ""
	var action := InputState.consume("interact")
	if player.vehicle:
		var v := player.vehicle
		if v.is_upside_down():
			hint = Loc.t("hint_flip")
			if action:
				v.flip_upright()
				return
		if action:
			player.exit_vehicle()
			return
	else:
		var p := player.global_position
		var shop := shops.shop_in_range(p)
		var veh := player.nearest_vehicle()
		var tcar: TrafficCar = traffic.nearest_car(p, 3.2) if veh == null else null
		if shop != "":
			hint = Loc.t("hint_talk", [shops.shop_title(shop)])
			if action:
				shops.open(shop)
				return
		elif veh:
			hint = Loc.t("hint_enter", [veh.display_name()])
			if action:
				player.enter_vehicle(veh)
				return
		elif tcar:
			hint = Loc.t("hint_steal", [tcar.display_name()])
			if action:
				_carjack(tcar)
				return
	Events.hint_changed.emit(hint)


func _carjack(c: TrafficCar) -> void:
	var xf := c.global_transform
	var type := c.type
	var col := c.color
	var police := c.is_police
	var pos := c.global_position
	traffic.take_car(c)
	var v := spawn_vehicle(type, col, xf.origin + Vector3(0, 0.15, 0), xf.basis.get_euler().y)
	player.enter_vehicle(v)
	Events.crime.emit(2 if police else 1, pos, "carjack")


func _check_traffic_hits() -> void:
	# Le joueur à pied percuté par un véhicule de la circulation
	if player.vehicle or player.knocked > 0.0:
		return
	var p := player.global_position
	for c in traffic.cars:
		if c.speed < 4.0:
			continue
		var local := c.global_transform.affine_inverse() * p
		var hl: float = float(c.spec["length"]) * 0.5 + 0.5
		var hw: float = float(c.spec["width"]) * 0.5 + 0.4
		if absf(local.x) < hw and local.z > -hl and local.z < hl + 0.6 and absf(local.y) < 2.5:
			player.knock_down(c.global_transform.basis.z * c.speed * 0.8, c.speed * 3.0)
			Sfx.play("hit", -2.0)
			c.speed = 0.0
			return


func _on_vehicle_changed() -> void:
	touch.set_driving(player.is_driving())
	Sfx.set_music(player.is_driving())
	Events.hint_changed.emit("")


func _on_dialog_opened() -> void:
	touch._release_all()
	touch.visible = false


func _on_dialog_closed() -> void:
	if state == "play":
		touch.visible = true


# ---------------------------------------------------------------- monde

func spawn_vehicle(type: String, color: Color, pos: Vector3, yaw: float) -> Vehicle:
	var v := Vehicle.new()
	v.setup(type, color)
	world_root.add_child(v)
	v.global_transform = Transform3D(Basis(Vector3.UP, yaw), pos + Vector3(0, 0.5, 0))
	return v


func _cleanup_vehicles() -> void:
	var p := player.focus_position()
	var loose := []
	for n in get_tree().get_nodes_in_group("vehicles"):
		var v := n as Vehicle
		if v == null or v.get_parent() != world_root or v == player.vehicle or v.mission_vehicle:
			continue
		if v.owned:
			continue
		var d := v.global_position.distance_to(p)
		if d > 220.0:
			v.queue_free()
		else:
			loose.append(v)
	# au plus 6 véhicules abandonnés à la fois
	if loose.size() > 6:
		loose.sort_custom(func(a, b): return a.global_position.distance_to(p) > b.global_position.distance_to(p))
		for i in loose.size() - 6:
			(loose[i] as Vehicle).queue_free()


func _update_streaming() -> void:
	if city == null:
		return
	var p := player.focus_position() if (state == "play" or state == "pause") and not camera.cinematic else camera.global_position
	var radius: float = Settings.get_value("stream_radius")
	for i in city.blocks.size():
		var node: Node3D = city.blocks[i]
		var c: Vector3 = city.block_centers[i]
		var d := Vector2(c.x - p.x, c.z - p.z).length()
		var inside := node.get_parent() != null
		if d > radius + 30.0 and inside:
			city.remove_child(node)
		elif d < radius and not inside:
			city.add_child(node)


func map_points() -> Array:
	var pts: Array = missions.map_points()
	pts.append_array(shops.map_points())
	for u in wanted.units:
		if is_instance_valid(u):
			pts.append({"pos": u.global_position, "kind": "police"})
	return pts


func teleport_player(pos: Vector3) -> void:
	if player.vehicle:
		var v := player.vehicle
		player.exit_vehicle()
		v.queue_free()
	player.global_position = pos + Vector3(0, 0.3, 0)
	player.velocity = Vector3.ZERO
	camera.snap_to_target()


func fade(mid: Callable) -> void:
	if _busy:
		return
	_busy = true
	var tw := create_tween()
	tw.set_pause_mode(Tween.TWEEN_PAUSE_PROCESS)
	tw.tween_property(_fade, "color:a", 1.0, 0.45)
	tw.tween_callback(mid)
	tw.tween_interval(0.35)
	tw.tween_property(_fade, "color:a", 0.0, 0.45)
	tw.tween_callback(func(): _busy = false)


func _on_busted() -> void:
	if state != "play" or _busy:
		return
	var fine := mini(GameState.money, maxi(100, int(GameState.money * 0.1)))
	GameState.stats["busted"] = int(GameState.stats["busted"]) + 1
	fade(func():
		wanted.clear()
		teleport_player(L.location_pos("police"))
		GameState.spend(fine, Loc.t("loc_police"))
		hud.big_message(Loc.t("busted", [fine]), "")
	)


func _on_died() -> void:
	if state != "play" or _busy:
		return
	var fee := mini(GameState.money, 100)
	GameState.stats["hospital"] = int(GameState.stats["hospital"]) + 1
	fade(func():
		wanted.clear()
		teleport_player(L.location_pos("hospital"))
		GameState.health = 100.0
		GameState.spend(fee, Loc.t("loc_hospital"))
		hud.big_message(Loc.t("wasted", [fee]), "")
	)


# ---------------------------------------------------------------- test automatique

## Lancé avec « -- --smoke » : joue une séquence complète et vérifie les systèmes clés.
func _run_smoke_test() -> void:
	var results := []
	var ok := true
	dialog.auto_advance = true
	await get_tree().create_timer(0.5).timeout
	await new_game()
	results.append("new_game: state=%s" % state)
	if state != "play":
		ok = false
	await _wait(1.0)
	# 1) Déplacement à pied
	var start := player.global_position
	camera.yaw = PI
	InputState.touch_move = Vector2(0, 1)
	await _wait(2.0)
	InputState.touch_move = Vector2.ZERO
	var walked := player.global_position.distance_to(start)
	results.append("walk: %.2f m" % walked)
	if walked < 2.0:
		ok = false
	await _shot("walk")
	# 2) Saut
	await _wait(0.3)
	var y0 := player.global_position.y
	InputState.set_touch_button("jump", true)
	await _wait(0.25)
	InputState.set_touch_button("jump", false)
	var jumped := player.global_position.y - y0
	results.append("jump: %.2f m" % jumped)
	if jumped < 0.3:
		ok = false
	await _wait(1.0)
	# 3) Conduite : véhicule sur l'avenue la plus proche
	var road := Vector3(L.ave_x(1) - 2.2, 0.0, L.st_z(3) + 20.0)
	teleport_player(road + Vector3(3.0, 0, 0))
	var car := spawn_vehicle("sedan", Color(0.15, 0.2, 0.35), road, 0.0)
	await _wait(1.0)
	player.enter_vehicle(car)
	var cstart := car.global_position
	InputState.set_touch_button("gas", true)
	await _wait(4.0)
	var fspeed := car.forward_speed()
	InputState.set_touch_button("gas", false)
	var driven := car.global_position.distance_to(cstart)
	results.append("drive: %.1f m, forward speed %.1f m/s, inside=%s" % [driven, fspeed, player.is_driving()])
	if driven < 8.0 or fspeed < 3.0 or not player.is_driving():
		ok = false
	await _shot("drive")
	InputState.set_touch_button("brake", true)
	await _wait(2.5)
	InputState.set_touch_button("brake", false)
	results.append("brake: speed %.1f m/s" % car.speed())
	if car.speed() > 3.0:
		ok = false
	# 4) Sortie du véhicule
	player.exit_vehicle()
	await _wait(0.5)
	var out_d := player.global_position.distance_to(car.global_position)
	results.append("exit: driving=%s dist=%.1f" % [player.is_driving(), out_d])
	if player.is_driving() or out_d > 6.0:
		ok = false
	# 5) Mission 1 : rejoindre le garage
	var m_active := missions.is_active()
	teleport_player(L.location_pos("garage"))
	await _wait(1.5)
	results.append("mission m01 active=%s done=%s" % [m_active, GameState.is_done("m01_arrival")])
	if not GameState.is_done("m01_arrival"):
		ok = false
	# 6) Sauvegarde / chargement
	GameState.add_money(77, "test")
	var money := GameState.money
	_store_state()
	var saved := GameState.save_game()
	GameState.money = 0
	var loaded := GameState.load_game()
	results.append("save=%s load=%s money %d/%d" % [saved, loaded, GameState.money, money])
	if not saved or not loaded or GameState.money != money:
		ok = false
	# 7) Circulation et piétons actifs
	var moving := 0
	for c in traffic.cars:
		if c.speed > 1.0:
			moving += 1
	results.append("traffic cars=%d moving=%d peds=%d" % [traffic.cars.size(), moving, peds.peds.size()])
	if traffic.cars.is_empty() or peds.peds.is_empty():
		ok = false
	# 8) Niveau de recherche
	Events.crime.emit(3, player.global_position, "test")
	await _wait(0.2)
	results.append("wanted level=%d" % wanted.level)
	if wanted.level < 1:
		ok = false
	wanted.clear()
	# 9) Vues : ville de jour, de nuit, Times Square
	camera.cinematic = true
	var ts := L.node_pos(L.TIMES_SQUARE)
	camera.set_cinematic(ts + Vector3(-45, 22, 60), ts + Vector3(0, 12, 0))
	await _wait(0.5)
	await _shot("times_square_day")
	GameState.time_minutes = 22 * 60
	await _wait(1.0)
	await _shot("times_square_night")
	GameState.time_minutes = 15 * 60
	camera.set_cinematic(Vector3(-50, 140, 520), ts + Vector3(0, 30, 0))
	await _wait(0.8)
	await _shot("skyline")
	camera.cinematic = false
	# 10) Interface : menu pause et carte
	pause()
	menus.show_screen("map")
	await _wait(0.3)
	await _shot("map")
	resume()
	hud.visible = true
	await _wait(0.3)
	await _shot("hud")
	# 11) Conduite de nuit vue de la caméra de jeu
	GameState.time_minutes = 21 * 60 + 30
	var car2 := spawn_vehicle("taxi", Color(0.98, 0.76, 0.05), Vector3(L.ave_x(2) - 2.2, 0.0, L.st_z(1) + 15.0), 0.0)
	await _wait(0.5)
	player.enter_vehicle(car2)
	camera.yaw = PI
	InputState.set_touch_button("gas", true)
	await _wait(2.5)
	InputState.set_touch_button("gas", false)
	await _shot("drive_night")
	player.exit_vehicle()
	GameState.time_minutes = 15 * 60
	results.append("fps=%d" % Engine.get_frames_per_second())
	# 12) Écran d'accueil
	show_menu()
	await _wait(0.6)
	await _shot("menu")
	for r in results:
		print("SMOKE ", r)
	print("SMOKE RESULT: ", "PASS" if ok else "FAIL")
	get_tree().quit(0 if ok else 1)


func _on_skip() -> void:
	_skip = true


func _wait(seconds: float) -> void:
	await get_tree().create_timer(seconds, true, true).timeout


func _shot(shot_name: String) -> void:
	if _shots_dir == "" or DisplayServer.get_name() == "headless":
		return
	await RenderingServer.frame_post_draw
	var tex := get_viewport().get_texture()
	if tex == null:
		return
	var img := tex.get_image()
	if img and not img.is_empty():
		img.save_png(_shots_dir.path_join(shot_name + ".png"))
		print("SMOKE screenshot ", shot_name)
