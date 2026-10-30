class_name MissionDefs
extends RefCounted
## Données des missions (aucune logique ici) : ajouter une mission = ajouter une entrée.
## Étapes : dialog, goto, spawn_vehicle, enter_vehicle, timer, pickup, dropoff,
## checkpoints, choice. Échecs : timeout, wrecked, busted, died.

const MISSIONS := {
	"m01_arrival": {
		"title": "m01_title", "desc": "m01_desc", "giver": "terminal", "requires": [], "reward": 100,
		"story": true, "auto_start": true,
		"steps": [
			{"type": "dialog", "lines": [["Noah", "m01_d1"], ["Noah", "m01_d2"]]},
			{"type": "goto", "loc": "garage", "text": "m01_o1", "radius": 4.0},
			{"type": "dialog", "lines": [["Dayo", "m01_d3"], ["Noah", "m01_d4"], ["Dayo", "m01_d5"]]},
		],
	},
	"m02_first_wheel": {
		"title": "m02_title", "desc": "m02_desc", "giver": "garage", "requires": ["m01_arrival"], "reward": 250,
		"story": true, "fail": ["wrecked", "busted", "died"],
		"steps": [
			{"type": "spawn_vehicle", "vehicle": "sedan", "loc": "garage", "tag": "car", "color": [0.15, 0.2, 0.35]},
			{"type": "dialog", "lines": [["Dayo", "m02_d1"]]},
			{"type": "enter_vehicle", "tag": "car", "text": "m02_o1"},
			{"type": "goto", "loc": "deli", "text": "m02_o2", "vehicle": true, "radius": 6.0},
			{"type": "dialog", "lines": [["Rosa", "m02_d2"], ["Noah", "m02_d3"]]},
			{"type": "timer", "seconds": 120},
			{"type": "goto", "loc": "garage", "text": "m02_o3", "vehicle": true, "radius": 6.0},
			{"type": "dialog", "lines": [["Dayo", "m02_d4"]]},
		],
	},
	"m03_express": {
		"title": "m03_title", "desc": "m03_desc", "giver": "deli", "requires": ["m02_first_wheel"], "reward": 400,
		"story": true, "fail": ["timeout", "wrecked", "busted", "died"],
		"steps": [
			{"type": "dialog", "lines": [["Rosa", "m03_d1"]]},
			{"type": "spawn_vehicle", "vehicle": "van", "loc": "deli", "tag": "car", "color": [0.92, 0.92, 0.9]},
			{"type": "enter_vehicle", "tag": "car", "text": "m03_o1"},
			{"type": "timer", "seconds": 85},
			{"type": "goto", "loc": "hotel", "text": "m03_o2", "vehicle": true, "radius": 6.0},
			{"type": "goto", "loc": "office", "text": "m03_o3", "vehicle": true, "radius": 6.0},
			{"type": "dialog", "lines": [["Rosa", "m03_d2"]]},
		],
	},
	"m04_cab": {
		"title": "m04_title", "desc": "m04_desc", "giver": "garage", "requires": ["m03_express"], "reward": 600,
		"story": true, "fail": ["timeout", "wrecked", "busted", "died"],
		"steps": [
			{"type": "dialog", "lines": [["Dayo", "m04_d1"]]},
			{"type": "spawn_vehicle", "vehicle": "taxi", "loc": "garage", "tag": "car"},
			{"type": "enter_vehicle", "tag": "car", "text": "m04_o1"},
			{"type": "pickup", "loc": "times", "text": "m04_o2"},
			{"type": "dialog", "lines": [["Mme Ellis", "m04_d2"], ["Noah", "m04_d3"], ["Mme Ellis", "m04_d4"]]},
			{"type": "timer", "seconds": 90},
			{"type": "dropoff", "loc": "office", "text": "m04_o3"},
			{"type": "dialog", "lines": [["Mme Ellis", "m04_d5"]]},
		],
	},
	"m05_offer": {
		"title": "m05_title", "desc": "m05_desc", "giver": "office", "requires": ["m04_cab"], "reward": 0,
		"story": true,
		"steps": [
			{"type": "dialog", "lines": [["Leon Draven", "m05_d1"], ["Leon Draven", "m05_d2"]]},
			{"type": "choice", "title": "m05_choice", "options": [
				{"text": "m05_opt_accept", "flag": "sold_to_draven", "money": 1500},
				{"text": "m05_opt_refuse", "flag": "loyal_to_dayo", "money": 0},
			]},
			{"type": "goto", "loc": "garage", "text": "m05_o1", "radius": 5.0},
			{"type": "dialog_flag", "flag": "sold_to_draven",
				"if_true": [["Dayo", "m05_d3a"]], "if_false": [["Dayo", "m05_d3b"]]},
		],
	},
	"r01_race": {
		"title": "r01_title", "desc": "r01_desc", "giver": "dealer", "requires": ["m02_first_wheel"], "reward": 800,
		"repeat_reward": 200, "repeatable": true, "need_vehicle": true, "fail": ["timeout", "wrecked", "busted", "died"],
		"steps": [
			{"type": "timer", "seconds": 95},
			{"type": "checkpoints", "nodes": [[3, 3], [3, 0], [1, 0], [0, 1], [0, 4], [1, 6], [3, 6], [4, 4], [3, 4]], "text": "r01_o1"},
		],
	},
	"j01_delivery": {
		"title": "j01_title", "desc": "j01_desc", "giver": "deli", "requires": ["m03_express"], "reward": 0,
		"repeatable": true, "job": true, "fail": ["timeout", "wrecked", "busted", "died"],
		"steps": [],
	},
}

