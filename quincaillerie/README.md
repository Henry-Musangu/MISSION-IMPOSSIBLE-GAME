# Ma Quincaillerie

Application Android pour la gestion quotidienne d'une quincaillerie : ventes, stock, prix et
marges, caisse, dépenses, dettes des clients et bilan. Tout est en français, fonctionne sans
Internet, et **tous les chiffres sont fixés par le propriétaire** (fonds injecté, prix d'achat,
marge, prix de vente, seuils d'alerte, monnaie).

## Installer sur le téléphone

Depuis le téléphone, ouvrir ce lien puis le fichier téléchargé :

**https://github.com/Henry-Musangu/MISSION-IMPOSSIBLE-GAME/releases/latest/download/ma-quincaillerie.apk**

Android demande d'autoriser l'installation depuis le navigateur : accepter. Pour mettre à jour,
retélécharger avec le même lien et installer par-dessus : les données sont conservées
(l'APK est toujours signé avec la même clé, `debug.keystore`).

## Les cinq onglets

| Onglet | À quoi il sert |
| --- | --- |
| **Accueil** | Ventes et bénéfice du jour, caisse, valeur du stock, dettes clients, produits à réapprovisionner, ventes des 7 derniers jours, « où est votre argent » |
| **Vendre** | Toucher un produit pour l'ajouter, changer la quantité ou le prix (remise), puis **Encaisser** ou **Vente à crédit** |
| **Stock** | Liste des produits avec prix d'achat, prix de vente, marge et stock ; ajouter, modifier, réapprovisionner |
| **Bilan** | Ventes, bénéfice sur ventes, dépenses et bénéfice net (aujourd'hui, 7 jours, ce mois, tout) ; produits les plus rentables ; journal de toutes les opérations (annulation possible) |
| **Gérer** | Fonds injecté et retraits, dépenses, dettes clients, nom/emplacement/monnaie, catégories et marges, couleur (bleu, rose, vert, gris), rapport du jour à envoyer, sauvegarde et restauration |

### Comment sont faits les calculs

- **Prix de vente = prix d'achat × (1 + marge %)**. On peut écrire la marge (le prix se calcule)
  ou écrire le prix (la marge se calcule).
- **Caisse** = fonds injectés − retraits + ventes payées − achats de stock − dépenses.
- **Bénéfice sur ventes** = (prix de vente − prix d'achat) × quantité, pour chaque article vendu.
- **Bénéfice net** = bénéfice sur ventes − dépenses de la période.
- **Valeur de la boutique** = caisse + stock (au prix d'achat) + dettes des clients.

Au premier lancement, l'application contient une **démonstration**. Le bouton
**« Démarrer avec mes chiffres »** l'efface : on indique son propre fonds, sa monnaie et sa marge,
et on peut garder la liste des produits (stocks à zéro) pour ne pas tout retaper.

## Analyse de démarrage proposée (chiffres de la démonstration)

Ces chiffres sont une **hypothèse de travail en dollars**, pour une petite quincaillerie de
quartier. Les prix réels dépendent de la ville et des grossistes : à vérifier et à corriger dans
l'application avant de s'y fier.

### Emplacement

Une avenue passante dans un **quartier où l'on construit** (parcelles en chantier, maisons en
finition), près d'un arrêt de transport ou d'un marché. Les clients y sont des maçons, plombiers
et électriciens qui achètent tous les jours en petites quantités. Éviter de s'installer juste à
côté d'un grand dépôt de ciment : on ne peut pas le battre sur le prix.

### Répartition du fonds de départ : 1 500 $

| Poste | Montant | Part |
| --- | ---: | ---: |
| Stock de départ (30 produits) | 1 050 $ | 70 % |
| Loyer : 1er mois + 2 mois de garantie (50 $/mois) | 150 $ | 10 % |
| Aménagement : étagères, comptoir, enseigne | 130 $ | 9 % |
| Taxes, patente, documents | 50 $ | 3 % |
| Réserve de caisse | 120 $ | 8 % |

### Produits, prix et marges

Avec 1 500 $, on ne peut pas vivre du ciment et du fer : ils immobilisent beaucoup d'argent pour
8 à 15 % de marge. Ils servent de **produits d'appel** (le client vient pour eux), et le bénéfice
se fait sur les **petits articles** achetés en même temps.

| Catégorie | Marge sur le prix d'achat | Exemples (achat → vente) |
| --- | ---: | --- |
| Gros œuvre | 9 à 11 % (fil à ligaturer 33 %) | Ciment 50 kg 9,00 → 9,80 $ · Fer Ø10 6,50 → 7,20 $ |
| Toiture | environ 15 % | Tôle ondulée 5,50 → 6,30 $ |
| Clouterie et fixation | 33 à 47 % | Clous 10 cm 1,20 → 1,60 $/kg · Vis (boîte) 1,50 → 2,20 $ |
| Plomberie | 33 à 100 % | Robinet ½" 2,50 → 3,80 $ · Téflon 0,15 → 0,30 $ |
| Électricité | 23 à 71 % | Câble 2,5 mm² 22 → 27 $ · Ampoule LED 0,70 → 1,20 $ |
| Peinture | 27 à 100 % | Peinture à eau 4 L 7,50 → 9,50 $ · Pinceau 0,60 → 1,00 $ |
| Outillage | 21 à 75 % | Marteau 2,50 → 3,80 $ · Brouette 28 → 34 $ |

### Rentabilité

- Charges fixes du mois : loyer 50 $ + transport 30 $ + taxes 15 $ + électricité et téléphone
  15 $ = **110 $** (boutique tenue par le propriétaire, sans salarié).
- Dans la démonstration, la semaine fait **625,80 $ de ventes pour 101,35 $ de bénéfice**, soit
  16 % des ventes : le ciment, le fer et la tôle pèsent lourd et tirent la marge vers le bas.
- **Seuil de rentabilité** : 110 $ ÷ 16 % ≈ 680 $ de ventes par mois, soit environ **26 $ par
  jour** sur 26 jours.
- À 60 $ de ventes par jour (1 560 $/mois) : environ 250 $ de bénéfice sur ventes, donc
  **140 $ net par mois**. Monter la part des petits articles à forte marge est le moyen le plus
  rapide d'améliorer ce chiffre.

### Règles de gestion conseillées

1. Noter **chaque** vente et **chaque** dépense le jour même.
2. Ne pas prendre l'argent de la caisse pour soi sans le noter comme **retrait**.
3. Réinvestir le bénéfice dans le stock pendant les 6 premiers mois.
4. Limiter le crédit à des clients connus, avec un plafond (par exemple 50 $ par client).
5. Réapprovisionner dès qu'un produit passe en « stock bas », sans attendre la rupture.
6. Faire une **sauvegarde** chaque semaine (onglet Gérer).

## Pour les développeurs

Toute l'interface est une seule page, `src/main/assets/index.html` (HTML, CSS et JavaScript, sans
bibliothèque), affichée dans une `WebView` par `MainActivity.java`. Les données sont un fichier
JSON privé de l'application (`donnees.json`, avec une copie `.bak`). La page s'ouvre aussi dans
un navigateur d'ordinateur pour la tester : elle utilise alors le stockage du navigateur.

Compiler : `./gradlew :quincaillerie:assembleDebug`
(APK dans `quincaillerie/build/outputs/apk/debug/`).
