// Données de l'application : d'abord sur le téléphone, puis synchronisées par Internet
// (Firebase Realtime Database) dès qu'un code de connexion est configuré.
import { initializeApp, deleteApp } from "firebase/app";
import { initializeAuth, indexedDBLocalPersistence, browserLocalPersistence, signInAnonymously, onAuthStateChanged, connectAuthEmulator } from "firebase/auth";
import { getDatabase, ref, onValue, set, remove, get, off, connectDatabaseEmulator } from "firebase/database";
import { BAKED } from "./config.js";

// Collections suivies en permanence (petites). Les images ne sont chargées qu'à la demande.
export const COLLS = ["membres", "departements", "invites", "tables", "arrivees", "presences", "signalements", "photos", "taches", "stock", "service", "annonces", "programme", "etat", "reglages", "positions", "demandes"];
export const LAZY = ["vignettes"];
// Seul l'administrateur modifie ces collections (mêmes règles que firebase/database.rules.json).
export const ADMIN_ONLY = ["invites", "tables", "membres", "departements", "programme", "annonces", "etat", "reglages"];

export const LS = {
  get(k, d) { try { const v = localStorage.getItem(k); return v == null ? d : JSON.parse(v); } catch (e) { return d; } },
  set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); return true; } catch (e) { return false; /* stockage plein ou bloqué */ } },
  del(k) { try { localStorage.removeItem(k); } catch (e) { } }
};

export const S = {};
[...COLLS, ...LAZY].forEach(c => S[c] = []);

const st = {
  mode: "local",      // "local" (ce téléphone seulement) ou "cloud" (équipe synchronisée)
  online: false,      // connecté au serveur en ce moment
  uid: null,
  team: null,
  isAdmin: false,
  ready: false,       // premières données reçues du serveur
  error: null,
  loaded: {}
};
export const Store = st;

let app = null, db = null, auth = null, listeners = [], onChangeFn = () => { }, onLateError = () => { };
const loadedLazy = new Set();
const toArr = o => Object.entries(o || {}).map(([id, d]) => ({ id, ...(d && typeof d === "object" ? d : { v: d }) }));
const clean = o => JSON.parse(JSON.stringify(o, (k, v) => v === undefined ? null : v));

export function onChange(fn) { onChangeFn = fn; }
export function onError(fn) { onLateError = fn; }
const emit = () => onChangeFn();