const TEXTS_FR := {
	"m01_title": "Bienvenue à New York",
	"m01_desc": "Retrouve ton cousin Dayo à son garage de Midtown.",
	"m01_d1": "Bon. Le bus m'a laissé ici. Tout est immense…",
	"m01_d2": "Le garage de Dayo est un peu plus haut, vers la 6e Avenue. Je vais suivre le GPS.",
	"m01_o1": "Rejoins le Garage Kanza (marqueur jaune)",
	"m01_d3": "Noah ! Enfin ! Regarde-toi, tu as fait le voyage en entier ?",
	"m01_d4": "Cousin, je suis prêt à travailler. N'importe quoi.",
	"m01_d5": "Repose-toi un peu, puis reviens me voir : j'ai déjà une course pour toi. Et passe chez Rosa, son épicerie est toujours ouverte.",
	"m02_title": "Premier volant",
	"m02_desc": "Dayo te prête une berline pour aller chercher le déjeuner chez Rosa.",
	"m02_d1": "Prends la berline bleue devant le garage. Pas une rayure, hein ! Va chercher la commande chez Rosa, 8e Avenue.",
	"m02_o1": "Monte dans la berline",
	"m02_o2": "Va à l'épicerie Chez Rosa",
	"m02_d2": "Ah, c'est toi le cousin ? Tiens, deux sacs pour Dayo. Et dis-lui qu'il me doit encore un café !",
	"m02_d3": "Merci Rosa ! Je reviendrai.",
	"m02_o3": "Rapporte le déjeuner au garage",
	"m02_d4": "Pile à l'heure, et la voiture est entière. Tu conduis mieux que moi ! Tiens, pour ta peine.",
	"m03_title": "Livraison express",
	"m03_desc": "Rosa doit livrer deux commandes avant le rush de midi.",
	"m03_d1": "Mon livreur est malade. Prends l'utilitaire : un plateau-repas pour l'Hôtel Meridian, puis le traiteur de la tour Draven. Vite !",
	"m03_o1": "Monte dans l'utilitaire de Rosa",
	"m03_o2": "Livre l'Hôtel Meridian",
	"m03_o3": "Livre la tour Draven Holdings",
	"m03_d2": "Les deux clients sont contents. Tu peux revenir livrer pour moi quand tu veux : passe à l'épicerie.",
	"m04_title": "La cliente de Times Square",
	"m04_desc": "Remplace un chauffeur de taxi pour une cliente importante.",
	"m04_d1": "Mon ami de la compagnie de taxis a besoin d'un chauffeur. Une cliente t'attend à Times Square. Sois poli.",
	"m04_o1": "Monte dans le taxi",
	"m04_o2": "Prends la cliente à Times Square (arrête-toi dans le cercle)",
	"m04_d2": "Tour Draven Holdings, et vite. J'ai une réunion avec M. Draven.",
	"m04_d3": "Draven ? Comme les panneaux « Bientôt : Draven Plaza » partout dans le quartier ?",
	"m04_d4": "Il rachète tout Midtown, bloc par bloc. Même le garage de la 6e Avenue, paraît-il.",
	"m04_o3": "Dépose la cliente à la tour Draven",
	"m04_d5": "Parfait. Tenez, gardez la monnaie. M. Draven aime les gens efficaces… il voudra peut-être vous voir.",
	"m05_title": "L'offre de Draven",
	"m05_desc": "Leon Draven veut te parler dans sa tour.",
	"m05_d1": "Asseyez-vous, Noah. On m'a dit que vous conduisiez vite et que vous ne posiez pas de questions.",
	"m05_d2": "Votre cousin refuse de vendre son garage. Convainquez-le de signer et je vous donne 1 500 dollars, tout de suite.",
	"m05_choice": "Que réponds-tu à Draven ?",
	"m05_opt_accept": "Accepter l'argent (+$1500)",
	"m05_opt_refuse": "Refuser : la famille d'abord",
	"m05_o1": "Retourne au garage parler à Dayo",
	"m05_d3a": "Tu as pris son argent ? … Je comprends, la vie est chère ici. Mais ne compte plus sur moi pour les réparations gratuites.",
	"m05_d3b": "Tu as refusé ? Noah… Merci. Ici, c'est chez toi aussi. Désormais les réparations sont pour moi.",
	"r01_title": "Circuit de Midtown",
	"r01_desc": "Course contre la montre : passe tous les points de passage en moins de 95 secondes. Il faut un véhicule.",
	"r01_o1": "Atteins le point de passage",
	"j01_title": "Livreur pour Rosa",
	"j01_desc": "Livre une commande dans le quartier. Plus c'est loin, plus ça paie.",
	"j01_o1": "Livre la commande : {0}",
	"need_vehicle": "Il te faut un véhicule pour cette activité.",
	"mission_offer": "{0}\n\n{1}\n\nRécompense : ${2}",
}

