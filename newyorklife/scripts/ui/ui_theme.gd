class_name UITheme
extends RefCounted
## Thème sobre : panneaux sombres translucides, texte clair, une seule couleur d'accent.

const BG := Color(0.07, 0.08, 0.1, 0.88)
const BG_SOLID := Color(0.07, 0.08, 0.1, 0.97)
const PANEL := Color(0.12, 0.13, 0.16, 0.92)
const ACCENT := Color(0.96, 0.7, 0.2)
const TEXT := Color(0.94, 0.94, 0.92)
const MUTED := Color(0.62, 0.64, 0.68)
const GOOD := Color(0.4, 0.85, 0.5)
const BAD := Color(0.95, 0.35, 0.3)
const MONEY := Color(0.45, 0.85, 0.45)

static var _theme: Theme


static func box(c: Color, radius := 10, border := 0, border_col := ACCENT, pad := 12) -> StyleBoxFlat:
	var s := StyleBoxFlat.new()
	s.bg_color = c
	s.set_corner_radius_all(radius)
	s.set_border_width_all(border)
	s.border_color = border_col
	s.content_margin_left = pad
	s.content_margin_right = pad
	s.content_margin_top = pad * 0.6
	s.content_margin_bottom = pad * 0.6
	return s


static func get_theme() -> Theme:
	if _theme:
		return _theme
	var t := Theme.new()
	t.default_font_size = 22
	t.set_color("font_color", "Label", TEXT)
	t.set_color("font_color", "Button", TEXT)
	t.set_color("font_hover_color", "Button", Color.WHITE)
	t.set_color("font_pressed_color", "Button", Color(0.1, 0.1, 0.1))
	t.set_color("font_focus_color", "Button", TEXT)
	t.set_stylebox("normal", "Button", box(Color(0.16, 0.17, 0.21, 0.95), 10, 0, ACCENT, 18))
	t.set_stylebox("hover", "Button", box(Color(0.22, 0.23, 0.28, 0.95), 10, 2, ACCENT, 18))
	t.set_stylebox("pressed", "Button", box(ACCENT, 10, 0, ACCENT, 18))
	t.set_stylebox("focus", "Button", box(Color(0, 0, 0, 0), 10, 2, ACCENT, 18))
	t.set_stylebox("disabled", "Button", box(Color(0.12, 0.12, 0.14, 0.6), 10, 0, ACCENT, 18))
	t.set_color("font_disabled_color", "Button", MUTED)
	t.set_stylebox("panel", "PanelContainer", box(BG, 14, 0, ACCENT, 18))
	t.set_stylebox("panel", "Panel", box(BG, 14))
	t.set_stylebox("slider", "HSlider", box(Color(0.25, 0.26, 0.3), 4, 0, ACCENT, 3))
	t.set_stylebox("grabber_area", "HSlider", box(ACCENT, 4, 0, ACCENT, 3))
	t.set_stylebox("grabber_area_highlight", "HSlider", box(ACCENT, 4, 0, ACCENT, 3))
	t.set_stylebox("background", "ProgressBar", box(Color(0.1, 0.1, 0.12, 0.8), 5, 0, ACCENT, 0))
	t.set_stylebox("fill", "ProgressBar", box(GOOD, 5, 0, ACCENT, 0))
	t.set_constant("separation", "VBoxContainer", 10)
	t.set_constant("separation", "HBoxContainer", 10)
	_theme = t
	return t


static func label(text: String, size := 22, color := TEXT) -> Label:
	var l := Label.new()
	l.text = text
	l.add_theme_font_size_override("font_size", size)
	l.add_theme_color_override("font_color", color)
	l.add_theme_color_override("font_outline_color", Color(0, 0, 0, 0.8))
	l.add_theme_constant_override("outline_size", 4)
	l.mouse_filter = Control.MOUSE_FILTER_IGNORE
	return l


static func button(text: String, cb: Callable, min_w := 320, size := 24) -> Button:
	var b := Button.new()
	b.text = text
	b.custom_minimum_size = Vector2(min_w, 58)
	b.add_theme_font_size_override("font_size", size)
	b.pressed.connect(func():
		Sfx.play("click", -8.0)
		cb.call()
	)
	return b
