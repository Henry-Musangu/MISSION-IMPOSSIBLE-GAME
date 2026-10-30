class_name Shops
extends Node3D
## Commerces et services : concession, garage, boutique, appartement, métro,
## station-service, hôpital. Points d'interaction (marqueurs bleus) sur les trottoirs.

const L = preload("res://scripts/world/city_layout.gd")
const SHOP_COLOR := Color(0.25, 0.6, 1.0)
const REPAINT_PRICE := 150
const METRO_PRICE := 3
const FUEL_PRICE_FULL := 60

const SHOPS := {
	"dealer": {"loc": "dealer", "title": "shop_dealer"},
	"garage": {"loc": "garage", "title": "shop_garage"},
	"clothes": {"loc": "clothes", "title": "shop_clothes"},
	"apartment": {"loc": "apartment", "title": "shop_apartment"},
	"metro_n": {"loc": "metro_n", "title": "shop_metro"},
	"metro_s": {"loc": "metro_s", "title": "shop_metro"},
	"gas": {"loc": "gas", "title": "shop_gas"},
	"hospital": {"loc": "hospital", "title": "shop_hospital"},
}

var world: Node
var player: Player
var _markers := {}
var _rng := RandomNumberGenerator.new()


func _ready() -> void:
	_rng.randomize()
	for id in SHOPS:
		var m := Marker.create(SHOP_COLOR, 1.0, 1.8)
		add_child(m)
		m.global_position = shop_pos(id)
		_markers[id] = m
		# Enseigne 3D au-dessus de l'entrée
		var label := Label3D.new()
		label.text = Loc.t(SHOPS[id]["title"]).to_upper()
		label.font_size = 64
		label.pixel_size = 0.012
		label.outline_size = 12
		label.modulate = Color(1.0, 0.95, 0.85)
		label.billboard = BaseMaterial3D.BILLBOARD_FIXED_Y
		label.position = Vector3(0, 4.2, 0)
		label.visibility_range_end = 60.0
		m.add_child(label)
		m.set_meta("label", label)
	Events.language_changed.connect(_relabel)


func _relabel() -> void:
	for id in _markers:
		(_markers[id].get_meta("label") as Label3D).text = Loc.t(SHOPS[id]["title"]).to_upper()


func shop_pos(id: String) -> Vector3:
	var loc: String = SHOPS[id]["loc"]
	var l: Dictionary = L.LOCATIONS[loc]
	var side: int = l["side"]
	var tangent := Vector3(0, 0, 1) if side < 2 else Vector3(1, 0, 0)
	return L.location_pos(loc) - tangent * 3.4


func map_points() -> Array:
	var out := []
	for id in _markers:
		out.append({"pos": (_markers[id] as Marker).global_position, "kind": "shop"})
	return out


## Commerce à portée du joueur à pied ("" sinon).
func shop_in_range(p: Vector3) -> String:
	for id in _markers:
		var mp: Vector3 = (_markers[id] as Marker).global_position
		if Vector2(mp.x - p.x, mp.z - p.z).length() < 2.0:
			return id
	return ""


func shop_title(id: String) -> String:
	return Loc.t(SHOPS[id]["title"])


