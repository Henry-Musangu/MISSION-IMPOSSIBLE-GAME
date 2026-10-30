class_name SaveSystem
extends RefCounted
## Sauvegarde locale JSON : écriture atomique (fichier temporaire + renommage),
## copie de secours, somme de contrôle et migration des anciennes versions.

const SAVE_VERSION := 1
const DEFAULT_PATH := "user://save_slot1.json"


static func backup_path(path: String) -> String:
	return path.get_basename() + ".bak.json"


static func exists(path: String = DEFAULT_PATH) -> bool:
	return FileAccess.file_exists(path) or FileAccess.file_exists(backup_path(path))


static func _checksum(text: String) -> String:
	return text.sha256_text().substr(0, 16)


static func write(data: Dictionary, path: String = DEFAULT_PATH) -> bool:
	var payload := data.duplicate(true)
	payload["save_version"] = SAVE_VERSION
	var body := JSON.stringify(payload)
	var wrapper := {"checksum": _checksum(body), "body": body}
	var tmp := path + ".tmp"
	var f := FileAccess.open(tmp, FileAccess.WRITE)
	if f == null:
		push_warning("Sauvegarde impossible : %s" % FileAccess.get_open_error())
		return false
	f.store_string(JSON.stringify(wrapper))
	f.close()
	# Vérifie la relecture avant de remplacer l'ancienne sauvegarde.
	if read_file(tmp).is_empty():
		return false
	var dir := DirAccess.open(path.get_base_dir())
	if dir == null:
		return false
	if FileAccess.file_exists(path):
		if FileAccess.file_exists(backup_path(path)):
			dir.remove(backup_path(path).get_file())
		dir.rename(path.get_file(), backup_path(path).get_file())
	var err := dir.rename(tmp.get_file(), path.get_file())
	return err == OK


## Lit un fichier ; renvoie {} si absent, illisible ou corrompu.
static func read_file(path: String) -> Dictionary:
	if not FileAccess.file_exists(path):
		return {}
	var f := FileAccess.open(path, FileAccess.READ)
	if f == null:
		return {}
	var text := f.get_as_text()
	f.close()
	var wrapper = JSON.parse_string(text)
	if not (wrapper is Dictionary) or not wrapper.has("body"):
		return {}
	var body: String = str(wrapper["body"])
	if _checksum(body) != str(wrapper.get("checksum", "")):
		push_warning("Sauvegarde corrompue (somme de contrôle) : %s" % path)
		return {}
	var data = JSON.parse_string(body)
	if not (data is Dictionary):
		return {}
	return migrate(data)


## Lit la sauvegarde principale, ou la copie de secours si elle est corrompue.
static func read(path: String = DEFAULT_PATH) -> Dictionary:
	var d := read_file(path)
	if d.is_empty():
		d = read_file(backup_path(path))
	return d


static func delete(path: String = DEFAULT_PATH) -> void:
	var dir := DirAccess.open(path.get_base_dir())
	if dir == null:
		return
	for p in [path, backup_path(path), path + ".tmp"]:
		if FileAccess.file_exists(p):
			dir.remove(p.get_file())


## Met à niveau les sauvegardes des versions précédentes.
static func migrate(data: Dictionary) -> Dictionary:
	var v := int(data.get("save_version", 0))
	if v < 1:
		# Version 0 (prototype) : pas de liste de véhicules ni de drapeaux.
		if not data.has("owned_vehicles"):
			data["owned_vehicles"] = []
		if not data.has("flags"):
			data["flags"] = {}
		data["save_version"] = 1
	return data
