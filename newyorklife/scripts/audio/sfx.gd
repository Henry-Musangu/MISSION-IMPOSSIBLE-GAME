extends Node
## Audio entièrement synthétisé par le code (aucun fichier externe, aucune licence) :
## moteur, klaxon, sirène, chocs, interface, ambiance urbaine et musique originale.

const RATE := 22050

var _streams := {}
var _players: Array[AudioStreamPlayer] = []
var _next := 0
var _music: AudioStreamPlayer
var _ambience: AudioStreamPlayer
var _music_task := -1
var _music_data := PackedFloat32Array()
var _mb := PackedFloat32Array()     ## tampon de rendu de la musique (thread)
var _music_wanted := false


func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	_make_buses()
	Settings.apply_audio()
	for i in 8:
		var p := AudioStreamPlayer.new()
		p.bus = "SFX"
		add_child(p)
		_players.append(p)
	_music = AudioStreamPlayer.new()
	_music.bus = "Music"
	add_child(_music)
	_ambience = AudioStreamPlayer.new()
	_ambience.bus = "Ambience"
	_ambience.volume_db = -10.0
	add_child(_ambience)
	_build_all()
	_music_task = WorkerThreadPool.add_task(_render_music)


func _make_buses() -> void:
	for bus_name in ["Music", "SFX", "Ambience"]:
		if AudioServer.get_bus_index(bus_name) < 0:
			AudioServer.add_bus()
			var idx := AudioServer.bus_count - 1
			AudioServer.set_bus_name(idx, bus_name)
			AudioServer.set_bus_send(idx, "Master")


func get_stream(sound: String) -> AudioStream:
	return _streams.get(sound)


func play(sound: String, volume_db := 0.0, pitch := 1.0) -> void:
	var s: AudioStream = _streams.get(sound)
	if s == null:
		return
	var p := _players[_next]
	_next = (_next + 1) % _players.size()
	p.stream = s
	p.volume_db = volume_db
	p.pitch_scale = pitch
	p.play()


func set_music(on: bool) -> void:
	_music_wanted = on
	if on and _streams.has("music"):
		if not _music.playing:
			_music.stream = _streams["music"]
			_music.play()
	elif not on and _music.playing:
		_music.stop()


func set_ambience(on: bool) -> void:
	if on and not _ambience.playing:
		_ambience.stream = _streams["ambience"]
		_ambience.play()
	elif not on and _ambience.playing:
		_ambience.stop()


func _process(_delta: float) -> void:
	if _music_task >= 0 and WorkerThreadPool.is_task_completed(_music_task):
		WorkerThreadPool.wait_for_task_completion(_music_task)
		_music_task = -1
		_streams["music"] = _to_stream(_music_data, true)
		_music_data = PackedFloat32Array()
		if _music_wanted:
			set_music(true)


# ------------------------------------------------------------------ synthèse

func _to_stream(buf: PackedFloat32Array, loop: bool) -> AudioStreamWAV:
	var bytes := PackedByteArray()
	bytes.resize(buf.size() * 2)
	for i in buf.size():
		bytes.encode_s16(i * 2, int(clampf(buf[i], -1.0, 1.0) * 32000.0))
	var s := AudioStreamWAV.new()
	s.format = AudioStreamWAV.FORMAT_16_BITS
	s.mix_rate = RATE
	s.stereo = false
	s.data = bytes
	if loop:
		s.loop_mode = AudioStreamWAV.LOOP_FORWARD
		s.loop_begin = 0
		s.loop_end = buf.size()
	return s


func _buf(seconds: float) -> PackedFloat32Array:
	var b := PackedFloat32Array()
	b.resize(int(seconds * RATE))
	return b