func open(id: String) -> void:
	var title := shop_title(id)
	var opts: Array = []          # [{text, action: Callable}]
	match id:
		"dealer":
			for t in VehicleSpecs.DEALER_STOCK:
				var s := VehicleSpecs.get_spec(t)
				var tt: String = t
				opts.append({"text": Loc.t("vehicle_price", [Loc.t(s["name"]), s["price"]]), "action": func(): _buy_vehicle(tt)})
			var v := _nearby_vehicle(25.0)
			if v and v.owned:
				var price := int(int(v.spec["price"]) * 0.6 * (1.0 - v.damage / 200.0))
				opts.append({"text": Loc.t("sell_vehicle", [v.display_name(), price]), "action": func(): _sell_vehicle(v, price)})
		"garage":
			var v := _nearby_vehicle(25.0)
			if v:
				var free: bool = GameState.flags.get("loyal_to_dayo", false)
				var cost := 0 if free else int(ceil(v.damage * 4.0))
				var txt := Loc.t("repair_free") if free else Loc.t("repair", [cost])
				opts.append({"text": txt, "action": func(): _repair(v, cost)})
				opts.append({"text": Loc.t("repaint", [REPAINT_PRICE]), "action": func(): _repaint(v)})
				if v.owned:
					opts.append({"text": Loc.t("store_vehicle"), "action": func(): _store(v)})
			else:
				Events.notify.emit(Loc.t("no_vehicle_here"), "info")
			var idx := 0
			for rec in GameState.owned_vehicles:
				var r: Dictionary = rec
				if not r.get("out", false):
					var i := idx
					var nm := Loc.t(VehicleSpecs.get_spec(r["type"])["name"])
					opts.append({"text": "%s — %s" % [nm, Loc.t("garage_spawn")], "action": func(): _retrieve(i)})
				idx += 1
		"clothes":
			for o in HumanoidRig.OUTFITS:
				var price: int = HumanoidRig.OUTFITS[o]["price"]
				var oid: String = o
				var owned: bool = o in GameState.outfits_owned
				var label := Loc.t(o) + (" ✓" if o == GameState.outfit else (" —" if owned else " — $%d" % price))
				opts.append({"text": label, "action": func(): _buy_outfit(oid, 0 if owned else price)})
		"apartment":
			if GameState.apartment == "":
				opts.append({"text": Loc.t("rent_apartment", [GameState.RENT_DEPOSIT + GameState.RENT_PRICE]), "action": _rent})
			else:
				title += "\n" + Loc.t("apartment_owned")
				opts.append({"text": Loc.t("sleep"), "action": _sleep})
		"metro_n", "metro_s":
			var other := "metro_s" if id == "metro_n" else "metro_n"
			var dest := L.location_name(SHOPS[other]["loc"])
			opts.append({"text": Loc.t("metro_to", [dest, METRO_PRICE]), "action": func(): _metro(other)})
		"gas":
			var v := _nearby_vehicle(25.0)
			if v:
				var cost := int(ceil((1.0 - v.fuel) * FUEL_PRICE_FULL))
				opts.append({"text": Loc.t("refuel", [cost]), "action": func(): _refuel(v, cost)})
			else:
				Events.notify.emit(Loc.t("no_vehicle_here"), "info")
		"hospital":
			var cost := int((100.0 - GameState.health) * 0.5)
			opts.append({"text": Loc.t("hint_talk", ["%s ($%d)" % ["+100 ♥", cost]]), "action": func(): _heal(cost)})
	var texts := []
	for o in opts:
		texts.append(o["text"])
	texts.append(Loc.t("close"))
	InputState.clear()
	Events.choice_requested.emit(title, texts, func(idx: int):
		if idx >= 0 and idx < opts.size():
			(opts[idx]["action"] as Callable).call()
	)


func _nearby_vehicle(radius: float) -> Vehicle:
	var best: Vehicle = null
	var bd := radius
	for n in get_tree().get_nodes_in_group("vehicles"):
		var v := n as Vehicle
		if v == null or v.ai_driving:
			continue
		var d := v.global_position.distance_to(player.global_position)
		if d < bd:
			bd = d
			best = v
	return best


func _pay(amount: int, label: String) -> bool:
	if amount <= 0:
		return true
	if not GameState.spend(amount, label):
		Events.notify.emit(Loc.t("not_enough_money"), "bad")
		Sfx.play("fail", -8.0)
		return false
	Sfx.play("cash", -4.0)
	return true


func _buy_vehicle(type: String) -> void:
	var s := VehicleSpecs.get_spec(type)
	if not _pay(s["price"], Loc.t(s["name"])):
		return
	var col := VehicleSpecs.random_color(type, _rng)
	var rec := {"type": type, "color": [col.r, col.g, col.b], "damage": 0.0, "fuel": 1.0, "out": true}
	GameState.owned_vehicles.append(rec)
	var v: Vehicle = world.spawn_vehicle(type, col, L.location_road_pos("dealer"), L.location_road_yaw("dealer"))
	v.owned = true
	v.set_meta("record", GameState.owned_vehicles.size() - 1)
	Events.notify.emit(Loc.t("bought_vehicle", [Loc.t(s["name"])]), "money")
	GameState.save_game()


func _sell_vehicle(v: Vehicle, price: int) -> void:
	if player.vehicle == v:
		player.exit_vehicle()
	if v.has_meta("record"):
		var i: int = v.get_meta("record")
		if i >= 0 and i < GameState.owned_vehicles.size():
			GameState.owned_vehicles.remove_at(i)
			_reindex_records(i)
	GameState.add_money(price, Loc.t("sell"))
	Sfx.play("cash", -4.0)
	v.queue_free()
	GameState.save_game()


