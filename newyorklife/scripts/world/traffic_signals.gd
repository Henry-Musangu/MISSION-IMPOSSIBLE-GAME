class_name TrafficSignals
extends Node
## Cycle synchronisé des feux tricolores (onde verte simplifiée).
## Axe "ave" = circulation nord-sud (avenues), "st" = est-ouest (rues).

const AVE_GREEN := 14.0
const ST_GREEN := 11.0
const YELLOW := 3.0
const ALL_RED := 1.5

static var cycle_time := 0.0
static var ave_state := "g"
static var st_state := "r"


static func cycle_length() -> float:
	return AVE_GREEN + YELLOW + ALL_RED + ST_GREEN + YELLOW + ALL_RED


## Calcule l'état des deux axes à un instant donné du cycle.
static func states_at(t: float) -> Array:
	t = fposmod(t, cycle_length())
	if t < AVE_GREEN:
		return ["g", "r"]
	t -= AVE_GREEN
	if t < YELLOW:
		return ["y", "r"]
	t -= YELLOW
	if t < ALL_RED:
		return ["r", "r"]
	t -= ALL_RED
	if t < ST_GREEN:
		return ["r", "g"]
	t -= ST_GREEN
	if t < YELLOW:
		return ["r", "y"]
	return ["r", "r"]


static func state(axis: String) -> String:
	return ave_state if axis == "ave" else st_state


static func axis_of(dir: Vector2i) -> String:
	return "ave" if dir.y != 0 else "st"


func _ready() -> void:
	_apply()


func _process(delta: float) -> void:
	cycle_time = fposmod(cycle_time + delta, cycle_length())
	var s := states_at(cycle_time)
	if s[0] != ave_state or s[1] != st_state:
		ave_state = s[0]
		st_state = s[1]
		_apply()


func _apply() -> void:
	for axis in ["ave", "st"]:
		var cur := state(axis)
		for c in ["r", "y", "g"]:
			Mats.set_traffic_lamp(axis, c, cur == c)
