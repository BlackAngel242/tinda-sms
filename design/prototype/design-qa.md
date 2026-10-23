# Product Design QA et Fixit : Tinda SMS

final result: passed

Périmètre de ce verdict : prototype de démonstration, pas application Android de production. Pas de certification.

## Sources et preuves

- Vérité visuelle : `/workspace/scratch/a477a6535d3a/generated_images/exec-8bf87743-2b1d-4027-8a5a-8415d587d2f1.png` (proposition 2), 853 × 1844 pixels, composition logique environ 390 × 844.
- Modification explicitement demandée ensuite : calmer les couleurs de l'accueil. Avatars uniformes et badges SIM neutres sont donc des écarts intentionnels à la maquette initiale.
- Capture navigateur finale : `/workspace/scratch/a477a6535d3a/qa-home-full-v2.jpg`, 1363 × 936 pixels.
- Capture écran normalisée pour analyse : `/workspace/scratch/a477a6535d3a/qa-home-screen-final.jpg`.
- Comparaison combinée : `/workspace/scratch/a477a6535d3a/qa-comparison-final.jpg` ; source et implémentation ouvertes ensemble.
- Appareil : Pixel 10, écran CSS 427 × 952 ; surface applicative fermée au clavier : 427 × 904 ; chrome de navigation Android conservé.
- Navigateur : 1363 × 936, DPR 1 ; cadre réduit automatiquement à environ 0,869, écran observé environ 371 × 827 pixels. Capture remontée à 427 × 952 pour l'analyse ; chrome OS retiré du panneau de comparaison. Aucune déformation indépendante de largeur/hauteur du contenu applicatif.
- Limite : il s'agit d'une comparaison visuelle normalisée ; la capture n'est pas un rendu matériel au pixel près à échelle native. Les hauteurs source sans OS et écran Android avec OS diffèrent. Aucun écart dû uniquement au chrome n'est compté comme défaut.
- États supplémentaires observés : fil et clavier, automatisations, formulaire de transfert, apparence claire, coffre et diagnostic.

## Historique des corrections

1. P1 : les deux premiers messages étaient derrière l'en-tête. Cause : règle générique `inset: 0` du conteneur mobile plus tard dans la cascade. Correction des sélecteurs applicatifs ; premier fil et titre de section de nouveau visibles.
2. P1 : un appui sur Envoyer pouvait seulement fermer le clavier. Cause : perte de focus avant le clic, avec déplacement du bouton. Les boutons ne volent plus le focus au pointerdown ; leurs actions ferment explicitement le clavier. Rejeu : envoi fictif au premier clic et nouvelle bulle.
3. P1 : le cadre entier se déplaçait après focus d'un champ. Preuve DOM : `device-screen.scrollTop = 317`. Le cadre utilise désormais le clipping ; les listes gardent leur défilement dédié. Rejeu : `scrollTop = 0`, clavier et navigation dans leur zone.
4. P2 : le bouton de rédaction masquait l'horaire du dernier message. Mesure puis réduction des espaces de liste et déplacement du bouton dans l'espace libre. Preuve finale : dernier fil bas 710,229 px ; bouton haut 710,419 px. Aucun contenu masqué dans l'état initial Pixel.
5. IMPORTANT Fixit : trop de couleurs concurrentes à l'accueil. Correction demandée par DrSmoke : avatars et badges neutres ; turquoise réservé à sélection/actions/non-lus.
6. P2 : contraste des accents en mode clair. Palettes d'accent plus foncées et texte blanc sur boutons pleins ; panneau modal assorti au thème clair.

## Surfaces de fidélité

- Typographie : Roboto embarquée, graisses normales et grasses ; titre 38 px ; noms 17 px, aperçus 15 px ; lecture agrandissable. La famille approche le sans-serif de la référence sans revendiquer son identification exacte.
- Espacement : filtre SIM, onglets, groupes par date, liste unifiée et navigation basse conservés. Six fils visibles sur Pixel. Le cadre plus petit iPhone garde le défilement pour la fin de liste.
- Couleurs : ardoise #0B0F14, surfaces #121820/#1A222D, texte #F3F5F7/#94A3B8, turquoise #00D2B4. SIM numérotées et neutres à l'accueil conformément au dernier retour.
- Assets : aucun visuel décoratif à produire dans la source. Avatars à initiales = composants UI natifs, pas faux portraits. Icônes de bibliothèque Lucide ; aucun asset YAATA. Cadre, barres système et clavier fournis par le runtime conservés ; contrôle d'intégrité de 28 fichiers réussi.
- Texte : français, libellés concrets, faux contacts et faux messages. Mentions de simulation sur coffre, diagnostic et envoi ; bouclier marqué prévu. Date ancrée au 23 octobre 2026.
- Revue focalisée : les badges SIM, heures, non-lus et dernière ligne ont été mesurés dans le DOM et lus sur la capture normalisée. Comparaison plein écran suffisante pour la hiérarchie ; les champs et bulles ont aussi été inspectés sur captures dédiées.

