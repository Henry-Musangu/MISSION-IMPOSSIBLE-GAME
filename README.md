# Henry le Hérisson

Un jeu de plateforme rapide inspiré de *Sonic the Hedgehog*, écrit en **Java pur (Swing)**,
sans aucune bibliothèque externe ni fichier d'image ou de son : tout est dessiné et synthétisé par le code.

Henry, un hérisson violet à l'écharpe rouge, doit traverser trois zones et battre le
**Dr. Vortex**, qui a enfermé les animaux dans ses robots.

## Lancer le jeu

Il faut Java 8 ou plus récent (JDK, pour avoir `javac`).

- **Windows** : double-clique sur `jouer.bat`
- **Mac / Linux** : `./jouer.sh`

Ou à la main :

```bash
javac -encoding UTF-8 -d out src/henry/*.java
java -cp out henry.Main
```

## Commandes

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

## Organisation du code (`src/henry/`)

| Fichier | Rôle |
| --- | --- |
| `Main.java` | Ouvre la fenêtre |
| `GamePanel.java` | Boucle de jeu à 60 images/s, clavier, mise à l'échelle |
| `Game.java` | États (titre, jeu, fin de niveau, game over, victoire), collisions, caméra, affichage |
| `Player.java` | Physique et dessin d'Henry (élan, saut, roulade, spin dash) |
| `Level.java` / `Levels.java` | Carte en tuiles et construction des trois niveaux |
| `Theme.java` | Couleurs et tuiles de chaque zone |
| `Enemy.java`, `Boss.java`, `Bomb.java` | Les robots et le Dr. Vortex |
| `Ring.java`, `Spring.java`, `Checkpoint.java`, `Goal.java` | Objets du niveau |
| `Particle.java`, `Animal.java` | Effets visuels |
| `Sound.java` | Bruitages synthétisés |

### Créer ton propre niveau

Dans `Levels.java`, chaque niveau est construit avec de petites instructions :

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
