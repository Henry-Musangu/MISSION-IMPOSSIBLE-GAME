# Henry le Hérisson

Un jeu de plateforme rapide inspiré de *Sonic the Hedgehog*, écrit en **Java**, pour **PC** et **Android**.
Aucune bibliothèque externe ni fichier d'image ou de son : tout est dessiné et synthétisé par le code.

Henry, un hérisson violet à l'écharpe rouge, doit traverser trois zones et battre le
**Dr. Vortex**, qui a enfermé les animaux dans ses robots.

## Jouer sur Android

### Option 1 : télécharger l'APK compilé par GitHub

À chaque envoi de code sur GitHub, l'APK est compilé automatiquement :

1. Sur GitHub, ouvre l'onglet **Actions** du dépôt, puis la dernière exécution « APK Android ».
2. En bas de la page, dans **Artifacts**, télécharge `henry-le-herisson-apk` (un fichier .zip qui contient l'APK).
3. Copie `henry-le-herisson.apk` sur le téléphone et ouvre-le. Android demande d'autoriser
   l'installation d'applications provenant de cette source : accepte.

### Option 2 : avec Android Studio

1. Ouvre le dossier du dépôt dans **Android Studio** (« Open »).
2. Attends la synchronisation Gradle, branche le téléphone (mode développeur + débogage USB activés).
3. Choisis la configuration **app** et clique sur ▶ **Run**.

Ou en ligne de commande, avec le SDK Android installé : `./gradlew :app:assembleDebug`
(l'APK se trouve dans `android/build/outputs/apk/debug/`).

### Commandes tactiles

| Bouton | Action |
| --- | --- |
| ◀ ▶ (en bas à gauche) | Courir |
| ▼ | Se mettre en boule en courant ; à l'arrêt : ▼ maintenu + **A** plusieurs fois = spin dash |
| **A** (en bas à droite) | Sauter (maintenir pour sauter plus haut) |
| ⏸ (en haut à droite) | Pause (le bouton « retour » du téléphone aussi) |
| 🔈 | Couper ou remettre le son |

## Jouer sur PC

Il faut Java 8 ou plus récent (JDK, pour avoir `javac`).

- **Windows** : double-clique sur `jouer.bat`
- **Mac / Linux** : `./jouer.sh`

| Action | Touches |
| --- | --- |
| Courir | ← → (ou Q / D, ou A / D) |
| Sauter (maintenir pour sauter plus haut) | Espace (ou X / K) |
| Se mettre en boule en courant | ↓ (ou S) |
| Spin dash | à l'arrêt : ↓ maintenu + Espace plusieurs fois, puis relâcher ↓ |
| Regarder en haut | ↑ (ou Z / W) |
| Pause | P ou Échap |
| Couper le son | M |
| Commencer / passer | Entrée |

## Règles

- Ramasse des **anneaux** : si tu es touché, tu les perds au lieu de perdre une vie (tu peux en
  rattraper quelques-uns). Touché sans anneau = une vie perdue. 100 anneaux = une vie en plus.
- Saute ou roule sur les **robots** pour libérer les animaux.
- Les **ressorts** jaunes t'envoient très haut, les **pics** font mal.
- Touche les **bornes bleues** pour repartir de là après une chute.
- Passe devant le **panneau** pour finir le niveau : plus tu vas vite, plus le bonus est gros.
- Dans l'usine, touche le Dr. Vortex **8 fois** en boule pour gagner.

## Les niveaux

1. **Zone Prairie Émeraude** : collines vertes, idéal pour apprendre.
2. **Zone Canyon Crépuscule** : falaises, piliers au-dessus du vide et pics.
3. **Usine du Dr. Vortex** : la zone finale et le combat contre le boss.

## Organisation du code

Le jeu lui-même est écrit une seule fois dans `core/` ; les versions PC et Android ne font
qu'afficher l'image, lire les commandes et jouer les sons.

| Dossier / fichier | Rôle |
| --- | --- |
| `core/.../Game.java` | États (titre, jeu, fin de niveau, game over, victoire), collisions, caméra, affichage |
| `core/.../Player.java` | Physique et dessin d'Henry (élan, saut, roulade, spin dash) |
| `core/.../Level.java`, `Levels.java` | Carte en tuiles et construction des trois niveaux |
| `core/.../Theme.java` | Couleurs et tuiles de chaque zone |
| `core/.../Enemy.java`, `Boss.java`, `Bomb.java` | Les robots et le Dr. Vortex |
| `core/.../Ring.java`, `Spring.java`, `Checkpoint.java`, `Goal.java` | Objets du niveau |
| `core/.../Gfx.java` | Interface de dessin commune aux deux plateformes |
| `core/.../Input.java`, `Sound.java` | Boutons du jeu et bruitages synthétisés |
| `desktop/` | Version PC : fenêtre Swing, clavier, son javax.sound |
| `android/` | Version Android : écran tactile, `SurfaceView`, son `SoundPool`, icône |

### Créer ton propre niveau

Dans `core/src/main/java/henry/Levels.java`, chaque niveau est construit avec de petites instructions :

```java
b.ground(0, 45, 11);    // sol de la colonne 0 à 45, surface à la ligne 11
b.rings(8, 13, 9);      // anneaux de la colonne 8 à 13, ligne 9
b.at(20, 10, 'E');      // un robot (E), une guêpe (B), un ressort (J),
                        // une borne (C), le panneau d'arrivée (F), le départ (P)
b.spikes(86, 87, 10);   // des pics
b.plat(64, 69, 8);      // une plate-forme traversable par dessous
```

La grille fait 15 lignes de haut, et chaque case mesure 32 pixels.

Pour changer la couleur d'Henry, modifie `BODY`, `SHOE` et `SCARF` au début de `Player.java`.
