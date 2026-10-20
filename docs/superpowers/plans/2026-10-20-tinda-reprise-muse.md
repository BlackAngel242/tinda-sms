# Tinda SMS : plan de reprise pour Muse

Date : 20 octobre 2026. Base vérifiée : master, dfddee719be55b4c71fd49cef7f26e92b1bed986.
Responsabilités : Muse développe ; Codex relit et coordonne ; SmokeSama arbitre le produit.

> Exécution : Muse applique ce plan par petits lots avec superpowers:executing-plans si disponible, sinon avec les mêmes étapes explicites. Les outils disponibles chez Codex ne sont pas présumés disponibles chez Muse.

**Objectif :** sécuriser les données et rendre les statuts fiables avant les finitions Radiant Slate.
**Architecture :** conserver Kotlin, Compose, Room, Hilt, WorkManager et SQLCipher. Séparer la session du coffre, le stockage de fond et le suivi durable des envois.
**Spécification :** [dossier de Muse](../../reference/dossier-muse-2026-10-20.md), AGENTS.md et décisions approuvées par SmokeSama. Le présent plan propose des corrections, sans remplacer un arbitrage produit.

## 1. Comparaison et limites des preuves

- GitHub et le dossier de Muse correspondent au même SHA. Aucun écart de code à réconcilier à cette date.
- Muse déclare son arbre propre ; Codex ne peut pas vérifier son disque local.
- La revue statique confirme les chemins sensibles décrits : repli coffre vers provider, confusion db/session, absence d'agrégation multipart et statut programmé prématuré.
- Le dossier précise que le PIN reste demandé à l'entrée de l'interface : aucun contournement UI démontré. Ne pas transformer le défaut de séparation interne en exploitation prouvée.
- 57 méthodes @Test déclarées ne prouvent pas 57 tests exécutés avec succès.
- Dernier APK dont l'installation est documentée par Muse : 0242b6a. Les ajouts suivants jusqu'à dfddee7 ne sont pas validés sur appareil.
- Les chemins ui/newconversation/, ui/search/ et data/ContactsRepository.kt cités dans le tableau de Muse ne correspondent pas à l'arborescence examinée. Localiser les symboles, notamment ui/newmessage/NewConversationScreen.kt, plutôt que créer des doublons.
- Codex n'a exécuté aucun build Android pendant cette passation. Les résultats historiques restent attribués à Muse.

## 2. Contraintes globales

- Android API 26 minimum, cible 36 ; Kotlin, Compose Material 3 ; interfaces FR/EN.
- Aucune donnée personnelle, contenu SMS, numéro, PIN ou secret dans logs, commits et rapports.
- Aucune reprise de code, d'assets ou de marque d'une autre application.
- Aucun SMS privé ni simulé dans le provider système. Aucun serveur de collecte.
- Préserver keystore permanent, keystore.properties et local.properties hors git.
- Aucun reset destructif, désinstallation, changement de signature ou effacement de base pour contourner une migration.
- Ne pas fusionner automatiquement ni publier une release au titre de ce planning.
- Ne pas modifier silencieusement DEC-004 ou DEC-005.
- Autorisation actuelle : PR documentaire et consignes. L'exécution est déclenchée par le prompt de SmokeSama.

## 3. Points à corriger dans le programme proposé

### Coffre
Une file chiffrée n'est pas une solution complète si elle dépend de la clé devenue inaccessible. Définir séparément panne SQLCipher, corruption, indisponibilité temporaire et perte de clé.
Recommandation à faire approuver : file privée chiffrée avec mécanisme de secours indépendant, politique de reprise et test de panne. Jamais de corps en clair dans Room principal ni dans WorkManager Data.
Si aucune clé utilisable ne subsiste, documenter la limite : ni conservation garantie ni récupération magique. Ne pas choisir la perte silencieuse par défaut.
La réception de fond ne doit pas accorder une session UI. Envelopper une même clé par le PIN et par une clé appareil accessible sans authentification ne rend pas le PIN indispensable au déchiffrement. Concevoir le modèle de menace avant de promettre « PIN oublié = perte cryptographique ».
Le déverrouillage par SMS contenant le PIN peut laisser des traces hors de Tinda : appareil émetteur, opérateur, autres composants. Ne pas promettre « aucune trace » et ne pas le développer sans réexamen de DEC-005.

### Envois
L'API ne fournit pas nécessairement trois accusés distincts « antenne / SMSC / terminal ». Utiliser seulement les états observables, avec un état non confirmé.
La documentation Android expose des erreurs au-delà de 1 à 4. Le retour 0 observé sur HiOS est une observation liée à une ROM, pas une règle universelle de réussite.
Capturer resultCode dans onReceive avant goAsync et avant le lancement de coroutine. Vérifier le rapport de livraison reçu, pas seulement l'existence du callback.
Un verrou local évite des exécutions concurrentes ; il ne garantit pas un envoi exactement une fois après une panne située entre émission radio et écriture locale. Ne jamais renvoyer automatiquement un état ambigu.