/* ---------- Code de connexion ---------- */
export function encodeCode(c, t) {
  const json = JSON.stringify({ c, t });
  return "PJ1-" + btoa(unescape(encodeURIComponent(json))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
export function decodeCode(txt) {
  const m = String(txt || "").match(/PJ1-([A-Za-z0-9_-]+)/);
  if (!m) return null;
  try {
    let b = m[1].replace(/-/g, "+").replace(/_/g, "/"); while (b.length % 4) b += "=";
    const o = JSON.parse(decodeURIComponent(escape(atob(b))));
    if (o && o.c && o.c.apiKey && o.c.databaseURL && o.t) return o;
  } catch (e) { }
  return null;
}
// Lit la configuration copiée depuis la console Firebase (bloc « const firebaseConfig = {...} »).
export function parseFirebaseConfig(txt, dbUrl) {
  const pick = k => { const m = String(txt || "").match(new RegExp(k + "\\s*[:=]\\s*[\"']([^\"']+)[\"']")); return m ? m[1].trim() : ""; };
  const c = { apiKey: pick("apiKey"), authDomain: pick("authDomain"), databaseURL: (dbUrl || pick("databaseURL") || "").trim().replace(/\/+$/, ""), projectId: pick("projectId"), appId: pick("appId") };
  if (!c.authDomain && c.projectId) c.authDomain = c.projectId + ".firebaseapp.com";
  if (!c.apiKey || !c.projectId) return { error: "Je ne trouve pas apiKey et projectId dans le texte collé." };
  if (!/^https:\/\/[a-z0-9.-]+\.(firebaseio\.com|firebasedatabase\.app)$/i.test(c.databaseURL)) return { error: "Adresse de la base manquante ou incorrecte (elle se termine par firebaseio.com ou firebasedatabase.app)." };
  return { c };
}
export const savedConnection = () => LS.get("pj.connexion", null) || BAKED;
export function saveConnection(conn) { LS.set("pj.connexion", conn); }
export function forgetConnection() { LS.del("pj.connexion"); }
export const newTeamCode = () => "cj-" + Array.from(crypto.getRandomValues(new Uint8Array(9)), b => "abcdefghjkmnpqrstuvwxyz23456789"[b % 31]).join("");

/* ---------- Démarrage ---------- */
export async function start() {
  const conn = savedConnection();
  if (conn && conn.c && conn.t) {
    try { await connectCloud(conn); return; } catch (e) { st.error = e; }
  }
  startLocal();
}

function startLocal() {
  st.mode = "local"; st.isAdmin = true; st.online = false; st.ready = true;
  [...COLLS, ...LAZY].forEach(c => { S[c] = toArr(LS.get("pj.data." + c, {})); st.loaded[c] = true; });
  emit();
}

let cacheTimer = null;
function saveCache() {
  clearTimeout(cacheTimer);
  cacheTimer = setTimeout(() => { const o = {}; COLLS.forEach(c => o[c] = S[c]); LS.set("pj.cache." + st.team, o); }, 800);
}

async function connectCloud({ c, t }) {
  st.mode = "cloud"; st.team = t;
  const cache = LS.get("pj.cache." + t, null);
  if (cache) COLLS.forEach(k => { if (Array.isArray(cache[k])) S[k] = cache[k]; });
  app = initializeApp(c, "pj-" + t);
  auth = initializeAuth(app, { persistence: [indexedDBLocalPersistence, browserLocalPersistence] });
  db = getDatabase(app, c.databaseURL);
  // Tests uniquement : émulateurs Firebase locaux (jamais défini dans l'application installée).
  const emu = LS.get("pj.emu", null);
  if (emu) { connectAuthEmulator(auth, "http://" + emu.auth, { disableWarnings: true }); connectDatabaseEmulator(db, emu.host, emu.port); }
  const infoRef = ref(db, ".info/connected");
  onValue(infoRef, s => { st.online = !!s.val(); emit(); });
  listeners.push(infoRef);
  emit();
  onAuthStateChanged(auth, u => {
    if (u && u.uid !== st.uid) { st.uid = u.uid; subscribeAll(); emit(); }
  });
  trySignIn();
}

let signTimer = null;
function trySignIn() {
  if (!auth || auth.currentUser) return;
  signInAnonymously(auth).catch(e => {
    st.error = e; emit();
    clearTimeout(signTimer); signTimer = setTimeout(trySignIn, 8000); // réessaie quand le réseau revient
  });
}
if (typeof window !== "undefined") window.addEventListener("online", () => trySignIn());

function path(c, id) { return "equipes/" + st.team + "/" + c + (id ? "/" + id : ""); }

function subscribeAll() {
  COLLS.forEach(c => listen(c));
  loadedLazy.forEach(c => listen(c));
  const r = ref(db, "adminclaims/" + st.team + "/" + st.uid);
  onValue(r, s => { st.isAdmin = s.exists(); emit(); }, () => { st.isAdmin = false; emit(); });
  listeners.push(r);
}
function listen(c) {
  const r = ref(db, path(c));
  onValue(r, snap => {
    S[c] = toArr(snap.val());
    st.loaded[c] = true;
    if (COLLS.every(k => st.loaded[k])) st.ready = true;
    if (COLLS.includes(c)) saveCache();
    emit();
  }, err => { st.error = err; emit(); });
  listeners.push(r);
}
// Charge une collection lourde seulement quand un écran en a besoin.
export function ensure(c) {
  if (loadedLazy.has(c)) return;
  loadedLazy.add(c);
  if (st.mode === "cloud" && st.uid) listen(c);
}

/* ---------- Écriture ---------- */
// Le serveur confirme plus tard si le téléphone est hors ligne : on n'attend pas plus de 1,2 s,
// la modification est déjà visible et sera envoyée au retour du réseau.
function ack(p) {
  return new Promise((res, rej) => {
    let late = false;
    const t = setTimeout(() => { late = true; res("en attente"); }, 1200);
    p.then(() => { if (!late) { clearTimeout(t); res("ok"); } },
      e => { if (!late) { clearTimeout(t); rej(e); } else onLateError(e); });
  });
}
const queues = {};
function queued(key, fn) { const p = (queues[key] || Promise.resolve()).catch(() => { }).then(fn); queues[key] = p; return p; }

export function put(c, id, data) {
  const body = clean(data);
  if (st.mode === "cloud") {
    if (!st.uid) return Promise.reject({ code: "NO_AUTH" });
    return queued(c + "/" + id, () => ack(set(ref(db, path(c, id)), body)));
  }
  const o = LS.get("pj.data." + c, {}); o[id] = body;
  if (!LS.set("pj.data." + c, o)) return Promise.reject({ code: "quota" });
  if (c !== "images") { S[c] = toArr(o); emit(); }
  return Promise.resolve("ok");
}
export function del(c, id) {
  if (st.mode === "cloud") {
    if (!st.uid) return Promise.reject({ code: "NO_AUTH" });
    return queued(c + "/" + id, () => ack(remove(ref(db, path(c, id)))));
  }
  const o = LS.get("pj.data." + c, {}); delete o[id]; LS.set("pj.data." + c, o); S[c] = toArr(o); emit();
  return Promise.resolve("ok");
}
// Lecture ponctuelle (image en grand format).
export async function getOne(c, id) {
  if (st.mode === "cloud") { const s = await get(ref(db, path(c, id))); return s.exists() ? s.val() : null; }
  const o = LS.get("pj.data." + c, {}); return o[id] ?? null;
}

/* ---------- Administration ---------- */
async function sha256(txt) {
  const b = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(txt));
  return Array.from(new Uint8Array(b), x => x.toString(16).padStart(2, "0")).join("");
}
const isPerm = e => /permission/i.test(String((e && (e.code || e.message)) || ""));
// Déverrouille l'administration avec le mot de passe. La toute première fois, il est créé.
export async function claimAdmin(password) {
  if (st.mode !== "cloud") return { ok: true };
  if (!st.uid || !st.online) return { ok: false, msg: "Connexion Internet nécessaire." };
  const h = await sha256(st.team + ":" + password);
  try { await set(ref(db, "adminclaims/" + st.team + "/" + st.uid), h); return { ok: true }; }
  catch (e) { if (!isPerm(e)) return { ok: false, msg: "Connexion impossible. Réessayez." }; }
  try { await set(ref(db, "adminpass/" + st.team), { [h]: true }); }
  catch (e) { return { ok: false, msg: isPerm(e) ? "Mot de passe incorrect." : "Connexion impossible. Réessayez." }; }
  try { await set(ref(db, "adminclaims/" + st.team + "/" + st.uid), h); return { ok: true, created: true }; }
  catch (e) { return { ok: false, msg: "Mot de passe créé, mais l'accès a échoué. Réessayez." }; }
}
export async function changeAdminPassword(password) {
  const h = await sha256(st.team + ":" + password);
  await set(ref(db, "adminpass/" + st.team), { [h]: true });
  await set(ref(db, "adminclaims/" + st.team + "/" + st.uid), h);
}
export async function leaveAdmin() {
  if (st.mode === "cloud" && st.uid) await remove(ref(db, "adminclaims/" + st.team + "/" + st.uid));
}

export async function disconnect() {
  listeners.forEach(r => off(r)); listeners = [];
  if (app) { try { await deleteApp(app); } catch (e) { } }
  app = db = auth = null;
}
