# Tinda SMS : décisions du prototype

Direction validée : proposition 2, Radiant Slate. Dépôt source en lecture seule : BlackAngel242/tinda-sms, branche master. Aucune branche, aucun commit et aucune PR créés.

## Cadrage PRD Table Ronde

Périmètre STANDARD : prototype isolé avec données fictives et plusieurs parcours. Le niveau de risque de l'application réelle est RENFORCÉ, compte tenu du contenu SMS et du coffre ; ce document ne vaut pas validation de sa sécurité.

Objectif : découvrir les fonctions et tester leurs interactions avant une adaptation Kotlin/Compose. Hors périmètre : lecture/envoi SMS, authentification réelle, chiffrement, workers Android, réseau, MMS et mesures terrain.

| Regard | Arbitrage apporté |
|---|---|
| Porteur produit, également décideur | Conserver la proposition 2 ; ne pas modifier le dépôt |
| Utilisateur novice | Libellés explicites ; réglages regroupés |
| Utilisateur pressé | Rédaction au pouce ; accès rapide aux filtres et SIM |
| UX | Trois entrées stables : Messages, Automatisations, Réglages |
| Graphisme | Ardoise, turquoise, rythme de liste et icônes cohérents |
| Accessibilité | Texte agrandissable ; SIM identifiables par numéro et couleur |
| Android/Compose | Respect du périmètre SMS et des routes du dépôt |
| Sécurité/vie privée | Coffre et biométrie explicitement simulés ; aucun accès aux SMS |
| QA fonctionnelle | Vérifier parcours complet, validation et retour arrière |
| Support | États vides explicites et diagnostic démonstratif |
| Maintenance | Prototype séparé ; ne pas ajouter de dépendance au dépôt Android |

Ce tableau correspond à des perspectives d'analyse appliquées par l'assistant, pas à des entretiens réalisés ni à des agents indépendants.

## Tensions et décisions

| ID | Tension | Décision | Statut / révision |
|---|---|---|---|
| UI-01 | Richesse / simplicité | Trois onglets ; outils et réglages sur des écrans dédiés | CONFIRMÉ par le périmètre ; à tester avec utilisateurs |
| UI-02 | Discrétion / découverte | Coffre par appui long sur rédaction ; indice dans le menu | CONFIRMÉ par le code/brief ; test de découverte nécessaire |
| UI-03 | Personnalisation / cohérence | Palettes du dépôt ; taille, densité et bulles en options | HYPOTHÈSE de design ; revenir en arrière si surcharge |
| UI-04 | Démonstration / sécurité réelle | Aucun vrai SMS ou chiffrement ; avertissement aux parcours sensibles | CONFIRMÉ ; invariant du prototype |
| UI-05 | Inspiration YAATA / originalité | Listes par date, actions contextuelles et confort de lecture ; conserver identité Tinda | CONFIRMÉ par demande utilisateur ; aucun asset/code YAATA |
| UI-06 | Fonctions futures / promesse | Bouclier identifié comme prévu ; aucun moteur simulé présenté comme actif | CONFIRMÉ par feuille de route |

Approver : DrSmoke, au retour de revue du prototype, avant intégration Android. Driver : assistant, périmètre visuel uniquement.

## Red Team et gate

- Ne pas construire une seconde application SMS de production : le prototype sert à vérifier l'interface.
- Hypothèse qui invaliderait l'organisation : impossibilité de trouver rédaction, choix SIM ou automatismes sans aide. Tester à faible coût avant intégration.
- Feature prématurée : moteur anti-arnaque non alimenté par corpus. Conserver uniquement l'écran d'attente.
- Gate : GO pour prototype fictif ; aucune conclusion de GO pour mise en production.
- Alternatives : copier YAATA écarté pour originalité ; remplacer la base Kotlin écarté pour coût ; ne rien dessiner écarté car les parcours doivent être revus ; prototype jetable retenu.

## Parcours et acceptation

