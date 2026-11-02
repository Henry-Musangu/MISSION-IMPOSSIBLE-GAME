import "@fontsource/jost/latin-300.css";
import "@fontsource/jost/latin-400.css";
import "@fontsource/jost/latin-500.css";
import "@fontsource/jost/latin-600.css";
import "@fontsource/cormorant-garamond/latin-400.css";
import "@fontsource/cormorant-garamond/latin-500.css";
import "@fontsource/cormorant-garamond/latin-600.css";
import "@fontsource/cormorant-garamond/latin-400-italic.css";
import "@fontsource/pinyon-script/latin-400.css";
import "leaflet/dist/leaflet.css";
import "./styles.css";
import L from "leaflet";
import { App } from "@capacitor/app";
import { Capacitor, SystemBars, SystemBarsStyle } from "@capacitor/core";
import { S, Store, LS, start, put, del, getOne, ensure, onChange, onError, claimAdmin, changeAdminPassword, leaveAdmin,
  encodeCode, decodeCode, parseFirebaseConfig, savedConnection, saveConnection, forgetConnection, newTeamCode } from "./store.js";
import { Geo, onGeo, startWatch, stopWatch, startCompass, stopCompass, getOnce, geoErrorText, distance, bearing, fmtDist, dirName, walkMinutes, mapsDirections } from "./geo.js";
import { APP_VERSION } from "./config.js";

/* ================= Constantes ================= */
const TZ = "Africa/Kinshasa";
const APK_URL = "https://github.com/Henry-Musangu/MISSION-IMPOSSIBLE-GAME/releases/download/protocole/protocole-clarisse-john.apk";
const EVENTS = [
  { t: Date.UTC(2026, 10, 12, 8, 0), label: "la mairie" },
  { t: Date.UTC(2026, 10, 12, 14, 0), label: "la bénédiction nuptiale" },
  { t: Date.UTC(2026, 10, 13, 19, 0), label: "la réception" }
];
const DEFAULT_PROG = [
  { id: "mairie", jour: "12", heure: "09:00", titre: "Mairie", lieu: "Maison communale de Limete", adresse: "13e rue, côté résidentiel, Limete", note: "" },
  { id: "benediction", jour: "12", heure: "15:00", titre: "Bénédiction nuptiale", lieu: "Église évangélique Nuée de Gloire", adresse: "N°33, avenue Bosembo, 7e rue Limete Industrielle", note: "Référence : Collègue le Cartésien" },
  { id: "reception", jour: "13", heure: "20:00", titre: "Réception", lieu: "Salle de fête B Best Palace", adresse: "N°140, avenue Ingende, Ngiri-Ngiri", note: "Référence : croisement des avenues Saïo et Ingende, près de la boulangerie UPAK / Maman Poto" }
];
const DEFAULT_DEPTS = [
  { id: "accueil", nom: "Accueil & installation", court: "Accueil", ic: "door", ordre: 1, chef: "audry-tshiamala", adjoint: "", mission: "Recevoir chaque invité avec élégance et le conduire jusqu'à sa table." },
  { id: "boissons", nom: "Service boissons", court: "Boissons", ic: "glass", ordre: 2, chef: "lewis-mulunda", adjoint: "", mission: "Servir chaque table à temps, sans jamais manquer de rien." },
  { id: "nourriture", nom: "Service nourriture", court: "Nourriture", ic: "plate", ordre: 3, chef: "ruth-kaseka", adjoint: "", mission: "Servir chaque table dans l'ordre : entrée, plat, dessert." },
  { id: "coordination", nom: "Coordination", court: "Coordination", ic: "compass", ordre: 4, chef: "", adjoint: "", mission: "Veiller au déroulé, aux annonces et à chaque imprévu." }
];
const SEED_MEMBERS = [
  ["audry-tshiamala", "Audry Tshiamala", "Sa Majesté 👑👑", "accueil"], ["lewis-mulunda", "Lewis Mulunda", "lewiston business", "boissons"],
  ["ruth-kaseka", "Ruth Kaseka", "Ruth Kanana", "nourriture"], ["elmorisson-lukusa", "Elmorisson Lukusa", "", "coordination", "Administrateur du groupe"],
  ["charlene-mbombo", "Charlène Mbombo"], ["chico-kazadi", "Chico Kazadi"], ["fbt", "FBT"], ["fleurette", "Fleurette"], ["jermie-tshim", "Jermie Tshim's"],
  ["joyce-muntu", "Joyce Muntu"], ["olivier-mk", "Olivier MK"], ["paulwoto", "PaulWoto"], ["ya-serge", "Ya Serge"], ["aimerance", "Aimerance"],
  ["caleb-ilunga", "Caleb Ilunga"], ["cherubin-kjr", "Chérubin Kjr"], ["dieu-est-fidele", "Dieu est Fidèle"], ["dr-lucien-kaso", "Dr Lucien Kaso…"],
  ["dr-martin-malu", "Dr Martin Malu"], ["grace-inda", "Grace Inda"], ["gradikbg", "Gradikbg"], ["mima-shenda", "Mima Shenda"], ["miss-ka", "Miss Ka"],
  ["mj-blaze", "Mj-blaze"], ["mosche-nkishi", "Mosché Nkishi"], ["nadege-bitota", "Nadege Bitota"], ["niceyou", "Niceyou"], ["prisca-mbulu", "Prisca Mbulu"],
  ["ruth-kn", "Ruth KN"], ["sem", "SEM"], ["a-identifier", "À identifier", "+243 832 992 616"]
];
const SEED_TASKS = [
  ["accueil", "Imprimer la liste des invités par table (copie de secours)"], ["accueil", "Poser les numéros de table avant l'ouverture des portes"],
  ["accueil", "Répartir l'équipe : entrée, allées, table d'honneur"], ["accueil", "Réserver les places des familles à la mairie et à l'église"],
  ["boissons", "Compter le stock à la livraison et le saisir dans l'application"], ["boissons", "Vérifier glaces, glacières, ouvre-bouteilles et verres"],
  ["boissons", "Attribuer un groupe de tables à chaque serveur"], ["boissons", "Préparer le service de la table d'honneur"],
  ["nourriture", "Confirmer avec le traiteur l'heure du service"], ["nourriture", "Vérifier assiettes, couverts et serviettes par table"],
  ["nourriture", "Servir la table d'honneur en premier"], ["nourriture", "Repérer les invités avec un régime particulier"],
  ["coordination", "Fixer les heures de convocation du 12 et du 13"], ["coordination", "Valider la tenue de chaque protocole à l'arrivée"],
  ["coordination", "Caler le déroulé de la soirée avec le maître de cérémonie"]
];
const CATS = ["Famille de la mariée", "Famille du marié", "Amis", "Collègues", "Église", "Officiels", "Autre"];
const GRAV = { info: "À savoir", urgent: "Urgent", critique: "Critique" };
const STAT = { ouvert: "Ouvert", en_cours: "En cours", resolu: "Résolu" };
const LIEUX = ["Mairie", "Église", "Salle de fête", "Autre"];
const MOMENTS = ["Mairie", "Bénédiction", "Réception", "Coulisses"];
const JOURS = { "12": "Jeudi 12 novembre", "13": "Vendredi 13 novembre" };
const SKINS = [
  ["auto", "Automatique", "#0D0A0B", "#FBF7F1", "#D2B07A"], ["noir", "Noir velours", "#0D0A0B", "#161013", "#D2B07A"],
  ["blanc", "Blanc ivoire", "#FBF7F1", "#FFFFFF", "#6E1428"], ["rouge", "Rouge bordeaux", "#3A0915", "#561326", "#E6C58E"],
  ["mixte", "Mixte", "#0D0A0B", "#F5EFE6", "#8A1A33"], ["uni", "Uni sobre", "#18181B", "#222226", "#FFFFFF"],
  ["champagne", "Champagne", "#F3EADC", "#FBF6EE", "#94692A"]
];
const DARK_BARS = { noir: 1, rouge: 1, mixte: 1, uni: 1 };
const DEPT_ICONS = { door: "Porte", glass: "Verre", plate: "Assiette", compass: "Boussole", shield: "Bouclier", gift: "Cadeau", music: "Musique", car: "Voiture", star: "Étoile", heart: "Cœur", camera: "Appareil photo" };

const IC = {
  home: "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z",
  users: "M16 20v-1.5a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4V20M9.5 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7M21 20v-1.5a4 4 0 0 0-3-3.87M15.5 4.13a3.5 3.5 0 0 1 0 6.75",
  list: "M9 6h12M9 12h12M9 18h12M4 6h.01M4 12h.01M4 18h.01",
  alert: "M12 3.5 2.5 20h19L12 3.5zM12 10v4.5M12 17.5h.01",
  camera: "M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1zM12 17a4 4 0 1 0 0-8 4 4 0 0 0 0 8",
  calendar: "M4 5h16v16H4zM4 9.5h16M8.5 3v4M15.5 3v4",
  shirt: "M8.5 3 3 6.2l2.2 4.3 2.3-1.1V21h9V9.4l2.3 1.1L21 6.2 15.5 3a3.5 3.5 0 0 1-7 0z",
  check: "M5 12.5 10 17.5 19.5 7",
  clock: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5.2l3.2 2",
  phone: "M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a1 1 0 0 1-1 1A16 16 0 0 1 4 5a1 1 0 0 1 1-1",
  chat: "M4 20l1.3-3.9A8 8 0 1 1 8 19.2z",
  crown: "M3 8.5l4.2 3.8L12 5l4.8 7.3L21 8.5 19 19H5z",
  search: "M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM20.5 20.5l-4.5-4.5",
  plus: "M12 5v14M5 12h14",
  pin: "M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21zM12 12a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5",
  nav: "M3 11 21 3l-8 18-2-8z",
  menu: "M4 7h16M4 12h16M4 17h10",
  glass: "M7 3h10l-1 8.5a4 4 0 0 1-8 0zM12 15.5V21M8 21h8",
  plate: "M12 20a8 8 0 1 0 0-16 8 8 0 0 0 0 16zM12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6",
  door: "M5 21V4a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v17M3 21h18M15 12h.01",
  compass: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM15.5 8.5l-2 5-5 2 2-5z",
  shield: "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z",
  gift: "M4 10h16v10H4zM3 7h18v3H3zM12 7v13M12 7S10 3 7.5 4 8 7 12 7zM12 7s2-4 4.5-3S16 7 12 7z",
  music: "M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM20 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0z",
  car: "M5 16h14M6 16l1.5-6h9L18 16M5 16v3h3v-3M16 16v3h3v-3M8 13h.01M16 13h.01",
  star: "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z",
  heart: "M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7a4.3 4.3 0 0 1 7.5 2.8C19.5 15.4 12 20 12 20z",
  megaphone: "M3 10v4h3l7 4.5v-13L6 10zM17 9a4 4 0 0 1 0 6",
  x: "M6 6l12 12M18 6 6 18",
  upload: "M12 16V4M7 9l5-5 5 5M4 16v4h16v-4",
  table: "M12 9a8 3 0 1 0 0-.01M4 9v3a8 3 0 0 0 16 0V9M12 15v6M8 21h8",
  edit: "M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4",
  trash: "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3",
  gear: "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
  wifi: "M2 8.8a15 15 0 0 1 20 0M5 12.3a10 10 0 0 1 14 0M8.5 15.8a5 5 0 0 1 7 0M12 19.5h.01",
  lock: "M6 11h12v10H6zM8 11V7a4 4 0 0 1 8 0v4",
  palette: "M12 21a9 9 0 1 1 9-9c0 2-1.5 3-3 3h-2a2 2 0 0 0-1.5 3.3A1.6 1.6 0 0 1 12 21zM7.5 11.5h.01M10 7.5h.01M14.5 7.5h.01M17 11h.01",
  download: "M12 4v12M7 11l5 5 5-5M4 20h16"
};
const ic = (n, s = 20) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${IC[n] || IC.star}"/></svg>`;
const DIAMOND = `<svg width="9" height="9" viewBox="0 0 10 10" aria-hidden="true"><path d="M5 0 10 5 5 10 0 5z" fill="currentColor"/></svg>`;
const ARROW = `<svg class="arrow" id="g-arrow" viewBox="0 0 100 100" aria-hidden="true"><path d="M50 4 82 88 50 70 18 88z" fill="currentColor"/></svg>`;

