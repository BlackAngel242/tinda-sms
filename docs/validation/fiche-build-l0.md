# Fiche de build L0 : référence reproductible

Date : 21 octobre 2026. Branche : `claude/reprise`, créée depuis `master`.
SHA de référence : `dfddee719be55b4c71fd49cef7f26e92b1bed986` (identique à `origin/master`, vérifié par `git ls-remote`).

## Environnement

- JDK 21 (fourni localement), Gradle 8.14.3 local, mode `--offline`. `./gradlew` est inutilisable hors ligne ; l'équivalence est consignée ici comme le demande le plan.
- Android SDK local (`local.properties`, hors git). Signature : keystore debug permanent du projet (alias `tinda-debug`, RSA 2048), fichiers hors git et préservés.
- Limite connue : pas de KVM, pas d'émulateur ; aucun test instrumenté ne peut être exécuté dans cet environnement.

## Commandes exécutées et résultats exacts

Commande de référence (historique du projet) :

```
GRADLE_USER_HOME=/home/hatch/.gradle JDK_JAVA_OPTIONS="-Djava.net.preferIPv4Stack=true" ~/workspace/.tools/gradle-8.14.3/bin/gradle testDebugUnitTest assembleDebug --offline
```

- `testDebugUnitTest` : **exécuté le 21 octobre 2026** après suppression des résultats précédents (les rapports du 19 octobre, produits sur des sources identiques, ne suffisaient pas comme preuve du jour). Résultat : **57 tests, 0 échec, 0 erreur, 0 ignoré**, en 9 classes : FtsMatchTest 9, ScheduledStatusTest 2, AutoDeleteSelectionTest 5, AutoReplyMatchingTest 6, ForwardMatchingTest 7, TindaThemesTest 5, EventLogTest 3, OtpDetectionTest 8, SmsEncodingTest 12. Comptes lus dans les rapports XML générés par l'exécution, pas déclarés.
- `assembleDebug` : succès, tâche à jour sur des sources inchangées (l'APK présent est celui du même code source).
- `lintDebug` : **non exécutable dans cet environnement**. L'exécution forcée échoue à la résolution : `com.android.tools.lint:lint-gradle:31.10.1` est absent du cache hors ligne. Constat d'environnement, pas échec du code. À réessayer si le cache est alimenté, ou via la CI si elle est décidée.

## APK de référence

- Fichier : `app/build/outputs/apk/debug/app-debug.apk`, environ 15,9 Mo.
- versionName `0.1.0`, versionCode `1` (inchangés depuis le lot 0).
- Dernier APK dont l'installation sur téléphone est prouvée : build du commit `0242b6a` (19 octobre, captures). Aucun APK du présent lot n'est installé : l'accès adb est coupé et l'installation attend le feu vert de SmokeSama.

## Proposition de version (à valider)

- Prochain APK distribué : versionCode `2` (strictement croissant), versionName `0.2.0`.
- Chaque fiche de build ultérieure consignera : SHA du commit, versionCode, versionName, date, résultats de tests exécutés et statut d'installation. Ne pas rebaptiser les anciens APK.

## Écarts et limites consignés au L0

- La matrice des fonctionnalités est dans `docs/validation/matrice-fonctionnalites.md` ; elle distingue prévu, codé, testé JVM et testé appareil, et signale les écarts PRD (récurrence et alarmes exactes F9, mot-clé F10, suppression après lecture F13, couverture de recherche F3, alerte coût RB-06).
- Le PRD est désormais accessible dans le dépôt : `docs/reference/prd-tinda-sms.md` (provenance en tête de fichier).
- Aucun test instrumenté écrit à ce stade ; les tests de migration du lot L2 seront distingués entre exécutés (JVM) et écrits mais non exécutables ici.
