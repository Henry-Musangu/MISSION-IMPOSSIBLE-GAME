extends Node
## Tests automatisés (exécutés en CI, sans écran) : logique de la ville, feux,
## économie, sauvegarde, localisation, données de missions, physique du véhicule
## et déplacement du personnage. Code de sortie 0 = tout est passé.

const L = preload("res://scripts/world/city_layout.gd")

var _passed := 0
var _failed := 0


func check(cond: bool, label: String) -> void:
	if cond:
		_passed += 1
		print("  ok   ", label)
	else:
		_failed += 1
		print("  FAIL ", label)


func _ready() -> void:
	print("== New York Life : tests ==")
	test_layout()
	test_signals()
	test_economy()
	test_save()
	test_localization()
	test_missions_data()
	test_vehicle_specs()
	await test_vehicle_physics()
	await test_player_movement()
	print("== %d réussis, %d échecs ==" % [_passed, _failed])
	print("TESTS RESULT: ", "PASS" if _failed == 0 else "FAIL")
	get_tree().quit(0 if _failed == 0 else 1)


func test_layout() -> void:
	print("[ville]")
	var pts := L.lane_points(Vector2i(0, 0), Vector2i(0, 1))
	check(pts[0].z < pts[1].z, "voie avenue orientée vers le sud")
	check(pts[0].x < L.ave_x(0), "circulation à droite (vers le sud = côté ouest)")
	check(L.is_on_road(pts[0]) and L.is_on_road(pts[1]), "voie sur la chaussée")
	var p2 := L.lane_points(Vector2i(1, 2), Vector2i(2, 2))
	check(p2[0].z > L.st_z(2), "rue vers l'est : voie côté sud")
	var path := L.path_nodes(Vector2i(0, 0), Vector2i(4, 6))
	check(path.size() == 11, "plus court chemin = 11 intersections (%d)" % path.size())
	check(L.neighbors(Vector2i(0, 0)).size() == 2, "coin : 2 voisins")
	check(L.neighbors(Vector2i(2, 3)).size() == 4, "centre : 4 voisins")
	for id in L.LOCATIONS:
		var p := L.location_pos(id)
		check(not L.is_on_road(p), "lieu %s sur un trottoir" % id)
		check(L.is_on_road(L.location_road_pos(id)), "lieu %s : place sur la chaussée" % id)
	var bc := L.block_count()
	var overlap := false
	for i in bc.x:
		for j in bc.y:
			var r := L.block_rect(Vector2i(i, j))
			if L.is_on_road(Vector3(r.get_center().x, 0, r.get_center().y)):
				overlap = true
	check(not overlap, "aucun bloc sur une chaussée")


func test_signals() -> void:
	print("[feux]")
	var both_green := false
	var saw := {"g": false, "y": false, "r": false}
	var t := 0.0
	while t < TrafficSignals.cycle_length():
		var s := TrafficSignals.states_at(t)
		if s[0] != "r" and s[1] != "r":
			both_green = true
		saw[s[0]] = true
		t += 0.25
	check(not both_green, "jamais deux axes ouverts en même temps")
	check(saw["g"] and saw["y"] and saw["r"], "cycle vert/orange/rouge complet")


func test_economy() -> void:
	print("[économie]")
	GameState.reset_new_game()
	var start := GameState.money
	GameState.add_money(500, "test")
	check(GameState.money == start + 500, "ajout d'argent")
	check(GameState.spend(200, "test"), "dépense possible")
	check(not GameState.spend(999999, "test"), "dépense refusée si fonds insuffisants")
	check(GameState.money == start + 300, "solde correct")
	check(GameState.transactions.size() == 2, "transactions enregistrées")


