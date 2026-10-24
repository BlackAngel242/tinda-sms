# Correctifs de la revue Codex du 24 octobre 2026 sur la PR nº 2

Branche : `claude/reprise`. Ces correctifs répondent aux deux
blocages restants avant fusion. Ils servent le parcours du profil
Novice décrit dans `docs/design/PARCOURS-PERSONAS-WORKFLOW.md` :
« Réponse : SIM affichée, envoi confirmé ou incertain », puis
« Historique cohérent avec le fournisseur SMS ». Le tableau des
priorités du même document classe ce travail en P0 : « limiter
l'exception du code SMS 0 aux preuves appareil réelles et empêcher
un état refusé par Room d'être inscrit dans le fournisseur », avec
comme sortie vérifiable « Tests des transitions et résultats de
build au SHA final ; aucune réussite inventée ».

## 1. Le code 0 n'est plus déduit d'une famille d'appareils

### Preuve du défaut

La politique précédente retournait vrai pour tout appareil Xiaomi
ou Redmi dont le modèle contenait « Note 14 » avec une indication
HyperOS, et pour tout Tecno affichant HiOS. Ses propres tests
validaient ces entrées génériques. La preuve terrain du 18 octobre
2026 portait pourtant sur des appareils observés, pas sur leurs
familles ni sur toutes leurs versions système.

### Correction

`RomPolicy` utilise maintenant une empreinte Build complète :
fabricant, marque, modèle, affichage et version incrémentale. Une
entrée ne correspond que si les cinq valeurs sont exactement égales.
Chaque entrée sépare aussi les deux preuves : `zeroProvesSent` pour
l'émission et `zeroProvesDelivered` pour la livraison.

Le Redmi Note 14 de SmokeSama a été capturé par adb le 24 octobre
2026 : Xiaomi, Redmi, modèle 24117RN76G, affichage
BP2A.250605.031.A3 et version incrémentale OS3.0.302.0.WOGMIDC.
Son modèle avait déjà été observé le 19 octobre 2026. L'entrée
exacte est donc inscrite, mais ses deux drapeaux restent à faux :
les preuves émission et livraison par observation des accusés sur
cet appareil restent à établir pendant la recette. Aucune entrée
Tecno n'est inscrite, faute d'empreinte capturée.

Conséquence immédiate et assumée : le code 0 devient « non
confirmé » sur les deux téléphones de SmokeSama, comme sur les
autres, jusqu'à une preuve séparée et documentée. Le code -1
continue de confirmer l'émission et la livraison.

L'écran Diagnostic affiche désormais le fabricant, la marque, le
modèle, l'affichage et la version incrémentale de Build, afin que
la future capture ne repose pas sur des valeurs reconstruites.

### Leçon

Ce qui était faux : une observation sur un appareil avait été
généralisée à une famille de modèles et à des générations de ROM.
Pourquoi : la politique cherchait des traits reconnaissables au
lieu de conserver l'identité exacte de l'appareil observé.
Correction : correspondance exacte et drapeaux séparés par type
d'accusé. Réflexe à garder : sans empreinte complète et observation
datée pour le type d'accusé concerné, le résultat reste non
confirmé ; ne jamais transformer une compatibilité plausible en
succès.

## 2. Le fournisseur suit l'état réellement persisté

### Preuve du défaut

Dans `updateMessageStatus`, le retour de l'écriture Room
conditionnelle était ignoré. La fonction écrivait ensuite dans le
fournisseur SMS le statut demandé, même si Room venait de refuser
la transition parce qu'un statut terminal était déjà persisté.
L'interface, qui lit Room, et le fournisseur pouvaient donc
raconter deux états différents pour le même message.

### Correction

La décision a été extraite dans `MessageStatusSync`, avec deux
ports : le magasin local tente la transition conditionnelle et se
laisse relire ; le fournisseur reçoit uniquement le statut relui.
Si la transition est refusée, le fournisseur reçoit l'état
persisté, pas l'état demandé. Si le message a disparu, aucune
écriture fournisseur n'est tentée. `SmsRepository` branche cette
unité sur Room et le ContentResolver sous le Mutex existant ;
`SmsSender` passe aussi son chemin d'exception par cette
synchronisation.