func _build_all() -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = 77
	# Moteur : dents de scie + harmoniques, 24 périodes exactes pour une boucle propre
	var b := _buf(0.4)
	var lp := 0.0
	for i in b.size():
		var t := float(i) / RATE
		var ph := fmod(t * 60.0, 1.0)
		var saw := ph * 2.0 - 1.0
		lp += (rng.randf_range(-1, 1) - lp) * 0.08
		b[i] = saw * 0.35 + sin(t * TAU * 120.0) * 0.25 + sin(t * TAU * 30.0) * 0.3 + lp * 0.25
	_streams["engine"] = _to_stream(b, true)
	# Klaxon (deux tons)
	b = _buf(0.6)
	for i in b.size():
		var t := float(i) / RATE
		var v := sin(t * TAU * 415.0) + sin(t * TAU * 523.0) * 0.8
		b[i] = clampf(v * 1.4, -1.0, 1.0) * 0.45
	_streams["horn"] = _to_stream(b, true)
	# Sirène (balayage)
	b = _buf(2.0)
	var phase := 0.0
	for i in b.size():
		var t := float(i) / RATE
		var f := 950.0 + sin(t * TAU * 0.5 - PI / 2.0) * 380.0
		phase += f / RATE
		b[i] = (sin(phase * TAU) * 0.7 + sin(phase * TAU * 2.0) * 0.15) * 0.5
	_streams["siren"] = _to_stream(b, true)
	# Choc
	b = _buf(0.5)
	lp = 0.0
	for i in b.size():
		var t := float(i) / RATE
		lp += (rng.randf_range(-1, 1) - lp) * 0.3
		b[i] = (lp * 1.6 + sin(t * TAU * 70.0) * 0.6) * exp(-t * 9.0)
	_streams["crash"] = _to_stream(b, false)
	_streams["hit"] = _tone([[110.0, 0.0, 0.18]], 0.2, 25.0, 0.8, true)
	_streams["jump"] = _sweep(300.0, 520.0, 0.12, 0.3)
	_streams["door"] = _tone([[90.0, 0.0, 0.2], [180.0, 0.0, 0.1]], 0.25, 18.0, 0.7, true)
	_streams["cash"] = _tone([[1318.5, 0.0, 0.3], [1760.0, 0.08, 0.3]], 0.45, 7.0, 0.35, false)
	_streams["success"] = _tone([[523.25, 0.0, 0.3], [659.25, 0.15, 0.3], [783.99, 0.3, 0.3], [1046.5, 0.45, 0.7]], 1.2, 3.5, 0.35, false)
	_streams["fail"] = _tone([[392.0, 0.0, 0.3], [349.23, 0.25, 0.3], [261.63, 0.5, 0.5]], 1.0, 3.0, 0.35, false)
	_streams["checkpoint"] = _tone([[1046.5, 0.0, 0.2], [1568.0, 0.06, 0.2]], 0.3, 10.0, 0.35, false)
	_streams["alert"] = _tone([[880.0, 0.0, 0.18], [660.0, 0.2, 0.18]], 0.45, 4.0, 0.3, false)
	_streams["click"] = _tone([[1200.0, 0.0, 0.04]], 0.05, 60.0, 0.25, false)
	_streams["metro"] = _tone([[659.25, 0.0, 0.4], [523.25, 0.35, 0.4], [392.0, 0.7, 0.6]], 1.4, 2.5, 0.3, false)
	# Tonnerre
	b = _buf(2.6)
	lp = 0.0
	var lp2 := 0.0
	for i in b.size():
		var t := float(i) / RATE
		lp += (rng.randf_range(-1, 1) - lp) * 0.05
		lp2 += (lp - lp2) * 0.1
		b[i] = lp2 * 6.0 * exp(-t * 1.4) * (1.0 + 0.5 * sin(t * 9.0))
	_streams["thunder"] = _to_stream(b, false)
	# Ambiance urbaine : grondement grave continu + souffle
	b = _buf(6.0)
	lp = 0.0
	lp2 = 0.0
	for i in b.size():
		var t := float(i) / RATE
		lp += (rng.randf_range(-1, 1) - lp) * 0.02
		lp2 += (rng.randf_range(-1, 1) - lp2) * 0.25
		var swell := 0.7 + 0.3 * sin(t * TAU / 6.0)
		b[i] = (lp * 3.0 + lp2 * 0.05) * swell
	# fondu de boucle
	var fade := int(0.3 * RATE)
	for i in fade:
		var a := float(i) / fade
		b[i] = b[i] * a + b[b.size() - fade + i] * (1.0 - a)
	b.resize(b.size() - fade)
	_streams["ambience"] = _to_stream(b, true)


## notes : [[fréquence, début (s), durée (s)], …]
func _tone(notes: Array, length: float, decay: float, vol: float, low: bool) -> AudioStreamWAV:
	var b := _buf(length)
	for n in notes:
		var f: float = n[0]
		var start := int(float(n[1]) * RATE)
		var dur := int(float(n[2]) * RATE)
		for k in dur:
			var i := start + k
			if i >= b.size():
				break
			var t := float(k) / RATE
			var env := exp(-t * decay) * minf(1.0, t * 400.0)
			var v := sin(t * TAU * f)
			if not low:
				v += sin(t * TAU * f * 2.0) * 0.25
			b[i] += v * env * vol
	return _to_stream(b, false)