func test_save() -> void:
	print("[sauvegarde]")
	var path := "user://test_save.json"
	SaveSystem.delete(path)
	GameState.reset_new_game()
	GameState.money = 4321
	GameState.missions_done = ["m01_arrival"]
	GameState.owned_vehicles = [{"type": "sport", "color": [1, 0, 0], "damage": 12.5, "fuel": 0.5}]
	GameState.flags = {"loyal_to_dayo": true}
	check(SaveSystem.write(GameState.to_dict(), path), "écriture")
	var d := SaveSystem.read(path)
	check(int(d.get("money", 0)) == 4321, "relecture de l'argent")
	GameState.reset_new_game()
	GameState.from_dict(d)
	check(GameState.money == 4321 and GameState.is_done("m01_arrival"), "restauration de l'état")
	check(GameState.owned_vehicles.size() == 1 and GameState.flags.get("loyal_to_dayo", false), "véhicules et choix restaurés")
	# 2e écriture -> la 1re devient la copie de secours ; on corrompt la principale
	GameState.money = 1111
	SaveSystem.write(GameState.to_dict(), path)
	var f := FileAccess.open(path, FileAccess.WRITE)
	f.store_string("{corrompu")
	f.close()
	var d2 := SaveSystem.read(path)
	check(int(d2.get("money", 0)) == 4321, "copie de secours utilisée si corruption")
	var old := SaveSystem.migrate({"money": 50})
	check(old.has("owned_vehicles") and int(old["save_version"]) == SaveSystem.SAVE_VERSION, "migration d'une ancienne version")
	SaveSystem.delete(path)


func test_localization() -> void:
	print("[langues]")
	var fr: Dictionary = Loc.STRINGS["fr"]
	var en: Dictionary = Loc.STRINGS["en"]
	var missing := []
	for k in fr:
		if not en.has(k):
			missing.append(k)
	for k in en:
		if not fr.has(k):
			missing.append(k)
	check(missing.is_empty(), "mêmes clés FR/EN %s" % str(missing))
	var m_missing := []
	for k in MissionDefs.TEXTS_FR:
		if not MissionDefs.TEXTS_EN.has(k):
			m_missing.append(k)
	for k in MissionDefs.TEXTS_EN:
		if not MissionDefs.TEXTS_FR.has(k):
			m_missing.append(k)
	check(m_missing.is_empty(), "textes de missions FR/EN %s" % str(m_missing))
	check(Loc.t("money", [42]) == "$42", "substitution des arguments")
	Loc.lang = "fr"


func test_missions_data() -> void:
	print("[missions]")
	MissionDefs.register_texts()
	var types := ["dialog", "dialog_flag", "goto", "spawn_vehicle", "enter_vehicle", "timer", "pickup", "dropoff", "checkpoints", "choice"]
	for id in MissionDefs.MISSIONS:
		var m: Dictionary = MissionDefs.MISSIONS[id]
		check(L.LOCATIONS.has(m["giver"]), "%s : donneur valide" % id)
		check(MissionDefs.TEXTS_FR.has(m["title"]) and MissionDefs.TEXTS_FR.has(m["desc"]), "%s : titre et description" % id)
		for r in m.get("requires", []):
			check(MissionDefs.MISSIONS.has(r), "%s : prérequis %s existe" % [id, r])
		for s in m["steps"]:
			check(str(s["type"]) in types, "%s : étape %s connue" % [id, s["type"]])
			if s.has("loc"):
				check(L.LOCATIONS.has(s["loc"]), "%s : lieu %s" % [id, s["loc"]])
			if s.has("text"):
				check(MissionDefs.TEXTS_FR.has(s["text"]) or Loc.STRINGS["fr"].has(s["text"]), "%s : texte %s" % [id, s["text"]])
			if s.has("lines"):
				for l in s["lines"]:
					check(MissionDefs.TEXTS_FR.has(l[1]), "%s : réplique %s" % [id, l[1]])
	var rng := RandomNumberGenerator.new()
	rng.seed = 3
	var job := MissionDefs.make_delivery_job(rng)
	check(int(job["reward"]) > 0 and job["steps"].size() == 2, "génération d'un emploi de livreur")
	GameState.reset_new_game()
	check(MissionDefs.is_available("m01_arrival") and not MissionDefs.is_available("m02_first_wheel"), "déblocage progressif")


