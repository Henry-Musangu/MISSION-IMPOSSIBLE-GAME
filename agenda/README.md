# Agenda

Un agenda personnel pour **PC** et **Android**, écrit en **Java**, sans aucune bibliothèque externe.
Les deux versions partagent le même cœur (`agenda/core`) et le même format de fichier.

**Fonctions :**

- vues **Mois**, **Semaine**, **Jour** et **Liste** des prochains événements (sur Android : mois + programme du jour, et liste) ;
- événements à heure fixe, **sur la journée entière** ou **sur plusieurs jours** ;
- **répétitions** : tous les jours, toutes les semaines (avec le choix des jours : « lundi et jeudi »),
  tous les mois (« le 15 », « le 2e mardi », « le dernier vendredi »), tous les ans, tous les *n* jours / semaines…,
  sans fin, jusqu'à une date ou un nombre de fois ;
- modifier ou supprimer **une seule occurrence**, **celle-ci et les suivantes**, ou **toute la série** ;
- **7 catégories en couleur** : Personnel, Travail, Famille, Santé, Loisirs, Anniversaire, Autre ;
- **rappels** (de « au début » à « 1 semaine avant ») : notifications sur Android, fenêtre de rappel sur PC ;
- **recherche** dans les titres, lieux, notes et catégories (sans se soucier des accents ni des majuscules) ;
- **import et export au format .ics** (iCalendar), compatible Google Agenda, Outlook, Apple Calendrier, Thunderbird.
  Taper « Anniversaire de… » propose tout seul la catégorie Anniversaire, la journée entière et la répétition annuelle.

## Sur Android

### Télécharger l'APK

Depuis le téléphone, ouvre ce lien :

**https://github.com/Henry-Musangu/MISSION-IMPOSSIBLE-GAME/releases/download/agenda/agenda.apk**

puis ouvre le fichier téléchargé et autorise l'installation depuis le navigateur. L'icône « Agenda » apparaît.
Au premier lancement, accepte les **notifications** : c'est ce qui permet les rappels.

L'APK est recompilé à chaque envoi de code sur GitHub (Release « agenda »). Il est toujours signé avec la même
clé (`agenda/android/debug.keystore`), donc une nouvelle version **s'installe par-dessus l'ancienne sans
perdre les événements**.

Avec Android Studio ou le SDK Android : `./gradlew :agenda-app:assembleDebug`
(l'APK est dans `agenda/android/build/outputs/apk/debug/`). Il faut Android 8.0 ou plus récent.

### Utilisation

| Geste | Action |
| --- | --- |
| Toucher un jour | Voir le programme de ce jour sous le calendrier |
| Glisser vers la gauche / la droite | Mois suivant / précédent |
| Appui long sur un jour | Nouvel événement ce jour-là |
| Bouton rond **+** | Nouvel événement |
| Toucher un événement | Le modifier ou le supprimer |
| Menu ⋮ | Vue liste, événement sur la journée, importer / exporter un fichier .ics |
| Loupe | Rechercher |

Ouvrir un fichier `.ics` reçu (pièce jointe, gestionnaire de fichiers) propose de l'importer dans l'agenda.

## Sur PC

Il faut Java 8 ou plus récent (JDK, pour avoir `javac`).

- **Windows** : double-clique sur `agenda.bat`
- **Mac / Linux** : `./agenda.sh`

| Action | Souris / clavier |
| --- | --- |
| Nouvel événement | bouton **+ Nouvel événement**, Ctrl+N, Entrée, ou double-clic sur un jour / une heure |
| Modifier un événement | clic dessus |
| Changer de vue | Jour / Semaine / Mois / Liste, ou Ctrl+1 à Ctrl+4 |
| Période précédente / suivante | ‹ ›, Page préc. / Page suiv., molette en vue Mois |
| Changer de jour | flèches du clavier, ou le petit calendrier à droite |
| Revenir à aujourd'hui | Aujourd'hui, ou Ctrl+T |
| Rechercher | Ctrl+F (Échap pour effacer) |
| Importer / exporter un .ics | menu Fichier (Ctrl+I / Ctrl+E) |

Les rappels s'affichent tant que l'agenda est ouvert.

## Où sont mes événements ?

Dans un fichier iCalendar standard, enregistré automatiquement à chaque modification
(la version précédente est gardée à côté, en `.bak`) :

- **Windows** : `%APPDATA%\Agenda\agenda.ics`
- **Mac / Linux** : `~/.agenda/agenda.ics`
- **Android** : dans la mémoire privée de l'application (sauvegardée par la sauvegarde Google du téléphone).
  Menu ⋮ → *Exporter* pour en faire une copie.

Pour passer du téléphone au PC (ou l'inverse) : *Exporter* d'un côté, *Importer* de l'autre.
Un événement déjà présent (même identifiant) est remplacé, pas dupliqué.

## Organisation du code

| Dossier / fichier | Rôle |
| --- | --- |
| `core/.../Event.java`, `Recurrence.java`, `Category.java` | Un événement, sa règle de répétition, sa catégorie |
| `core/.../Agenda.java` | Les événements : occurrences d'une période, rappels, recherche, modification d'une occurrence |
| `core/.../Ics.java`, `AgendaFile.java` | Lecture / écriture du format .ics, enregistrement sûr du fichier |
| `core/.../Fmt.java`, `DayLayout.java` | Dates en français ; placement des événements qui se chevauchent |
| `core/src/test/.../AgendaTests.java` | Tests du cœur |
| `desktop/` | Version PC (Swing) : fenêtre, vues mois / semaine / jour / liste, formulaire, rappels |
| `android/` | Version Android : écran principal, formulaire, notifications et alarmes |

Lancer les tests :

```sh
mkdir -p out/agenda-tests
javac -encoding UTF-8 -d out/agenda-tests agenda/core/src/main/java/agenda/*.java agenda/core/src/test/java/agenda/*.java
java -cp out/agenda-tests agenda.AgendaTests
```
