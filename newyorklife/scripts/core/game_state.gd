extends Node
## État persistant de la partie : argent, progression, possessions, temps.

const START_MONEY := 120
const DAY_MINUTES := 1440.0
const GAME_MINUTES_PER_SECOND := 1.0   ## une journée = 24 minutes réelles
const RENT_PRICE := 150
const RENT_DEPOSIT := 500
const RENT_PERIOD_DAYS := 3

var money := START_MONEY
var transactions: Array = []         ## [{label, amount}]
var missions_done: Array = []        ## identifiants
var flags := {}                      ## choix scénaristiques et déblocages
var outfit := "outfit_default"
var outfits_owned: Array = ["outfit_default"]
var owned_vehicles: Array = []       ## [{type, color: [r,g,b], damage, fuel}]
var apartment := ""                  ## identifiant du logement loué
var rent_due_day := 0
var day := 1
var time_minutes := 400.0            ## 6 h 40
var health := 100.0
var wanted_level := 0
var player_position := Vector3.ZERO
var player_yaw := 0.0
var current_vehicle := {}            ## véhicule conduit au moment de la sauvegarde
var playing := false
var time_scale := 1.0
var stats := {"distance_driven": 0.0, "deliveries": 0, "busted": 0, "hospital": 0}


func reset_new_game() -> void:
	money = START_MONEY
	transactions = []
	missions_done = []
	flags = {}
	outfit = "outfit_default"
	outfits_owned = ["outfit_default"]
	owned_vehicles = []
	apartment = ""
	rent_due_day = 0
	day = 1
	time_minutes = 400.0
	health = 100.0
	wanted_level = 0
	current_vehicle = {}
	stats = {"distance_driven": 0.0, "deliveries": 0, "busted": 0, "hospital": 0}
	player_position = CityLayout.location_pos("terminal")
	player_yaw = 0.0


func _process(delta: float) -> void:
	if not playing or get_tree().paused:
		return
	time_minutes += delta * GAME_MINUTES_PER_SECOND * time_scale
	if time_minutes >= DAY_MINUTES:
		time_minutes -= DAY_MINUTES
		day += 1
		_on_new_day()


func _on_new_day() -> void:
	if apartment != "" and day >= rent_due_day:
		if spend(RENT_PRICE, "rent"):
			Events.notify.emit(Loc.t("rent_paid", [RENT_PRICE]), "money")
			rent_due_day = day + RENT_PERIOD_DAYS
		else:
			apartment = ""
			Events.notify.emit(Loc.t("rent_unpaid"), "bad")


func hours_until_rent() -> int:
	var days_left := rent_due_day - day
	return maxi(0, int(days_left * 24 + (DAY_MINUTES - time_minutes) / 60.0) - 24)


# ---------- Économie ----------

func add_money(amount: int, label: String) -> void:
	if amount == 0:
		return
	money = maxi(0, money + amount)
	transactions.push_front({"label": label, "amount": amount})
	if transactions.size() > 25:
		transactions.resize(25)
	Events.money_changed.emit(money, amount)


func can_afford(amount: int) -> bool:
	return money >= amount


func spend(amount: int, label: String) -> bool:
	if amount < 0 or money < amount:
		return false
	add_money(-amount, label)
	return true


# ---------- Progression ----------

func is_done(mission_id: String) -> bool:
	return mission_id in missions_done


func mark_done(mission_id: String) -> void:
	if not is_done(mission_id):
		missions_done.append(mission_id)


func time_phase() -> String:
	var h := time_minutes / 60.0
	if h >= 5.5 and h < 11.0:
		return "phase_morning"
	if h >= 11.0 and h < 14.0:
		return "phase_noon"
	if h >= 14.0 and h < 18.0:
		return "phase_afternoon"
	if h >= 18.0 and h < 21.0:
		return "phase_evening"
	return "phase_night"


func clock_text() -> String:
	var m := int(time_minutes)
	return "%02d:%02d" % [m / 60, m % 60]


# ---------- Sauvegarde ----------

func to_dict() -> Dictionary:
	return {
		"money": money, "transactions": transactions, "missions_done": missions_done,
		"flags": flags, "outfit": outfit, "outfits_owned": outfits_owned,
		"owned_vehicles": owned_vehicles, "apartment": apartment,
		"rent_due_day": rent_due_day, "day": day, "time_minutes": time_minutes,
		"health": health,
		"player_position": [player_position.x, player_position.y, player_position.z],
		"player_yaw": player_yaw, "current_vehicle": current_vehicle, "stats": stats,
	}


func from_dict(d: Dictionary) -> void:
	reset_new_game()
	money = maxi(0, int(d.get("money", START_MONEY)))
	transactions = d.get("transactions", []) as Array
	missions_done = d.get("missions_done", []) as Array
	flags = d.get("flags", {}) as Dictionary
	outfit = str(d.get("outfit", "outfit_default"))
	outfits_owned = d.get("outfits_owned", ["outfit_default"]) as Array
	owned_vehicles = d.get("owned_vehicles", []) as Array
	apartment = str(d.get("apartment", ""))
	rent_due_day = int(d.get("rent_due_day", 0))
	day = maxi(1, int(d.get("day", 1)))
	time_minutes = fmod(float(d.get("time_minutes", 400.0)), DAY_MINUTES)
	health = clampf(float(d.get("health", 100.0)), 10.0, 100.0)
	var p = d.get("player_position", null)
	if p is Array and p.size() == 3:
		player_position = Vector3(float(p[0]), float(p[1]), float(p[2]))
		if not CityLayout.is_inside_city(player_position):
			player_position = CityLayout.location_pos("terminal")
	player_yaw = float(d.get("player_yaw", 0.0))
	current_vehicle = d.get("current_vehicle", {}) as Dictionary
	var s = d.get("stats", {})
	if s is Dictionary:
		stats.merge(s, true)


func save_game() -> bool:
	return SaveSystem.write(to_dict())


func load_game() -> bool:
	var d := SaveSystem.read()
	if d.is_empty():
		return false
	from_dict(d)
	return true


func has_save() -> bool:
	return SaveSystem.exists()