func test_vehicle_specs() -> void:
	print("[véhicules]")
	for t in VehicleSpecs.SPECS:
		var s: Dictionary = VehicleSpecs.SPECS[t]
		for key in ["name", "price", "mass", "engine", "max_speed", "grip", "length", "width", "height", "wheel_r", "colors"]:
			check(s.has(key), "%s.%s" % [t, key])
		check(Loc.STRINGS["fr"].has(s["name"]), "%s : nom traduit" % t)
	for t in VehicleSpecs.DEALER_STOCK:
		check(VehicleSpecs.SPECS.has(t) and int(VehicleSpecs.SPECS[t]["price"]) > 0, "en vente : %s" % t)


func _make_ground() -> StaticBody3D:
	var body := StaticBody3D.new()
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(400, 2, 400)
	cs.shape = sh
	cs.position = Vector3(0, -1, 0)
	body.add_child(cs)
	add_child(body)
	return body


func _frames(n: int) -> void:
	for i in n:
		await get_tree().physics_frame


func test_vehicle_physics() -> void:
	print("[physique de conduite]")
	var ground := _make_ground()
	for type in ["sedan", "sport", "suv", "bus"]:
		var v := Vehicle.new()
		v.setup(type, Color.RED)
		add_child(v)
		v.global_transform = Transform3D(Basis(), Vector3(0, 1.0, 0))
		await _frames(60)
		var rest_y := v.global_position.y
		check(rest_y > 0.0 and rest_y < 1.2, "%s repose sur ses roues (y=%.2f)" % [type, rest_y])
		check(not v.is_upside_down(), "%s reste droit" % type)
		v.throttle = 1.0
		await _frames(180)
		var fs := v.forward_speed()
		check(fs > 4.0, "%s accélère vers l'avant (%.1f m/s)" % [type, fs])
		v.throttle = 0.0
		v.steer_input = 1.0
		v.throttle = 0.5
		var yaw0 := v.global_rotation.y
		await _frames(60)
		check(absf(angle_difference(yaw0, v.global_rotation.y)) > 0.1, "%s tourne" % type)
		v.steer_input = 0.0
		v.throttle = -1.0
		await _frames(150)
		check(v.speed() < 6.0, "%s freine (%.1f m/s)" % [type, v.speed()])
		v.throttle = -1.0
		await _frames(120)
		check(v.forward_speed() < -0.5, "%s recule (%.1f m/s)" % [type, v.forward_speed()])
		check(not v.is_upside_down(), "%s ne se retourne pas" % type)
		v.queue_free()
		await _frames(2)
	ground.queue_free()


func test_player_movement() -> void:
	print("[personnage]")
	var ground := _make_ground()
	var p := Player.new()
	add_child(p)
	p.global_position = Vector3(20, 0.2, 20)
	await _frames(30)
	check(p.is_on_floor(), "au sol")
	p.camera_yaw = 0.0
	InputState.touch_move = Vector2(0, 1)
	InputState._process(0.016)
	var start := p.global_position
	await _frames(60)
	InputState.touch_move = Vector2.ZERO
	var moved := p.global_position - start
	check(moved.length() > 2.0, "avance (%.2f m)" % moved.length())
	check(moved.z < -1.0, "dans la direction de la caméra")
	InputState._process(0.016)
	await _frames(20)
	var y0 := p.global_position.y
	InputState.set_touch_button("jump", true)
	await _frames(12)
	InputState.set_touch_button("jump", false)
	check(p.global_position.y > y0 + 0.3, "saute")
	await _frames(60)
	check(p.is_on_floor(), "retombe au sol")
	# Entrée / sortie de véhicule
	var v := Vehicle.new()
	v.setup("sedan", Color.BLUE)
	add_child(v)
	v.global_transform = Transform3D(Basis(), p.global_position + Vector3(3, 0.8, 0))
	await _frames(30)
	check(p.nearest_vehicle() == v, "véhicule à portée")
	p.enter_vehicle(v)
	check(p.is_driving() and not p.visible, "entre dans le véhicule")
	await _frames(5)
	p.exit_vehicle()
	await _frames(5)
	check(not p.is_driving() and p.visible, "sort du véhicule")
	check(p.global_position.distance_to(v.global_position) < 5.0, "ressort à côté du véhicule")
	v.queue_free()
	p.queue_free()
	ground.queue_free()