### Données, CI et mesures
fallbackToDestructiveMigration ne signifie pas que toute erreur de schéma est automatiquement réparée : il autorise une destruction dans certains chemins sans migration. Vérifier chaque chemin supporté.
Une CI peut signer via des secrets protégés ; cela ne nécessite pas de publier le keystore. Commencer par vérifications sans la clé de terrain ; décider séparément d'une signature CI.
La conservation de N messages récents doit compter tous les messages du fil, pas uniquement les candidats anciens. Examiner ce comportement dans AutoDeleteWorker.
Le corpus de 200 SMS est un premier jalon, pas une preuve statistique suffisante de performance générale. Préparer une séparation apprentissage/réglage/évaluation, des cas légitimes et une collecte consentie et anonymisée. Aucun SMS réel dans GitHub.

Sources techniques à consulter lors de l'implémentation :
- https://developer.android.com/reference/android/telephony/SmsManager
- https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work

## 4. Planning par livrables

Ordre : L0, L1, L2, L3, L4, L5. Une PR par lot cohérent ; séparer L1 en propositions et correction lorsque l'arbitrage manque.
Pas de durée inventée : après L0, Muse fournit une estimation en sessions de travail avec hypothèses. L'accès adb conditionne la recette, pas tout le développement.

| Lot | Livrable | Dépendance / sortie |
|---|---|---|
| L0 | Référence de build, documentation et décisions ouvertes | Possible immédiatement |
| L1 | Coffre, session et politique de panne | Arbitrage pour modèle de clés et secours |
| L2 | Migrations et suppression automatique | À vérifier avant toute installation cumulative |
| L3 | Accusés multipart et programmation durable | Préserve les observations ROM |
| L4 | Interface Radiant Slate et parcours accessibles | Après stabilisation des contrats de données |
| L5 | Recette appareil et build candidat identifié | adb disponible et validations sensibles |

### L0 : reprise et preuve de référence

Fichiers : README.md, ROADMAP_TODO.md, AGENTS.md, app/build.gradle.kts ; docs/validation/ à créer.
- [ ] Lire le dossier et AGENTS.md ; vérifier git status, branche, SHA et écarts distants. Préserver les fichiers locaux non suivis.
- [ ] Rendre le PRD accessible par un chemin du dépôt, en conservant sa provenance et ses écarts ouverts. Ne pas convertir ses affirmations de marché ou juridiques en faits vérifiés.
- [ ] Construire la matrice F1 à F22 et DEC-S1 à DEC-S6 : prévu, codé, testé JVM, testé appareil. Inclure alarmes exactes/récurrence, mots-clés du répondeur, suppression après lecture, recherche réellement complète et alerte coût groupé.
- [ ] Exécuter testDebugUnitTest, lintDebug et assembleDebug ; consigner SHA, commande, outil, résultat et compte des tests depuis les rapports.
- [ ] Si wrapper hors ligne impossible, employer le Gradle local 8.14.3 documenté par Muse, avec JDK/SDK disponibles ; consigner l'équivalence et la limite. Ne pas télécharger des dépendances aveuglément.
- [ ] Sans keystore disponible : ne pas prétendre produire l'APK installable de terrain. Proposer un variant de vérification distinct sans changer la clé existante.
- [ ] Proposer une version suivante avec versionCode strictement croissant et SHA dans la fiche de build ; ne pas rebaptiser les anciens APK.
- [ ] Présenter les arbitrages ouverts et poursuivre L2/L3 si L1 reste bloqué.

Sortie : un rapport reproductible, aucun test déclaré vert sans résultat actuel.

### L1 : coffre et session

Fichiers : data/VaultRepository.kt, data/local/VaultCrypto.kt, data/local/VaultDatabase.kt, sms/SmsReceiver.kt, navigation/TindaNavGraph.kt, ui/vault/, notifications/NotificationHelper.kt.
Contrat : accès technique de fond distinct d'une autorisation UI explicite. Toute lecture UI exige la session autorisée.

- [ ] Écrire les tests révélant : réception de fond sans session, ouverture sans authentification, verrouillage au retour arrière/arrière-plan et expiration de session.
- [ ] Fixer et documenter un délai de session centralisé ; le proposer comme paramètre technique révisable, sans nouveau réglage inutile.
- [ ] Préparer la politique de panne et le modèle de clés : menace, réception verrouillée, biométrie, oubli du PIN, panne de secours et migration. Attendre l'arbitrage uniquement pour les changements qui en dépendent.
- [ ] Tester une ouverture défaillante et une écriture défaillante réelle : aucun provider, index, aperçu, notification ou log ne reçoit le contenu privé.
- [ ] Tester concurrence réception/verrouillage, arrêt du processus et reprise de la file si retenue.
- [ ] Migrer les données existantes selon une procédure réversible et testée ; ne pas effacer la base.
- [ ] Réexécuter les tests, compiler et soumettre la lentille sécurité. Une simple lecture du code ne clôt pas la recette téléphone.

