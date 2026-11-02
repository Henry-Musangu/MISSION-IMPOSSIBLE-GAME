// Localisation : position GPS du téléphone, boussole, distance et direction vers un protocole.
import { Geolocation } from "@capacitor/geolocation";
import { Capacitor } from "@capacitor/core";

export const Geo = {
  me: null,          // {lat, lng, acc, at, speed, course}
  heading: null,     // cap de la boussole en degrés (0 = nord)
  watching: false,
  error: null
};
let watchId = null, subs = new Set(), compassOn = false;
const notify = () => subs.forEach(f => { try { f(); } catch (e) { } });
export const onGeo = fn => { subs.add(fn); return () => subs.delete(fn); };

export function geoErrorText(e) {
  const m = String((e && (e.message || e.code)) || e || "");
  if (/denied|permission|refus/i.test(m)) return "Autorisez la localisation pour cette application dans les réglages du téléphone.";
  if (/disabled|services|0007|0003|unavailable|location is off/i.test(m)) return "Activez la localisation (GPS) du téléphone, puis réessayez.";
  if (/timeout|0010/i.test(m)) return "Le GPS ne répond pas. Placez-vous près d'une fenêtre ou à l'extérieur.";
  return "Position introuvable pour l'instant. Vérifiez que le GPS est activé.";
}

export async function ensurePermission() {
  if (!Capacitor.isNativePlatform()) return true;
  let p = await Geolocation.checkPermissions();
  if (p.location !== "granted" && p.coarseLocation !== "granted") {
    p = await Geolocation.requestPermissions({ permissions: ["location", "coarseLocation"] });
  }
  if (p.location !== "granted" && p.coarseLocation !== "granted") throw new Error("permission denied");
  return true;
}

export async function startWatch() {
  if (watchId != null) return;
  await ensurePermission();
  Geo.error = null;
  watchId = await Geolocation.watchPosition(
    { enableHighAccuracy: true, timeout: 20000, maximumAge: 4000, interval: 5000, minimumUpdateInterval: 3000 },
    (pos, err) => {
      if (err) { Geo.error = err; notify(); return; }
      if (!pos) return;
      const c = pos.coords;
      Geo.me = { lat: c.latitude, lng: c.longitude, acc: Math.round(c.accuracy || 0), at: pos.timestamp || Date.now(), speed: c.speed ?? null, course: (c.heading ?? c.course) ?? null };
      Geo.error = null; notify();
    });
  Geo.watching = true; notify();
}
export async function getOnce() {
  await ensurePermission();
  const pos = await Geolocation.getCurrentPosition({ enableHighAccuracy: true, timeout: 9000, maximumAge: 60000 });
  const c = pos.coords;
  return { lat: c.latitude, lng: c.longitude, acc: Math.round(c.accuracy || 0), at: pos.timestamp || Date.now() };
}
export async function stopWatch() {
  if (watchId != null) { try { await Geolocation.clearWatch({ id: watchId }); } catch (e) { } }
  watchId = null; Geo.watching = false; notify();
}

/* Boussole : cap magnétique du téléphone tenu à plat. */
function onOrient(e) {
  let h = null;
  if (typeof e.webkitCompassHeading === "number") h = e.webkitCompassHeading;
  else if (e.absolute && typeof e.alpha === "number") h = 360 - e.alpha;
  if (h == null) return;
  const ang = (screen.orientation && screen.orientation.angle) || 0;
  Geo.heading = (h + ang + 360) % 360;
  notify();
}
export function startCompass() {
  if (compassOn) return; compassOn = true;
  if ("ondeviceorientationabsolute" in window) window.addEventListener("deviceorientationabsolute", onOrient);
  else window.addEventListener("deviceorientation", onOrient);
}
export function stopCompass() {
  compassOn = false;
  window.removeEventListener("deviceorientationabsolute", onOrient);
  window.removeEventListener("deviceorientation", onOrient);
}

/* Calculs */
const rad = d => d * Math.PI / 180, deg = r => r * 180 / Math.PI;
export function distance(a, b) {
  const R = 6371000, dLat = rad(b.lat - a.lat), dLng = rad(b.lng - a.lng);
  const x = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(x));
}
export function bearing(a, b) {
  const y = Math.sin(rad(b.lng - a.lng)) * Math.cos(rad(b.lat));
  const x = Math.cos(rad(a.lat)) * Math.sin(rad(b.lat)) - Math.sin(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.cos(rad(b.lng - a.lng));
  return (deg(Math.atan2(y, x)) + 360) % 360;
}
export function fmtDist(m) {
  if (m == null || !isFinite(m)) return "—";
  if (m < 10) return "moins de 10 m";
  if (m < 1000) return Math.round(m / 5) * 5 + " m";
  return (m / 1000).toFixed(m < 10000 ? 1 : 0).replace(".", ",") + " km";
}
const DIRS = ["nord", "nord-est", "est", "sud-est", "sud", "sud-ouest", "ouest", "nord-ouest"];
export const dirName = b => DIRS[Math.round(b / 45) % 8];
export const walkMinutes = m => Math.max(1, Math.round(m / 75)); // environ 4,5 km/h
export const mapsDirections = (p, mode = "walking") => `https://www.google.com/maps/dir/?api=1&destination=${p.lat},${p.lng}&travelmode=${mode}`;