| Story | Décision | Critère observable |
|---|---|---|
| US-01 Retrouver un message | UI-01 | Recherche par nom/texte, filtres SIM/non lus/favoris et état vide |
| US-02 Envoyer une réponse | UI-01, UI-04 | Ouvrir un fil, écrire, choisir la SIM, envoyer : bulle et confirmation fictives |
| US-03 Programmer un SMS | UI-01, UI-04 | Refuser message vide/date passée ; afficher puis modifier/annuler la programmation |
| US-04 Envoi groupé | UI-01, UI-04 | Sélection multiple ; préciser que les SMS sont individuels |
| US-05 Automatiser | UI-01, UI-04 | Ajouter, activer/désactiver, supprimer une règle ; refuser transfert vers soi |
| US-06 Personnaliser | UI-03 | Thème, clair/sombre, taille, densité, bulles et son modifient l'aperçu |
| US-07 Ranger/protéger | UI-02, UI-04 | Favoris, archive, blocage, coffre, liste jaune ; verrouillage et avertissement de simulation |
| US-08 Découvrir | UI-06 | Onboarding et diagnostic simulés ; bouclier identifié comme non actif |

## Hypothèses et mesures proposées

HYP-01 : trois onglets suffisent. Test : 3 personnes novices + 2 habituées ; leur faire retrouver un message, programmer un SMS, changer la SIM. Cible proposée : 4/5 personnes réussissent chaque tâche sans aide en moins de 60 s ; mesure par observation chronométrée. Aucun résultat terrain n'est revendiqué.
HYP-02 : coffre découvrable sans être exposé. Test : retrouver l'astuce dans le menu en moins de 60 s, puis le verrouiller. Si échec de 2/5 personnes, revoir l'aide.
Garde-fous : zéro SMS réel envoyé, zéro accès carnet d'adresses, aucune promesse de chiffrement réel dans l'interface de démonstration.

## Architecture et transmission

React/TypeScript est utilisé uniquement pour le prototype navigateur. L'application cible reste Kotlin/Jetpack Compose/Material 3. État de session uniquement ; recharger réinitialise. Les contrôles système (biométrie, autorisations, cartes SIM) sont simulés. La langue anglaise est un aperçu partiel de navigation, pas une localisation complète.

Lots proposés pour une future intégration, estimation À VALIDER :
1. Liste, recherche, fil et SIM : 2 à 4 jours ; confiance moyenne ; vérification appareil double SIM.
2. Personnalisation, archives et menus : 1 à 3 jours ; confiance moyenne ; vérification lisibilité et persistance.
3. Automatisations et parcours sensibles : 3 à 6 jours ; confiance faible ; revue sécurité et tests Android indispensables.
Ces fourchettes incluent adaptation, revue et vérification, pas une promesse de planning. Aucun développement Android autorisé par ce prototype.

Réentrée : changement de fonction -> reprendre routes et critères ; difficulté utilisateur -> reprendre UI-01/03 et test HYP-01 ; modification du coffre -> revue sécurité de l'application réelle avant intégration.

## Références

- android/compose-samples : Jetchat et Reply comme références de conversations/navigation.
- android/adaptive-apps-samples : adaptation d'écran pour une intégration ultérieure.
- composablehorizons/compose-unstyled : référence de personnalisation, pas dépendance ajoutée.
- jordond/MaterialKolor : référence de cohérence des palettes, pas dépendance ajoutée.
- YAATA : captures de boîte de réception et de fil observées dans les résultats d'images, listes par dates et thème sombre/clair. Aucune inspection de sa version actuelle sur un appareil ; aucune licence de code ou d'assets supposée.


## Extension des besoins utilisateurs

Ajouts : listes de diffusion modifiables, personnalisation par variables, aperçu individuel, exclusions, historique par destinataire et relance des seuls échecs avec texte et SIM d'origine. Modèles, étiquettes, notes privées séparées, suivi manuel et rappel fictif sur la fiche contact. Détails SMS au clic ou appui long. Import volontaire d'une image locale, aperçu et choix de portée ; aucune connexion WhatsApp/Messenger.

Les règles de confidentialité combinent contact ET mot-clé et proposent coffre à réception, phrase de lecture fictive, coffre ou suppression après lecture et option conserver. Le bac à sable permet de simuler réception, autorisation et fermeture ; il ne sécurise pas de vrais messages. Les phrases de démonstration sont en mémoire en clair. Aucun secret réel ne doit y être saisi.

Toutes les données restent dans la session : recharger réinitialise la démo. SMS réseau, accusés réels, programmation en arrière-plan, carnet Android, biométrie et chiffrement nécessitent une implémentation native ultérieure. Les photos privées dans les caches d'autres applications ne sont pas accessibles à une application Android ordinaire par simple connaissance du chemin. Un helper d'accessibilité ne contourne pas le sandbox ni les fenêtres sécurisées.
