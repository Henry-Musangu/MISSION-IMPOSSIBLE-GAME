# Protocole Clarisse & John

Application Android du protocole du mariage de Clarisse & John (12 et 13 novembre 2026, Kinshasa).

## Installer l'application

Depuis le téléphone Android, ouvrez ce lien :

**https://github.com/Henry-Musangu/MISSION-IMPOSSIBLE-GAME/releases/download/protocole/protocole-clarisse-john.apk**

Ouvrez ensuite le fichier téléchargé. Android demande d'autoriser l'installation depuis cette
source (le navigateur ou WhatsApp) : acceptez. L'icône « Protocole C&J » apparaît.

Pour mettre à jour, retéléchargez avec le même lien et installez par-dessus : les données sont conservées.

## Ce que fait l'application

- **Invités & tables** : recherche par nom ou par table, arrivées en direct, remplissage des tables.
- **Départements** : chaque département a son chef (et un adjoint), son équipe, sa check-list,
  ses outils (stock des boissons, service par table) et ses alertes. L'administrateur nomme les chefs,
  compose les équipes et peut créer de nouveaux départements.
- **Localiser** : chaque protocole peut partager sa position (GPS activé). Les autres le voient sur
  une carte et se font guider jusqu'à lui : flèche-boussole, distance, temps de marche, et
  itinéraire Google Maps à pied ou en voiture. On peut aussi **demander** sa position à un
  protocole : il reçoit un bandeau dans l'application.
- **Signalements** avec photo et position, **appel** (ponctualité et tenue), **photos** partagées,
  **déroulé** des deux jours, **tenue & charte**, **annonces** de l'administrateur.
- **Thèmes** : Noir velours, Blanc ivoire, Rouge bordeaux, Mixte, Uni sobre, Champagne, Automatique.
- **Hors ligne** : sans réseau, l'application affiche les dernières informations reçues et envoie
  vos actions dès le retour d'Internet.

## Connecter l'équipe par Internet (une seule fois, par l'administrateur)

La synchronisation utilise **Firebase** (Google), gratuit pour un événement de cette taille.

1. Ouvrez https://console.firebase.google.com, cliquez **Créer un projet** (par exemple « protocole-cj »).
   Google Analytics n'est pas nécessaire.
2. Menu **Créer > Authentication > Commencer**, onglet **Mode de connexion** : activez **Anonyme**.
3. Menu **Créer > Realtime Database > Créer une base de données** : emplacement **Belgique (europe-west1)**,
   démarrez en **mode verrouillé**. Onglet **Règles** : remplacez tout par le contenu du fichier
   [`firebase/database.rules.json`](firebase/database.rules.json), puis **Publier**.
4. Roue dentée > **Paramètres du projet** > **Vos applications** > icône **Web `</>`** : donnez un nom,
   cliquez **Enregistrer**, puis copiez le bloc `const firebaseConfig = { ... }`.
5. Dans l'application : **Réglages > Créer l'équipe (administrateur)**. Collez le bloc, choisissez un
   **mot de passe administrateur**, puis **Créer et connecter**.
6. L'application affiche le **code de connexion** de l'équipe. Envoyez-le sur WhatsApp aux protocoles :
   chacun ouvre **Réglages > J'ai un code de connexion** et le colle une fois.

Un co-administrateur déverrouille l'administration depuis l'onglet **Administration** avec le même mot de passe.

## Développement

```bash
npm ci
npm run build                 # construit www/
bash scripts/prepare-android.sh   # crée le projet Android (Capacitor)
cd android && ./gradlew assembleDebug
```

L'APK est compilé et publié automatiquement par GitHub Actions
(`.github/workflows/protocole-apk.yml`) à chaque modification de ce dossier.
`keystore/debug.keystore` est la clé de signature fixe de l'APK, pour que les mises à jour
s'installent par-dessus la version précédente.