## Interactions vérifiées dans le navigateur

- Filtrage SIM 2 : uniquement conversations SIM 2.
- Recherche Aline : résultat correspondant ; ouverture du bon fil.
- Rédaction puis envoi fictif : bulle ajoutée, brouillon vidé et confirmation.
- Programmation depuis le compositeur : nouvelle entrée visible dans la liste.
- Transfert : expéditeur et destinataire identiques refusés ; règle valide enregistrée.
- Favori ajouté depuis le fil puis retrouvé dans le filtre Favoris.
- Envoi groupé à deux contacts : confirmation de deux SMS individuels fictifs.
- Apparence claire et taille Grand : classes et rendu modifiés.
- Appui long de 900 ms sur rédaction : accès au coffre ; code fictif : ouverture et avertissement de simulation.
- Options avancées par biométrie simulée ; diagnostic accessible et explicitement fictif.
- Version HTML autonome servie séparément : accueil et envoi fictif vérifiés.
- Console consultée : aucune erreur applicative observée ; messages de l'extension navigateur exclus et signalés comme extérieurs au prototype.
- TypeScript/Vite : build réussi. Runtime mobile : intégrité réussie.

## Fixit : double lecture

Expert : hiérarchie stable, couleurs contrôlées, pas d'asset parasite, contrastes et alignements corrigés, bouton de rédaction dégagé.
Utilisateur novice : tâche principale immédiatement visible ; menus nommés ; aucun effet réel sur le téléphone ; parcours sensibles expliqués.
Verdict : VALIDÉ SOUS RÉSERVE pour la démonstration. Réserve : test de prise en main sur le téléphone de DrSmoke et tests terrain Android avant intégration. Pas de défaut P0/P1/P2 restant observé dans les parcours contrôlés.

## Suites et limites

- P3 : localisation anglaise partielle dans le prototype.
- Gestes : configuration interactive et actions dans les menus ; balayage natif Android à vérifier dans l'application réelle.
- Coffre, biométrie, SIM, autorisations, envoi et workers : simulations sans connexion au système SMS.
- État en mémoire de session uniquement, perdu au rechargement.
- Sons à activer par geste utilisateur ; copie du code soumise au support du navigateur.
- Parcours secondaires non vérifiés exhaustivement, notamment toutes les combinaisons de règles, les modes système/dynamique sur chaque appareil et la version de fichier local Android.

Checklist de livraison : comparaison après corrections faite ; parcours principaux testés ; prototype ouvert sur l'accueil ; dépôt GitHub inchangé ; HTML autonome et sources à conserver.


## Vérification de l'extension

- Build TypeScript/Vite réussi ; intégrité des 28 fichiers protégés du runtime vérifiée.
- 4 tests de logique passent : contact ET mot-clé, exclusions/dédoublonnage, variables manquantes, statuts distincts.
- Navigateur : modification de liste ; personnalisation de trois SMS ; statuts par destinataire ; reprise limitée à l'échec.
- Lecture privée : phrase incorrecte refusée, phrase correcte révélant le texte, fermeture entraînant la suppression de l'exemple ; état et journal vérifiés.
- Revue visuelle de l'écran de règles : palette sombre/turquoise cohérente, champs et résultat lisibles, défilement interne conservé.
- Corrections complémentaires : ordre des listes préservé à la modification ; relance conservant texte personnalisé et SIM d'origine ; horodatage des diffusions à Brazzaville.
- Limites de validation : pas de terminal Android, réseau opérateur, API Meta ni vraie écriture Contacts. Import fichier et délais réels de 30 s/5 min non validés sur appareil ; contrôles présents dans la maquette.