### L2 : conservation des données

Fichiers : data/local/TindaDatabase.kt, di/AppModule.kt, sms/AutoDeleteWorker.kt, configuration Room et tests.
Contrat : une mise à jour conserve brouillons, règles, programmations, SIM et favoris.

- [ ] Exporter les schémas Room ; documenter l'absence de certains schémas historiques et reconstruire des fixtures fiables si nécessaire.
- [ ] Ajouter des tests de migration des versions supportées 3 à 10 vers la nouvelle version : structure et données, FTS inclus.
- [ ] Tester les versions antérieures : support explicite ou refus non destructif. Retirer le repli destructif sans laisser un chemin d'installation non documenté.
- [ ] Tester suppression : messages récents et anciens mélangés, minimum conservé, règles superposées, provider absent, refus de suppression provider et cohérence du miroir Room.
- [ ] Tester les collisions de suffixe et un format international différent ; proposer une normalisation partagée adaptée sans ajouter une bibliothèque inutile.
- [ ] Distinguer tests JVM exécutés et tests instrumentés écrits mais non exécutables sans appareil. Ne pas déclarer ces derniers validés.

### L3 : fiabilité de l'envoi

Fichiers : sms/SmsSender.kt, sms/SmsStatusReceiver.kt, sms/ScheduledSmsWorker.kt, sms/ScheduledSmsManager.kt, data/local/TindaDatabase.kt, data/SmsRepository.kt et écrans de statut.
Contrat : token durable par tentative, index de segment et lien programmation/message ; transitions persistées et monotones.

- [ ] Ajouter les tests : callbacks inversés, doublons, segment manquant, échec partiel, livraison tardive, callback inconnu et redémarrage. Un échec partiel ne devient pas un succès global par simple écrasement.
- [ ] Agréger tous les segments ; garder non confirmé si les preuves manquent. Préserver une stratégie HiOS spécifique et justifiée plutôt qu'accepter 0 comme succès partout.
- [ ] Ajouter une prise en charge atomique de programmation et un token de tentative ; définir PENDING, SENDING et états finaux, avec état ambigu.
- [ ] Simuler un arrêt avant appel radio, après appel radio et avant persistance, ainsi qu'une course annulation/worker.
- [ ] Reprendre sans renvoi automatique des cas ambigus ; rendre une relance manuelle explicite avec risque de doublon.
- [ ] Réconcilier provider, miroir et écran : ne pas présenter comme envoyé un message seulement soumis ou échoué.
- [ ] Tester disparition de SIM, changement d'identifiant après redémarrage, perte du rôle SMS et permissions révoquées.
- [ ] Documenter retard WorkManager ; traiter exactitude et récurrence comme écarts PRD ouverts, pas comme déjà livrés.

### L4 : interface utile

Fichiers : ui/newmessage/NewConversationScreen.kt, ui/conversations/, ui/groupsend/, ui/settings/, navigation/, theme/, res/values*/.
- [ ] Terminer brouillons, contacts récents/fréquents et épinglage avec les repositories existants.
- [ ] Harmoniser envoi groupé ; afficher segments multipliés par destinataires, seuil d'alerte par défaut 5 selon RB-06 et SIM avant confirmation.
- [ ] Construire Messages / Protection / Archivés / Paramètres selon DEC-S6. Aucun bouclier prétendument actif sans moteur validé.
- [ ] Proposer couleurs SIM personnalisables ; vérifier contraste et identification par texte en plus de la couleur.
- [ ] Vérifier FR/EN, thème clair/sombre, clavier, grandes polices et TalkBack.
- [ ] Passer Fixit sur captures réelles après chaque parcours significatif ; noter capture, défaut, correction et verdict. Pas de capture disponible = audit visuel restant à faire.

### L5 : recette et candidat

- [ ] Consigner modèle, API Android, ROM, versionCode/versionName, SHA et empreinte de l'APK ; aucune adresse adb privée dans le rapport public.
- [ ] Installer par adb install -r avec la signature conservée, seulement après disponibilité et autorisation de la connexion. Ne pas désinstaller pour contourner une erreur.
- [ ] Tester PIN, routage privé, notification masquée, double SIM réelle, SMS long, réseau absent, redémarrage, programmation et conservation après mise à jour.
- [ ] Rattraper les ajouts non installés depuis 0242b6a : statistiques, favoris, brouillons, épinglage, verrouillage avancé et vitesse de liste.
- [ ] Définir une matrice API 26 à 36 avec profils ancien/milieu/récent. L'absence de KVM reste une limite explicite, pas une validation.
- [ ] Mesurer démarrage et mémoire sur appareil avec Android Performance si disponible. APK < 25 Mo et démarrage < 1,5 s sont des objectifs à mesurer.
- [ ] Produire un candidat, ses limites et sa checklist. Publication et fusion restent distinctes.

