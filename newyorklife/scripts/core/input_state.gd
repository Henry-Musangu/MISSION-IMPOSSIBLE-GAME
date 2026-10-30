extends Node
## État d'entrée unifié : commandes tactiles + clavier/souris + manette.
## Les systèmes de jeu lisent uniquement ce nœud, jamais les périphériques directement.

var move := Vector2.ZERO           ## x : droite, y : avant (−1..1)
var look_delta := Vector2.ZERO     ## pixels accumulés depuis la dernière lecture
var touch_move := Vector2.ZERO
var _touch_held := {}
var _pressed := {}
var enabled := true

func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS


const KEY_ACTIONS := {
	"jump": [KEY_SPACE],
	"sprint": [KEY_SHIFT],
	"interact": [KEY_E, KEY_F, KEY_ENTER],
	"gas": [KEY_W, KEY_UP],
	"brake": [KEY_S, KEY_DOWN],
	"handbrake": [KEY_SPACE],
	"horn": [KEY_H],
	"pause": [KEY_ESCAPE, KEY_P],
}


func set_touch_button(button: String, held: bool) -> void:
	var was: bool = _touch_held.get(button, false)
	_touch_held[button] = held
	if held and not was:
		_pressed[button] = true


func add_look(delta: Vector2) -> void:
	look_delta += delta


## Lit et remet à zéro le déplacement de caméra accumulé.
func take_look() -> Vector2:
	var d := look_delta
	look_delta = Vector2.ZERO
	return d


func is_held(button: String) -> bool:
	if not enabled:
		return false
	if _touch_held.get(button, false):
		return true
	for k in KEY_ACTIONS.get(button, []):
		if Input.is_physical_key_pressed(k):
			return true
	var joy := _joy_button(button)
	if joy >= 0 and Input.is_joy_button_pressed(0, joy):
		return true
	return false


## Retourne vrai une seule fois par appui.
func consume(button: String) -> bool:
	if _pressed.get(button, false):
		_pressed[button] = false
		return enabled
	return false


func clear() -> void:
	_pressed.clear()
	_touch_held.clear()
	touch_move = Vector2.ZERO
	move = Vector2.ZERO
	look_delta = Vector2.ZERO


func _joy_button(button: String) -> int:
	match button:
		"jump": return JOY_BUTTON_A
		"interact": return JOY_BUTTON_Y
		"sprint": return JOY_BUTTON_LEFT_SHOULDER
		"handbrake": return JOY_BUTTON_B
		"horn": return JOY_BUTTON_LEFT_STICK
		"pause": return JOY_BUTTON_START
	return -1


func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and not event.echo:
		for action in KEY_ACTIONS:
			if event.physical_keycode in KEY_ACTIONS[action]:
				_pressed[action] = true
	elif event is InputEventJoypadButton and event.pressed:
		for action in ["jump", "interact", "sprint", "handbrake", "horn", "pause"]:
			if _joy_button(action) == event.button_index:
				_pressed[action] = true


func _process(_delta: float) -> void:
	var k := Vector2.ZERO
	if Input.is_physical_key_pressed(KEY_A) or Input.is_physical_key_pressed(KEY_LEFT):
		k.x -= 1.0
	if Input.is_physical_key_pressed(KEY_D) or Input.is_physical_key_pressed(KEY_RIGHT):
		k.x += 1.0
	if Input.is_physical_key_pressed(KEY_W) or Input.is_physical_key_pressed(KEY_UP):
		k.y += 1.0
	if Input.is_physical_key_pressed(KEY_S) or Input.is_physical_key_pressed(KEY_DOWN):
		k.y -= 1.0
	var joy := Vector2(Input.get_joy_axis(0, JOY_AXIS_LEFT_X), -Input.get_joy_axis(0, JOY_AXIS_LEFT_Y))
	if joy.length() < 0.2:
		joy = Vector2.ZERO
	var total := touch_move + k + joy
	move = total.limit_length(1.0) if enabled else Vector2.ZERO
	var rs := Vector2(Input.get_joy_axis(0, JOY_AXIS_RIGHT_X), Input.get_joy_axis(0, JOY_AXIS_RIGHT_Y))
	if rs.length() > 0.2:
		look_delta += rs * 12.0
	# Gâchettes de manette : accélérateur / frein
	if Input.get_joy_axis(0, JOY_AXIS_TRIGGER_RIGHT) > 0.3:
		_touch_held["gas_pad"] = true
	else:
		_touch_held["gas_pad"] = false


func throttle_axis() -> float:
	var t := 0.0
	if is_held("gas") or _touch_held.get("gas_pad", false):
		t += 1.0
	if is_held("brake") or Input.get_joy_axis(0, JOY_AXIS_TRIGGER_LEFT) > 0.3:
		t -= 1.0
	return t


func _input(event: InputEvent) -> void:
	# Souris (PC) : clic droit maintenu = rotation caméra
	if event is InputEventMouseMotion and Input.is_mouse_button_pressed(MOUSE_BUTTON_RIGHT):
		look_delta += event.relative
