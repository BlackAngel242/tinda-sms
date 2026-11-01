# Audit TINDA SMS : Fixit 2.1 + critique UX (analyse statique)

**Date :** 1er novembre 2026  
**Révision examinée :** `master` à `473292b636c6e1b37fd426cf2030d74a1f834524` (29 octobre 2026).  
**Méthode :** module UX de Fixit 2.1, principes de [design-critique d'Anthropic](https://github.com/anthropics/knowledge-work-plugins/tree/main/design/skills/design-critique), relecture des choix du PRD et du protocole UI/UX.  
**Périmètre :** code Kotlin/Jetpack Compose, thème Material 3, maquette HTML et documentation de validation. Pas d'APK exécuté ni de capture Android de cette version obtenue pendant cet audit. **Vérification visuelle non effectuée** sur le build natif. Aucun test Android, TalkBack, vidéo ou contraste rendu n'a été exécuté ici. Pas de code modifié.

## Distinction des sources

- **Produit actuel** : `master` et composants `app/src/main/java/tech/dmampro/tinda/{navigation,ui,theme}`. Les PR [nº 8](https://github.com/BlackAngel242/tinda-sms/pull/8) et [nº 9](https://github.com/BlackAngel242/tinda-sms/pull/9) sont ouvertes au jour de la revue et ne constituent pas le produit fusionné.
- **Référence de démonstration** : `design/preview/Tinda-SMS.html` et `design/prototype/`, basés sur `dfddee7` selon `design/manifest.json` : données fictives, SMS et permissions simulés, non équivalent à l'APK.
- **Intention utilisateur** : `docs/validation/protocole-ui-ux.md` du 21 octobre déclare la maquette Stitch et **quatre onglets (Messages, Protection, Archivés, Paramètres)** scellés ; `docs/design/INTEGRATION-ANDROID.md` du 23 octobre retient **trois destinations (Messages, Automatisations, Réglages)** ; `TindaBottomBar` et `TindaNavGraph` codent ces trois destinations. C'est un conflit de décisions, pas une permission implicite de redessiner.

## Première impression, première tâche

**Force documentée :** un vocabulaire de couleurs limité, fond ardoise `#0B0F14`, surfaces `#121820`/`#1A222D`/`#242E3D`, accent `#00D2B4`, textes `#F3F5F7` et `#94A3B8`. Le fond et les composants Material 3 suggèrent une interface ordonnée et compatible avec les sept thèmes. La confirmation perceptive « en deux secondes » **reste à observer** sur capture native.

**Hypothèse à mesurer :** l'en-tête, la recherche, les filtres SIM/OTP, les conversations et le bouton de rédaction se disputent l'espace sur un appareil compact. On ne conclut ni à un chevauchement ni à une surcharge visuelle sans capture au même état.

## Parcours, preuves et améliorations

| ID | Priorité | Étape/écran | Constat et preuve | Action la plus utile | Validation attendue |
|---|---|---|---|---|---|
| D01 | IMPORTANT, arbitrage | Navigation globale | `docs/validation/protocole-ui-ux.md` fige quatre onglets ; `TindaComponents.kt` et `TindaNavGraph.kt` en codent trois ; `INTEGRATION-ANDROID.md` les recommande. Incohérence des références. | Réconcilier explicitement les décisions scellées et la réalité fonctionnelle ; ne pas créer un onglet « Protection » qui suggère un bouclier anti-arnaque fonctionnel avant F15. Préférer arbitrage UX avec parcours des deux variantes. | Décision signée dans le PRD et parcours testés, pas seulement une quatrième icône |
| D02 | IMPORTANT | Accueil / bouton de rédaction | `ConversationListScreen.kt` : FAB de 56 dp ouvre le coffre par appui long, sans entrée explicite sur ce composant. Découvrabilité faible pour les personnes non informées ; discrétion voulue à préserver. | Ajouter un chemin **discret mais découvrable après authentification** dans Réglages ou aide contextuelle, sans rendre visibles les SMS privés. Documenter le geste dans l'aide protégée. | Personne novice réussit l'accès autorisé sans consigne orale ; aucune donnée privée en clair |
| D03 | IMPORTANT, déjà ciblé PR 8 | Nouveau message | Sur `master`, `NewConversationScreen.kt` propose champ destinataire et picker natif ; saisie avec suggestions de contacts et numéros multiples améliorée dans PR 8 encore ouverte. | Examiner/fusionner PR 8 après tests, sans reproduire la recherche ailleurs ; choix explicite quand le contact possède plusieurs numéros. | Recherche par nom sur Redmi, permission refusée, contact multi-numéros, erreur et sélection correctes |
| D04 | IMPORTANT, déjà ciblé PR 8/9 | Fil / accusés | Sur `master`, `ConversationDetailScreen.kt` affiche `StatusIcon` à côté de l'heure. PR 8 apporte les statuts en mots et horaire prouvé, PR 9 le délai « Livraison non confirmée ». | Prioriser le texte court distinct de l'icône ; ne jamais écrire « Lu » pour SMS classique ou déduire « Livré » sans accusé. | En cours, envoyé, livré, échec, non confirmé, accusé tardif et redémarrage sur appareil |
| D05 | IMPORTANT | Envoi groupé | `GroupSendScreen.kt` permet d'ajouter des contacts, choisir une SIM et envoyer, mais l'alerte de coût `segments × destinataires` prévue RB-06/F14 n'est pas présente dans la matrice du 21 oct. | Placer le nombre total de SMS/segments et une confirmation explicite **juste avant** l'envoi, avec estimations non trompeuses, sans tarif inventé. | SMS long, accents Unicode, plusieurs destinataires, aucune facturation sans confirmation consciente |
| D06 | IMPORTANT (confidentialité) | Carte OTP | `ConversationDetailScreen.kt` / `OtpCard` affichent le code en clair en 24 sp et le corps SMS ; bouton Copier. Très commode, mais exposition au regard ou capture d'écran. | Étudier masquage par défaut du code (révéler au toucher, bouton Copier optionnel) et contrôler notifications et captures. Décision produit/sécurité préalable ; **ne pas modifier silencieusement**. | Capture écran verrouillé, notifications, bascule appli, TalkBack, absence de fuite dans listes |
| D07 | IMPORTANT | Petits écrans / automatisations | PR 9 documente un formulaire de répondeur comprimé, jours tronqués, un bouton Copier masqué par le FAB, styles de « + » différents. **Correctifs proposés sur branche non fusionnée**, non présents sur master. | Revoir PR 9 après PR 8 ; évaluer à largeur compacte et échelle de police 200 %, y compris la dernière carte entièrement visible. | Captures avant/après même appareil, clavier, mode clair/sombre, TalkBack |
| D08 | IMPORTANT, arbitrage produit | Parcours Automatisations et Réglages | L'accès aux automatismes existe dans onglet dédié et par les paramètres : risques de doublons de menus, pas d'échec démontré. | Un seul point d'entrée principal par tâche, liens secondaires explicites seulement si utiles ; test novice/expert. Conserver les modèles métiers et services. | L'utilisateur trouve la programmation, le répondeur et le transfert en moins de détours |
| D09 | OPTIONNEL, vérification nécessaire | Titrage / densité | `NewConversationScreen.kt` contient une accroche en 11 sp ; `AutomationsScreen.kt` utilise 12 sp pour un surtitre. Les petites tailles peuvent devenir difficiles à lire. | Tester 200 % de police, petit écran et textes français longs ; remplacer l'accroche par un rôle typographique accessible si nécessaire. | Texte intégral lisible, pas de coupure/chevauchement, TalkBack |
| D10 | IMPORTANT, à contrôler | Sept thèmes et Material You | `TindaThemes.kt` définit des rôles clair/sombre ; des parties de l'UI ont été modernisées vers `MaterialTheme.colorScheme`. Les anciennes notes sur des couleurs codées en dur sont datées et ne prouvent pas un défaut actuel. | Audit d'inventaire de tous les usages de couleurs, contrastes effectifs, états focus/désactivé, cartes OTP, surfaces, dialogues et navigation dans 7 thèmes. Pas de remplacement automatique de la palette. | Sept thèmes × clair/sombre × plusieurs écrans, contraste mesuré sur composants réels |
| D11 | OPTIONNEL | Hiérarchie des écrans | Les grands titres et le FAB primaire sont une bonne base ; la séparation entre messages, outils avancés et statistiques dépend de la taille d'écran. Aucune collision native prouvée ici. | Garder la rédaction prioritaire, éviter que cartes/statistiques/OTP secondaires occultent un message ; ajuster seulement sur capture vérifiée. | Test première impression (2 s) puis recherche, lecture et réponse, utilisateurs réels |
| D12 | IMPORTANT (prévention de confiance trompeuse) | Réglages / sécurité | PR 9 documente les corrections de trois promesses trompeuses (« Chiffré & stocké sur l'appareil », taux de « Délivrance GSM » et moteur « 100 % hors-ligne »). | Appliquer la terminologie honnête après revue de PR 9 ; séparer coffre chiffré, messages locaux et taux d'accusés réellement reçus. | Les libellés correspondent au stockage et aux compteurs réels |
| D13 | IMPORTANT, non résolu | Traçabilité et cycle de livraison | PR 8 base master, PR 9 base PR 8 ; aucune des deux n'est fusionnée. Le verdict de la maquette `design-qa.md` ne vaut pas validation Android. | Garder le pipeline tests JVM/Room/CI, inspection native, UX, contre-revue Fixit avant merge. | Capture+SHA de l'APK et tests Redmi Note 14 selon matrice mise à jour |

## Contrôles graphiques proposés

1. **Parcours A, première ouverture** : permission et rôle SMS, refus, retour, état vide. Photographier écran natif (données de démonstration).
2. **Parcours B, SMS du quotidien** : liste, recherche, destinataire, SIM, saisie (clavier ouvert), envoi, états et retour.
3. **Parcours C, commerçant** : SMS longs/groupés, noms de SIM longs, coût avant confirmation, accusés incertains.
4. **Parcours D, automatisations** : configuration d'un répondeur et d'un transfert, jours et délais, validation/erreurs avec grande police.
5. **Parcours E, confidentialité** : OTP, coffre, verrouillage, notifications sans exposition ; ne jamais capturer de vrais messages.
6. Pour chaque parcours : petit écran et Redmi Note 14, thème sombre et clair, polices normale et 200 %, clavier ouvert, TalkBack ; garder le commit, le build et la capture attachés.

## Séquence de remédiation sans perdre l'identité

**Lot 0, décision :** résoudre D01 (trois vs quatre onglets) et D06 (OTP masqué) au niveau PRD, sans changement de code.  
**Lot 1, fiabilité perçue :** traiter D03 et D04 via revue puis intégration de PR 8 et PR 9, en respectant leur dépendance ; conserver les statuts métier exacts.  
**Lot 2, simplicité et coût :** D02, D05 et D08 avec parcours utilisateur et tests.  
**Lot 3, finition :** D07, D09, D10 et D11 seulement après captures et mesures Android.  
**Lot 4, sécurité / microcopy :** D12 et recette finale sans promesses exagérées.

## Verdict Fixit

**À CORRIGER pour validation du produit natif** : incohérence de navigation, frictions ou risques identifiés en code et contrôles Android critiques encore manquants. **Vérification visuelle non effectuée** au cours de cet audit faute de capture fraîche du build Android. L'existence d'une maquette HTML testée antérieurement ne permet pas de délivrer le verdict « VALIDÉ » pour l'application.

Aucune modification de l'identité Radiant Slate, du code Android, des assets, des SMS, des permissions ou des comportements sensibles. Il s'agit d'un rapport de recommandations et de preuves à réunir.