## 5. Méthode, leçons et outils

Pour chaque tâche : test qui révèle le défaut, constat d'échec, correction minimale, nouveau test, contrôle adapté, commit ciblé et rapport.
Pour une tâche purement documentaire, vérifier liens, provenance et cohérence ; pas de build Android inutile.

Leçons :
1. Une particularité HiOS n'est pas une loi Android : conserver modèle, API, ROM, résultat et reproduction.
2. Une exception attrapée ne prouve pas une dégradation sûre : tester ce qu'elle fait aux données privées.
3. La signature permanente est un actif à préserver ; sa sauvegarde sécurisée n'est jamais un commit.
4. Les identifiants du provider constructeur diffèrent : conserver les replis testés et leur justification.
5. R8 peut casser le coffre : vérifier aussi l'APK minifié et le chargement SQLCipher.
6. Un booléen « ouvert » ne vaut pas une authentification ; tester les transitions.
7. Les métriques calculées doivent préciser leur dénominateur, les accusés absents et la couverture réelle.
8. Éviter les dépendances non accessibles dans l'environnement hors ligne ; ne pas refaire les impasses biometric/hilt-work sans raison nouvelle.
9. Pas de boucle de relance Gradle sans diagnostic : distinguer mémoire insuffisante, dépendance absente et erreur de code.
10. Consigner une leçon seulement avec sa preuve et sa condition de révision.

Outils recommandés :
- GitHub : PR petites et traçables ; branches codex_ pour Codex, branche de travail Muse distincte.
- Honey : réutilisation et correction minimale, aucune réécriture générale.
- Superpowers diagnostic/tests/vérification : preuves avant annonce.
- Fixit et Product Design Audit : captures et parcours réels.
- Android QA / Performance : adb et mesures si environnement disponible.
- Mobbin : inspiration originale, seulement au lot interface.
- PRD Table Ronde : arbitrages du coffre et fonctions sensibles, sans relancer tout le produit.
- Pas de compte Auth0, serveur Railway ou backend Netlify pour ce périmètre local.

CI proposée, décision encore ouverte : un workflow Linux léger sur PR, cache Gradle, tests JVM, lint et vérification de build sans clé de terrain. Aucun push de keystore ; aucune modification des secrets au titre de ce plan. Ne pas créer une matrice coûteuse ni relancer les jobs inchangés.

## 6. Arbitrages à présenter ensemble

| Sujet | Recommandation de Codex | Exécution autorisée sans décision nouvelle |
|---|---|---|
| Panne du coffre | Conservation chiffrée privée avec secours indépendant et limites explicites | Analyse, tests et séparation session |
| Clés / PIN | Respecter la promesse approuvée ou soumettre une révision explicite de DEC-004 | Modèle de menace et proposition de migration |
| SMS contenant le PIN | Reporter la mise en oeuvre et réexaminer DEC-005 | Analyse des traces et alternatives locales |
| CI | Vérifications légères sans clé de terrain | Proposition de configuration |
| Versions | Incrémenter versionCode à chaque APK distribué ; SHA consigné | Inventaire et proposition |
| Corpus bouclier | Collecte consentie, anonymisée et évaluée séparément | Protocole ; pas de faux taux de précision |

Si une décision manque, poursuivre les tâches indépendantes et signaler le blocage précis. Ne pas suspendre tout le projet ; ne pas décider silencieusement d'une perte de données.

## 7. Backlog préservé

Signature automatique ; tags locaux mutualisés avec le bouclier ; sauvegarde/restauration locale chiffrée (y compris coffre selon modèle approuvé) ; modèles/réponses rapides ; compagnon local Wi-Fi/QR ; lingala ; chatheads et Bubbles optionnels ; SMSC lecture ; simulation locale marquée et exclue des exports. Le bouclier reste en signalement sans blocage et attend son corpus. Les fonctions sensibles repassent par la lentille sécurité.

## 8. Rapport de fin de lot

- Branche, SHA et changements.
- Tests exécutés, nombres et résultats ; tests non exécutés séparément.
- APK construit/installé ou non, version et signature conservée.
- Recette appareil et captures disponibles.
- Risques et décisions encore ouvertes.
- Une à trois leçons vérifiées.
- Prochaine action concrète.

Ne pas annoncer « terminé » si seule l'implémentation est finie.