/* ================= Utilitaires ================= */
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const norm = s => String(s || "").normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
const newId = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
function slug(s) { const x = norm(s).replace(/[^a-z0-9_-]+/g, "-").replace(/^-+|-+$/g, "").slice(0, 60); return x || ("t-" + newId()); }
const natCmp = (a, b) => String(a).localeCompare(String(b), "fr", { numeric: true, sensitivity: "base" });
const clean = o => { const r = { ...o }; delete r.id; return r; };
const hhmm = ts => new Date(ts).toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit", timeZone: TZ });
function ago(ts) { if (!ts) return ""; const m = Math.round((Date.now() - ts) / 60000); if (m < 1) return "à l'instant"; if (m < 60) return `il y a ${m} min`; const h = Math.floor(m / 60); if (h < 24) return `il y a ${h} h`; return new Date(ts).toLocaleDateString("fr-FR", { day: "numeric", month: "short", timeZone: TZ }); }
const kinDay = (ts = Date.now()) => new Date(ts).toLocaleDateString("en-CA", { timeZone: TZ });
const jourCourant = () => kinDay() >= "2026-11-13" ? "13" : "12";
function convocTs(jour, hm) { if (!hm || !/^\d{1,2}:\d{2}$/.test(hm)) return null; const [h, m] = hm.split(":").map(Number); return Date.UTC(2026, 10, +jour, h - 1, m); }
function initials(n) { const w = String(n || "?").replace(/[^\p{L}\p{N}\s'-]/gu, " ").trim().split(/\s+/).filter(Boolean); return esc(((w[0] || "?")[0] + ((w[1] || "")[0] || "")).toUpperCase()); }
const empty = (t, b) => `<div class="empty">${b ? `<b>${esc(b)}</b>` : ""}${esc(t)}</div>`;
const mapsUrl = p => "https://www.google.com/maps/search/?api=1&query=" + encodeURIComponent([p.lieu, p.adresse, "Kinshasa"].filter(Boolean).join(", "));
const telDigits = t => { let d = String(t || "").replace(/[^\d+]/g, ""); if (d.startsWith("00")) d = "+" + d.slice(2); if (/^0\d{8,9}$/.test(d)) d = "+243" + d.slice(1); return d; };
const waUrl = t => "https://wa.me/" + telDigits(t).replace(/^\+/, "");
const opt = (v, l, sel) => `<option value="${esc(v)}" ${v === sel ? "selected" : ""}>${esc(l)}</option>`;
const vibrate = ms => { try { navigator.vibrate && navigator.vibrate(ms); } catch (e) { } };

/* ================= Accès aux données ================= */
let ME = LS.get("pj.moi", null);
const membre = id => S.membres.find(m => m.id === id);
const nomMembre = id => (membre(id) || {}).nom || "un membre du protocole";
const prenom = id => (nomMembre(id).split(/\s+/)[0]) || "";
const arrivee = id => S.arrivees.find(a => a.id === id);
const presOf = (mid, jour) => S.presences.find(p => p.id === mid + "_" + jour);
const depts = () => (S.departements.length ? S.departements : DEFAULT_DEPTS).slice().sort((a, b) => (a.ordre || 99) - (b.ordre || 99) || natCmp(a.nom, b.nom));
const dept = id => depts().find(d => d.id === id);
const deptNom = id => (dept(id) || {}).nom || "Non affecté";
const deptCourt = id => (dept(id) || {}).court || (dept(id) || {}).nom || "Non affecté";
const chefOf = mid => depts().find(d => d.chef === mid);
const adjointOf = mid => depts().find(d => d.adjoint === mid);
function roleOf(m) { const c = chefOf(m.id); if (c) return "Chef · " + (c.court || c.nom); const a = adjointOf(m.id); if (a) return "Adjoint·e · " + (a.court || a.nom); return m.role || ""; }
const reglages = () => S.reglages.find(r => r.id === "general") || {};
const progList = () => (S.programme.length ? S.programme : DEFAULT_PROG).slice().sort((a, b) => (a.jour + a.heure).localeCompare(b.jour + b.heure));
function liveStep() { const e = S.etat.find(x => x.id === "direct"); if (!e || !e.etape) return null; return progList().find(p => p.id === e.etape) || null; }
const isAdmin = () => Store.isAdmin;
function tableInfo() {
  const map = new Map();
  S.tables.forEach(t => { if (t.label) map.set(t.label, { label: t.label, doc: t, guests: [] }); });
  S.invites.forEach(g => { const l = (g.table || "").trim(); if (!l) return; if (!map.has(l)) map.set(l, { label: l, doc: null, guests: [] }); map.get(l).guests.push(g); });
  return [...map.values()].map(t => {
    const prevues = t.guests.reduce((s, g) => s + (+g.places || 1), 0);
    const arrives = t.guests.filter(g => arrivee(g.id)).reduce((s, g) => s + (+(arrivee(g.id).nb) || +g.places || 1), 0);
    const cap = t.doc && +t.doc.places ? +t.doc.places : prevues;
    return { ...t, prevues, arrives, cap };
  }).sort((a, b) => natCmp(a.label, b.label));
}
function presStatus(mid, jour) {
  const p = presOf(mid, jour); if (!p) return { cls: "", txt: "Absent·e", pill: "dim" };
  const c = convocTs(jour, reglages()["convoc" + jour]);
  if (c && p.at > c + 5 * 60000) { const m = Math.round((p.at - c) / 60000); return { cls: "late", txt: `${hhmm(p.at)} · retard ${m} min`, pill: "warn" }; }
  return { cls: "on", txt: `${hhmm(p.at)}${c ? " · à l'heure" : ""}`, pill: "ok" };
}
const svcOf = label => S.service.find(s => s.id === slug(label));
const POS_MAX_AGE = 6 * 3600e3, POS_STALE = 5 * 60e3;
const livePositions = () => S.positions.filter(p => p.lat != null && p.at && Date.now() - p.at < POS_MAX_AGE && (!p.until || p.until > Date.now() - 60e3));
const posOf = mid => livePositions().find(p => p.id === mid);
const vignette = id => (S.vignettes.find(v => v.id === id) || {}).d || "";

/* ================= Interface commune ================= */
let toastT = null;
function toast(msg, undo) {
  const t = $("#toast"); t.innerHTML = `<span>${esc(msg)}</span>${undo ? `<button data-act="undo">Annuler</button>` : ""}`; t.hidden = false;
  toast.undo = undo || null; clearTimeout(toastT); toastT = setTimeout(() => { t.hidden = true; toast.undo = null; }, undo ? 5000 : 3400);
}
function errMsg(e) {
  const c = String((e && (e.code || e.message)) || "");
  if (/PERMISSION/i.test(c)) return "Action réservée à l'administrateur.";
  if (c === "NO_AUTH") return "Connexion à l'équipe en cours. Réessayez dans un instant.";
  if (/quota/i.test(c)) return "La mémoire du téléphone est pleine. Supprimez d'anciennes photos.";
  return "Action impossible pour le moment. Réessayez.";
}
async function run(fn, ok) { try { const r = await fn(); if (ok) toast(r === "en attente" ? ok + " · envoi dès le retour du réseau" : ok); return true; } catch (e) { toast(errMsg(e)); return false; } }
onError(e => toast(errMsg(e)));
function sheet(title, body) {
  const ov = $("#sheet");
  ov.innerHTML = `<div class="sh-back" data-act="close"></div><div class="sh" role="dialog" aria-modal="true" aria-label="${esc(title)}"><div class="sh-grip"></div><div class="sh-head"><h3>${esc(title)}</h3><button class="icon-btn" data-act="close" aria-label="Fermer">${ic("x", 18)}</button></div><div class="sh-body">${body}</div></div>`;
  ov.hidden = false; document.body.classList.add("lock");
}
function closeSheet() { const ov = $("#sheet"); ov.hidden = true; ov.innerHTML = ""; if ($("#lb").hidden) document.body.classList.remove("lock"); }
function lightbox(src, cap, meta, actions) {
  const lb = $("#lb");
  lb.innerHTML = `<button class="icon-btn x" data-act="lb-close" aria-label="Fermer">${ic("x", 18)}</button>${src ? `<img src="${esc(src)}" alt="${esc(cap || "Photo")}">` : `<p class="muted">Chargement…</p>`}${cap ? `<p class="cap">${esc(cap)}</p>` : ""}${meta ? `<p class="muted small">${esc(meta)}</p>` : ""}${actions || ""}`;
  lb.hidden = false; document.body.classList.add("lock");
}
function closeLb() { const lb = $("#lb"); lb.hidden = true; lb.innerHTML = ""; if ($("#sheet").hidden) document.body.classList.remove("lock"); }
async function copyText(txt) { try { await navigator.clipboard.writeText(txt); toast("Copié"); } catch (e) { toast("Sélectionnez le texte et copiez-le."); } }

/* ---------- Thème ---------- */
function resolvedSkin() { let s = LS.get("pj.skin", "noir"); if (s === "auto") s = matchMedia("(prefers-color-scheme: light)").matches ? "blanc" : "noir"; return SKINS.some(x => x[0] === s) ? s : "noir"; }
function applySkin() {
  const s = resolvedSkin(); document.documentElement.dataset.skin = s;
  const bar = getComputedStyle(document.documentElement).getPropertyValue("--bg").trim();
  const m = document.querySelector('meta[name="theme-color"]'); if (m) m.content = DARK_BARS[s] ? "#0D0A0B" : bar;
  if (Capacitor.isNativePlatform()) SystemBars.setStyle({ style: DARK_BARS[s] ? SystemBarsStyle.Dark : SystemBarsStyle.Light }).catch(() => { });
  Maps.retile();
}
matchMedia("(prefers-color-scheme: light)").addEventListener?.("change", () => { if (LS.get("pj.skin", "noir") === "auto") applySkin(); });

/* ---------- Cartes (Leaflet) ---------- */
const Maps = {
  list: new Set(),
  tiles() {
    const dark = DARK_BARS[resolvedSkin()] && resolvedSkin() !== "mixte";
    return L.tileLayer(`https://{s}.basemaps.cartocdn.com/${dark ? "dark_all" : "rastertiles/voyager"}/{z}/{x}/{y}{r}.png`, { subdomains: "abcd", maxZoom: 20, attribution: "© OpenStreetMap © CARTO" });
  },
  create(el) {
    const map = L.map(el, { zoomControl: false, attributionControl: true }).setView([-4.33, 15.31], 12);
    map._pjTiles = this.tiles().addTo(map);
    L.control.zoom({ position: "bottomright" }).addTo(map);
    map._pjMarkers = new Map(); map._pjFitted = false;
    this.list.add(map); return map;
  },
  destroy(map) { if (!map) return; this.list.delete(map); try { map.remove(); } catch (e) { } },
  retile() { this.list.forEach(m => { try { m.removeLayer(m._pjTiles); m._pjTiles = this.tiles().addTo(m); } catch (e) { } }); },
  pin(map, key, lat, lng, html, cls, onClick) {
    let mk = map._pjMarkers.get(key);
    const icon = L.divIcon({ className: "", html: `<div class="pin ${cls || ""}">${html}</div>`, iconSize: cls && cls.includes("me") ? [22, 22] : [38, 38] });
    if (!mk) { mk = L.marker([lat, lng], { icon }).addTo(map); if (onClick) mk.on("click", onClick); map._pjMarkers.set(key, mk); }
    else { mk.setLatLng([lat, lng]); mk.setIcon(icon); }
    mk._pjSeen = true; return mk;
  },
  sweep(map) { map._pjMarkers.forEach((mk, k) => { if (!mk._pjSeen) { map.removeLayer(mk); map._pjMarkers.delete(k); } mk._pjSeen = false; }); }
};

/* ---------- Partage de position ---------- */
let shareUntil = (LS.get("pj.share", null) || {}).until || null;
let lastPub = null;
const sharing = () => !!(shareUntil && shareUntil > Date.now() && ME);
const needWatch = () => sharing() || cur === "localiser" || cur === "guide";
async function startSharing() {
  if (!ME) { toast("Choisissez d'abord votre nom."); meSheet(); return; }
  try { await startWatch(); } catch (e) { toast(geoErrorText(e)); return; }
  shareUntil = Date.now() + (+LS.get("pj.shareHours", 4)) * 3600e3; LS.set("pj.share", { until: shareUntil });
  lastPub = null; publishPos(true);
  S.demandes.filter(d => d.cible === ME).forEach(d => del("demandes", d.id).catch(() => { }));
  toast("Votre position est partagée avec le protocole"); refresh();
}
async function stopSharing(silent) {
  shareUntil = null; LS.set("pj.share", null); lastPub = null;
  if (ME && S.positions.some(p => p.id === ME)) del("positions", ME).catch(() => { });
  if (!needWatch()) await stopWatch();
  if (!silent) toast("Partage de position arrêté"); refresh();
}
function publishPos(force) {
  const p = Geo.me; if (!p || !sharing()) return;
  const now = Date.now();
  if (!force && lastPub && distance(lastPub, p) < 8 && now - lastPub.at < 45000) return;
  lastPub = { lat: p.lat, lng: p.lng, at: now };
  put("positions", ME, { lat: +p.lat.toFixed(6), lng: +p.lng.toFixed(6), acc: p.acc, at: now, until: shareUntil }).catch(() => { });
}
setInterval(() => {
  if (shareUntil && shareUntil <= Date.now()) { stopSharing(true); toast("Partage de position terminé (durée écoulée)"); }
  else if (sharing()) publishPos(false);
}, 20000);
let geoRaf = 0;
onGeo(() => {
  if (sharing()) publishPos(false);
  if (geoRaf) return;
  geoRaf = requestAnimationFrame(() => { geoRaf = 0; if (cur === "guide") Guide.live(); else if (cur === "localiser") Localiser.update($("#view")); });
});

/* ================= Vues ================= */
let invF = "tous", invMode = "nom", invQ = "", deptTab = null, sigF = "actifs", phF = "toutes", appelJour = jourCourant(), GUIDE = null;

const Home = {
  title: "Protocole",
  mount(el) {
    el.innerHTML = `
    <section class="hero">
      <img class="hero-img" src="img/hero.jpg" alt="Clarisse et John">
      <div class="hero-veil"></div><div class="hero-frame"></div>
      <div class="hero-txt pad">
        <p class="eyebrow">Le protocole · Mariage</p>
        <h1 class="names">Clarisse<span class="amp">&amp;</span>John</h1>
        <p class="hero-sub">12 & 13 novembre 2026 · Kinshasa</p>
        <div class="cd" id="cd"></div>
      </div>
    </section>
    <div class="pad stack" id="dyn"></div>
    <div class="pad stack" style="padding-top:0">
      <section class="card">
        <p class="eyebrow">Le mot des mariés</p>
        <p class="quote">« Un bon protocole est élégant, ponctuel, respectueux, et communique la joie. »</p>
        <div class="vals">
          <div class="val"><b>Élégant</b><span>Full black impeccable, gestes posés.</span></div>
          <div class="val"><b>Ponctuel</b><span>À son poste avant l'heure.</span></div>
          <div class="val"><b>Respectueux</b><span>Chaque invité est un hôte d'honneur.</span></div>
          <div class="val"><b>Joyeux</b><span>Le sourire fait partie de la tenue.</span></div>
        </div>
      </section>
      <section><div class="sec-h"><h3>Les mariés</h3></div>
        <div class="strip"><img src="img/p1.jpg" alt="Clarisse et John en blanc"><img src="img/p2.jpg" alt="John et Clarisse en robe bordeaux"><img src="img/p3.jpg" alt="John embrassant Clarisse"></div>
      </section>
      <footer class="foot"><div class="sig-script">C &amp; J</div><p>Protocole · version ${esc(APP_VERSION)}</p></footer>
    </div>`;
    tick();
  },
  update(el) {
    const d = $("#dyn", el); if (!d) return;
    const me = membre(ME), jour = jourCourant();
    const pres = S.presences.filter(p => p.jour === jour).length;
    const tot = S.invites.length, arr = S.invites.filter(g => arrivee(g.id)).length;
    const open = S.signalements.filter(s => s.statut !== "resolu").length;
    const ann = [...S.annonces].sort((a, b) => (b.at || 0) - (a.at || 0))[0];
    const live = liveStep(), nPos = livePositions().length;
    d.innerHTML = `
      ${ann ? `<article class="card annonce ${ann.urgent ? "urgent" : ""}"><p class="eyebrow">${ic("megaphone", 14)} ${ann.urgent ? "Annonce urgente" : "Annonce"} · ${esc(ago(ann.at))}</p><p class="ann-t">${esc(ann.texte)}</p></article>` : ""}
      ${me ? `<article class="card me-card"><div class="av lg">${initials(me.nom)}</div><div class="grow"><p class="eyebrow">Bienvenue</p><p class="me-name">${esc(me.nom)}</p><p class="muted small">${esc(deptNom(me.dept))}${roleOf(me) ? " · " + esc(roleOf(me)) : ""}</p></div>${presOf(me.id, jour) ? `<span class="pill ok">Présent·e</span>` : `<a class="btn sm gold" href="#appel">Mon appel</a>`}</article>`
        : `<button class="card me-card ask" data-act="me"><div class="av lg">?</div><div class="grow"><p class="eyebrow">Identifiez-vous</p><p class="me-name">Qui êtes-vous ?</p><p class="muted small">Choisissez votre nom pour signer vos actions, faire votre appel et partager votre position.</p></div></button>`}
      ${live ? `<a class="card alert-card" href="#programme"><p class="eyebrow"><span class="live">En direct</span></p><p class="lv-t">${esc(live.titre)}</p><p class="muted">${esc(live.lieu)} · ${esc(live.heure)}</p></a>` : ""}
      <div class="stats">
        <a class="stat" href="#invites"><b>${arr}<small> / ${tot}</small></b><span>Invités arrivés</span></a>
        <a class="stat" href="#appel"><b>${pres}<small> / ${S.membres.length}</small></b><span>Protocole présent · ${jour} nov.</span></a>
        <a class="stat ${open ? "alert" : ""}" href="#alertes"><b>${open}</b><span>Alertes ouvertes</span></a>
        <a class="stat" href="#localiser"><b>${nPos}</b><span>Protocoles localisables</span></a>
      </div>
      <section><div class="sec-h"><h3>Raccourcis</h3></div>
        <div class="tiles">
          <a class="tile" href="#localiser">${ic("pin", 22)}Localiser</a>
          <a class="tile" href="#programme">${ic("calendar", 22)}Le déroulé</a>
          <a class="tile" href="#appel">${ic("clock", 22)}L'appel</a>
          <a class="tile" href="#photos">${ic("camera", 22)}Photos</a>
          <a class="tile" href="#tenue">${ic("shirt", 22)}Tenue & charte</a>
          <a class="tile" href="#reglages">${ic("palette", 22)}Thème & réglages</a>
          <a class="tile" href="#admin">${ic("crown", 22)}Administration</a>
        </div>
      </section>`;
  }
};
function tick() {
  const el = $("#cd"); if (!el) return;
  const now = Date.now(), nx = EVENTS.find(e => e.t > now);
  if (!nx) { el.innerHTML = `<p class="cd-done">Vive les mariés</p>`; return; }
  let s = Math.floor((nx.t - now) / 1000); const d = Math.floor(s / 86400); s %= 86400; const h = Math.floor(s / 3600); s %= 3600; const m = Math.floor(s / 60); s %= 60;
  el.innerHTML = `<p class="cd-lbl">Avant ${esc(nx.label)}</p><div class="cd-row">${[[d, "jours"], [h, "heures"], [m, "min"], [s, "sec"]].map(([v, l]) => `<div><b>${String(v).padStart(2, "0")}</b><span>${l}</span></div>`).join("")}</div>`;
}
setInterval(tick, 1000);

const Invites = {
  title: "Invités & tables",
  mount(el) {
    el.innerHTML = `
    <header class="vh pad"><p class="eyebrow c">Accueil & placement</p><h2 class="script-h">Les invités</h2><p class="lede">Trouver un nom, conduire à la bonne table.</p><div class="orn">${DIAMOND}</div></header>
    <div class="pad stack">
      <label class="search">${ic("search")}<input id="q-inv" type="search" placeholder="Nom de l'invité ou numéro de table…" autocomplete="off" aria-label="Rechercher un invité" value="${esc(invQ)}"></label>
      <div class="seg" id="seg-f" role="group" aria-label="Filtrer"><button data-act="inv-f" data-v="tous">Tous</button><button data-act="inv-f" data-v="attendus">Attendus</button><button data-act="inv-f" data-v="arrives">Arrivés</button></div>
      <div class="seg" id="seg-m" role="group" aria-label="Affichage"><button data-act="inv-m" data-v="nom">Par nom</button><button data-act="inv-m" data-v="table">Par table</button></div>
      <div class="row adm"><button class="btn sm" data-act="inv-add">${ic("plus", 14)} Invité</button><button class="btn sm" data-act="import">${ic("upload", 14)} Importer une liste</button><button class="btn sm" data-act="table-add">${ic("table", 14)} Table</button></div>
      <div id="dyn"></div>
    </div>`;
    $("#q-inv", el).addEventListener("input", e => { invQ = e.target.value; this.update(el); });
  },
  update(el) {
    $$("#seg-f button", el).forEach(b => b.setAttribute("aria-pressed", b.dataset.v === invF));
    $$("#seg-m button", el).forEach(b => b.setAttribute("aria-pressed", b.dataset.v === invMode));
    const d = $("#dyn", el); const q = norm(invQ.trim());
    const match = g => !q || norm(g.nom).includes(q) || norm(g.table) === q || norm("table " + g.table).includes(q) || norm(g.cat).includes(q);
    const filt = g => invF === "tous" || (invF === "arrives" ? !!arrivee(g.id) : !arrivee(g.id));
    const all = S.invites, list = all.filter(g => match(g) && filt(g)).sort((a, b) => natCmp(a.nom, b.nom));
    const nArr = all.filter(g => arrivee(g.id)).length, places = all.reduce((s, g) => s + (+g.places || 1), 0);
    let html = `<p class="sumline" style="margin-bottom:6px"><span><b>${nArr}</b> arrivés</span><span><b>${all.length - nArr}</b> attendus</span><span><b>${places}</b> places</span><span><b>${tableInfo().length}</b> tables</span></p>`;
    if (!all.length) { d.innerHTML = html + empty("L'administrateur ajoute les invités un par un ou importe toute la liste d'un coup. Ils apparaîtront ici avec leur table.", "La liste est vide"); return; }
    if (invMode === "nom") html += list.length ? `<ul class="list">${list.map(guestRow).join("")}</ul>` : empty("Aucun invité ne correspond à cette recherche.");
    else {
      const tabs = tableInfo().map(t => ({ ...t, guests: t.guests.filter(g => match(g) && filt(g)) })).filter(t => t.guests.length || (!q && invF === "tous"));
      const sans = list.filter(g => !(g.table || "").trim());
      html += `<div class="tgrid">${tabs.map(tableCard).join("")}</div>`;
      if (sans.length) html += `<p class="subh">Sans table</p><ul class="list">${sans.map(guestRow).join("")}</ul>`;
      if (!tabs.length && !sans.length) html += empty("Aucune table ne correspond à cette recherche.");
    }
    d.innerHTML = html;
  }
};
function guestRow(g) {
  const a = arrivee(g.id); const cat = g.cat ? `<span class="tag ${g.cat === "Officiels" ? "fill" : ""}">${esc(g.cat)}</span>` : "";
  return `<li class="g ${a ? "is-in" : ""}"><button class="g-main" data-act="guest" data-id="${esc(g.id)}"><span class="g-name">${esc(g.nom)}</span><span class="g-meta">${cat}${+g.places > 1 ? esc(g.places) + " places" : "1 place"}${a ? ` · arrivé à ${hhmm(a.at)}` : ""}${g.note ? " · " + esc(g.note) : ""}</span></button><span class="tbl ${g.table ? "" : "none"}">${g.table ? "Table " + esc(g.table) : "Sans table"}</span><button class="chk" data-act="arrive" data-id="${esc(g.id)}" aria-label="${a ? "Annuler l'arrivée de " : "Marquer arrivé : "}${esc(g.nom)}">${ic("check", 18)}</button></li>`;
}
function tableCard(t) {
  const pct = t.cap ? Math.min(100, Math.round(t.arrives / t.cap * 100)) : 0;
  const sub = [t.doc && t.doc.nom, t.doc && t.doc.zone].filter(Boolean).join(" · ");
  return `<article class="card tcard"><header><span class="t-num">${esc(String(t.label).slice(0, 4))}</span><div class="grow"><h4>Table ${esc(t.label)}</h4>${sub ? `<p class="muted small">${esc(sub)}</p>` : ""}</div><span class="t-occ">${t.arrives}/${t.cap}</span><button class="x-btn adm" data-act="table-edit" data-l="${esc(t.label)}" aria-label="Modifier la table">${ic("edit", 16)}</button></header><div class="bar"><i style="width:${pct}%"></i></div><ul class="chips">${t.guests.sort((a, b) => natCmp(a.nom, b.nom)).map(g => `<li><button class="chip ${arrivee(g.id) ? "in" : ""}" data-act="guest" data-id="${esc(g.id)}">${esc(g.nom)}${+g.places > 1 ? " ×" + esc(g.places) : ""}</button></li>`).join("")}</ul></article>`;
}

const Equipes = {
  title: "Départements",
  mount(el) {
    el.innerHTML = `
    <header class="vh pad"><p class="eyebrow c">Sous-commissions</p><h2 class="script-h">Les départements</h2><p class="lede">Chaque équipe, son chef, ses outils, sa check-list.</p><div class="orn">${DIAMOND}</div></header>
    <div class="pad stack"><div class="seg" id="seg-d" role="group" aria-label="Département"></div><div id="dyn" class="stack tight"></div></div>`;
  },
  update(el) {
    const ds = depts(); if (!ds.find(x => x.id === deptTab)) deptTab = (ds[0] || {}).id;
    $("#seg-d", el).innerHTML = ds.map(x => `<button data-act="dept" data-v="${esc(x.id)}" aria-pressed="${x.id === deptTab}">${esc(x.court || x.nom)}</button>`).join("") + `<button class="adm" data-act="dept-new">+ Nouveau</button>`;
    const D = dept(deptTab); if (!D) { $("#dyn", el).innerHTML = empty("Aucun département."); return; }
    const k = D.id, jour = jourCourant();
    const mem = (k === "coordination" ? S.membres : S.membres.filter(m => m.dept === k)).slice().sort((a, b) => (b.id === D.chef) - (a.id === D.chef) || (b.id === D.adjoint) - (a.id === D.adjoint) || natCmp(a.nom, b.nom));
    const tasks = S.taches.filter(t => t.dept === k).sort((a, b) => (!!a.fait - !!b.fait) || ((a.at || 0) - (b.at || 0)));
    const done = tasks.filter(t => t.fait).length;
    const inDept = S.membres.filter(m => m.dept === k);
    const pres = inDept.filter(m => presOf(m.id, jour)).length;
    const chef = membre(D.chef), adj = membre(D.adjoint);
    $("#dyn", el).innerHTML = `
      <section class="card dept-hero">
        <div class="dept-ic">${ic(D.ic || "star", 26)}</div>
        <p class="eyebrow c">Département</p>
        <h3 class="dept-name">${esc(D.nom)}</h3>
        ${D.mission ? `<p class="mission">${esc(D.mission)}</p>` : ""}
        <div class="row" style="justify-content:center">
          ${chef ? `<button class="chef" data-act="mem" data-id="${esc(chef.id)}"><span class="av">${initials(chef.nom)}</span><span><small>Chef de département</small><b>${esc(chef.nom)}</b></span></button>` : `<p class="muted small" style="margin-top:14px">Chef de département à nommer</p>`}
          ${adj ? `<button class="chef" data-act="mem" data-id="${esc(adj.id)}"><span class="av">${initials(adj.nom)}</span><span><small>Adjoint·e</small><b>${esc(adj.nom)}</b></span></button>` : ""}
        </div>
        <div class="kpis"><div><b>${inDept.length}</b><small>Protocoles</small></div><div><b>${pres}</b><small>Présents · ${jour} nov.</small></div><div><b>${done}/${tasks.length}</b><small>Tâches faites</small></div></div>
        <div class="adm-bar adm"><button class="btn sm gold" data-act="dept-chef" data-id="${esc(k)}">${ic("crown", 14)} Nommer le chef</button><button class="btn sm" data-act="dept-team" data-id="${esc(k)}">${ic("users", 14)} Composer l'équipe</button><button class="btn sm" data-act="dept-edit" data-id="${esc(k)}">${ic("edit", 14)} Modifier</button></div>
      </section>
      ${deptTool(k)}
      <section><div class="sec-h"><h3>Check-list</h3><button class="btn sm" data-act="tache-add" data-dept="${esc(k)}">${ic("plus", 14)} Tâche</button></div>
        ${tasks.length ? `<ul class="list card flat" style="padding-block:4px">${tasks.map(taskRow).join("")}</ul>` : empty("Aucune tâche pour ce département. Ajoutez la première.")}
      </section>
      <section><div class="sec-h"><h3>${k === "coordination" ? "Tout le protocole" : "L'équipe"}</h3><button class="btn sm adm" data-act="dept-team" data-id="${esc(k)}">${ic("plus", 14)} Ajouter des protocoles</button></div>
        ${membersBlock(mem, k, jour)}
      </section>
      ${deptAlerts(k)}`;
  }
};
function memberRow(m, jour) {
  const st = presStatus(m.id, jour), pos = posOf(m.id), r = roleOf(m);
  return `<li><button class="m" data-act="mem" data-id="${esc(m.id)}"><span class="av">${initials(m.nom)}</span><span class="grow"><b>${esc(m.nom)}${m.id === ME ? ' <span class="tag">Moi</span>' : ""}${chefOf(m.id) ? ' <span class="tag fill">Chef</span>' : ""}</b><small>${[r, pos ? "localisable" : ""].filter(Boolean).map(esc).join(" · ") || "&nbsp;"}</small></span>${pos ? `<span style="color:var(--ok)">${ic("pin", 16)}</span>` : ""}<span class="dot ${st.cls}" title="${esc(st.txt)}"></span></button></li>`;
}
function membersBlock(mem, k, jour) {
  if (!mem.length) return empty(k === "coordination" ? "Aucun membre enregistré pour l'instant." : "Personne n'est encore dans ce département. L'administrateur compose l'équipe avec le bouton « Ajouter des protocoles ».");
  if (k !== "coordination") return `<ul class="list card flat" style="padding-block:4px">${mem.map(m => memberRow(m, jour)).join("")}</ul>`;
  const groups = [...depts().map(d => d.id), ""].map(dk => [dk, mem.filter(m => (m.dept || "") === dk)]).filter(([, l]) => l.length);
  return `<div class="card flat" style="padding-block:4px 10px">${groups.map(([dk, l]) => `<p class="subh">${esc(dk ? deptCourt(dk) : "Non affectés")} · ${l.length}</p><ul class="list">${l.map(m => memberRow(m, jour)).join("")}</ul>`).join("")}</div>`;
}
function taskRow(t) {
  return `<li class="task ${t.fait ? "done" : ""}"><button class="tick" data-act="tache-tog" data-id="${esc(t.id)}" aria-label="${t.fait ? "Décocher" : "Cocher"} : ${esc(t.texte)}">${ic("check", 16)}</button><div class="grow"><p>${esc(t.texte)}</p><small>${t.fait ? `Fait${t.faitPar ? " par " + esc(nomMembre(t.faitPar)) : ""}${t.faitAt ? " · " + esc(ago(t.faitAt)) : ""}` : (t.suggestion ? "Suggestion" : (t.par ? "Ajouté par " + esc(nomMembre(t.par)) : ""))}</small></div><button class="x-btn" data-act="tache-del" data-id="${esc(t.id)}" data-confirm="×" aria-label="Supprimer la tâche">${ic("x", 14)}</button></li>`;
}
const pendStk = {}, stkT = {};
function stockRow(it) {
  const q = pendStk[it.id] ?? (+it.qte || 0), ini = +it.initial || 0, pct = ini ? Math.min(100, Math.round(q / ini * 100)) : 100;
  const lvl = ini && pct <= 10 ? "crit" : ini && pct <= 25 ? "warn" : "";
  return `<div class="st-row"><div class="grow"><b>${esc(it.nom)}</b><small>${q} / ${ini} ${esc(it.unite || "")}${lvl ? ` · <span style="color:var(--${lvl})">${lvl === "crit" ? "Rupture imminente" : "Stock bas"}</span>` : ""}</small></div>
    <div class="row"><div class="stepper"><button data-act="stk" data-id="${esc(it.id)}" data-d="-1" aria-label="Retirer un">−</button><output>${q}</output><button data-act="stk" data-id="${esc(it.id)}" data-d="1" aria-label="Ajouter un">+</button></div><button class="x-btn" data-act="stock-del" data-id="${esc(it.id)}" data-confirm="×" aria-label="Supprimer">${ic("trash", 15)}</button></div>
    <div class="bar ${lvl}"><i style="width:${pct}%"></i></div></div>`;
}
function deptTool(k) {
  const tabs = tableInfo();
  if (k === "accueil") {
    const tot = S.invites.length, arr = S.invites.filter(g => arrivee(g.id)).length, pct = tot ? Math.round(arr / tot * 100) : 0;
    const pl = tabs.reduce((s, t) => s + t.prevues, 0), pin = tabs.reduce((s, t) => s + t.arrives, 0);
    return `<section class="card"><div class="sec-h"><h3>Arrivées des invités</h3><a class="btn sm" href="#invites">Ouvrir la liste</a></div>
      <div class="big-num"><b>${arr}</b><span>sur ${tot} invités</span></div><div class="bar"><i style="width:${pct}%"></i></div>
      <p class="muted small" style="margin-top:8px">${pin} places occupées sur ${pl} prévues</p>
      ${tabs.length ? `<p class="subh">Remplissage des tables</p>${tabs.map(t => `<div class="trow"><span>Table ${esc(t.label)}${t.doc && t.doc.nom ? " · " + esc(t.doc.nom) : ""}</span><em>${t.arrives}/${t.cap}</em><div class="bar"><i style="width:${t.cap ? Math.min(100, Math.round(t.arrives / t.cap * 100)) : 0}%"></i></div></div>`).join("")}` : ""}
    </section>`;
  }
  if (k === "boissons") {
    const items = S.stock.slice().sort((a, b) => natCmp(a.nom, b.nom));
    const served = tabs.filter(t => (svcOf(t.label) || {}).boissons).length;
    return `<section class="card"><div class="sec-h"><h3>Stock des boissons</h3><button class="btn sm" data-act="stock-add">${ic("plus", 14)} Boisson</button></div>
      ${items.length ? items.map(stockRow).join("") : empty("Ajoutez chaque boisson avec sa quantité de départ (casiers, bouteilles, cartons). L'équipe ajuste en direct pendant la soirée.")}</section>
    <section class="card"><div class="sec-h"><h3>Tables servies</h3><span class="pill ${served === tabs.length && tabs.length ? "ok" : "gold"}">${served}/${tabs.length}</span></div>
      ${tabs.length ? `<div class="tgs">${tabs.map(t => `<button class="tg" data-act="svc" data-l="${esc(t.label)}" data-k="boissons" aria-pressed="${!!(svcOf(t.label) || {}).boissons}">T. ${esc(t.label)}</button>`).join("")}</div>` : empty("Les tables apparaîtront dès que les invités seront placés.")}</section>`;
  }
  if (k === "nourriture") {
    const cnt = key => tabs.filter(t => (svcOf(t.label) || {})[key]).length;
    return `<section class="card"><div class="sec-h"><h3>Service par table</h3></div>
      ${tabs.length ? `<p class="sumline" style="margin-bottom:6px"><span>Entrées <b>${cnt("entree")}/${tabs.length}</b></span><span>Plats <b>${cnt("plat")}/${tabs.length}</b></span><span>Desserts <b>${cnt("dessert")}/${tabs.length}</b></span></p>
      ${tabs.map(t => { const s = svcOf(t.label) || {}; return `<div class="svc-row"><span class="tbl">T. ${esc(t.label)}</span><div class="tgs grow">${[["entree", "Entrée"], ["plat", "Plat"], ["dessert", "Dessert"]].map(([kk, l]) => `<button class="tg" data-act="svc" data-l="${esc(t.label)}" data-k="${kk}" aria-pressed="${!!s[kk]}">${l}</button>`).join("")}</div><span class="muted small">${t.prevues} pers.</span></div>`; }).join("")}` : empty("Les tables apparaîtront dès que les invités seront placés.")}</section>`;
  }
  if (k === "coordination") {
    const live = liveStep(), jour = jourCourant();
    const rows = depts().filter(d => d.id !== "coordination").map(d => { const m = S.membres.filter(x => x.dept === d.id); const p = m.filter(x => presOf(x.id, jour)).length; return `<div class="trow"><span>${esc(d.court || d.nom)}</span><em>${p}/${m.length} présents</em><div class="bar"><i style="width:${m.length ? Math.round(p / m.length * 100) : 0}%"></i></div></div>`; }).join("");
    return `<section class="card"><div class="sec-h"><h3>Vue d'ensemble</h3><a class="btn sm adm" href="#admin">Administration</a></div>
      <p class="muted">${live ? `En direct : <b style="color:var(--text)">${esc(live.titre)}</b> · ${esc(live.lieu)}` : "Aucune étape n'est marquée en cours."}</p>${rows}</section>`;
  }
  return "";
}
function deptAlerts(k) {
  const l = S.signalements.filter(s => s.dept === k && s.statut !== "resolu").sort((a, b) => (b.at || 0) - (a.at || 0));
  return `<section><div class="sec-h"><h3>Alertes du département</h3><a class="btn sm" href="#alertes">Toutes</a></div>${l.length ? l.slice(0, 3).map(sigCard).join("") : empty("Aucune alerte ouverte. Tout est calme.")}</section>`;
}

/* ---------- Localiser ---------- */
const Localiser = {
  title: "Localiser",
  map: null,
  mount(el) {
    el.innerHTML = `
    <header class="vh pad"><p class="eyebrow c">Sur le terrain</p><h2 class="script-h">Localiser</h2><p class="lede">Retrouvez un protocole et laissez-vous guider jusqu'à lui.</p><div class="orn">${DIAMOND}</div></header>
    <div class="pad stack">
      <div id="share-card"></div>
      <div class="map" id="map"></div>
      <div id="dyn" class="stack tight"></div>
    </div>`;
    this.map = Maps.create($("#map", el));
    startWatch().catch(e => { Geo.error = e; this.update(el); });
  },
  unmount() { Maps.destroy(this.map); this.map = null; },
  update(el) {
    const sc = $("#share-card", el); if (!sc) return;
    const on = sharing(), me = membre(ME), hrs = LS.get("pj.shareHours", 4);
    sc.innerHTML = `<section class="card share-card"><div class="av lg" style="color:${on ? "var(--ok)" : "var(--title)"}">${ic("pin", 24)}</div><div class="grow"><p>${on ? "Vous êtes visible" : "Partager ma position"}</p><p class="muted small">${on ? `Le protocole peut vous retrouver jusqu'à ${hhmm(shareUntil)}.${Geo.me ? ` Précision ± ${Geo.me.acc} m.` : " Recherche du GPS…"}` : me ? `Pendant ${hrs} h, tant que l'application reste ouverte. Le GPS doit être activé.` : "Choisissez d'abord votre nom."}</p></div><button class="switch" role="switch" aria-checked="${on}" aria-label="Partager ma position" data-act="share-toggle"></button></section>
      ${Geo.error ? `<p class="warnline" style="margin-top:10px">${esc(geoErrorText(Geo.error))}</p>` : ""}`;
    // carte
    if (this.map) {
      const pts = [];
      livePositions().forEach(p => {
        if (p.id === ME) return;
        const m = membre(p.id); const stale = Date.now() - p.at > POS_STALE;
        Maps.pin(this.map, p.id, p.lat, p.lng, initials(m ? m.nom : "?"), stale ? "old" : "", () => openGuide({ type: "member", id: p.id }));
        pts.push([p.lat, p.lng]);
      });
      S.signalements.filter(s => s.statut !== "resolu" && s.pos).forEach(s => { Maps.pin(this.map, "sig-" + s.id, s.pos.lat, s.pos.lng, "!", "alert", () => openGuide({ type: "point", lat: s.pos.lat, lng: s.pos.lng, label: s.titre, at: s.at })); pts.push([s.pos.lat, s.pos.lng]); });
      if (Geo.me) { Maps.pin(this.map, "me", Geo.me.lat, Geo.me.lng, "", "me"); pts.push([Geo.me.lat, Geo.me.lng]); }
      Maps.sweep(this.map);
      if (!this.map._pjFitted && pts.length) { this.map._pjFitted = true; if (pts.length === 1) this.map.setView(pts[0], 17); else this.map.fitBounds(pts, { padding: [40, 40], maxZoom: 18 }); }
    }
    // listes
    const here = Geo.me;
    const pos = livePositions().filter(p => p.id !== ME).map(p => ({ p, m: membre(p.id), d: here ? distance(here, p) : null })).filter(x => x.m).sort((a, b) => (a.d ?? 1e12) - (b.d ?? 1e12) || b.p.at - a.p.at);
    const posIds = new Set(livePositions().map(p => p.id));
    const others = S.membres.filter(m => m.id !== ME && !posIds.has(m.id)).sort((a, b) => natCmp(a.nom, b.nom));
    const asked = id => S.demandes.some(d => d.cible === id && d.de === ME && Date.now() - d.at < 2 * 3600e3);
    $("#dyn", el).innerHTML = `
      <section class="card flat"><div class="sec-h"><h3>Localisables</h3><span class="pill ${pos.length ? "ok" : "dim"}">${pos.length}</span></div>
        ${pos.length ? `<ul class="list">${pos.map(({ p, m, d }) => `<li class="who"><span class="av">${initials(m.nom)}</span><div class="grow"><b>${esc(m.nom)}</b><small>${esc(deptCourt(m.dept))} · ${Date.now() - p.at > POS_STALE ? "position " : "vu "}${esc(ago(p.at))}</small></div>${d != null ? `<span class="dist">${esc(fmtDist(d))}</span>` : ""}<button class="btn sm gold" data-act="guide" data-id="${esc(m.id)}">${ic("nav", 14)} Guider</button></li>`).join("")}</ul>` : empty("Personne ne partage sa position pour l'instant. Demandez à un protocole d'activer le partage.")}
      </section>
      <section class="card flat"><div class="sec-h"><h3>Ne partagent pas</h3><span class="pill dim">${others.length}</span></div>
        ${others.length ? `<ul class="list">${others.map(m => `<li class="who"><span class="av">${initials(m.nom)}</span><div class="grow"><b>${esc(m.nom)}</b><small>${esc(deptCourt(m.dept))}</small></div>${ME ? (asked(m.id) ? `<span class="pill gold">Demandé</span>` : `<button class="btn sm" data-act="ask-pos" data-id="${esc(m.id)}">Demander</button>`) : ""}</li>`).join("")}</ul>` : empty("Tout le monde partage sa position.")}
      </section>`;
  }
};

/* ---------- Guide (boussole + carte + itinéraire) ---------- */
function openGuide(t) { GUIDE = t; if (cur === "guide") route(); else location.hash = "guide"; }
function guideTarget() {
  if (!GUIDE) return null;
  if (GUIDE.type === "point") return { lat: GUIDE.lat, lng: GUIDE.lng, at: GUIDE.at, acc: null, label: GUIDE.label };
  const p = posOf(GUIDE.id); const m = membre(GUIDE.id);
  return p ? { ...p, label: m ? m.nom : "Protocole" } : { label: m ? m.nom : "Protocole", missing: true };
}
let nearBuzzed = false;
const Guide = {
  title: "Guidage",
  map: null, line: null,
  mount(el) {
    const t = guideTarget(); const m = GUIDE && GUIDE.type === "member" ? membre(GUIDE.id) : null;
    nearBuzzed = false;
    el.innerHTML = `
    <header class="vh pad"><p class="eyebrow c">${GUIDE && GUIDE.type === "point" ? "Aller vers une alerte" : "Aller vers"}</p><h2 class="script-h" style="font-size:clamp(40px,11vw,56px)">${esc(t ? t.label : "—")}</h2>${m ? `<p class="lede">${esc(deptNom(m.dept))}${roleOf(m) ? " · " + esc(roleOf(m)) : ""}</p>` : ""}</header>
    <div class="pad stack">
      <section class="card guide" id="g-box">
        <div class="compass" id="g-compass"><div class="ring"></div><span class="n" id="g-n">N</span>${ARROW}</div>
        <p class="g-dist" id="g-dist">…</p>
        <p class="g-dir" id="g-dir">Recherche de votre position…</p>
        <p class="g-meta" id="g-meta"></p>
        <p class="warnline" id="g-warn"></p>
      </section>
      <div class="map small" id="gmap"></div>
      <div class="row" id="g-actions"></div>
      <p class="muted small">Tenez le téléphone à plat, haut de l'écran devant vous : la flèche montre la direction à suivre. Pour un trajet en voiture ou à pied dans la ville, utilisez « Itinéraire ».</p>
    </div>`;
    this.map = Maps.create($("#gmap", el)); this.line = null;
    startWatch().catch(e => { Geo.error = e; this.live(); });
    startCompass();
    this.live();
  },
  unmount() { stopCompass(); Maps.destroy(this.map); this.map = null; this.line = null; },
  update() { this.live(); },
  live() {
    const box = $("#g-box"); if (!box) return;
    const t = guideTarget(), me = Geo.me;
    const actions = [];
    if (t && !t.missing) actions.push(`<a class="btn gold" href="${esc(mapsDirections(t, "walking"))}" target="_blank" rel="noopener">${ic("nav", 16)} Itinéraire à pied</a>`, `<a class="btn" href="${esc(mapsDirections(t, "driving"))}" target="_blank" rel="noopener">En voiture</a>`);
    const m = GUIDE && GUIDE.type === "member" ? membre(GUIDE.id) : null;
    if (m && m.tel) actions.push(`<a class="btn" href="tel:${esc(telDigits(m.tel))}">${ic("phone", 16)} Appeler</a>`, `<a class="btn" href="${esc(waUrl(m.tel))}" target="_blank" rel="noopener">${ic("chat", 16)} WhatsApp</a>`);
    if (m && t && t.missing && ME) actions.push(`<button class="btn gold" data-act="ask-pos" data-id="${esc(m.id)}">Lui demander sa position</button>`);
    const ga = $("#g-actions"); const html = actions.join(""); if (ga.dataset.h !== html) { ga.innerHTML = html; ga.dataset.h = html; }
    const arrow = $("#g-arrow"), n = $("#g-n");
    const setTxt = (id, v) => { const e = $("#" + id); if (e && e.textContent !== v) e.textContent = v; };
    if (!t || t.missing) { setTxt("g-dist", "—"); setTxt("g-dir", m ? `${m.nom} ne partage pas sa position en ce moment.` : "Destination introuvable."); setTxt("g-meta", ""); setTxt("g-warn", ""); return; }
    if (!me) { setTxt("g-dist", "…"); setTxt("g-dir", Geo.error ? geoErrorText(Geo.error) : "Recherche de votre position…"); setTxt("g-meta", t.at ? "Sa position : " + ago(t.at) : ""); }
    else {
      const d = distance(me, t), b = bearing(me, t), h = Geo.heading;
      const rel = h == null ? b : (b - h + 360) % 360;
      arrow.style.transform = `rotate(${rel}deg)`;
      n.style.transform = h == null ? "" : `rotate(${-h}deg)`; n.style.transformOrigin = "50% " + ($("#g-compass").offsetWidth / 2 - 14) + "px";
      const near = d <= Math.max(12, Math.min(40, (me.acc || 0) + (t.acc || 0)));
      box.classList.toggle("near", near);
      if (near && !nearBuzzed) { nearBuzzed = true; vibrate([120, 80, 120]); }
      setTxt("g-dist", near ? "Vous y êtes" : fmtDist(d));
      setTxt("g-dir", near ? `${t.label} est tout près de vous. Regardez autour.` : `Direction ${dirName(b)} · environ ${walkMinutes(d)} min à pied${h == null ? " · flèche orientée nord (pas de boussole)" : ""}`);
      setTxt("g-meta", [t.at ? (GUIDE.type === "point" ? "Signalé " : "Position mise à jour ") + ago(t.at) : "", `votre précision ± ${me.acc} m`].filter(Boolean).join(" · "));
      setTxt("g-warn", (t.at && Date.now() - t.at > POS_STALE && GUIDE.type === "member") ? "Sa position date un peu : il ou elle a peut-être bougé." : (me.acc > 60 ? "Signal GPS faible : sortez à découvert pour plus de précision." : ""));
    }
    if (this.map) {
      Maps.pin(this.map, "t", t.lat, t.lng, GUIDE.type === "point" ? "!" : initials(t.label), GUIDE.type === "point" ? "alert" : "");
      const pts = [[t.lat, t.lng]];
      if (me) { Maps.pin(this.map, "me", me.lat, me.lng, "", "me"); pts.push([me.lat, me.lng]); }
      Maps.sweep(this.map);
      if (me) { if (!this.line) this.line = L.polyline(pts, { color: "#D2B07A", weight: 3, dashArray: "6 8" }).addTo(this.map); else this.line.setLatLngs(pts); }
      if (!this.map._pjFitted || (me && !this.map._pjFitMe)) { this.map._pjFitted = true; if (me) this.map._pjFitMe = true; if (pts.length === 1) this.map.setView(pts[0], 17); else this.map.fitBounds(pts, { padding: [36, 36], maxZoom: 19 }); }
    }
  }
};

/* ---------- Signalements ---------- */
const Alertes = {
  title: "Signalements",
  mount(el) {
    ensure("vignettes");
    el.innerHTML = `
    <header class="vh pad"><p class="eyebrow c">En cas de problème</p><h2 class="script-h">Signalements</h2><p class="lede">Ne quittez pas votre poste : signalez, l'équipe s'en occupe.</p><div class="orn">${DIAMOND}</div></header>
    <div class="pad stack">
      <button class="btn wine big" data-act="sig-new">${ic("alert", 18)} Signaler un problème</button>
      <div class="seg" id="seg-s" role="group" aria-label="Filtrer"><button data-act="sig-f" data-v="actifs">À traiter</button><button data-act="sig-f" data-v="resolu">Résolus</button><button data-act="sig-f" data-v="tous">Tous</button></div>
      <div id="dyn" class="stack tight"></div>
    </div>`;
  },
  update(el) {
    $$("#seg-s button", el).forEach(b => b.setAttribute("aria-pressed", b.dataset.v === sigF));
    const rank = s => (s.statut === "resolu" ? 10 : 0) + (s.gravite === "critique" ? 0 : s.gravite === "urgent" ? 1 : 2) + (s.statut === "en_cours" ? .5 : 0);
    const l = S.signalements.filter(s => sigF === "tous" || (sigF === "resolu" ? s.statut === "resolu" : s.statut !== "resolu")).sort((a, b) => rank(a) - rank(b) || (b.at || 0) - (a.at || 0));
    $("#dyn", el).innerHTML = l.length ? l.map(sigCard).join("") : empty(sigF === "resolu" ? "Aucun signalement résolu pour l'instant." : "Aucun problème signalé. Tout se passe bien.", "Calme plat");
  }
};
function sigCard(s) {
  const g = s.gravite in GRAV ? s.gravite : "info", st = s.statut in STAT ? s.statut : "ouvert";
  const th = s.photo ? vignette(s.photo) : "";
  const photo = s.photo ? `<button class="thumb ${th ? "" : "ph wait"}" data-act="img" data-id="${esc(s.photo)}" aria-label="Voir la photo">${th ? `<img src="${esc(th)}" alt="Photo du signalement">` : ""}</button>` : "";
  return `<article class="card sig sev-${g} ${st === "resolu" ? "done" : ""}">
    <div class="sig-h"><span class="pill ${g === "critique" ? "crit" : g === "urgent" ? "warn" : "dim"}">${GRAV[g]}</span><span class="pill ${st === "resolu" ? "ok" : st === "en_cours" ? "gold" : "dim"}">${STAT[st]}</span><span class="when">${esc(ago(s.at))}</span></div>
    <h4 class="sig-t">${esc(s.titre)}</h4>
    <p class="meta">${esc(s.dept ? deptNom(s.dept) : "Général")}${s.lieu ? " · " + esc(s.lieu) : ""} · signalé par ${esc(nomMembre(s.par))}</p>
    ${s.detail ? `<p class="sig-d">${esc(s.detail)}</p>` : ""}${photo}
    ${st === "en_cours" ? `<p class="meta" style="color:var(--title)">Pris en charge par ${esc(nomMembre(s.prisPar))}</p>` : ""}
    ${st === "resolu" ? `<p class="meta" style="color:var(--ok)">Résolu ${esc(ago(s.resoluAt))}${s.resoluPar ? " par " + esc(nomMembre(s.resoluPar)) : ""}</p>` : ""}
    <div class="row">${s.pos && st !== "resolu" ? `<button class="btn sm gold" data-act="sig-go" data-id="${esc(s.id)}">${ic("nav", 14)} Y aller</button>` : ""}${st === "ouvert" ? `<button class="btn sm" data-act="sig-take" data-id="${esc(s.id)}">Je m'en occupe</button>` : ""}${st !== "resolu" ? `<button class="btn sm" data-act="sig-done" data-id="${esc(s.id)}">${ic("check", 14)} Résolu</button>` : `<button class="btn sm" data-act="sig-reopen" data-id="${esc(s.id)}">Rouvrir</button>`}<button class="btn sm danger adm" data-act="sig-del" data-id="${esc(s.id)}" data-confirm="Confirmer">Supprimer</button></div>
  </article>`;
}

/* ---------- Photos ---------- */
const Photos = {
  title: "Photos",
  mount(el) {
    ensure("vignettes");
    el.innerHTML = `
    <header class="vh pad"><p class="eyebrow c">Album du protocole</p><h2 class="script-h">Nos souvenirs</h2><p class="lede">Les coulisses et les grands moments, vus par l'équipe.</p><div class="orn">${DIAMOND}</div></header>
    <div class="pad stack">
      <button class="btn gold big" data-act="ph-add">${ic("camera", 18)} Ajouter des photos</button>
      <div class="seg" id="seg-p" role="group" aria-label="Moment"><button data-act="ph-f" data-v="toutes">Toutes</button>${MOMENTS.map(m => `<button data-act="ph-f" data-v="${esc(m)}">${esc(m)}</button>`).join("")}</div>
      <div id="dyn"></div>
    </div>`;
  },
  update(el) {
    $$("#seg-p button", el).forEach(b => b.setAttribute("aria-pressed", b.dataset.v === phF));
    const l = S.photos.filter(p => phF === "toutes" || p.moment === phF).sort((a, b) => (b.at || 0) - (a.at || 0));
    $("#dyn", el).innerHTML = l.length ? `<div class="pgrid">${l.map(p => { const th = vignette(p.img); return `<button class="ph ${th ? "" : "wait"}" data-act="photo" data-id="${esc(p.id)}" aria-label="${esc(p.legende || "Photo")}">${th ? `<img src="${esc(th)}" alt="${esc(p.legende || "Photo du mariage")}" loading="lazy">` : ""}</button>`; }).join("")}</div>` : empty("Aucune photo pour le moment. Les photos ajoutées par l'équipe s'affichent ici pour tout le monde.");
  }
};

/* ---------- Programme ---------- */
const Programme = {
  title: "Le déroulé",
  mount(el) {
    el.innerHTML = `
    <header class="vh pad"><p class="eyebrow c">Programme</p><h2 class="script-h">Le déroulé</h2><p class="lede">Deux jours, trois lieux, une seule équipe.</p><div class="orn">${DIAMOND}</div></header>
    <div class="pad stack"><div id="dyn"></div><button class="btn adm" data-act="prog-add">${ic("plus", 14)} Ajouter un moment</button></div>`;
  },
  update(el) {
    const live = liveStep(), l = progList();
    $("#dyn", el).innerHTML = ["12", "13"].map(j => {
      const it = l.filter(p => p.jour === j); if (!it.length) return "";
      return `<section style="margin-bottom:10px"><div class="day-h"><b>${j === "12" ? "Jeudi" : "Vendredi"}</b><span>${j} novembre 2026</span></div><ol class="tl">${it.map(p => `<li class="${live && live.id === p.id ? "on" : ""}"><time>${esc((p.heure || "").replace(":", "h"))}</time><span class="rail"></span><div class="body"><h4>${esc(p.titre)}${live && live.id === p.id ? ' <span class="live">En cours</span>' : ""}</h4>${p.lieu ? `<p class="lieu">${esc(p.lieu)}</p>` : ""}${p.adresse ? `<p class="addr">${esc(p.adresse)}</p>` : ""}${p.note ? `<p class="addr" style="font-style:italic">${esc(p.note)}</p>` : ""}<div class="row">${p.lieu || p.adresse ? `<a class="btn sm" href="${esc(mapsUrl(p))}" target="_blank" rel="noopener">${ic("pin", 14)} Itinéraire</a>` : ""}${live && live.id === p.id ? `<button class="btn sm adm" data-act="prog-stop">Terminer</button>` : `<button class="btn sm adm" data-act="prog-live" data-id="${esc(p.id)}">Mettre en cours</button>`}<button class="btn sm adm" data-act="prog-edit" data-id="${esc(p.id)}" aria-label="Modifier">${ic("edit", 14)}</button></div></div></li>`).join("")}</ol></section>`;
    }).join("");
  }
};

/* ---------- Tenue ---------- */
const TENUE = {
  dames: [["d1", "Chemise noire"], ["d2", "Jupe noire"], ["d3", "Bas noirs"], ["d4", "Crêpes noires"], ["d5", "Gilet en tissu du thème", 1]],
  messieurs: [["h1", "Chemise noire"], ["h2", "Pantalon noir"], ["h3", "Chaussures noires"], ["h4", "Nœud en tissu du thème", 1]]
};
const Tenue = {
  title: "Tenue & charte",
  mount(el) {
    const ck = LS.get("pj.tenue", {});
    const li = ([k, l, t]) => `<li><label><input type="checkbox" data-tenue="${k}" ${ck[k] ? "checked" : ""}><span class="${t ? "theme" : ""}">${esc(l)}</span></label></li>`;
    const leads = depts().filter(d => d.id !== "coordination");
    el.innerHTML = `
    <header class="vh pad"><p class="eyebrow c">Décision du 18 octobre</p><h2 class="script-h">Full black</h2><p class="lede">Tout le protocole en noir intégral. Le tissu du thème nous distingue.</p><div class="orn">${DIAMOND}</div></header>
    <div class="pad stack">
      <div class="dress"><section class="card"><h4>Les dames</h4><ul>${TENUE.dames.map(li).join("")}</ul></section><section class="card"><h4>Les messieurs</h4><ul>${TENUE.messieurs.map(li).join("")}</ul></section></div>
      <p class="muted small center">Cochez ce que vous avez déjà. Cette liste reste sur votre téléphone.</p>
      <section class="card rule"><p class="eyebrow" style="color:var(--crit)">Règle</p><p style="margin-top:8px">Chaque protocole est tenu de se conformer à cette décision. Toute personne qui ira à l'encontre sera tout simplement mise de côté.</p><p class="muted small" style="margin-top:10px">En cas de souci, écrivez en privé aux responsables.</p></section>
      <section class="card"><div class="sec-h"><h3>Les chefs de département</h3></div><dl class="kv" style="margin:0">${leads.map(d => `<dt>${esc(d.court || d.nom)}</dt><dd>${esc(d.chef ? nomMembre(d.chef) : "À nommer")}</dd>`).join("")}</dl><p class="muted small" style="margin-top:12px">Chaque chef instruit son équipe avant le jour J.</p></section>
      <section class="card"><div class="sec-h"><h3>Bonnes pratiques</h3><span class="pill gold">Suggestions</span></div>
        <ol class="tips"><li>Arriver avant l'heure de convocation et faire son appel dans l'application.</li><li>Téléphone en silencieux pendant la mairie et la bénédiction.</li><li>Saluer chaque invité, vérifier son nom, le conduire jusqu'à sa table.</li><li>Ne jamais quitter son poste sans prévenir : signaler plutôt dans l'application.</li><li>Garder une posture droite et un sourire, même quand tout s'accélère.</li></ol>
      </section>
    </div>`;
  },
  update() { }
};

/* ---------- Appel ---------- */
const Appel = {
  title: "L'appel",
  mount(el) {
    el.innerHTML = `
    <header class="vh pad"><p class="eyebrow c">Ponctualité</p><h2 class="script-h">L'appel</h2><p class="lede">Qui est à son poste, et depuis quand.</p><div class="orn">${DIAMOND}</div></header>
    <div class="pad stack"><div class="seg" id="seg-j" role="group" aria-label="Jour"><button data-act="jour" data-v="12">Jeudi 12</button><button data-act="jour" data-v="13">Vendredi 13</button></div><div id="dyn" class="stack tight"></div></div>`;
  },
  update(el) {
    $$("#seg-j button", el).forEach(b => b.setAttribute("aria-pressed", b.dataset.v === appelJour));
    const j = appelJour, me = membre(ME), hm = reglages()["convoc" + j];
    const mem = S.membres.slice().sort((a, b) => natCmp(a.nom, b.nom));
    const st = mem.map(m => presStatus(m.id, j));
    const n = st.filter(s => s.cls).length, late = st.filter(s => s.cls === "late").length;
    const mine = me && presOf(me.id, j);
    $("#dyn", el).innerHTML = `
      <section class="card big-cta">
        <p class="eyebrow c">${esc(JOURS[j])} · convocation ${hm ? esc(hm.replace(":", "h")) : "à définir"}</p>
        ${!me ? `<p class="quote" style="margin:0">Choisissez d'abord votre nom.</p><button class="btn gold" data-act="me">Qui êtes-vous ?</button>`
        : mine ? `<p class="quote" style="margin:0">${esc(me.nom)}, vous êtes présent·e depuis ${hhmm(mine.at)}.</p><span class="pill ${presStatus(me.id, j).pill}">${esc(presStatus(me.id, j).txt)}</span><button class="btn sm" data-act="pres-undo" data-id="${esc(me.id)}">Annuler mon appel</button>`
          : `<p class="quote" style="margin:0">${esc(me.nom)}, êtes-vous à votre poste ?</p><form class="form" data-form="appel" style="width:100%"><label class="check"><input type="checkbox" name="tenue" value="1"> Je suis en tenue complète (full black + tissu du thème)</label><label class="check"><input type="checkbox" name="share" value="1" ${sharing() ? "checked disabled" : ""}> Partager aussi ma position avec l'équipe</label><button class="btn gold big" type="submit">${ic("check", 18)} Je suis arrivé·e</button></form>`}
      </section>
      <div class="stats" style="grid-template-columns:repeat(3,1fr)">
        <div class="stat"><b>${n}<small> / ${mem.length}</small></b><span>Présents</span></div>
        <div class="stat"><b>${n - late}</b><span>À l'heure</span></div>
        <div class="stat ${late ? "alert" : ""}"><b>${late}</b><span>En retard</span></div>
      </div>
      <section><div class="sec-h"><h3>Tout le protocole</h3></div>
      ${mem.length ? `<ul class="list card flat" style="padding-block:4px">${mem.map((m, i) => `<li><button class="m" data-act="mem" data-id="${esc(m.id)}"><span class="av">${initials(m.nom)}</span><span class="grow"><b>${esc(m.nom)}</b><small>${esc(m.dept ? deptCourt(m.dept) : "Non affecté")}${presOf(m.id, j) && presOf(m.id, j).tenue ? " · tenue ✓" : ""}</small></span><span class="pill ${st[i].pill}">${esc(st[i].txt)}</span></button></li>`).join("")}</ul>` : empty("La liste des membres est vide.")}
      </section>`;
  }
};

/* ---------- Réglages ---------- */
const Reglages = {
  title: "Réglages",
  mount(el) {
    el.innerHTML = `
    <header class="vh pad"><p class="eyebrow c">Personnaliser</p><h2 class="script-h">Réglages</h2><p class="lede">Thème, connexion de l'équipe, localisation.</p><div class="orn">${DIAMOND}</div></header>
    <div class="pad stack">
      <section class="card"><div class="sec-h"><h3>Thème</h3></div><div class="skins" id="skins"></div><p class="muted small" style="margin-top:12px">Le thème ne change que sur votre téléphone.</p></section>
      <section class="card" id="conn"></section>
      <section class="card"><div class="sec-h"><h3>Mon identité</h3></div><div id="who"></div></section>
      <section class="card"><div class="sec-h"><h3>Localisation</h3></div>
        <form class="form" data-form="share-hours"><label>Durée du partage de position<select name="h" id="share-h">${[1, 2, 3, 4, 6, 8, 12].map(h => opt(String(h), h + " heure" + (h > 1 ? "s" : ""), String(LS.get("pj.shareHours", 4)))).join("")}</select></label><button class="btn sm" type="submit">Enregistrer</button></form>
        <p class="muted small" style="margin-top:12px">Le partage fonctionne tant que l'application est ouverte (écran allumé ou application en arrière-plan récente). Il s'arrête tout seul à la fin de la durée choisie.</p>
      </section>
      <section class="card"><div class="sec-h"><h3>Mettre à jour</h3></div><p class="muted">Version ${esc(APP_VERSION)}. Pour installer la dernière version, téléchargez l'APK avec ce lien puis ouvrez-le.</p><div class="row" style="margin-top:12px"><a class="btn sm gold" href="${APK_URL}" target="_blank" rel="noopener">${ic("download", 14)} Télécharger l'APK</a><button class="btn sm" data-act="copy" data-v="${APK_URL}">Copier le lien</button></div></section>
    </div>`;
  },
  update(el) {
    const cur = LS.get("pj.skin", "noir");
    $("#skins", el).innerHTML = SKINS.map(([k, l, a, b, c]) => `<button class="skin" data-act="skin" data-v="${k}" aria-pressed="${k === cur}"><span class="swatch"><i style="background:${a}"></i><em style="background:${b};color:${c}">C&amp;J</em></span>${esc(l)}</button>`).join("");
    const me = membre(ME);
    $("#who", el).innerHTML = me ? `<div class="me-card"><div class="av lg">${initials(me.nom)}</div><div class="grow"><p class="me-name">${esc(me.nom)}</p><p class="muted small">${esc(deptNom(me.dept))}</p></div><button class="btn sm" data-act="me">Changer</button></div>` : `<button class="btn gold" data-act="me">Choisir mon nom</button>`;
    const c = $("#conn", el);
    if (Store.mode === "local") c.innerHTML = `<div class="sec-h"><h3>Connexion de l'équipe</h3><span class="pill warn">Ce téléphone seul</span></div>
      <p class="muted">Pour que tout le protocole voie les mêmes informations en direct, connectez l'application à Internet avec le code de l'équipe.</p>
      <div class="row" style="margin-top:14px"><button class="btn gold" data-act="join">${ic("wifi", 16)} J'ai un code de connexion</button><button class="btn" data-act="setup">Créer l'équipe (administrateur)</button></div>
      ${savedConnection() ? `<p class="warnline" style="margin-top:12px">La connexion enregistrée ne fonctionne pas. Vérifiez le code ou quittez cette équipe.</p><button class="btn sm danger" style="margin-top:8px" data-act="leave-team" data-confirm="Confirmer : quitter">Quitter cette équipe</button>` : ""}`;
    else c.innerHTML = `<div class="sec-h"><h3>Connexion de l'équipe</h3><span class="pill ${Store.online ? "ok" : "warn"}">${Store.online ? "En ligne" : "Hors ligne"}</span></div>
      <p class="muted">${Store.online ? "Toutes vos actions sont partagées en direct avec le protocole." : "Pas de réseau : vous voyez les dernières informations reçues. Vos actions seront envoyées dès le retour d'Internet."}</p>
      <dl class="kv"><dt>Équipe</dt><dd>${esc(Store.team)}</dd><dt>Rôle</dt><dd>${Store.isAdmin ? "Administrateur" : "Protocole"}</dd></dl>
      <div class="row"><button class="btn sm gold" data-act="share-code">Partager le code de connexion</button><button class="btn sm danger" data-act="leave-team" data-confirm="Confirmer : quitter">Quitter cette équipe</button></div>`;
  }
};

/* ---------- Administration ---------- */
const Admin = {
  title: "Administration",
  mount(el) {
    el.innerHTML = `
    <header class="vh pad"><p class="eyebrow c">Administrateur principal</p><h2 class="script-h">Administration</h2><p class="lede">Départements, chefs, membres, invités, annonces.</p><div class="orn">${DIAMOND}</div></header>
    <div class="pad stack">
      <section class="card not-adm"><div class="sec-h"><h3>Accès administrateur</h3>${ic("lock", 20)}</div>
        <form class="form" data-form="unlock"><label>Mot de passe administrateur<input name="pass" type="password" required minlength="6" autocomplete="current-password"></label><p class="hint">La toute première fois, ce mot de passe est créé : gardez-le précieusement et ne le donnez qu'aux co-administrateurs.</p><button class="btn gold" type="submit">Déverrouiller</button></form>
      </section>
      <div class="adm stack tight">
        <section class="card"><div class="sec-h"><h3>Diffuser une annonce</h3></div>
          <form class="form" data-form="annonce"><label>Message à tout le protocole<textarea name="texte" required maxlength="500" placeholder="Ex. : Les mariés arrivent dans 10 minutes. Tout le monde en place."></textarea></label><label class="check"><input type="checkbox" name="urgent" value="1"> Annonce urgente</label><button class="btn gold" type="submit">${ic("megaphone", 16)} Publier</button></form>
          <div id="ann-list"></div>
        </section>
        <section class="card"><div class="sec-h"><h3>Départements & chefs</h3><button class="btn sm gold" data-act="dept-new">${ic("plus", 14)} Département</button></div><div id="dept-list"></div></section>
        <section class="card"><div class="sec-h"><h3>Membres du protocole</h3><button class="btn sm" data-act="mem-add" data-dept="">${ic("plus", 14)} Membre</button></div><p class="muted small">Touchez un membre pour changer son département, son rôle ou son téléphone.</p><div id="mem-list"></div></section>
        <section class="card"><div class="sec-h"><h3>Invités & tables</h3></div>
          <div class="row"><button class="btn sm" data-act="inv-add">${ic("plus", 14)} Invité</button><button class="btn sm" data-act="import">${ic("upload", 14)} Importer une liste</button><button class="btn sm" data-act="table-add">${ic("table", 14)} Table</button><a class="btn sm" href="#invites">Voir la liste</a></div>
          <p class="muted small" id="adm-counts" style="margin-top:12px"></p></section>
        <section class="card"><div class="sec-h"><h3>Heures de convocation</h3></div>
          <form class="form" data-form="convoc"><div class="two"><label>Jeudi 12<input id="cv12" name="convoc12" type="time"></label><label>Vendredi 13<input id="cv13" name="convoc13" type="time"></label></div><p class="hint">Sert à repérer les retards dans l'appel.</p><button class="btn" type="submit">Enregistrer</button></form></section>
        <section class="card" id="sec-card"></section>
      </div>
    </div>`;
  },
  update(el) {
    const a = [...S.annonces].sort((x, y) => (y.at || 0) - (x.at || 0));
    const al = $("#ann-list", el); if (!al) return;
    al.innerHTML = a.length ? `<p class="subh">Annonces publiées</p><ul class="list">${a.map(n => `<li class="task"><div class="grow"><p>${esc(n.texte)}</p><small>${n.urgent ? "Urgente · " : ""}${esc(ago(n.at))}</small></div><button class="x-btn" data-act="ann-del" data-id="${esc(n.id)}" data-confirm="×" aria-label="Supprimer l'annonce">${ic("trash", 15)}</button></li>`).join("")}</ul>` : "";
    $("#dept-list", el).innerHTML = `<ul class="list">${depts().map(d => { const n = S.membres.filter(m => m.dept === d.id).length; return `<li class="who"><span class="av">${ic(d.ic || "star", 18)}</span><div class="grow"><b>${esc(d.nom)}</b><small>Chef : ${esc(d.chef ? nomMembre(d.chef) : "à nommer")} · ${n} protocole${n > 1 ? "s" : ""}</small></div><button class="x-btn" data-act="dept-chef" data-id="${esc(d.id)}" aria-label="Nommer le chef">${ic("crown", 17)}</button><button class="x-btn" data-act="dept-team" data-id="${esc(d.id)}" aria-label="Composer l'équipe">${ic("users", 17)}</button><button class="x-btn" data-act="dept-edit" data-id="${esc(d.id)}" aria-label="Modifier">${ic("edit", 17)}</button></li>`; }).join("")}</ul>`;
    const r = reglages();[["cv12", "convoc12"], ["cv13", "convoc13"]].forEach(([i, k]) => { const inp = $("#" + i, el); if (inp && document.activeElement !== inp && !inp.dataset.dirty) inp.value = r[k] || ""; });
    $("#adm-counts", el).textContent = `${S.invites.length} invités · ${tableInfo().length} tables · ${S.invites.reduce((s, g) => s + (+g.places || 1), 0)} places`;
    const mem = S.membres.slice().sort((x, y) => natCmp(x.nom, y.nom));
    $("#mem-list", el).innerHTML = mem.length ? `<ul class="list">${mem.map(m => `<li><button class="m" data-act="mem" data-id="${esc(m.id)}"><span class="av">${initials(m.nom)}</span><span class="grow"><b>${esc(m.nom)}</b><small>${esc(deptNom(m.dept))}${roleOf(m) ? " · " + esc(roleOf(m)) : ""}</small></span>${ic("edit", 16)}</button></li>`).join("")}</ul>` : empty("Aucun membre. Ajoutez le premier.");
    $("#sec-card", el).innerHTML = Store.mode === "cloud" ? `<div class="sec-h"><h3>Sécurité</h3></div><form class="form" data-form="newpass"><label>Nouveau mot de passe administrateur<input name="pass" type="password" minlength="6" required autocomplete="new-password"></label><button class="btn sm" type="submit">Changer le mot de passe</button></form><div class="row" style="margin-top:14px"><button class="btn sm" data-act="share-code">Partager le code de connexion</button><button class="btn sm danger" data-act="leave-admin" data-confirm="Confirmer">Quitter le rôle d'administrateur</button></div>`
      : `<div class="sec-h"><h3>Connexion Internet</h3></div><p class="muted">L'application fonctionne pour l'instant sur ce téléphone seulement. Créez l'équipe pour synchroniser tout le protocole.</p><div class="row" style="margin-top:12px"><button class="btn sm gold" data-act="setup">Créer l'équipe</button></div>`;
  }
};

const VIEWS = { accueil: Home, invites: Invites, equipes: Equipes, localiser: Localiser, guide: Guide, alertes: Alertes, photos: Photos, programme: Programme, tenue: Tenue, appel: Appel, reglages: Reglages, admin: Admin };
const NAV = [["accueil", "home", "Accueil"], ["invites", "list", "Invités"], ["equipes", "users", "Équipes"], ["localiser", "pin", "Localiser"], ["alertes", "alert", "Alertes"]];
let cur = null;
function route() {
  let h = location.hash.slice(1); if (!VIEWS[h]) h = "accueil";
  if (h === "guide" && !GUIDE) h = "localiser";
  if (cur && VIEWS[cur].unmount) VIEWS[cur].unmount();
  const prev = cur; cur = h;
  const el = $("#view"); el.innerHTML = ""; VIEWS[h].mount(el); VIEWS[h].update(el);
  $("#top-t").textContent = VIEWS[h].title; updateChrome();
  if (prev !== h) window.scrollTo(0, 0);
  if (!needWatch()) stopWatch();
}
function updateChrome() {
  const open = S.signalements.filter(s => s.statut !== "resolu").length, np = livePositions().filter(p => p.id !== ME).length;
  $("#nav").innerHTML = NAV.map(([k, i, l]) => `<a href="#${k}" ${cur === k || (k === "localiser" && cur === "guide") ? 'aria-current="page"' : ""}>${ic(i, 22)}<span>${l}</span>${k === "alertes" && open ? `<span class="badge">${open}</span>` : ""}${k === "localiser" && np ? `<span class="badge ok">${np}</span>` : ""}</a>`).join("");
  const me = membre(ME); $("#me-btn").innerHTML = me ? initials(me.nom) : "?";
  const sy = $("#sync"); sy.className = "sync " + (Store.mode === "local" ? "" : Store.online ? "on" : "off"); sy.lastChild.textContent = Store.mode === "local" ? "Local" : Store.online ? "En ligne" : "Hors ligne";
  document.body.classList.toggle("admin", isAdmin());
  // bandeau : demande de position, partage actif
  const req = ME ? S.demandes.filter(d => d.cible === ME && Date.now() - d.at < 2 * 3600e3) : [];
  let b = "";
  if (req.length && !sharing()) b = `<div class="banner-wrap pad"><div class="banner">${ic("pin", 18)}<span class="grow">${esc(req.map(r => prenom(r.de)).join(", "))} ${req.length > 1 ? "vous demandent" : "vous demande"} votre position.</span><button class="btn sm" data-act="share-on">Partager</button></div></div>`;
  else if (sharing()) b = `<div class="banner-wrap pad"><div class="banner">${ic("pin", 18)}<span class="grow">Vous partagez votre position jusqu'à ${hhmm(shareUntil)}.</span><button class="btn sm" data-act="share-off">Arrêter</button></div></div>`;
  const bn = $("#banner"); if (bn.dataset.h !== b) { bn.innerHTML = b; bn.dataset.h = b; }
  if (req.length && !sharing()) { const key = req.map(r => r.id).join(","); if (updateChrome.lastReq !== key) { updateChrome.lastReq = key; vibrate(300); } }
}
let rq = 0;
function refresh() { if (rq) return; rq = requestAnimationFrame(() => { rq = 0; if (cur) VIEWS[cur].update($("#view")); updateChrome(); afterData(); }); }
window.addEventListener("hashchange", route);

/* ================= Feuilles ================= */
function meSheet() {
  const l = S.membres.slice().sort((a, b) => natCmp(a.nom, b.nom));
  sheet("Qui êtes-vous ?", `<label class="search">${ic("search")}<input id="q-me" type="search" placeholder="Votre nom…" autocomplete="off" aria-label="Chercher mon nom"></label>
  <ul class="list" id="pick" style="margin-top:8px">${l.map(m => `<li data-s="${esc(norm(m.nom + " " + (m.whatsapp || "")))}"><button class="m" data-act="pick-me" data-id="${esc(m.id)}"><span class="av">${initials(m.nom)}</span><span class="grow"><b>${esc(m.nom)}</b><small>${esc(deptNom(m.dept))}</small></span>${m.id === ME ? ic("check") : ""}</button></li>`).join("")}</ul>
  ${l.length ? "" : empty("La liste des membres est vide pour le moment.")}
  <p class="muted small" style="margin-top:14px">Votre nom n'apparaît pas ? Demandez à l'administrateur de vous ajouter.</p>
  ${ME ? `<button class="btn sm" style="margin-top:12px" data-act="forget-me">Ce n'est pas moi</button>` : ""}`);
  $("#q-me").addEventListener("input", e => { const q = norm(e.target.value); $$("#pick li").forEach(li => li.hidden = !li.dataset.s.includes(q)); });
}
function menuSheet() {
  const items = [["accueil", "home", "Accueil"], ["localiser", "pin", "Localiser un protocole"], ["programme", "calendar", "Le déroulé"], ["invites", "list", "Invités & tables"], ["equipes", "users", "Départements"], ["appel", "clock", "L'appel"], ["alertes", "alert", "Signalements"], ["photos", "camera", "Photos"], ["tenue", "shirt", "Tenue & charte"], ["reglages", "palette", "Thème & réglages"], ["admin", "crown", "Administration"]];
  sheet("Menu", `<ul class="menu-list">${items.map(([k, i, l]) => `<li><a href="#${k}" data-act="close">${ic(i, 22)}${l}</a></li>`).join("")}</ul>`);
}
const memberOptions = (sel, first) => S.membres.slice().sort((a, b) => natCmp(a.nom, b.nom)).sort((a, b) => (b.dept === first) - (a.dept === first)).map(m => opt(m.id, m.nom + (m.dept ? " · " + deptCourt(m.dept) : ""), sel)).join("");
function chefSheet(id) {
  const d = dept(id); if (!d) return;
  sheet("Chef · " + (d.court || d.nom), `<form class="form" data-form="chef" data-id="${esc(id)}">
    <label>Chef de département<select name="chef">${opt("", "— Aucun —", d.chef || "")}${memberOptions(d.chef || "", id)}</select></label>
    <label>Adjoint·e (facultatif)<select name="adjoint">${opt("", "— Aucun —", d.adjoint || "")}${memberOptions(d.adjoint || "", id)}</select></label>
    <p class="hint">Le chef et l'adjoint sont ajoutés automatiquement à ce département.</p>
    <button class="btn gold" type="submit">Enregistrer</button></form>`);
}
function teamSheet(id) {
  const d = dept(id); if (!d) return;
  const l = S.membres.slice().sort((a, b) => (b.dept === id) - (a.dept === id) || natCmp(a.nom, b.nom));
  sheet("Équipe · " + (d.court || d.nom), `<label class="search">${ic("search")}<input id="q-team" type="search" placeholder="Chercher un protocole…" autocomplete="off" aria-label="Chercher"></label>
  <form class="form" data-form="team" data-id="${esc(id)}"><ul class="list picklist" id="team-pick" style="margin-top:6px">${l.map(m => `<li data-s="${esc(norm(m.nom))}"><label class="m"><input type="checkbox" name="m" value="${esc(m.id)}" ${m.dept === id ? "checked" : ""}><span class="av">${initials(m.nom)}</span><span class="grow"><b>${esc(m.nom)}</b><small>${m.dept && m.dept !== id ? "Actuellement : " + esc(deptCourt(m.dept)) : m.dept === id ? "Dans ce département" : "Non affecté"}</small></span></label></li>`).join("")}</ul>
  <p class="hint">Cocher un protocole déjà affecté ailleurs le déplace dans ce département.</p><button class="btn gold big" type="submit">Enregistrer l'équipe</button></form>`);
  $("#q-team").addEventListener("input", e => { const q = norm(e.target.value); $$("#team-pick li").forEach(li => li.hidden = !li.dataset.s.includes(q)); });
}
function deptForm(id) {
  const d = id ? dept(id) : null; const x = d || {};
  sheet(d ? "Modifier" : "Nouveau département", `<form class="form" data-form="dept" data-id="${esc(x.id || "")}">
    <label>Nom du département<input name="nom" required maxlength="60" value="${esc(x.nom || "")}" placeholder="Ex. : Sécurité & parking"></label>
    <div class="two"><label>Nom court<input name="court" maxlength="20" value="${esc(x.court || "")}" placeholder="Ex. : Sécurité"></label><label>Icône<select name="ic">${Object.entries(DEPT_ICONS).map(([k, v]) => opt(k, v, x.ic || "star")).join("")}</select></label></div>
    <label>Mission<textarea name="mission" maxlength="200" placeholder="Une phrase qui résume le rôle de l'équipe">${esc(x.mission || "")}</textarea></label>
    <div class="row"><button class="btn gold" type="submit">Enregistrer</button>${d ? `<button type="button" class="btn danger" data-act="dept-del" data-id="${esc(d.id)}" data-confirm="Confirmer la suppression">Supprimer</button>` : ""}</div>
    ${d ? `<p class="hint">Supprimer le département ne supprime pas ses membres : ils deviennent non affectés.</p>` : ""}</form>`);
}
function guestForm(g = {}) {
  const tl = tableInfo().map(t => `<option value="${esc(t.label)}">`).join("");
  return `<form class="form" data-form="inv" data-id="${esc(g.id || "")}">
    <label>Nom complet<input name="nom" required maxlength="120" value="${esc(g.nom || "")}" autocomplete="off"></label>
    <div class="two"><label>Table<input name="table" list="dl-t" maxlength="40" value="${esc(g.table || "")}" placeholder="Ex. : 4" autocomplete="off"></label><label>Places<input name="places" type="number" min="1" max="30" inputmode="numeric" value="${esc(g.places || 1)}"></label></div>
    <datalist id="dl-t">${tl}</datalist>
    <label>Catégorie<select name="cat">${opt("", "—", g.cat || "")}${CATS.map(c => opt(c, c, g.cat)).join("")}</select></label>
    <label>Note<textarea name="note" maxlength="300" placeholder="Accompagnant, mobilité réduite, régime…">${esc(g.note || "")}</textarea></label>
    <div class="row"><button class="btn gold" type="submit">Enregistrer</button>${g.id ? `<button type="button" class="btn danger" data-act="inv-del" data-id="${esc(g.id)}" data-confirm="Confirmer la suppression">Supprimer</button>` : ""}</div></form>`;
}
function guestSheet(id) {
  const g = S.invites.find(x => x.id === id); if (!g) return; const a = arrivee(id);
  const t = tableInfo().find(x => x.label === g.table);
  sheet("Invité", `<p class="detail-name">${esc(g.nom)}</p>
    <dl class="kv"><dt>Table</dt><dd>${g.table ? "Table " + esc(g.table) + (t && t.doc && t.doc.nom ? " · " + esc(t.doc.nom) : "") : "Non placée"}</dd>${t && t.doc && t.doc.zone ? `<dt>Zone</dt><dd>${esc(t.doc.zone)}</dd>` : ""}<dt>Places</dt><dd>${esc(g.places || 1)}</dd>${g.cat ? `<dt>Catégorie</dt><dd>${esc(g.cat)}</dd>` : ""}${g.note ? `<dt>Note</dt><dd>${esc(g.note)}</dd>` : ""}<dt>Statut</dt><dd>${a ? `Arrivé à ${hhmm(a.at)}${a.par ? " · accueilli par " + esc(nomMembre(a.par)) : ""}` : "Attendu"}</dd></dl>
    <button class="btn ${a ? "" : "gold"} big" data-act="arrive" data-id="${esc(id)}" data-close="1">${a ? "Annuler l'arrivée" : ic("check", 18) + " Marquer arrivé"}</button>
    <div class="adm"><div class="sep"></div><p class="eyebrow" style="margin-bottom:12px">Modifier</p>${guestForm(g)}</div>`);
}
function tableForm(label) {
  const t = label ? tableInfo().find(x => x.label === label) : null; const d = (t && t.doc) || {};
  sheet(label ? "Table " + label : "Nouvelle table", `<form class="form" data-form="table">
    <div class="two"><label>Numéro<input name="label" required maxlength="40" value="${esc(label || "")}" ${label ? "readonly" : ""} placeholder="Ex. : 4"></label><label>Places<input name="places" type="number" min="1" max="60" inputmode="numeric" value="${esc(d.places || "")}" placeholder="Ex. : 10"></label></div>
    <label>Nom de la table<input name="nom" maxlength="60" value="${esc(d.nom || "")}" placeholder="Ex. : Table d'honneur"></label>
    <label>Zone<input name="zone" maxlength="60" value="${esc(d.zone || "")}" placeholder="Ex. : Près de la piste, côté mariée"></label>
    <div class="row"><button class="btn gold" type="submit">Enregistrer</button>${d.id ? `<button type="button" class="btn danger" data-act="table-del" data-id="${esc(d.id)}" data-confirm="Confirmer">Supprimer la fiche</button>` : ""}</div>
    ${label ? `<p class="hint">Pour déplacer un invité, modifiez sa table dans sa fiche.</p>` : ""}</form>`);
}
function memberSheet(id, deptDefault = "") {
  const m = id ? membre(id) : null; if (id && !m) return; const x = m || {};
  const j = jourCourant(), p = m && presOf(m.id, j), st = m ? presStatus(m.id, j) : null, pos = m && posOf(m.id);
  const asked = m && S.demandes.some(d => d.cible === m.id && d.de === ME && Date.now() - d.at < 2 * 3600e3);
  sheet(m ? "Protocole" : "Nouveau membre", `${m ? `<p class="detail-name">${esc(m.nom)}</p>
    <dl class="kv"><dt>Département</dt><dd>${esc(deptNom(m.dept))}</dd>${roleOf(m) ? `<dt>Rôle</dt><dd>${esc(roleOf(m))}</dd>` : ""}${m.tel ? `<dt>Téléphone</dt><dd>${esc(m.tel)}</dd>` : ""}${m.whatsapp ? `<dt>WhatsApp</dt><dd>~ ${esc(m.whatsapp)}</dd>` : ""}<dt>Appel · ${j} nov.</dt><dd>${esc(st.txt)}${p && p.tenue ? " · tenue confirmée" : ""}</dd><dt>Position</dt><dd>${pos ? "Partagée · " + esc(ago(pos.at)) : "Non partagée"}</dd></dl>
    <div class="row">${pos && m.id !== ME ? `<button class="btn gold" data-act="guide" data-id="${esc(m.id)}">${ic("nav", 16)} Me guider</button>` : ""}${!pos && m.id !== ME && ME ? (asked ? `<span class="pill gold">Position demandée</span>` : `<button class="btn" data-act="ask-pos" data-id="${esc(m.id)}">Demander sa position</button>`) : ""}${m.tel ? `<a class="btn" href="tel:${esc(telDigits(m.tel))}">${ic("phone", 16)} Appeler</a><a class="btn" href="${esc(waUrl(m.tel))}" target="_blank" rel="noopener">${ic("chat", 16)} WhatsApp</a>` : ""}</div>
    <div class="row" style="margin-top:10px">${p ? `<button class="btn sm" data-act="pres-undo" data-id="${esc(m.id)}">Annuler la présence</button>` : `<button class="btn sm" data-act="pres-mark" data-id="${esc(m.id)}">${ic("check", 14)} Marquer présent·e</button>`}</div>` : ""}
    <div class="adm">${m ? `<div class="sep"></div><p class="eyebrow" style="margin-bottom:12px">Modifier</p>` : ""}
    <form class="form" data-form="membre" data-id="${esc(x.id || "")}">
      <label>Nom<input name="nom" required maxlength="80" value="${esc(x.nom || "")}"></label>
      <div class="two"><label>Téléphone<input name="tel" type="tel" maxlength="20" value="${esc(x.tel || "")}" placeholder="+243 …"></label><label>Nom WhatsApp<input name="whatsapp" maxlength="80" value="${esc(x.whatsapp || "")}"></label></div>
      <div class="two"><label>Département<select name="dept">${opt("", "Non affecté", x.dept || deptDefault)}${depts().map(d => opt(d.id, d.court || d.nom, x.dept || deptDefault)).join("")}</select></label>
      <label>Rôle<input name="role" list="dl-r" maxlength="40" value="${esc(x.role || "")}" placeholder="Membre"></label></div>
      <datalist id="dl-r"><option value="Membre"><option value="Chef du protocole"><option value="Administrateur du groupe"></datalist>
      <p class="hint">Les chefs de département se nomment depuis l'onglet du département (bouton « Nommer le chef »).</p>
      <div class="row"><button class="btn gold" type="submit">Enregistrer</button>${m ? `<button type="button" class="btn danger" data-act="mem-del" data-id="${esc(m.id)}" data-confirm="Confirmer">Retirer</button>` : ""}</div>
    </form></div>`);
}
function sigForm() {
  const me = membre(ME), fresh = Geo.me && Date.now() - Geo.me.at < 120000;
  sheet("Signaler", `<form class="form" data-form="sig">
    <label>Que se passe-t-il ?<input name="titre" required maxlength="100" placeholder="Ex. : Table 7 sans chaises" autocomplete="off"></label>
    <div class="two"><label>Département<select name="dept">${opt("", "Général", "")}${depts().map(d => opt(d.id, d.court || d.nom, me && me.dept === d.id ? d.id : "")).join("")}</select></label>
    <label>Lieu<select name="lieu">${LIEUX.map(l => opt(l, l, jourCourant() === "13" ? "Salle de fête" : "")).join("")}</select></label></div>
    <fieldset><legend>Gravité</legend><div class="radios" style="margin-top:7px">${Object.entries(GRAV).map(([k, v], i) => `<label><input type="radio" name="gravite" value="${k}" ${i === 1 ? "checked" : ""}> ${v}</label>`).join("")}</div></fieldset>
    <label>Détails<textarea name="detail" maxlength="800" placeholder="Où exactement, qui est concerné, ce qu'il faut…"></textarea></label>
    <label class="check"><input type="checkbox" name="pos" value="1" ${sharing() || fresh ? "checked" : ""}> Joindre ma position pour qu'on me retrouve</label>
    <label class="file">Photo (facultatif)<input type="file" name="photo" accept="image/*"></label>
    ${me ? "" : `<p class="hint">Astuce : choisissez votre nom (bouton en haut à droite) pour signer vos signalements.</p>`}
    <button class="btn wine big" type="submit">Envoyer le signalement</button></form>`);
}
function progForm(p = {}) {
  sheet(p.id ? "Modifier le moment" : "Nouveau moment", `<form class="form" data-form="prog" data-id="${esc(p.id || "")}">
    <div class="two"><label>Jour<select name="jour">${opt("12", "Jeudi 12", p.jour || "13")}${opt("13", "Vendredi 13", p.jour || "13")}</select></label><label>Heure<input name="heure" type="time" required value="${esc(p.heure || "")}"></label></div>
    <label>Titre<input name="titre" required maxlength="80" value="${esc(p.titre || "")}" placeholder="Ex. : Entrée des mariés"></label>
    <label>Lieu<input name="lieu" maxlength="100" value="${esc(p.lieu || "")}"></label>
    <label>Adresse<input name="adresse" maxlength="160" value="${esc(p.adresse || "")}"></label>
    <label>Note<input name="note" maxlength="200" value="${esc(p.note || "")}"></label>
    <div class="row"><button class="btn gold" type="submit">Enregistrer</button>${p.id && S.programme.some(x => x.id === p.id) ? `<button type="button" class="btn danger" data-act="prog-del" data-id="${esc(p.id)}" data-confirm="Confirmer">Supprimer</button>` : ""}</div></form>`);
}
function shareCodeSheet() {
  const conn = savedConnection(); if (!conn) return;
  const code = encodeCode(conn.c, conn.t);
  const msg = `Protocole Clarisse & John\n1. Installez l'application : ${APK_URL}\n2. Ouvrez Réglages > « J'ai un code de connexion » et collez ce code :\n${code}`;
  sheet("Code de l'équipe", `<p class="muted">Envoyez ce code aux protocoles. Ils le collent une seule fois dans Réglages.</p><p class="codebox" style="margin-top:12px">${esc(code)}</p>
    <div class="row" style="margin-top:14px"><a class="btn gold" href="https://wa.me/?text=${encodeURIComponent(msg)}" target="_blank" rel="noopener">${ic("chat", 16)} Envoyer sur WhatsApp</a><button class="btn" data-act="copy" data-v="${esc(code)}">Copier le code</button></div>
    <p class="hint" style="margin-top:14px">Ce code donne accès aux données de l'équipe : ne le publiez pas en dehors du groupe du protocole.</p>`);
}
function joinSheet() {
  sheet("Rejoindre l'équipe", `<form class="form" data-form="join"><label>Code de connexion<textarea name="code" required rows="5" placeholder="PJ1-…"></textarea></label><p class="hint">Collez le code reçu de l'administrateur (il commence par PJ1-).</p><button class="btn gold big" type="submit">${ic("wifi", 18)} Se connecter</button></form>`);
}
function setupSheet() {
  sheet("Créer l'équipe", `<form class="form" data-form="setup">
    <p class="muted">Étape réservée à l'administrateur principal, une seule fois. Suivez le guide « Firebase » fourni avec l'application, puis collez ici la configuration.</p>
    <label>Configuration Firebase<textarea name="cfg" required rows="7" placeholder='const firebaseConfig = { apiKey: "…", authDomain: "…", databaseURL: "…", projectId: "…", appId: "…" };'></textarea></label>
    <label>Adresse de la base (si absente ci-dessus)<input name="dburl" type="url" placeholder="https://…-default-rtdb.europe-west1.firebasedatabase.app"></label>
    <label>Mot de passe administrateur<input name="pass" type="password" required minlength="6" autocomplete="new-password"></label>
    <label class="check"><input type="checkbox" name="migrate" value="1" checked> Envoyer vers l'équipe les données déjà saisies sur ce téléphone</label>
    <button class="btn gold big" type="submit">Créer et connecter</button></form>`);
}

/* ================= Images ================= */
async function shrink(file, max, q) {
  const url = URL.createObjectURL(file);
  try {
    const img = await new Promise((res, rej) => { const i = new Image(); i.onload = () => res(i); i.onerror = rej; i.src = url; });
    const sc = Math.min(1, max / Math.max(img.naturalWidth, img.naturalHeight));
    const c = document.createElement("canvas"); c.width = Math.max(1, Math.round(img.naturalWidth * sc)); c.height = Math.max(1, Math.round(img.naturalHeight * sc));
    c.getContext("2d").drawImage(img, 0, 0, c.width, c.height);
    return c.toDataURL("image/jpeg", q);
  } finally { URL.revokeObjectURL(url); }
}
async function saveImage(file) {
  const local = Store.mode === "local";
  const full = await shrink(file, local ? 900 : 1400, local ? .62 : .78), thumb = await shrink(file, 320, .6);
  const id = newId();
  await put("images", id, { d: full }); await put("vignettes", id, { d: thumb });
  return id;
}

/* ================= Formulaires ================= */
const FORMS = {
  async inv(v, f) {
    const id = f.dataset.id || newId(), old = S.invites.find(g => g.id === id) || {};
    if (await run(() => put("invites", id, { ...clean(old), nom: v.nom.trim(), table: (v.table || "").trim().replace(/^table\s*/i, ""), places: Math.max(1, parseInt(v.places) || 1), cat: v.cat || "", note: (v.note || "").trim(), maj: Date.now() }), "Invité enregistré")) closeSheet();
  },
  async table(v) {
    const label = (v.label || "").trim().replace(/^table\s*/i, ""); if (!label) return;
    if (await run(() => put("tables", slug(label), { label, nom: (v.nom || "").trim(), zone: (v.zone || "").trim(), places: parseInt(v.places) || null }), "Table enregistrée")) closeSheet();
  },
  async import(v, f) {
    const rows = (v.liste || "").split(/\r?\n/).map(l => l.trim()).filter(Boolean).map(l => l.split(/\t|;|\|/).map(x => x.trim())).filter(c => c[0] && !/^nom$/i.test(c[0]))
      .map(c => ({ nom: c[0].slice(0, 120), table: (c[1] || "").replace(/^table\s*/i, "").slice(0, 40), places: Math.min(30, Math.max(1, parseInt(c[2]) || 1)), cat: (c[3] || "").slice(0, 40) }));
    if (!rows.length) { toast("Aucune ligne lisible. Une ligne par invité : Nom ; Table ; Places"); return; }
    const out = $("#imp-prog", f); let i = 0; const ex = new Map(S.invites.map(g => [norm(g.nom), g]));
    for (const r of rows) { const e = ex.get(norm(r.nom)); const id = e ? e.id : newId(); try { await put("invites", id, { ...(e ? clean(e) : {}), ...r, cat: r.cat || (e && e.cat) || "", note: (e && e.note) || "", maj: Date.now() }); } catch (err) { toast(errMsg(err)); break; } i++; if (out) out.textContent = `${i} / ${rows.length} importés…`; }
    toast(`${i} invités importés`); if (i === rows.length) closeSheet();
  },
  async membre(v, f) {
    const id = f.dataset.id || (slug(v.nom).slice(0, 40) + "-" + Math.random().toString(36).slice(2, 6)), old = membre(id) || {};
    if (await run(() => put("membres", id, { ...clean(old), nom: v.nom.trim(), tel: (v.tel || "").trim(), whatsapp: (v.whatsapp || "").trim(), dept: v.dept || "", role: (v.role || "").trim() }), "Membre enregistré")) closeSheet();
  },
  async chef(v, f) {
    const id = f.dataset.id, d = dept(id); if (!d) return;
    const ok = await run(async () => {
      for (const o of depts()) if (o.id !== id && (o.chef && o.chef === v.chef || o.adjoint && o.adjoint === v.adjoint || o.chef && o.chef === v.adjoint || o.adjoint && o.adjoint === v.chef)) await put("departements", o.id, { ...clean(o), chef: [v.chef, v.adjoint].includes(o.chef) ? "" : o.chef || "", adjoint: [v.chef, v.adjoint].includes(o.adjoint) ? "" : o.adjoint || "" });
      await put("departements", id, { ...clean(d), chef: v.chef || "", adjoint: v.adjoint && v.adjoint !== v.chef ? v.adjoint : "" });
      for (const mid of [v.chef, v.adjoint].filter(Boolean)) { const m = membre(mid); if (m && m.dept !== id) await put("membres", mid, { ...clean(m), dept: id }); }
    }, v.chef ? `${nomMembre(v.chef)} est chef de ${d.court || d.nom}` : "Enregistré");
    if (ok) closeSheet();
  },
  async team(v, f) {
    const id = f.dataset.id, d = dept(id); if (!d) return;
    const on = new Set($$('input[name="m"]:checked', f).map(i => i.value));
    const changes = S.membres.filter(m => (m.dept === id) !== on.has(m.id));
    const ok = await run(async () => {
      for (const m of changes) await put("membres", m.id, { ...clean(m), dept: on.has(m.id) ? id : "" });
      if ((d.chef && !on.has(d.chef)) || (d.adjoint && !on.has(d.adjoint))) await put("departements", id, { ...clean(d), chef: on.has(d.chef) ? d.chef : "", adjoint: on.has(d.adjoint) ? d.adjoint : "" });
    }, `${on.size} protocole${on.size > 1 ? "s" : ""} dans ${d.court || d.nom}`);
    if (ok) closeSheet();
  },
  async dept(v, f) {
    const id = f.dataset.id || slug(v.court || v.nom), old = dept(f.dataset.id) || {};
    const ordre = old.ordre || (Math.max(0, ...depts().map(d => d.ordre || 0)) + 1);
    if (await run(async () => { if (!S.departements.length) await seedDepts(); await put("departements", id, { ...clean(old), nom: v.nom.trim(), court: (v.court || "").trim() || v.nom.trim(), ic: v.ic || "star", mission: (v.mission || "").trim(), ordre, chef: old.chef || "", adjoint: old.adjoint || "" }); }, "Département enregistré")) { deptTab = id; closeSheet(); }
  },
  async tache(v, f) { if (await run(() => put("taches", newId(), { dept: f.dataset.dept, texte: v.texte.trim(), fait: false, par: ME || null, at: Date.now() }), "Tâche ajoutée")) closeSheet(); },
  async stock(v) { const n = Math.max(0, parseInt(v.qte) || 0); if (await run(() => put("stock", newId(), { nom: v.nom.trim(), qte: n, initial: n, unite: (v.unite || "").trim(), maj: Date.now() }), "Boisson ajoutée")) closeSheet(); },
  async sig(v, f) {
    const btn = f.querySelector("[type=submit]"); let photo = null, pos = null; const file = f.querySelector("input[type=file]");
    if (v.pos) { btn.textContent = "Position…"; const p = (Geo.me && Date.now() - Geo.me.at < 120000) ? Geo.me : await getOnce().catch(() => null); if (p) pos = { lat: +p.lat.toFixed(6), lng: +p.lng.toFixed(6), acc: p.acc }; }
    if (file && file.files && file.files[0]) { btn.textContent = "Envoi de la photo…"; try { photo = await saveImage(file.files[0]); } catch (e) { toast(errMsg(e)); return; } }
    if (await run(() => put("signalements", newId(), { titre: v.titre.trim(), detail: (v.detail || "").trim(), dept: v.dept || "", lieu: v.lieu || "", gravite: v.gravite || "urgent", statut: "ouvert", photo, pos, par: ME || null, at: Date.now() }), "Signalement envoyé à l'équipe")) { closeSheet(); if (cur !== "alertes") location.hash = "alertes"; }
  },
  async photos(v, f) {
    const files = [...(f.querySelector("input[type=file]").files || [])]; if (!files.length) { toast("Choisissez au moins une photo."); return; }
    const out = $("#ph-prog", f); let i = 0;
    for (const file of files) { if (out) out.textContent = `Envoi ${i + 1} / ${files.length}…`; try { const img = await saveImage(file); await put("photos", newId(), { img, legende: (v.legende || "").trim(), moment: v.moment || "", par: ME || null, at: Date.now() }); i++; } catch (e) { toast(errMsg(e)); break; } }
    if (i) toast(i > 1 ? `${i} photos ajoutées` : "Photo ajoutée"); if (i === files.length) closeSheet();
  },
  async annonce(v, f) { if (await run(() => put("annonces", newId(), { texte: v.texte.trim(), urgent: !!v.urgent, at: Date.now(), par: ME || null }), "Annonce publiée")) f.reset(); },
  async convoc(v, f) { if (await run(() => put("reglages", "general", { ...clean(reglages()), convoc12: v.convoc12 || "", convoc13: v.convoc13 || "" }), "Horaires enregistrés")) $$("input", f).forEach(i => delete i.dataset.dirty); },
  async prog(v, f) { const id = f.dataset.id || newId(); if (await run(async () => { await seedProg(); await put("programme", id, { jour: v.jour, heure: v.heure, titre: v.titre.trim(), lieu: (v.lieu || "").trim(), adresse: (v.adresse || "").trim(), note: (v.note || "").trim() }); }, "Programme mis à jour")) closeSheet(); },
  async appel(v) { if (!ME) return; const j = appelJour; await run(() => put("presences", ME + "_" + j, { membre: ME, jour: j, at: Date.now(), tenue: !!v.tenue, par: ME }), "Appel enregistré. Bon service !"); if (v.share && !sharing()) startSharing(); },
  "share-hours"(v) { LS.set("pj.shareHours", +v.h || 4); toast("Durée enregistrée"); },
  async unlock(v) { const r = await claimAdmin(v.pass); if (!r.ok) { toast(r.msg); return; } toast(r.created ? "Mot de passe créé. Vous êtes administrateur." : "Vous êtes administrateur."); setTimeout(seedIfEmpty, 1500); },
  async newpass(v, f) { try { await changeAdminPassword(v.pass); toast("Mot de passe changé"); f.reset(); } catch (e) { toast(errMsg(e)); } },
  join(v) { const o = decodeCode(v.code); if (!o) { toast("Code illisible. Vérifiez qu'il commence par PJ1- et qu'il est complet."); return; } saveConnection(o); toast("Connexion à l'équipe…"); setTimeout(() => location.reload(), 600); },
  setup(v) {
    const r = parseFirebaseConfig(v.cfg, v.dburl); if (r.error) { toast(r.error); return; }
    if ((v.pass || "").length < 6) { toast("Mot de passe : 6 caractères minimum."); return; }
    saveConnection({ c: r.c, t: newTeamCode() }); LS.set("pj.pending", { pass: v.pass, migrate: !!v.migrate });
    toast("Création de l'équipe…"); setTimeout(() => location.reload(), 600);
  }
};

/* ================= Données de départ ================= */
async function seedDepts() { for (const d of DEFAULT_DEPTS) await put("departements", d.id, clean(d)); }
async function seedProg() { if (!S.programme.length) for (const p of DEFAULT_PROG) await put("programme", p.id, clean(p)); }
let seeding = false;
async function seedIfEmpty() {
  if (seeding || !isAdmin() || !Store.ready) return; seeding = true;
  try {
    if (!S.departements.length) await seedDepts();
    if (!S.membres.length) for (const [id, nom, whatsapp = "", d = "", role = ""] of SEED_MEMBERS) await put("membres", id, { nom, whatsapp: id === "a-identifier" ? "" : whatsapp, tel: id === "a-identifier" ? whatsapp : "", dept: d, role });
    if (!S.programme.length) await seedProg();
    if (!S.taches.length) { let i = 0; for (const [d, t] of SEED_TASKS) await put("taches", "s-" + (++i), { dept: d, texte: t, fait: false, suggestion: true, at: i }); }
  } catch (e) { } finally { seeding = false; }
}
// Après création de l'équipe : mot de passe administrateur + envoi des données locales.
let pendingBusy = false;
async function afterData() {
  if (Store.mode === "local" && !LS.get("pj.seeded", false) && Store.ready) { LS.set("pj.seeded", true); seedIfEmpty(); return; }
  const p = LS.get("pj.pending", null);
  if (!p || pendingBusy || Store.mode !== "cloud" || !Store.uid || !Store.online || !Store.ready) return;
  pendingBusy = true;
  const r = await claimAdmin(p.pass);
  if (!r.ok) { toast(r.msg); pendingBusy = false; LS.del("pj.pending"); return; }
  LS.del("pj.pending");
  if (p.migrate) {
    for (const c of ["departements", "membres", "invites", "tables", "programme", "taches", "stock", "annonces", "reglages", "arrivees", "presences", "signalements", "photos", "vignettes", "images", "service"]) {
      const o = LS.get("pj.data." + c, {}); for (const [id, d] of Object.entries(o)) { try { await put(c, id, d); } catch (e) { } }
    }
  }
  setTimeout(async () => { await seedIfEmpty(); toast("Équipe créée. Partagez le code de connexion."); shareCodeSheet(); }, 1500);
}

/* ================= Actions ================= */
const ACT = {
  close: () => closeSheet(),
  "lb-close": () => closeLb(),
  undo: () => { const u = toast.undo; $("#toast").hidden = true; toast.undo = null; if (u) u(); },
  me: () => meSheet(),
  menu: () => menuSheet(),
  copy: d => copyText(d.v),
  "pick-me": d => { if (sharing() && ME && ME !== d.id) stopSharing(true); ME = d.id; LS.set("pj.moi", ME); closeSheet(); toast("Bienvenue, " + nomMembre(ME)); refresh(); },
  "forget-me": () => { if (sharing()) stopSharing(true); ME = null; LS.set("pj.moi", null); closeSheet(); refresh(); },
  skin: d => { LS.set("pj.skin", d.v); applySkin(); refresh(); },
  "inv-f": d => { invF = d.v; refresh(); }, "inv-m": d => { invMode = d.v; refresh(); },
  dept: d => { deptTab = d.v; refresh(); }, "sig-f": d => { sigF = d.v; refresh(); }, "ph-f": d => { phF = d.v; refresh(); }, jour: d => { appelJour = d.v; refresh(); },
  arrive: (d, b) => {
    const g = S.invites.find(x => x.id === d.id); if (!g) return; const a = arrivee(d.id);
    if (b.dataset.close) closeSheet();
    if (a) run(() => del("arrivees", d.id), "Arrivée annulée");
    else run(() => put("arrivees", d.id, { at: Date.now(), par: ME || null, nb: +g.places || 1 })).then(ok => { if (ok) toast(`${g.nom} → table ${g.table || "?"}`, () => run(() => del("arrivees", d.id))); });
  },
  guest: d => guestSheet(d.id),
  "inv-add": () => sheet("Nouvel invité", guestForm()),
  "inv-del": d => run(async () => { await del("invites", d.id); await del("arrivees", d.id); }, "Invité supprimé").then(closeSheet),
  import: () => sheet("Importer", `<form class="form" data-form="import"><label>Une ligne par invité<textarea name="liste" rows="9" required placeholder="Papa Jean Mukendi ; 1 ; 2 ; Famille du marié&#10;Maman Rose Kabeya ; 1 ; 1&#10;Pasteur David ; 2"></textarea></label><p class="hint">Format : Nom ; Table ; Places ; Catégorie. Seul le nom est obligatoire. Vous pouvez aussi coller deux colonnes copiées d'Excel. Un nom déjà présent est mis à jour au lieu d'être dupliqué.</p><p class="progress" id="imp-prog"></p><button class="btn gold" type="submit">${ic("upload", 16)} Importer</button></form>`),
  "table-add": () => tableForm(""), "table-edit": d => tableForm(d.l),
  "table-del": d => run(() => del("tables", d.id), "Fiche de table supprimée").then(closeSheet),
  "dept-new": () => deptForm(null), "dept-edit": d => deptForm(d.id), "dept-chef": d => chefSheet(d.id), "dept-team": d => teamSheet(d.id),
  "dept-del": d => run(async () => { for (const m of S.membres.filter(x => x.dept === d.id)) await put("membres", m.id, { ...clean(m), dept: "" }); if (!S.departements.length) await seedDepts(); await del("departements", d.id); }, "Département supprimé").then(closeSheet),
  "tache-add": d => sheet("Nouvelle tâche", `<form class="form" data-form="tache" data-dept="${esc(d.dept)}"><label>${esc(deptCourt(d.dept))} · à faire<input name="texte" required maxlength="140" autocomplete="off" placeholder="Ex. : Vérifier les nappes des tables 1 à 5"></label><button class="btn gold" type="submit">Ajouter</button></form>`),
  "tache-tog": d => { const t = S.taches.find(x => x.id === d.id); if (t) run(() => put("taches", t.id, { ...clean(t), fait: !t.fait, faitPar: !t.fait ? (ME || null) : null, faitAt: !t.fait ? Date.now() : null })); },
  "tache-del": d => run(() => del("taches", d.id)),
  "stock-add": () => sheet("Nouvelle boisson", `<form class="form" data-form="stock"><label>Boisson<input name="nom" required maxlength="60" placeholder="Ex. : Eau minérale" autocomplete="off"></label><div class="two"><label>Quantité de départ<input name="qte" type="number" min="0" max="100000" inputmode="numeric" required></label><label>Unité<input name="unite" maxlength="20" placeholder="casiers"></label></div><button class="btn gold" type="submit">Ajouter</button></form>`),
  "stock-del": d => run(() => del("stock", d.id), "Boisson retirée"),
  stk: d => {
    const it = S.stock.find(x => x.id === d.id); if (!it) return;
    pendStk[d.id] = Math.max(0, (pendStk[d.id] ?? (+it.qte || 0)) + (+d.d)); refresh();
    clearTimeout(stkT[d.id]); stkT[d.id] = setTimeout(async () => { const c = S.stock.find(x => x.id === d.id), q = pendStk[d.id]; if (c && q != null) await run(() => put("stock", d.id, { ...clean(c), qte: q, maj: Date.now(), par: ME || null })); delete pendStk[d.id]; refresh(); }, 700);
  },
  svc: d => { const id = slug(d.l), c = S.service.find(x => x.id === id) || {}; run(() => put("service", id, { ...clean(c), label: d.l, [d.k]: !c[d.k], maj: Date.now(), par: ME || null })); },
  "sig-new": () => sigForm(),
  "sig-take": d => { const s = S.signalements.find(x => x.id === d.id); if (s) run(() => put("signalements", s.id, { ...clean(s), statut: "en_cours", prisPar: ME || null, prisAt: Date.now() }), "Pris en charge"); },
  "sig-done": d => { const s = S.signalements.find(x => x.id === d.id); if (s) run(() => put("signalements", s.id, { ...clean(s), statut: "resolu", resoluPar: ME || null, resoluAt: Date.now() }), "Marqué résolu"); },
  "sig-reopen": d => { const s = S.signalements.find(x => x.id === d.id); if (s) run(() => put("signalements", s.id, { ...clean(s), statut: "ouvert", resoluPar: null, resoluAt: null })); },
  "sig-del": d => run(() => del("signalements", d.id), "Signalement supprimé"),
  "sig-go": d => { const s = S.signalements.find(x => x.id === d.id); if (s && s.pos) openGuide({ type: "point", lat: s.pos.lat, lng: s.pos.lng, label: s.titre, at: s.at }); },
  img: async d => { lightbox(vignette(d.id), "", ""); const full = await getOne("images", d.id).catch(() => null); if (!$("#lb").hidden && full && full.d) $("#lb img") ? ($("#lb img").src = full.d) : lightbox(full.d, "", ""); },
  "ph-add": () => sheet("Ajouter des photos", `<form class="form" data-form="photos"><label class="file">Photos<input type="file" accept="image/*" multiple required></label><div class="two"><label>Moment<select name="moment">${MOMENTS.map(m => opt(m, m, jourCourant() === "13" ? "Réception" : "")).join("")}</select></label><label>Légende<input name="legende" maxlength="120" placeholder="Facultatif"></label></div><p class="hint">Les photos sont allégées automatiquement avant l'envoi.</p><p class="progress" id="ph-prog"></p><button class="btn gold big" type="submit">${ic("upload", 18)} Envoyer</button></form>`),
  photo: async d => {
    const p = S.photos.find(x => x.id === d.id); if (!p) return;
    const canDel = isAdmin() || (ME && p.par === ME);
    const meta = [p.moment, p.par ? "par " + nomMembre(p.par) : "", ago(p.at)].filter(Boolean).join(" · ");
    const acts = canDel ? `<button class="btn sm danger" data-act="ph-del" data-id="${esc(p.id)}" data-confirm="Confirmer">Supprimer</button>` : "";
    lightbox(vignette(p.img), p.legende, meta, acts);
    const full = await getOne("images", p.img).catch(() => null);
    const im = $("#lb img"); if (!$("#lb").hidden && full && full.d) { if (im) im.src = full.d; else lightbox(full.d, p.legende, meta, acts); }
  },
  "ph-del": async d => { const p = S.photos.find(x => x.id === d.id); if (!p) return; if (await run(() => del("photos", p.id), "Photo supprimée")) { closeLb(); del("images", p.img).catch(() => { }); del("vignettes", p.img).catch(() => { }); } },
  "prog-live": d => run(async () => { await seedProg(); await put("etat", "direct", { etape: d.id, at: Date.now() }); }, "Étape marquée en cours"),
  "prog-stop": () => run(() => put("etat", "direct", { etape: null, at: Date.now() }), "Étape terminée"),
  "prog-edit": d => progForm(progList().find(p => p.id === d.id) || {}), "prog-add": () => progForm({}),
  "prog-del": d => run(() => del("programme", d.id), "Moment supprimé").then(closeSheet),
  mem: d => memberSheet(d.id), "mem-add": d => memberSheet(null, d.dept || ""),
  "mem-del": d => run(() => del("membres", d.id), "Membre retiré").then(closeSheet),
  "pres-mark": d => { const j = cur === "appel" ? appelJour : jourCourant(); run(() => put("presences", d.id + "_" + j, { membre: d.id, jour: j, at: Date.now(), tenue: false, par: ME || null }), "Présence enregistrée").then(closeSheet); },
  "pres-undo": d => { const j = cur === "appel" ? appelJour : jourCourant(); run(() => del("presences", d.id + "_" + j), "Appel annulé").then(closeSheet); },
  "ann-del": d => run(() => del("annonces", d.id), "Annonce supprimée"),
  "share-toggle": () => sharing() ? stopSharing() : startSharing(),
  "share-on": () => startSharing(), "share-off": () => stopSharing(),
  guide: d => { closeSheet(); openGuide({ type: "member", id: d.id }); },
  "ask-pos": d => { if (!ME) { meSheet(); return; } run(() => put("demandes", d.id + "__" + ME, { cible: d.id, de: ME, at: Date.now() }), `Demande envoyée à ${prenom(d.id)}`); },
  "share-code": () => shareCodeSheet(), join: () => joinSheet(), setup: () => setupSheet(),
  "leave-team": () => { if (sharing()) stopSharing(true); forgetConnection(); LS.del("pj.pending"); toast("Vous avez quitté l'équipe"); setTimeout(() => location.reload(), 600); },
  "leave-admin": () => run(() => leaveAdmin(), "Vous n'êtes plus administrateur")
};
document.addEventListener("click", e => {
  const b = e.target.closest("[data-act]"); if (!b) return;
  if (b.tagName === "A" && b.dataset.act === "close") { closeSheet(); return; }
  if (b.dataset.confirm && !b.dataset.armed) { e.preventDefault(); b.dataset.armed = "1"; b.dataset.label = b.innerHTML; b.textContent = b.dataset.confirm; setTimeout(() => { if (b.isConnected && b.dataset.armed) { delete b.dataset.armed; b.innerHTML = b.dataset.label; } }, 3000); return; }
  const fn = ACT[b.dataset.act]; if (fn) { e.preventDefault(); fn(b.dataset, b, e); }
});
document.addEventListener("submit", async e => {
  const f = e.target.closest("form[data-form]"); if (!f) return; e.preventDefault();
  const fn = FORMS[f.dataset.form]; if (!fn) return;
  const btn = f.querySelector("[type=submit]"); const lbl = btn && btn.innerHTML; if (btn) btn.disabled = true;
  try { await fn(Object.fromEntries(new FormData(f)), f); } finally { if (btn && btn.isConnected) { btn.disabled = false; btn.innerHTML = lbl; } }
});
document.addEventListener("change", e => { const k = e.target.dataset && e.target.dataset.tenue; if (!k) return; const o = LS.get("pj.tenue", {}); o[k] = e.target.checked; LS.set("pj.tenue", o); });
document.addEventListener("input", e => { if (e.target.matches("#cv12,#cv13")) e.target.dataset.dirty = "1"; });
document.addEventListener("keydown", e => { if (e.key === "Escape") { if (!$("#lb").hidden) closeLb(); else if (!$("#sheet").hidden) closeSheet(); } });

/* ================= Démarrage ================= */
$("#menu-btn").innerHTML = ic("menu", 18);
applySkin();
onChange(refresh);
route();
start().then(() => { refresh(); if (shareUntil && shareUntil > Date.now() && ME) startWatch().then(() => publishPos(true)).catch(() => { }); });
if (Capacitor.isNativePlatform()) {
  App.addListener("backButton", () => {
    if (!$("#lb").hidden) closeLb();
    else if (!$("#sheet").hidden) closeSheet();
    else if (cur !== "accueil") { if (history.length > 1) history.back(); else location.hash = "accueil"; }
    else App.minimizeApp();
  });
  App.addListener("resume", () => refresh());
}
