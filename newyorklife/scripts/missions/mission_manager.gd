class_name MissionManager
extends Node3D
## Exécute les missions définies dans MissionDefs, étape par étape.
## Les systèmes centraux ne connaissent pas les missions : tout passe par Events.

const L = preload("res://scripts/world/city_layout.gd")
const GIVER_COLOR := Color(1.0, 0.78, 0.15)
const TARGET_COLOR := Color(0.3, 0.85, 1.0)

var world: Node            ## nœud monde (spawn_vehicle)
var player: Player
var peds: PedestrianManager
var active_id := ""
var active: Dictionary = {}
var step_index := -1
var timer_left := -1.0
var tagged := {}
var passenger: Pedestrian = null
var target_pos := Vector3.ZERO
var has_target := false
var _target_marker: Marker
var _target_radius := 4.0
var _givers := {}           ## id -> Marker
var _cooldown := 0.0
var _waiting := false
var _cp_points: Array[Vector3] = []
var _cp_index := 0
var _rng := RandomNumberGenerator.new()
var _declined := {}


func _ready() -> void:
	MissionDefs.register_texts()
	_rng.randomize()
	Events.busted.connect(func(): _on_fail_event("busted"))
	Events.player_died.connect(func(): _on_fail_event("died"))
	_target_marker = Marker.create(TARGET_COLOR, 3.0, 3.0)
	_target_marker.visible = false
	add_child(_target_marker)


func is_active() -> bool:
	return active_id != ""


func refresh_givers() -> void:
	for id in MissionDefs.MISSIONS:
		var show := MissionDefs.is_available(id) and not is_active()
		var def: Dictionary = MissionDefs.MISSIONS[id]
		if def.get("auto_start", false):
			show = false
		if show and not _givers.has(id):
			var m := Marker.create(GIVER_COLOR, 1.4, 2.4)
			add_child(m)
			m.global_position = _giver_pos(id)
			_givers[id] = m
		elif not show and _givers.has(id):
			_givers[id].queue_free()
			_givers.erase(id)


func _giver_pos(id: String) -> Vector3:
	var def: Dictionary = MissionDefs.MISSIONS[id]
	var p := L.location_pos(def["giver"])
	# Décale les missions qui partagent un même lieu
	var k := 0
	for other in MissionDefs.MISSIONS:
		if other == id:
			break
		if MissionDefs.MISSIONS[other]["giver"] == def["giver"]:
			k += 1
	var l: Dictionary = L.LOCATIONS[def["giver"]]
	var side: int = l["side"]
	var tangent := Vector3(0, 0, 1) if side < 2 else Vector3(1, 0, 0)
	return p + tangent * (k * 3.2) - L.side_normal(side) * 0.3


## Points d'intérêt affichés sur la mini-carte.
func map_points() -> Array:
	var out := []
	for id in _givers:
		out.append({"pos": (_givers[id] as Marker).global_position, "kind": "mission"})
	if has_target:
		out.append({"pos": target_pos, "kind": "target"})
	return out


func start_auto_missions() -> void:
	for id in MissionDefs.MISSIONS:
		var def: Dictionary = MissionDefs.MISSIONS[id]
		if def.get("auto_start", false) and MissionDefs.is_available(id):
			start(id)
			return
	refresh_givers()


func _process(delta: float) -> void:
	if player == null or get_tree().paused:
		return
	_cooldown = maxf(0.0, _cooldown - delta)
	if not is_active():
		_check_givers()
		return
	if _waiting:
		return
	if timer_left > 0.0:
		timer_left -= delta
		if timer_left <= 0.0 and "timeout" in active.get("fail", []):
			fail("fail_timeout")
			return
	if "wrecked" in active.get("fail", []):
		for t in tagged:
			var v: Vehicle = tagged[t]
			if is_instance_valid(v) and v.is_wrecked():
				fail("fail_vehicle")
				return
		if active.get("need_vehicle", false) and player.vehicle and player.vehicle.is_wrecked():
			fail("fail_vehicle")
			return
	_update_step()


func _check_givers() -> void:
	if _cooldown > 0.0:
		return
	var pp := player.focus_position()
	for id in _givers:
		var m: Marker = _givers[id]
		var d := Vector2(m.global_position.x - pp.x, m.global_position.z - pp.z).length()
		var r := 2.0 if not player.is_driving() else 4.5
		if d < r:
			if _declined.get(id, false):
				continue
			_offer(id)
			return
		else:
			_declined.erase(id)


