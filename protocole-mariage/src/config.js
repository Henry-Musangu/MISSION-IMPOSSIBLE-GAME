// Connexion intégrée à l'APK (facultatif).
// Laissez `null` : l'administrateur colle alors le « code de connexion » dans Réglages.
// Pour que tous les téléphones se connectent tout seuls, remplacez null par :
//   { c: { apiKey: "...", authDomain: "...", databaseURL: "https://...firebasedatabase.app", projectId: "...", appId: "..." }, t: "cj-xxxxxxxxxxxx" }
// (c = configuration Firebase, t = code d'équipe), puis recompilez.
export const BAKED = null;

export const APP_VERSION = "1.0.0";