const TEXTS_EN := {
	"m01_title": "Welcome to New York",
	"m01_desc": "Meet your cousin Dayo at his Midtown garage.",
	"m01_d1": "Okay. The bus dropped me here. Everything is huge…",
	"m01_d2": "Dayo's garage is a bit further up, near 6th Avenue. I'll follow the GPS.",
	"m01_o1": "Reach Kanza Garage (yellow marker)",
	"m01_d3": "Noah! Finally! Look at you, you made the whole trip?",
	"m01_d4": "Cousin, I'm ready to work. Anything.",
	"m01_d5": "Rest a bit, then come see me: I already have a job for you. And stop by Rosa's, her deli never closes.",
	"m02_title": "First drive",
	"m02_desc": "Dayo lends you a sedan to pick up lunch at Rosa's.",
	"m02_d1": "Take the blue sedan in front of the garage. Not a scratch, okay? Pick up the order at Rosa's on 8th Avenue.",
	"m02_o1": "Get in the sedan",
	"m02_o2": "Drive to Rosa's Deli",
	"m02_d2": "So you're the cousin? Here, two bags for Dayo. And tell him he still owes me a coffee!",
	"m02_d3": "Thanks Rosa! I'll be back.",
	"m02_o3": "Bring lunch back to the garage",
	"m02_d4": "Right on time, and the car is in one piece. You drive better than me! Here, for your trouble.",
	"m03_title": "Express delivery",
	"m03_desc": "Rosa must deliver two orders before the lunch rush.",
	"m03_d1": "My courier is sick. Take the van: a lunch tray for the Meridian Hotel, then the catering for Draven tower. Hurry!",
	"m03_o1": "Get in Rosa's van",
	"m03_o2": "Deliver to the Meridian Hotel",
	"m03_o3": "Deliver to Draven Holdings tower",
	"m03_d2": "Both customers are happy. Come deliver for me anytime: just stop by the deli.",
	"m04_title": "The Times Square client",
	"m04_desc": "Stand in for a cab driver for an important client.",
	"m04_d1": "My friend at the cab company needs a driver. A client is waiting in Times Square. Be polite.",
	"m04_o1": "Get in the cab",
	"m04_o2": "Pick up the client in Times Square (stop in the circle)",
	"m04_d2": "Draven Holdings tower, and fast. I have a meeting with Mr. Draven.",
	"m04_d3": "Draven? Like the \"Coming soon: Draven Plaza\" signs all over the neighborhood?",
	"m04_d4": "He's buying all of Midtown, block by block. Even the garage on 6th Avenue, I hear.",
	"m04_o3": "Drop the client at Draven tower",
	"m04_d5": "Perfect. Keep the change. Mr. Draven likes efficient people… he may want to see you.",
	"m05_title": "Draven's offer",
	"m05_desc": "Leon Draven wants to talk to you in his tower.",
	"m05_d1": "Sit down, Noah. I'm told you drive fast and don't ask questions.",
	"m05_d2": "Your cousin refuses to sell his garage. Convince him to sign and I'll give you 1,500 dollars, right now.",
	"m05_choice": "What do you tell Draven?",
	"m05_opt_accept": "Take the money (+$1500)",
	"m05_opt_refuse": "Refuse: family first",
	"m05_o1": "Go back to the garage and talk to Dayo",
	"m05_d3a": "You took his money? … I get it, life is expensive here. But don't count on me for free repairs anymore.",
	"m05_d3b": "You refused? Noah… Thank you. This is your home too. From now on, repairs are on me.",
	"r01_title": "Midtown circuit",
	"r01_desc": "Time trial: pass every checkpoint in under 95 seconds. You need a vehicle.",
	"r01_o1": "Reach the checkpoint",
	"j01_title": "Courier for Rosa",
	"j01_desc": "Deliver an order in the neighborhood. The farther, the better it pays.",
	"j01_o1": "Deliver the order: {0}",
	"need_vehicle": "You need a vehicle for this activity.",
	"mission_offer": "{0}\n\n{1}\n\nReward: ${2}",
}