func _sweep(f0: float, f1: float, length: float, vol: float) -> AudioStreamWAV:
	var b := _buf(length)
	var ph := 0.0
	for i in b.size():
		var t := float(i) / b.size()
		ph += lerpf(f0, f1, t) / RATE
		b[i] = sin(ph * TAU) * vol * (1.0 - t)
	return _to_stream(b, false)


## Musique originale (boucle de 8 mesures, 96 BPM) : piano électrique, basse, batterie douce.
func _render_music() -> void:
	var bpm := 96.0
	var beat := 60.0 / bpm
	var bars := 8
	var total := bars * 4 * beat
	_mb = PackedFloat32Array()
	_mb.resize(int(total * RATE))
	# Progression : Am – F – C – G (2 mesures chacun)
	var chords := [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]]
	var bass := [45, 41, 48, 43]
	for bar in bars:
		var ch: Array = chords[int(bar / 2) % 4]
		var t0 := bar * 4 * beat
		# arpèges (croches)
		for step in 8:
			var note: int = ch[[0, 1, 2, 1, 0, 2, 1, 2][step]] + (12 if step % 4 == 2 else 0)
			_add_note(t0 + step * beat * 0.5, beat * 0.9, _midi(note), 0.12, 4.0, true)
		# nappes
		for n in ch:
			_add_note(t0, beat * 4.0, _midi(n), 0.035, 0.6, false)
		# basse
		var bn: int = bass[int(bar / 2) % 4]
		_add_note(t0, beat * 1.5, _midi(bn), 0.28, 2.0, false)
		_add_note(t0 + beat * 2.0, beat * 1.0, _midi(bn), 0.22, 2.5, false)
		_add_note(t0 + beat * 3.0, beat * 1.0, _midi(bn + 7), 0.18, 2.5, false)
		# batterie
		for k in 4:
			var tk := t0 + k * beat
			if k % 2 == 0:
				_add_kick(tk)
			else:
				_add_snare(tk, bar * 4 + k)
			_add_hat(tk + beat * 0.5, bar * 8 + k)
	# légère compression
	for i in _mb.size():
		_mb[i] = tanh(_mb[i] * 1.4) * 0.8
	_music_data = _mb


func _midi(n: int) -> float:
	return 440.0 * pow(2.0, (n - 69) / 12.0)


func _add_note(start: float, dur: float, f: float, vol: float, decay: float, bell: bool) -> void:
	var s := int(start * RATE)
	var n := int(dur * RATE)
	for k in n:
		var i := s + k
		if i >= _mb.size():
			return
		var t := float(k) / RATE
		var env := exp(-t * decay) * minf(1.0, t * 200.0) * minf(1.0, float(n - k) / 400.0)
		var v := sin(t * TAU * f)
		if bell:
			v = v * 0.8 + sin(t * TAU * f * 2.0) * 0.2 * exp(-t * 6.0)
		_mb[i] += v * env * vol


func _add_kick(start: float) -> void:
	var s := int(start * RATE)
	var ph := 0.0
	for k in int(0.25 * RATE):
		var i := s + k
		if i >= _mb.size():
			return
		var t := float(k) / RATE
		ph += (45.0 + 90.0 * exp(-t * 30.0)) / RATE
		_mb[i] += sin(ph * TAU) * exp(-t * 12.0) * 0.5


func _add_snare(start: float, seed_v: int) -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = seed_v
	var s := int(start * RATE)
	for k in int(0.18 * RATE):
		var i := s + k
		if i >= _mb.size():
			return
		var t := float(k) / RATE
		_mb[i] += (rng.randf_range(-1, 1) * 0.6 + sin(t * TAU * 190.0) * 0.4) * exp(-t * 22.0) * 0.22


func _add_hat(start: float, seed_v: int) -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = seed_v + 1000
	var s := int(start * RATE)
	var prev := 0.0
	for k in int(0.05 * RATE):
		var i := s + k
		if i >= _mb.size():
			return
		var t := float(k) / RATE
		var n := rng.randf_range(-1, 1)
		_mb[i] += (n - prev) * exp(-t * 60.0) * 0.08
		prev = n
