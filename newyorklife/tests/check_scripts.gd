extends Node
## Charge chaque script du projet (singletons actifs) pour détecter les erreurs de compilation.


func _ready() -> void:
	var files: Array[String] = []
	_collect("res://scripts", files)
	_collect("res://tests", files)
	var bad := 0
	for f in files:
		var s = load(f)
		if s == null or not (s as Script).can_instantiate():
			print("CHECK FAIL ", f)
			bad += 1
	print("CHECK RESULT: %d scripts, %d en erreur" % [files.size(), bad])
	get_tree().quit(0 if bad == 0 else 1)


func _collect(dir_path: String, out: Array[String]) -> void:
	var d := DirAccess.open(dir_path)
	if d == null:
		return
	for sub in d.get_directories():
		_collect(dir_path.path_join(sub), out)
	for f in d.get_files():
		if f.ends_with(".gd"):
			out.append(dir_path.path_join(f))