Les tests JVM de `MessageStatusSyncTest` utilisent des faux en
mémoire et exécutent le comportement : terminal DELIVERED puis
tentative SENT, terminal FAILED puis tentative SENT, transition
normale, callbacks inversés/tardifs et doublon, échec partiel
multipart suivi d'un calcul tardif, et message absent. Ils
vérifient à la fois l'état local final et la suite exacte des
écritures fournisseur.

### Leçon

Ce qui était faux : le code considérait une demande de transition
comme un résultat, alors que l'écriture conditionnelle peut la
refuser à juste titre. Pourquoi : le retour du DAO n'était pas
utilisé et la copie fournisseur était calculée depuis le paramètre
d'entrée. Correction : relire la source de vérité après chaque
tentative et aligner toutes les copies sur cette relecture.
Réflexe à garder : après une écriture conditionnelle, tester le
cas refusé et vérifier toutes les représentations de l'état, pas
seulement la base locale.

## 3. Niveaux de preuve et limites

### Résultats au code final

Commande exécutée avec le JDK 21 et le Gradle local, en mode
hors ligne : `testDebugUnitTest assembleDebug`. Résultat :
BUILD SUCCESSFUL ; comptes lus dans les rapports XML :
117 tests, 0 ignoré, 0 échec, 0 erreur. Les nouveaux tests ciblés
comptent 9 tests `RomPolicyTest`, 4 tests `SentVerdictTest` et
6 tests `MessageStatusSyncTest`, tous réussis. APK debug produit :
11 501 814 octets, non installé.

Preuve que les tests de synchronisation détectent l'ancien
défaut : en remplaçant temporairement, uniquement pour ce
contrôle, le statut persisté envoyé au fournisseur par le statut
demandé, 4 des 6 tests de `MessageStatusSyncTest` ont échoué.
Le code corrigé a ensuite été restauré avant la suite finale
réussie ci-dessus.

Exécuté en JVM dans cet environnement :

- la politique d'empreinte exacte et les verdicts du code 0 ;
- le comportement de `MessageStatusSync` avec magasin et
  fournisseur factices en mémoire ;
- toute la suite unitaire du projet, dont les anciens contrôles.

Contrôles secondaires conservés, à ne pas confondre avec des
preuves de comportement : `DaoQueryContractTest` lit les requêtes
déclarées dans le code des DAO, et `MigrationExecutionTest`
enregistre le SQL émis par les migrations sans l'exécuter dans
SQLite. Ils restent utiles comme filets de cohérence, mais ne
remplacent ni les tests JVM ci-dessus ni les tests Room réels.

Préparé mais non exécuté ici : `app/src/androidTest` contient des
tests Room en mémoire pour `resolveByToken`,
`markAmbiguousIfStillSending`, les transitions terminales des
messages et les segments, ainsi qu'une migration 10 vers 11 avec
`MigrationTestHelper` et conservation d'une programmation. Le
workflow `.github/workflows/android-ci.yml` exécute les tests
unitaires, le lint et `assembleDebug`, puis lance
`connectedDebugAndroidTest` sur un émulateur API 35 x86_64.

Non exécuté ici : les tests instrumentés et le lint.
`compileDebugAndroidTestKotlin` a été tenté et échoue dès
`processDebugAndroidTestManifest` : androidx.room:room-testing
2.7.0 et androidx.test.ext:junit 1.2.1 sont absents du cache
Gradle hors ligne. `lintDebug` a aussi été tenté et échoue avant
le lint, dans `generateDebugAndroidTestLintModel`, sur la même
résolution d'artefacts. Aucun appareil ou émulateur n'est
disponible dans cet environnement. La compilation et l'exécution
des tests instrumentés sont donc confiées à la CI. Sur appareil,
il restera
à valider les accusés tardifs, le multipart, le redémarrage, la
migration par mise à jour et la capture des preuves séparées du
code 0 avant d'activer un drapeau de l'empreinte Redmi.

Aucune mise à jour du graphe de parcours n'est requise : il décrit
déjà la PR nº 2 comme correction P0 en cours avant les tests de
comportement, la recette appareil et la fusion après revue.
