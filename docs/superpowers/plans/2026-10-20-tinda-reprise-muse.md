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

## 9. Complément Claude intégré le 21 octobre 2026

Source : Tinda-SMS_brief-Muse_fusion.txt transmis par SmokeSama, audit daté du 20 octobre au même SHA dfddee7. La PR et master ont été relus : base inchangée, PR toujours ouverte. Ce complément complète les sections précédentes. Les détails encore non reproduits restent des pistes à vérifier, sans certification implicite.

### 9.1 Changement d'ordre : L0 puis correctifs courts de sécurité

Après la référence L0, avancer d'abord sur les défauts de routage et de normalisation reproduits, puis notifications et thème. Développer ensuite les lots L1/L2/L3 selon leurs dépendances. Ne pas attendre adb pour écrire et exécuter les tests possibles ; la validation appareil reste une étape distincte.

| Référence Claude | Contrôle / statut Codex | Lot / action |
|---|---|---|
| P0-1, C1 | Repli coffre/provider déjà confirmé | L1, inchangé |
| E1 / P0-2 | Confirmé statiquement : adresse alphabétique devient vide et endsWith("") correspond à une règle numéro | Correctif immédiat, tests ci-dessous |
| E2 / P0-3 | Modèle PIN déjà documenté ; absence de limitation signalée dans l'écran examiné | L1, ajouter protection d'essais et revue crypto |
| P0-4 | Base ouverte/session déjà distinguées dans le plan | L1 |
| E3 / P0-5 | Repli destructif confirmé, sans généraliser à toute erreur Room | L2 |
| P0-6 | Fonction #PIN absente, écart PRD ; absence fonctionnelle distincte d'une vulnérabilité exploitable | Arbitrage DEC-005, pas d'implémentation immédiate |
| E4 / P0-7 | Confirmé statiquement : BlockRepository ne retire que les blancs | Correctif normalisation et migration, L2 |
| E5 / P0-8 | ForwardManager ne filtre pas les OTP et ne plafonne pas les transferts | L3, proposition de garde-fous et décision produit |
| M1 | Cache mutable partagé signalé, accès async à reproduire | L4, test concurrence puis structure sûre minimale |
| M2 | Requêtes par fil déjà vues, coût exact non mesuré | L4/L5, mesurer et grouper les compteurs avant debounce arbitraire |
| M3 | Confirmé : même ID notification et même requestCode possibles pour fil/coffre | Correctif immédiat avec tests des deux identités |
| M4 | Confirmé : initial lagoon dans MainActivity, défaut radiant dans PrefsRepository | Correctif immédiat, constante partagée et test |
| M5 | Sous-ensemble ancien utilisé par la suppression, déjà couvert au plan | L2, comportement recommandé à faire arbitrer avant changement destructif |
| M6 | Minima courts confirmés, risque de correspondance large | L1/L4, règles numériques exactes et aperçu ; pas de seuil universel improvisé |
| M7 | Repli provider constructeur déjà connu | L5 AOSP + ROM ; pas de repli supprimé sans preuve |
| M8 | Retour 0 HiOS déjà traité comme observation spécifique | L3 |
| M9 | Clé hors git volontaire, CI et signature déjà cadrées | L0, aucune publication de secret |
| M10 | Couverture des chemins sensibles insuffisante | Tests affectés aux lots ; nombre exact depuis exécution |
| M11 | Base principale sans SQLCipher, périmètre de chiffrement à expliciter | Analyse de menace et arbitrage, distinct du coffre |
| M12 | Envoi groupé : confirmation, coût et déduplication à vérifier en détail | L4, tests et garde-fous avant nouvel APK diffusé |

### 9.2 Tâches et critères supplémentaires

#### Routage alphabétique, tests avant correctif
Fichier : data/VaultRepository.kt, matchesSecretRule.
- [ ] Extraire au besoin le prédicat pur sans reproduire une couche complète de repository.
- [ ] Avec une règle NUMBER existante et aucune règle KEYWORD correspondante : expéditeur alphabétique et normalisation vide ne correspondent jamais.
- [ ] Avec une règle KEYWORD correspondante : cet expéditeur peut encore correspondre. Ne pas retourner false au début de toute la fonction, ce qui casserait les mots-clés.
- [ ] Tester aussi adresse vide, chiffres avec séparateurs, préfixe international, numéro court et deux pays différents ayant le même suffixe.
- [ ] Observer les tests échouer sur le prédicat actuel puis réussir après correction.
- [ ] Ne pas adopter takeLast(9) comme identité universelle. Distinguer numéro international, format national avec contexte régional, code court et expéditeur alphabétique.

#### Normalisation et migration
Fichiers : data/BlockRepository.kt, data/VaultRepository.kt, data/SimAssignmentRepository.kt, data/FavoriteRepository.kt, sms/ForwardManager.kt, sms/AutoReplyMatching.kt, sms/AutoDeleteWorker.kt et DAO concernés.
- [ ] Cartographier toutes les représentations et le besoin de chaque appelant. Une API partagée peut garder des traitements distincts par type d'adresse.
- [ ] Tester équivalence locale/internationale avec contexte explicite et non-équivalence de pays distincts ; ne pas utiliser les exemples du brief comme preuve d'un plan de numérotation valide.
- [ ] Préserver les expéditeurs alphanumériques qui peuvent être bloqués volontairement et les codes courts.
- [ ] Migrer les clés stockées, gérer collisions et contraintes uniques sans perte de règles ni association SIM erronée.
- [ ] Dédupliquer l'envoi groupé après canonicalisation et conserver la saisie originale pour affichage si nécessaire.

