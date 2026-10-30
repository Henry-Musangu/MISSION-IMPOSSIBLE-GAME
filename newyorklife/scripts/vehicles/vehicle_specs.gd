class_name VehicleSpecs
extends RefCounted
## Caractéristiques des catégories de véhicules : dimensions, physique, prix.
## engine = force par roue motrice (N), max_speed en m/s, grip = friction_slip.

const SPECS := {
	"sedan": {
		"name": "veh_sedan", "price": 3000, "mass": 1250.0, "engine": 2600.0, "max_speed": 42.0,
		"grip": 1.3, "length": 4.6, "width": 1.85, "height": 0.75, "cabin": 0.62, "cabin_len": 0.5,
		"cabin_offset": -0.15, "wheel_r": 0.34, "drive": "rwd", "steer": 0.6,
		"colors": [Color(0.15, 0.2, 0.35), Color(0.55, 0.55, 0.57), Color(0.08, 0.08, 0.09), Color(0.6, 0.1, 0.1), Color(0.85, 0.85, 0.83)],
	},
	"taxi": {
		"name": "veh_taxi", "price": 0, "mass": 1350.0, "engine": 2500.0, "max_speed": 38.0,
		"grip": 1.25, "length": 4.9, "width": 1.9, "height": 0.78, "cabin": 0.64, "cabin_len": 0.52,
		"cabin_offset": -0.12, "wheel_r": 0.35, "drive": "rwd", "steer": 0.6,
		"colors": [Color(0.98, 0.76, 0.05)], "taxi_sign": true,
	},
	"suv": {
		"name": "veh_suv", "price": 6500, "mass": 1900.0, "engine": 3600.0, "max_speed": 38.0,
		"grip": 1.2, "length": 4.9, "width": 2.0, "height": 1.0, "cabin": 0.8, "cabin_len": 0.62,
		"cabin_offset": -0.2, "wheel_r": 0.42, "drive": "awd", "steer": 0.55,
		"colors": [Color(0.1, 0.12, 0.1), Color(0.35, 0.38, 0.3), Color(0.75, 0.75, 0.77), Color(0.2, 0.25, 0.4)],
	},
	"sport": {
		"name": "veh_sport", "price": 15000, "mass": 1200.0, "engine": 4600.0, "max_speed": 62.0,
		"grip": 1.55, "length": 4.4, "width": 1.95, "height": 0.6, "cabin": 0.5, "cabin_len": 0.42,
		"cabin_offset": -0.05, "wheel_r": 0.34, "drive": "rwd", "steer": 0.55,
		"colors": [Color(0.85, 0.1, 0.08), Color(0.95, 0.6, 0.05), Color(0.1, 0.1, 0.12), Color(0.1, 0.35, 0.8)],
	},
	"luxury": {
		"name": "veh_luxury", "price": 28000, "mass": 2100.0, "engine": 4200.0, "max_speed": 50.0,
		"grip": 1.4, "length": 5.6, "width": 2.0, "height": 0.8, "cabin": 0.62, "cabin_len": 0.48,
		"cabin_offset": -0.1, "wheel_r": 0.38, "drive": "rwd", "steer": 0.5,
		"colors": [Color(0.04, 0.04, 0.05), Color(0.9, 0.9, 0.88), Color(0.25, 0.08, 0.1)],
	},
	"van": {
		"name": "veh_van", "price": 4500, "mass": 2300.0, "engine": 3400.0, "max_speed": 33.0,
		"grip": 1.2, "length": 5.4, "width": 2.05, "height": 1.65, "cabin": 0.0, "cabin_len": 0.0,
		"cabin_offset": 0.0, "wheel_r": 0.38, "drive": "rwd", "steer": 0.55,
		"colors": [Color(0.92, 0.92, 0.9), Color(0.3, 0.4, 0.6)], "box": true,
	},
	"truck": {
		"name": "veh_truck", "price": 9000, "mass": 5200.0, "engine": 6500.0, "max_speed": 27.0,
		"grip": 1.15, "length": 7.2, "width": 2.4, "height": 2.4, "cabin": 0.0, "cabin_len": 0.0,
		"cabin_offset": 0.0, "wheel_r": 0.48, "drive": "rwd", "steer": 0.5,
		"colors": [Color(0.85, 0.85, 0.82), Color(0.6, 0.15, 0.1)], "box": true, "truck": true,
	},
	"bus": {
		"name": "veh_bus", "price": 0, "mass": 9000.0, "engine": 9000.0, "max_speed": 22.0,
		"grip": 1.1, "length": 11.5, "width": 2.55, "height": 2.7, "cabin": 0.0, "cabin_len": 0.0,
		"cabin_offset": 0.0, "wheel_r": 0.52, "drive": "rwd", "steer": 0.45,
		"colors": [Color(0.15, 0.35, 0.75)], "box": true, "bus": true,
	},
	"police": {
		"name": "veh_police", "price": 0, "mass": 1500.0, "engine": 3800.0, "max_speed": 50.0,
		"grip": 1.45, "length": 4.9, "width": 1.9, "height": 0.75, "cabin": 0.62, "cabin_len": 0.5,
		"cabin_offset": -0.12, "wheel_r": 0.35, "drive": "rwd", "steer": 0.6,
		"colors": [Color(0.92, 0.93, 0.95)], "police": true,
	},
}

## Véhicules en vente chez Empire Motors.
const DEALER_STOCK := ["sedan", "van", "suv", "truck", "sport", "luxury"]

## Répartition de la circulation (poids).
const TRAFFIC_MIX := {"sedan": 10, "taxi": 8, "suv": 5, "van": 3, "truck": 1, "bus": 1, "luxury": 1, "sport": 1}


static func get_spec(type: String) -> Dictionary:
	return SPECS.get(type, SPECS["sedan"])


static func random_traffic_type(rng: RandomNumberGenerator) -> String:
	var total := 0
	for k in TRAFFIC_MIX:
		total += TRAFFIC_MIX[k]
	var r := rng.randi_range(1, total)
	for k in TRAFFIC_MIX:
		r -= TRAFFIC_MIX[k]
		if r <= 0:
			return k
	return "sedan"


static func random_color(type: String, rng: RandomNumberGenerator) -> Color:
	var cols: Array = get_spec(type)["colors"]
	return cols[rng.randi() % cols.size()]