func _reindex_records(removed: int) -> void:
	for n in get_tree().get_nodes_in_group("vehicles"):
		if n.has_meta("record"):
			var i: int = n.get_meta("record")
			if i > removed:
				n.set_meta("record", i - 1)


func _repair(v: Vehicle, cost: int) -> void:
	if _pay(cost, Loc.t("shop_garage")):
		v.repair()
		Events.notify.emit(Loc.t("repaired"), "good")


func _repaint(v: Vehicle) -> void:
	if _pay(REPAINT_PRICE, Loc.t("shop_garage")):
		var c := Color.from_hsv(_rng.randf(), _rng.randf_range(0.3, 0.9), _rng.randf_range(0.3, 0.9))
		v.repaint(c)
		_sync_record(v)


func _sync_record(v: Vehicle) -> void:
	if v.has_meta("record"):
		var i: int = v.get_meta("record")
		if i >= 0 and i < GameState.owned_vehicles.size():
			var rec: Dictionary = GameState.owned_vehicles[i]
			rec["color"] = [v.color.r, v.color.g, v.color.b]
			rec["damage"] = v.damage
			rec["fuel"] = v.fuel


func _store(v: Vehicle) -> void:
	if player.vehicle == v:
		player.exit_vehicle()
	_sync_record(v)
	if v.has_meta("record"):
		var i: int = v.get_meta("record")
		if i >= 0 and i < GameState.owned_vehicles.size():
			GameState.owned_vehicles[i]["out"] = false
	v.queue_free()
	Events.notify.emit(Loc.t("stored"), "good")
	GameState.save_game()


func _retrieve(i: int) -> void:
	if i < 0 or i >= GameState.owned_vehicles.size():
		return
	var rec: Dictionary = GameState.owned_vehicles[i]
	rec["out"] = true
	var v: Vehicle = world.spawn_vehicle(rec["type"], Vehicle.color_from(rec["color"]), L.location_road_pos("garage"), L.location_road_yaw("garage"))
	v.owned = true
	v.damage = float(rec.get("damage", 0.0))
	v.fuel = float(rec.get("fuel", 1.0))
	v.repaint(v.color)
	v.set_meta("record", i)


func _buy_outfit(id: String, price: int) -> void:
	if not _pay(price, Loc.t(id)):
		return
	if not id in GameState.outfits_owned:
		GameState.outfits_owned.append(id)
	GameState.outfit = id
	player.apply_outfit()
	Events.notify.emit(Loc.t("purchased", [Loc.t(id)]), "good")


func _rent() -> void:
	if _pay(GameState.RENT_DEPOSIT + GameState.RENT_PRICE, Loc.t("shop_apartment")):
		GameState.apartment = "apt_50"
		GameState.rent_due_day = GameState.day + GameState.RENT_PERIOD_DAYS
		Events.notify.emit(Loc.t("apartment_owned"), "good")
		GameState.save_game()


func _sleep() -> void:
	world.fade(func():
		if GameState.time_minutes > 7 * 60:
			GameState.day += 1
			GameState._on_new_day()
		GameState.time_minutes = 7 * 60
		GameState.health = 100.0
		GameState.save_game()
		Events.notify.emit(Loc.t("saved_ok"), "good")
	)


func _metro(dest_shop: String) -> void:
	if not _pay(METRO_PRICE, Loc.t("shop_metro")):
		return
	Sfx.play("metro", -4.0)
	world.fade(func():
		world.teleport_player(shop_pos(dest_shop) + L.side_normal(L.LOCATIONS[SHOPS[dest_shop]["loc"]]["side"]) * -0.5)
		GameState.time_minutes += 6.0
	)


func _refuel(v: Vehicle, cost: int) -> void:
	if _pay(cost, Loc.t("shop_gas")):
		v.fuel = 1.0
		Events.notify.emit(Loc.t("refueled"), "good")


func _heal(cost: int) -> void:
	if _pay(cost, Loc.t("shop_hospital")):
		GameState.health = 100.0
		Events.player_health_changed.emit(100.0)


## Met à jour les fiches des véhicules possédés avant une sauvegarde.
func sync_all_records() -> void:
	for n in get_tree().get_nodes_in_group("vehicles"):
		var v := n as Vehicle
		if v and v.owned:
			_sync_record(v)
