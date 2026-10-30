# NEW YORK LIFE — Open World

Jeu 3D en monde ouvert pour Android, situé dans une représentation originale de Midtown Manhattan.
Moteur : **Godot 4.5** (licence MIT, gratuit, sans compte ni licence à activer), rendu *Compatibility*
(OpenGL ES 3) pour fonctionner sur un maximum de téléphones.

## Installer sur le téléphone

Depuis le téléphone, ouvre :

**https://github.com/Henry-Musangu/MISSION-IMPOSSIBLE-GAME/releases/download/newyorklife/new-york-life.apk**

puis ouvre le fichier et autorise l'installation depuis le navigateur. L'APK est reconstruit, testé
et republié automatiquement à chaque modification (GitHub Actions → « New York Life (Android) »).

## Contenu de la version 0.1 (prototype jouable)

- **Ville** : secteur de Midtown (5 avenues × 7 rues, 24 blocs), immeubles générés (retraits new-yorkais,
  châteaux d'eau, tours vitrées), Times Square et ses écrans animés, parc avec fontaine, trottoirs,
  lampadaires, arbres, passages piétons, feux tricolores fonctionnels, bande d'immeubles périphérique.
- **Personnage** : marche, course, sprint, saut, marches de trottoir, chutes, animations procédurales.
- **Commandes tactiles** : joystick flottant, caméra au doigt, boutons contextuels (à pied / en voiture),
  taille et opacité réglables. Clavier, souris et manette aussi pris en charge.
- **Véhicules** : berline, taxi, SUV, sportive, limousine, utilitaire, camion, bus, police — physique
  à suspensions, accélération/freinage/marche arrière/frein à main, dégâts visuels et fumée, phares,
  feux stop, carburant, sons moteur et klaxon.
- **Circulation** : véhicules IA qui suivent les voies, respectent les feux, tournent aux intersections,
  freinent derrière les obstacles et klaxonnent si on les bloque.
- **Habitants** : marchent, s'arrêtent, discutent, entrent dans les commerces et en ressortent,
  se promènent dans le parc, fuient le danger, peuvent être renversés.
- **Police** : 5 niveaux de recherche (patrouilles, poursuites, renforts, barrages), arrestation, fuite.
- **Missions** : chapitre 1 de l'histoire (5 missions avec un choix et ses conséquences), course contre
  la montre, emploi de livreur répétable.
- **Économie** : portefeuille, transactions, concession automobile, garage (réparation, peinture,
  stockage), boutique de vêtements, location d'appartement (loyer), métro, station-service, hôpital.
- **Monde vivant** : cycle jour/nuit (24 min), éclairage public et fenêtres la nuit, météo (soleil,
  nuages, pluie, orages, brouillard) qui influence la visibilité et l'adhérence.
- **Interface** : écran d'accueil, HUD, mini-carte, carte complète, menu pause, paramètres, FR/EN.
- **Sauvegarde** locale automatique et manuelle, copie de secours, somme de contrôle, migration.
- **Audio** 100 % synthétisé (moteur, sirène, ambiance, musique originale), volumes séparés.
- **Optimisation** : géométrie fusionnée par bloc, chargement/déchargement des blocs selon la distance,
  pools de véhicules et d'habitants, distance de rendu, 3 profils graphiques BAS / MOYEN / HAUT.

## Structure

```
newyorklife/
  project.godot, export_presets.cfg
  scenes/main.tscn
  scripts/
    core/        main, événements, état de jeu, sauvegarde, paramètres, entrées, langues
    world/       plan de la ville, génération 3D, matériaux, feux, jour/nuit/météo
    player/      personnage et squelette animé
    camera/      caméra troisième personne
    vehicles/    caractéristiques, modèles 3D, physique
    traffic/     IA de circulation
    pedestrians/ IA des habitants
    police/      niveaux de recherche et IA de poursuite
    missions/    données des missions et moteur de missions
    economy/     commerces et services
    audio/       synthèse sonore et musique
    ui/          HUD, commandes tactiles, carte, dialogues, menus
  tests/         tests automatisés
```

Ajouter une mission : une entrée dans `scripts/missions/mission_defs.gd` (étapes `dialog`, `goto`,
`spawn_vehicle`, `enter_vehicle`, `timer`, `pickup`, `dropoff`, `checkpoints`, `choice`), sans toucher
aux autres systèmes.

## Tests

La CI exécute à chaque modification :
1. les tests automatisés (`tests/test_runner.tscn`) : plan de la ville, feux, économie, sauvegarde,
   langues, données de missions, physique des véhicules, déplacement, entrée/sortie de véhicule ;
2. une partie complète automatisée avec rendu OpenGL (marche, saut, conduite, freinage, mission,
   sauvegarde/chargement, police) et des captures d'écran publiées avec l'APK ;
3. l'export de l'APK signé.

Les performances réelles doivent être mesurées sur un téléphone (option « Afficher les FPS »).

## Limites connues (prochaines étapes)

- Un seul secteur de Manhattan ; les autres arrondissements viendront par extension du plan.
- Personnages et véhicules en géométrie simple générée par le code (pas encore de modèles Blender).
- Pas encore d'intérieurs visitables, de motos ni de métro souterrain (le métro est un trajet rapide).
- Clé de signature de test (`tools/android/nyl-test.keystore`) : à remplacer par une clé privée
  avant toute publication sur Google Play.