func _offer(id: String) -> void:
	var def: Dictionary = MissionDefs.MISSIONS[id]
	if def.get("need_vehicle", false) and not player.is_driving():
		Events.notify.emit(Loc.t("need_vehicle"), "info")
		_declined[id] = true
		return
	_cooldown = 1.0
	var reward: int = def["reward"]
	if GameState.is_done(id) and def.has("repeat_reward"):
		reward = def["repeat_reward"]
	var body := Loc.t("mission_offer", [Loc.t(def["title"]), Loc.t(def["desc"]), reward])
	if def.get("job", false):
		body = Loc.t("mission_offer", [Loc.t(def["title"]), Loc.t(def["desc"]), "60+"])
	InputState.clear()
	Events.choice_requested.emit(body, [Loc.t("accept"), Loc.t("decline")], func(idx: int):
		if idx == 0:
			start(id)
		else:
			_declined[id] = true
	)


func start(id: String) -> void:
	var def := MissionDefs.get_def(id)
	if def.is_empty():
		return
	if def.get("job", false):
		def = MissionDefs.make_delivery_job(_rng)
	active_id = id
	active = def
	step_index = -1
	timer_left = -1.0
	tagged.clear()
	_waiting = false
	refresh_givers()
	Events.mission_started.emit(id)
	Events.notify.emit(Loc.t(def["title"]), "mission")
	_next_step()


func _next_step() -> void:
	_waiting = false
	step_index += 1
	_clear_target()
	var steps: Array = active.get("steps", [])
	if step_index >= steps.size():
		complete()
		return
	_begin_step(steps[step_index])


func _begin_step(s: Dictionary) -> void:
	match str(s["type"]):
		"dialog":
			_dialog(s["lines"])
		"dialog_flag":
			var lines: Array = s["if_true"] if GameState.flags.get(s["flag"], false) else s["if_false"]
			_dialog(lines)
		"spawn_vehicle":
			var pos := L.location_road_pos(s["loc"])
			var yaw := L.location_road_yaw(s["loc"])
			var col := Vehicle.color_from(s["color"]) if s.has("color") else VehicleSpecs.random_color(s["vehicle"], _rng)
			var v: Vehicle = world.spawn_vehicle(s["vehicle"], col, pos, yaw)
			v.mission_vehicle = true
			tagged[s["tag"]] = v
			_next_step()
		"enter_vehicle":
			_objective(s)
		"timer":
			timer_left = float(s["seconds"])
			_next_step()
		"goto", "pickup", "dropoff":
			_set_target(L.location_road_pos(s["loc"]) if (s.get("vehicle", false) or s["type"] != "goto") else L.location_pos(s["loc"]),
				float(s.get("radius", 6.0)))
			if s["type"] == "pickup":
				passenger = peds.reserve_at(L.location_pos(s["loc"])) if peds else null
			_objective(s)
		"checkpoints":
			_cp_points.clear()
			for n in s["nodes"]:
				_cp_points.append(L.node_pos(Vector2i(int(n[0]), int(n[1]))))
			_cp_index = 0
			_set_target(_cp_points[0], 9.0)
			_objective(s)
		"choice":
			_waiting = true
			var texts := []
			for o in s["options"]:
				texts.append(Loc.t(o["text"]))
			InputState.clear()
			Events.choice_requested.emit(Loc.t(s["title"]), texts, func(idx: int):
				var opt: Dictionary = s["options"][idx]
				GameState.flags[opt["flag"]] = true
				if int(opt.get("money", 0)) > 0:
					GameState.add_money(int(opt["money"]), Loc.t(active.get("title", "")))
					Sfx.play("cash", -4.0)
				_next_step()
			)
		_:
			_next_step()


func _dialog(lines: Array) -> void:
	_waiting = true
	var out := []
	for l in lines:
		out.append([str(l[0]), Loc.t(str(l[1]))])
	InputState.clear()
	Events.dialog_requested.emit(out, _next_step)


func _objective(s: Dictionary) -> void:
	var text := Loc.t(s.get("text", ""))
	if s.get("text_arg_loc", false):
		text = Loc.t(s["text"], [L.location_name(s["loc"])])
	if s["type"] == "checkpoints":
		text = Loc.t("checkpoint", [_cp_index + 1, _cp_points.size()])
	Events.objective_changed.emit(text)