#### Notification et thème
Fichiers : notifications/NotificationHelper.kt, MainActivity.kt, data/PrefsRepository.kt, theme/TindaThemes.kt.
- [ ] Tester que fil et coffre ne se remplacent ni ne s'annulent mutuellement.
- [ ] Vérifier aussi l'identité PendingIntent : des extras différents ne suffisent pas à distinguer les deux. Définir une identité explicite pour l'ouverture du fil et celle du coffre.
- [ ] Couvrir les identifiants Long de fils : une conversion en Int peut créer d'autres collisions.
- [ ] Unifier le thème par défaut sans réécrire le thème choisi par l'utilisateur. Vérifier le tout premier affichage et le repli d'un identifiant inconnu.

#### PIN, OTP, journaux et stockage principal
- [ ] L1 : tester un délai progressif après échecs de PIN, concurrence, redémarrage du processus et horloge modifiée. Le nombre d'essais et les délais du brief sont des valeurs proposées, pas des décisions déjà prises. Aucun effacement de données.
- [ ] L3 : proposer une confirmation claire du transfert de contenu sensible, gestion OTP par défaut prudente, limites de volume et comportement de suspension ; ne pas imposer arbitrairement 20/jour ni casser silencieusement une règle validée.
- [ ] Ne pas présenter OtpDetection comme détecteur exhaustif : faux positifs et codes non détectés doivent rester explicites.
- [ ] L4 : revue de la copie OTP : indicateur sensible quand supporté, compatibilité ancienne et effacement éventuel seulement si le presse-papiers contient encore la copie de Tinda. Ne pas supprimer un contenu copié ensuite par l'utilisateur.
- [ ] L1/L2 : dresser les données de la base principale, FTS et fichiers de règles. Chiffrer seulement Room principal ne chiffre pas le provider SMS système. Ne pas promettre un historique entièrement protégé sur appareil compromis.
- [ ] TindaLogger.e transmet directement la Throwable : confirmer qu'une exception peut divulguer des données. Préférer un événement autorisé et une classe d'erreur plutôt qu'une regex supposée nettoyer toutes les données personnelles.
- [ ] EventLog : mesurer les écritures synchrones ; conserver un ordre et une rotation bornée si passage à IO, sans infrastructure superflue.
- [ ] VaultRepository : tester deux ouvertures concurrentes et verrouillage pendant réception, pas seulement fermer l'ancienne instance en aveugle.

### 9.3 Registre des constats faibles, à solder dans les lots

| Référence | Suite exigée |
|---|---|
| F1 | L3/L5 : documenter boot après déverrouillage. Ne pas ajouter directBootAware seul : vérifier stockage accessible avant déverrouillage et politique d'envoi privée |
| F2 | L0 : conserver le lien au PRD des alarmes exactes ; décisions sur permissions et fonctionnalité ensemble |
| F3 | L3 : examiner RespondViaMessageService, thread principal, choix SIM et retour d'échec ; conserver la compatibilité du rôle SMS |
| F4 | L4 : I/O des journaux, ordre et rotation bornée |
| F5 | L1 : cycle de vie Room et concurrence |
| F6 | L4/L5 : volume de fil, pagination ou limite seulement après mesure et tests de navigation |
| F7 | L4 : presse-papiers OTP et respect du contenu plus récent |
| F8 | L1 : Throwable et absence de données personnelles dans les logs |
| F9 | L0 : conventions de documentation sans remodeler le code sans nécessité |
| F10 | L0/L1 : commentaires historiques trompeurs, préremplissage ComposeSmsActivity à vérifier |
| F11 | Pas de migration hilt-work par principe : indisponibilité hors ligne déjà documentée ; conserver EntryPoint tant qu'aucun défaut concret ne justifie son remplacement |

### 9.4 Corrections de lecture et décisions

- Le verdict de l'annexe B « RB-01 respectée partout » est contredit par P0-1 dans le même brief. Conserver ce dernier comme défaut confirmé.
- « Aucun secret dans tout l'historique » ne se prouve pas par l'absence de valeurs dans deux propriétés Gradle : ne pas reprendre cette certification sans contrôle dédié.
- Les seuils de mots-clés, longueur de numéro, plafond de transfert, délai de refresh et nombre d'essais sont des propositions. Justifier leurs valeurs et leur compatibilité.
- Ne pas confondre « aucun build de l'audit » et « aucun build du projet » : les exécutions antérieures restent rapportées par Muse.
- Les propositions D4 (conservation de N) et D5 (chiffrement principal) s'ajoutent aux arbitrages du tableau section 6. Présenter recommandations et conséquences, pas sept questions bloquant tout le chantier.
- Le brief ancien interdit toute poussée sans autorisation, alors que le prompt de SmokeSama autorise les branches et PR de travail : appliquer les instructions directes les plus récentes. Elles n'autorisent toujours pas fusion, publication ni installation destructive.
- Ne pas attendre une validation terrain totale de L1 avant de travailler sur les tests indépendants L3 ; aucune fonction non validée n'est pour autant présentée comme prête à diffuser.

### 9.5 Prochaine action Muse

Lire ce complément avant d'exécuter le plan. Reprendre au premier livrable non réalisé ; si L0 est déjà fait, le vérifier et ne pas le refaire. Commencer par le prédicat de routage alphabétique et les tests de normalisation, puis notifications/thème et migrations. Pour chaque constat externe non vérifié ici, rapporter confirmé / infirmé / à reproduire avec preuve, avant correction.
