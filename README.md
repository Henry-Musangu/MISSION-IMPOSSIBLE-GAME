# Henry le Hérisson

Un jeu de plateforme rapide inspiré de *Sonic the Hedgehog*, écrit en **Java**, pour **PC** et **Android**.
Aucune bibliothèque externe ni fichier d'image ou de son : tout est dessiné et synthétisé par le code.

Henry, un hérisson violet à l'écharpe rouge, doit traverser trois zones et battre le
**Dr. Vortex**, qui a enfermé les animaux dans ses robots.

**Au programme :** physique à élan sur les pentes, tremplins et **loopings**, 6 actes (3 zones × 2),
un **boss différent à la fin de chaque zone**, moniteurs à bonus (bouclier, chaussures de vitesse,
invincibilité, vie, anneaux), plates-formes mobiles, routes cachées en hauteur, **musiques originales**
pour chaque zone, sauvegarde de la progression et du meilleur score, choix de la zone au menu,
vibrations sur téléphone.

## Jouer sur Android

### Option 1 : télécharger l'APK (le plus simple)

Depuis le téléphone, ouvre ce lien :

**https://github.com/Henry-Musangu/MISSION-IMPOSSIBLE-GAME/releases/latest/download/henry-le-herisson.apk**

puis ouvre le fichier téléchargé. Android demande d'autoriser l'installation d'applications
provenant de cette source (le navigateur) : accepte. L'icône « Henry le Hérisson » apparaît ensuite.

L'APK est recompilé et republié automatiquement à chaque envoi de code sur GitHub
(onglet **Actions** ; la Release s'appelle « android »). Pour mettre à jour le jeu,
retélécharge-le avec le même lien.

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
- Saute ou roule sur les **robots** pour libérer les animaux. Attention au **crabe** : il tire !
- **Les pentes comptent** : on accélère en descente, on ralentit en montée. En boule (↓ en courant),
  une descente donne énormément de vitesse. Un **looping** demande d'arriver vite.
- Casse les **moniteurs** (télévisions) en sautant dessus :

| Moniteur | Effet |
| --- | --- |
| Anneau | +10 anneaux |
| Bulle bleue | **Bouclier** : protège d'un coup sans perdre d'anneaux |
| Chaussure rouge | **Vitesse** pendant 15 secondes |
| Étoile | **Invincible** pendant 20 secondes |
| Tête d'Henry | Une vie en plus |

- Les **ressorts** jaunes t'envoient très haut, vers des routes secrètes. Les **pics** font mal.
- Touche les **bornes bleues** pour repartir de là après une chute.
- Passe devant le **panneau** pour finir l'acte : plus tu vas vite, plus le bonus est gros.
- À la fin de chaque zone, le **Dr. Vortex** attaque : touche-le en boule jusqu'à vider sa jauge.

## Les zones

1. **Prairie Émeraude** : collines, loopings et palmiers. Boss : bombes.
2. **Canyon Crépuscule** : falaises, tremplins et plates-formes mobiles. Boss : bombes et boules de feu.
3. **Usine du Dr. Vortex** : métal, liquide toxique et crabes. Boss final : bombes et piqués.

Chaque acte terminé est sauvegardé : au menu, la ligne « zone » permet de rejouer n'importe quel
acte déjà débloqué.

## Organisation du code

Le jeu lui-même est écrit une seule fois dans `core/` ; les versions PC et Android ne font
qu'afficher l'image, lire les commandes et jouer les sons.

| Dossier / fichier | Rôle |
| --- | --- |
| `core/.../Game.java` | États (titre, jeu, fin de niveau, game over, victoire), collisions, caméra, affichage |
| `core/.../Player.java` | Physique et dessin d'Henry (élan, saut, roulade, spin dash) |
| `core/.../Level.java`, `Levels.java` | Carte en tuiles et construction des trois niveaux |
| `core/.../Theme.java` | Couleurs et tuiles de chaque zone |
| `core/.../Enemy.java`, `Boss.java`, `Bomb.java`, `Shot.java` | Les robots, le Dr. Vortex et leurs projectiles |
| `core/.../Ring.java`, `Spring.java`, `Checkpoint.java`, `Goal.java`, `Monitor.java` | Objets du niveau |
| `core/.../Loop.java`, `Platform.java` | Loopings et plates-formes mobiles |
| `core/.../Music.java` | Les musiques, écrites note par note et synthétisées |
| `core/.../Storage.java` | Sauvegarde (meilleur score, zones débloquées, son) |
| `core/.../Gfx.java` | Interface de dessin commune aux deux plateformes |
| `core/.../Input.java`, `Sound.java` | Boutons du jeu et bruitages synthétisés |
| `desktop/` | Version PC : fenêtre Swing, clavier, son javax.sound |
| `android/` | Version Android : écran tactile, `SurfaceView`, son `SoundPool`, icône |

### Créer ton propre niveau

Dans `core/src/main/java/henry/Levels.java`, chaque acte est assemblé de gauche à droite
avec des « segments » :

```java
int c = b.flat(12);        // 12 colonnes de sol plat (c = première colonne)
b.put(c + 3, 'P');         // départ d'Henry posé sur le sol
b.up45(3);                 // montée à 45° de 3 cases ; b.down45(3) pour descendre
b.up22(2);                 // montée douce ; b.down22(2) pour descendre
b.loop();                  // un looping
c = b.pit(6);              // un gouffre de 6 colonnes
b.movingH(c + 1, b.gr, 3, 2.5, 0.02);   // plate-forme mobile au-dessus
b.put(c + 4, 'E');         // robot (E), guêpe (B), crabe (K), ressort (J), borne (C),
                           // panneau (F), moniteurs '1' à '5'
b.ringsOver(c, c + 6, 2);  // anneaux qui suivent le relief
b.arena();                 // arène du boss (à la fin de l'acte 2)
```

La grille fait 20 lignes de haut, et chaque case mesure 32 pixels.

Pour changer la couleur d'Henry, modifie `BODY`, `SHOE` et `SCARF` au début de `Player.java`.