func _set_target(p: Vector3, radius: float) -> void:
	target_pos = p
	has_target = true
	_target_radius = radius
	_target_marker.visible = true
	_target_marker.global_position = Vector3(p.x, maxf(p.y, 0.02), p.z)
	_target_marker.scale = Vector3.ONE * clampf(radius / 3.0, 0.7, 3.0)


func _clear_target() -> void:
	has_target = false
	_target_marker.visible = false
	Events.objective_changed.emit("")


func _flat_dist(a: Vector3, b: Vector3) -> float:
	return Vector2(a.x - b.x, a.z - b.z).length()


func _update_step() -> void:
	var steps: Array = active.get("steps", [])
	if step_index < 0 or step_index >= steps.size():
		return
	var s: Dictionary = steps[step_index]
	var pp := player.focus_position()
	match str(s["type"]):
		"enter_vehicle":
			var v: Vehicle = tagged.get(s["tag"])
			if v == null or not is_instance_valid(v):
				fail("fail_vehicle")
				return
			_set_target_quiet(v.global_position)
			if player.vehicle == v:
				_next_step()
		"goto":
			var need_v: bool = s.get("vehicle", false)
			if _flat_dist(pp, target_pos) < _target_radius:
				if need_v and not player.is_driving():
					return
				if need_v and player.vehicle.speed() > 6.0:
					return
				Sfx.play("checkpoint", -6.0)
				_next_step()
		"pickup":
			if player.is_driving() and _flat_dist(pp, target_pos) < 7.0 and player.vehicle.speed() < 2.0:
				if passenger:
					passenger.visible = false
				Sfx.play("door", -6.0)
				_next_step()
		"dropoff":
			if player.is_driving() and _flat_dist(pp, target_pos) < 7.0 and player.vehicle.speed() < 2.0:
				if passenger:
					passenger.global_position = L.location_pos(s["loc"])
					passenger.visible = true
					var p := passenger
					get_tree().create_timer(4.0).timeout.connect(func(): peds.release(p))
					passenger = null
				Sfx.play("door", -6.0)
				_next_step()
		"checkpoints":
			if _flat_dist(pp, target_pos) < _target_radius:
				_cp_index += 1
				Sfx.play("checkpoint", -4.0)
				Events.checkpoint_reached.emit(_cp_index, _cp_points.size())
				if _cp_index >= _cp_points.size():
					_next_step()
				else:
					_set_target(_cp_points[_cp_index], 9.0)
					_objective(s)


func _set_target_quiet(p: Vector3) -> void:
	target_pos = p
	has_target = true
	_target_marker.visible = false


func complete() -> void:
	var id := active_id
	var def := MissionDefs.get_def(id)
	var reward: int = active.get("reward", 0)
	if GameState.is_done(id) and def.has("repeat_reward"):
		reward = def["repeat_reward"]
	if active.get("job", false):
		GameState.stats["deliveries"] = int(GameState.stats["deliveries"]) + 1
	GameState.mark_done(id)
	if reward > 0:
		GameState.add_money(reward, Loc.t(active.get("title", "")))
	Sfx.play("success", -3.0)
	_cleanup()
	Events.mission_completed.emit(id, reward)
	refresh_givers()
	GameState.save_game()


func fail(reason_key: String) -> void:
	if not is_active():
		return
	var id := active_id
	_cleanup()
	Sfx.play("fail", -3.0)
	Events.mission_failed.emit(id, Loc.t(reason_key))
	_cooldown = 4.0
	_declined[id] = true
	refresh_givers()


func abort() -> void:
	_cleanup()
	refresh_givers()


func _on_fail_event(kind: String) -> void:
	if is_active() and kind in active.get("fail", []):
		fail("fail_busted" if kind == "busted" else "fail_died")


func _cleanup() -> void:
	active_id = ""
	active = {}
	step_index = -1
	timer_left = -1.0
	_waiting = false
	_clear_target()
	if passenger and peds:
		peds.release(passenger)
	passenger = null
	for t in tagged:
		var v: Vehicle = tagged[t]
		if is_instance_valid(v):
			v.mission_vehicle = false
	tagged.clear()


func active_title() -> String:
	return Loc.t(active.get("title", "")) if is_active() else ""