static func register_texts() -> void:
	Loc.register("fr", TEXTS_FR)
	Loc.register("en", TEXTS_EN)


static func get_def(id: String) -> Dictionary:
	return MISSIONS.get(id, {})


static func is_available(id: String) -> bool:
	var m: Dictionary = MISSIONS[id]
	if GameState.is_done(id) and not m.get("repeatable", false):
		return false
	for r in m.get("requires", []):
		if not GameState.is_done(r):
			return false
	return true


## Mission de livraison générée : destination aléatoire, récompense selon la distance.
static func make_delivery_job(rng: RandomNumberGenerator) -> Dictionary:
	var ids := CityLayout.LOCATIONS.keys()
	ids.erase("deli")
	var dest: String = ids[rng.randi() % ids.size()]
	var dist := CityLayout.location_pos("deli").distance_to(CityLayout.location_pos(dest))
	var reward := int(60 + dist * 0.6)
	var secs := int(25 + dist / 7.0)
	return {
		"title": "j01_title", "desc": "j01_desc", "giver": "deli", "reward": reward, "job": true,
		"repeatable": true, "fail": ["timeout", "wrecked", "busted", "died"], "need_vehicle": true,
		"steps": [
			{"type": "timer", "seconds": secs},
			{"type": "goto", "loc": dest, "text": "j01_o1", "text_arg_loc": true, "vehicle": true, "radius": 6.0},
		],
	}
