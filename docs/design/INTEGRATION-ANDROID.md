# Intégrer la nouvelle interface Tinda SMS

Date : 23 octobre 2026. Base examinée : `dfddee719be55b4c71fd49cef7f26e92b1bed986`, branche `master`.

## Décision et portée

Porter progressivement le design dans **Kotlin, Jetpack Compose et Material 3**. Garder Room, Hilt, DataStore, la navigation et les services SMS actuels. Le prototype React sert à comprendre les écrans, les interactions et les choix visuels ; il ne doit pas être intégré en WebView, ni convertir l'application en React Native.

Cette PR ajoute la référence et les instructions. Elle ne modifie pas les sources Android, ne livre pas ces nouvelles fonctions dans l'APK et ne valide pas la sécurité du coffre. Les exemples de la maquette sont entièrement fictifs, gardés dans la session. Aucun compte, SMS réel ou carnet de contacts n'est connecté.

Lire dans cet ordre :

1. [Référence et mode d'ouverture](../../design/README.md).
2. [Profils, trente questions et besoins](../../design/PROFILS-ET-BESOINS.md). Les six profils sont des hypothèses de conception, pas des entretiens réalisés.
3. [Thèmes, écarts actuels et critères](THEMES.md).
4. Ce guide et ses lots de portage.

## Direction visuelle à conserver

- Accueil sombre ardoise, accent turquoise Radiant, avatars uniformes, badges SIM neutres et explicitement nommés.
- Trois destinations principales : Messages, Automatisations, Réglages. Les fonctions secondaires restent accessibles par menus et écrans dédiés.
- Un accent par thème pour les actions, la sélection et les non-lus. Rouge réservé aux erreurs ou suppressions ; ne pas attribuer une couleur vive à chaque contact.
- Conserver recherche, filtres SIM/non-lus/favoris, conversations épinglées, rédaction accessible et informations SMS à l'appui long avec une alternative via menu.
- Icônes cohérentes. Utiliser d'abord les icônes Compose disponibles ; ne pas ajouter une bibliothèque uniquement pour quelques pictogrammes. Toute inspiration d'usage est à réaliser avec des composants et assets originaux.
- La barre d'état, les insets, le clavier et les gestes doivent être natifs. Ne pas reproduire le téléphone Pixel ni le clavier simulé de la maquette.

## Correspondance avec le code actuel

Les chemins ci-dessous sont relatifs à `app/src/main/java/tech/dmampro/tinda/`. Leur existence a été vérifiée au commit de base ; vérifier les changements plus récents avant chaque lot.

| Parcours de référence | Point d'entrée actuel | Travail à réaliser |
|---|---|---|
| Accueil, recherche, filtres | `ui/conversations/ConversationListScreen.kt`, `ConversationListViewModel.kt` | Traduire la présentation, garder les flux de données et les actions existantes ; remplacer les couleurs Radiant codées en dur par des rôles de thème |
| Conversation, détails SMS | `ui/conversation/ConversationDetailScreen.kt`, `ConversationDetailViewModel.kt` | Ajouter une fiche de détails par message : envoi, réception ou accusé disponible, SIM, segments, statut ; ne pas inventer un horodatage absent |
| Nouvelle rédaction | `ui/newmessage/NewConversationScreen.kt` | Modèles, aperçu, SIM et erreurs ; conserver le compteur d'encodage natif |
| Listes et diffusions | `ui/groupsend/GroupSendScreen.kt` | Étendre l'envoi individuel existant avec listes persistantes et suivi par destinataire |
| Programmation | `ui/schedule/`, `sms/ScheduledSmsManager.kt`, `ScheduledSmsWorker.kt` | Réutiliser le moteur, figer destinataires et contenu personnalisé ; respecter les limites réelles d'exécution en arrière-plan |
| Automatisations | `ui/autoreply/`, `ui/forward/`, `ui/autodelete/` | Regrouper les entrées sans réécrire les moteurs ; garde-fous et états vide/erreur |
| Coffre et règles privées | `ui/vault/`, `data/VaultRepository.kt`, `sms/SmsReceiver.kt` | Étudier le circuit sécurisé existant avant d'ajouter les règles après lecture ; revue sécurité obligatoire |
| Préférences et apparence | `ui/settings/`, `data/PrefsRepository.kt`, `theme/` | Réutiliser les identifiants de thèmes et les préférences ; voir THEMES.md |
| Contacts et assistant photo | Nouveau parcours à raccorder à `navigation/Routes.kt` et `TindaNavGraph.kt` | Séparer données Tinda et carnet Android ; import volontaire avant tout helper expérimental |

Ne pas remplacer les ViewModels par des états locaux équivalents aux `useState` de la maquette. Les règles doivent être testables hors des Composables, les opérations durables confiées aux repositories.

## Diffusions et accusés : changement de fond

### Ce qui existe

`GroupSendViewModel.sendAll` boucle sur les destinataires, appelle `SmsSender.send` et stocke un booléen dans `results`. Un retour sans exception ne prouve pas la livraison. `SmsStatusReceiver` reçoit déjà les événements d'envoi et de livraison avec le jeton du message. `Message` conserve `token`, `subscriptionId`, `status` et `failureCode`, mais pas deux horodatages séparés d'envoi et de livraison. La base Room est en version 10 au commit examiné.

### Modèle cible proposé, à adapter après étude du schéma

- Liste : identifiant stable, nom et membres dédoublonnés par adresse normalisée. Modifier une liste n'altère pas les anciens envois.
- Diffusion : identifiant, liste source facultative, date et snapshot des destinataires autorisés.
- Destinataire de diffusion : identifiant stable, message/token corrélé, texte personnalisé figé, abonnement choisi, tentative et états/horodatages disponibles. Relier le contenu à sa source de stockage autorisée et éviter une nouvelle copie en clair de contenus protégés.
- Préférences contact : étiquette, exclusion des diffusions, SIM et photo Tinda. Les notes sensibles nécessitent le stockage protégé, pas des préférences ordinaires.
- Migrer explicitement Room, sans `fallbackToDestructiveMigration`. Choisir le prochain numéro libre au moment du lot et tester la conservation des données.

À la confirmation d'un envoi : normaliser, dédoublonner, réévaluer blocages/coffre/exclusions, vérifier variables et SIM active, afficher nombre de destinataires et segments/coût potentiel. Demander une action d'envoi explicite. Une SIM supprimée impose un nouveau choix, jamais un remplacement silencieux.

Persister les tentatives avant l'appel radio. Corréler chaque événement au message **et au segment** ; agréger les segments avant d'afficher la livraison complète. Prévoir événements dupliqués, tardifs et hors ordre. Un statut livré ne régresse pas vers envoyé. Le traitement actuel des codes radio propres aux ROM doit être testé sur appareil avant toute modification.

Afficher distinctement : en attente, envoyé, livraison confirmée, livraison non confirmée, échec et partiel. Absence d'accusé ne prouve pas l'absence de réception. Ne jamais afficher « lu » pour un SMS opérateur.

Relancer uniquement les échecs explicitement sélectionnés, avec texte personnalisé et SIM d'origine, en réévaluant les exclusions actuelles. Créer une nouvelle tentative liée à l'ancienne ; ne pas doubler un SMS en cours ou de statut inconnu. L'envoi groupé reste une série de SMS individuels, sans MMS et sans conversation de groupe partagée.

## Confidentialité et après lecture

Le coffre natif possède déjà sa base et ses fonctions de verrouillage ; les secrets en clair et les simples booléens du prototype ne constituent jamais une implémentation de sécurité.

- Règle : contact ou numéro, mot-clé facultatif, déclencheur, action, délai, activation. Contact ET mot-clé doivent correspondre s'ils sont tous deux renseignés.
- À réception : routage vers le coffre avant aperçu de notification, recherche et automatismes pouvant exposer le contenu. Définir un comportement sûr si le coffre ne peut pas être ouvert en arrière-plan. Ne pas perdre le SMS.
- Après lecture : démarrer seulement après authentification et révélation volontaire. Aperçu, appui long, ouverture de notification ou synchronisation du champ `read` ne suffisent pas.
- Actions : mise au coffre ou suppression locale après fermeture/délai. « Conserver » est prioritaire et annule les tâches destructives. Signaler les règles en conflit ; ne pas choisir silencieusement une suppression.
- Délais : échéance persistée, opération idempotente, reprise après arrêt et redémarrage. Ne pas promettre une exécution à la seconde avec WorkManager ; si nécessaire, masquer l'accès dès l'échéance et exécuter la purge lorsque le système l'autorise.
- Déplacement vers le coffre : vérifier l'écriture protégée avant retrait de la source, concevoir la reprise sur panne. Aucune réapparition via le provider, l'index FTS, un export, une notification ou une sauvegarde.
- Authentification : reprendre les mécanismes natifs et faire revoir dérivation des clés, stockage des vérificateurs, cycle de verrouillage, récupération et résistance aux essais. Ne pas copier la phrase `lagune` ou le code de démonstration.
- Une phrase locale de déverrouillage ne protège pas le SMS pendant son transport et ne permet pas de faire disparaître la copie chez le destinataire. Une phrase commune entre deux personnes n'est pas un protocole sécurisé à elle seule.
- Suppression signifie retrait local dans les stockages prévus ; ne pas promettre un effacement physique garanti de tous les supports et sauvegardes.

Ces fonctions passent une revue sécurité distincte avant activation, conformément à AGENTS.md. Commencer par les règles non destructives et rendre la suppression explicitement volontaire.

## Contacts, photos et helper

Commencer par la lecture autorisée du carnet et l'import volontaire d'une photo locale. Proposer une prévisualisation, le contact et le numéro concernés, et la portée « Tinda seulement » ou « Mettre à jour le carnet ». La seconde option demande les permissions nécessaires et une confirmation ; gérer contacts agrégés, plusieurs numéros, comptes en lecture seule et échec de synchronisation.

Lier les données par identité de contact durable et adresse normalisée, pas uniquement par nom affiché ou position dans la liste. Observer les changements du carnet et invalider le cache Tinda sans écraser une photo choisie manuellement. Réduire les images importées avant stockage et conserver un chemin sans cloud par défaut.

WhatsApp et Messenger ne sont pas connectés dans cette livraison. Ne pas promettre une API universelle pour obtenir la photo d'un numéro. Connaître le chemin d'un cache privé ne donne pas accès au fichier. Un service d'accessibilité ne contourne pas le sandbox Android ni les fenêtres sécurisées. Un helper reste une piste séparée, volontaire et à tester ; il ne bloque pas la livraison du module Contacts. Pas de récupération silencieuse, de root requis ni de contournement des protections dans le produit courant.

## Lots de réalisation et critères de sortie

| Lot | Résultat concret | Preuves attendues avant la suite |
|---|---|---|
| 1. Design system et thèmes | Accueil, composants partagés, navigation à trois onglets, couleurs sémantiques | Sept thèmes × clair/sombre, système, redémarrage, police agrandie ; pas de couleurs Radiant imposées hors thème |
| 2. Conversations | Liste, rédaction, menus, détails et états | Données réelles conservées, recherche/archives/favoris/SIM fonctionnels ; clavier, TalkBack et longs textes |
| 3. Diffusions | Listes persistantes, modèles, exclusions, suivi et reprises | Migration testée, dédoublonnage, variables manquantes, reprise après arrêt, multipart et accusés tardifs ; tests réels double SIM |
| 4. Contacts | Étiquettes, notes, suivi manuel, rappels, import photo | Refus de permissions supporté, contacts multiples, portée Tinda/carnet explicite ; paiement jamais déclaré vérifié automatiquement |
| 5. Confidentialité | Règles, coffre à réception puis après lecture | Revue sécurité, tests fuite notifications/FTS/provider/exports, conserver prioritaire, interruption durant transfert et purge |
| 6. Finition | FR/EN complet, états et performance | Build, lint, tests, revue visuelle Fixit sur captures de vrais écrans ; parcours sur téléphone Android modeste |

Le bouclier anti-arnaque, une équipe multi-utilisateur et un helper Meta automatique restent des pistes, pas des fonctions livrées. Les rappels de paiement ne constituent ni une preuve de paiement ni une vérification Mobile Money.

## Consigne directement utilisable par Muse

> Lis AGENTS.md, design/README.md, docs/design/INTEGRATION-ANDROID.md, docs/design/THEMES.md et design/PROFILS-ET-BESOINS.md. Compare d'abord ton HEAD au commit de référence et conserve les améliorations déjà présentes. Ouvre la maquette comme référence de comportement, puis réalise le lot 1 en Kotlin/Compose sur une branche codex_. Ne copie ni React, ni données de démonstration, ni protections fictives dans Android. Réutilise les repositories et modèles actuels. Ajoute les rôles de couleurs manquants, supprime les couleurs Radiant imposées dans les écrans et garde l'accueil sobre. Teste les sept thèmes clair/sombre, le mode système, les préférences après redémarrage et la taille de police. Fournis des captures Android, les commandes exécutées et leurs résultats. Arrête-toi avant le lot suivant si un critère de sortie échoue. La PR doit expliquer simplement le problème corrigé et le changement visible.

## Vérification de cette livraison

Le prototype a passé `npm run build`, `npm run check:runtime` et les quatre tests de `tests/domain.test.mjs`. Les parcours de personnalisation, reprise par destinataire et lecture protégée ont été exercés dans le navigateur. La suppression après fermeture a été observée dans le bac à sable. Le rapport détaillé est dans `design/prototype/design-qa.md`.

Aucun build Android n'est annoncé pour cette PR documentaire : `app/`, Gradle et les ressources Android restent inchangés. La matrice visuelle Android, le réseau opérateur, les migrations futures et la sécurité après lecture restent à valider lors du portage.
