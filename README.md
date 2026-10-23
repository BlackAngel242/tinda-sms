# Tinda SMS

Nouvelle application SMS Android, 100 % originale — **DMAM PRO TECH**.
SMS uniquement (pas de MMS). Min Android 8.0 (API 26), cible Android 16 (API 36).

> Spec complète : [docs/reference/prd-tinda-sms.md](docs/reference/prd-tinda-sms.md) (table ronde, décisions DEC-001 à DEC-010).
> État réel des fonctionnalités : [docs/validation/matrice-fonctionnalites.md](docs/validation/matrice-fonctionnalites.md). Plan de reprise en cours : PR nº 1 du dépôt.

État au 21 octobre 2026 : lots 0 à 4 codés (socle, téléphonie, recherche/thèmes/gestes/liste noire, automatismes F9/F10/F11/F14, coffre secret et suppression automatique), design « Radiant Slate » appliqué à la liste, au fil, aux paramètres et au nouveau message. Le bouclier anti-arnaque (lot 5) n'est pas codé : il attend son corpus de validation.

## Stack

| Couche | Choix |
|---|---|
| Langage | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Données | Room (+ SQLCipher pour le coffre, lot 4) |
| Tâches différées | WorkManager |
| DI | Hilt |
| Préférences | DataStore |
| Navigation | Navigation Compose |
| Build | Gradle Kotlin DSL, R8, App Bundle |

## Prérequis

- JDK 17+ (`JAVA_HOME` défini)
- Android SDK : `platform-tools`, `platforms;android-36`, `build-tools;36.0.0`
- Fichier `local.properties` à la racine avec `sdk.dir=/chemin/vers/android-sdk` (non versionné)

## Builder

```bash
./gradlew assembleDebug        # APK debug -> app/build/outputs/apk/debug/
./gradlew assembleRelease      # release (minifié R8)
```

## Structure

```
app/src/main/java/tech/dmampro/tinda/
  TindaApp.kt            # Application (Hilt, StrictMode en debug uniquement)
  MainActivity.kt        # Hôte Compose + navigation
  theme/                 # Design system Material 3 original « Tinda » + Radiant Slate
  navigation/            # Routes + NavGraph
  ui/conversations/      # Liste des conversations
  ui/conversation/       # Fil de discussion
  ui/newmessage/         # Nouveau message
  ui/groupsend/          # Envoi groupé (F14)
  ui/schedule/           # SMS programmés (F9), ui/autoreply/ (F10), ui/forward/ (F11)
  ui/autodelete/         # Suppression automatique, ui/vault/ coffre secret
  ui/settings/           # Paramètres : thèmes, gestes, son, liste noire, diagnostic
  sms/                   # Receivers, envoi, workers + activités du rôle « default SMS app »
  data/                  # Repositories (messages, coffre, blocage, statistiques)
  data/local/            # Room (base principale v10) + coffre SQLCipher séparé
  notifications/         # Notifications messages + notification déguisée du coffre
  util/                  # Logger sans PII, journal d'événements, détection OTP
```

## Règles d'or

- Aucun code ni asset repris d'une autre app : tout est original.
- Jamais de contenu SMS ni de numéro dans les logs ou les commits.
- Tout ce qui touche au coffre-fort ou aux faux SMS ne va **jamais** dans le provider système (RB-01 / RB-03 du PRD).

## Tests terrain (lot 1) — checklist pour SmokeSama

Pas d'appareil réel dans l'environnement de build : ces vérifications se font
sur un vrai téléphone (ROM Tecno / Infinix / Itel recommandée).

1. **Installation** : installer l'APK debug ; l'onboarding en 2 écrans s'affiche.
2. **Rôle SMS par défaut** : accepter via le dialogue système ; vérifier que
   l'app apparaît comme « application SMS » dans les paramètres Android.
3. **Envoi / réception** : envoyer un SMS vers son propre numéro ; vérifier
   l'envoi, la réception, la notification et l'accusé « livré ».
4. **Double SIM** : changer de SIM dans un fil (badge) ; vérifier que la SIM
   affichée est bien celle utilisée pour l'envoi.
5. **Refus du rôle** : refuser le dialogue ; vérifier l'écran d'explication et
   que l'app ne bloque pas la visite.
6. **Non-régression** : l'app SMS système ne doit pas créer de doublons
   (Tinda écrit via le provider, le système reflète).
7. **Brouillon** : commencer un message, quitter le fil, revenir : le texte
   est toujours là.
8. **Message long** : envoyer un texte de 200 caractères ; vérifier le
   compteur « 2 segments » et la bonne réception en un seul SMS.

## Tests terrain (lot 2) — checklist pour SmokeSama

1. **Recherche** : taper un mot dans la barre de recherche ; vérifier les
   résultats avec passages surlignés ; ouvrir un résultat (bon fil affiché).
2. **Thèmes** : Paramètres → choisir chacun des 6 thèmes ; vérifier
   l'aperçu instantané ; basculer clair/sombre/système.
3. **Gestes** : balayer une conversation à droite (répondre par défaut) et à
   gauche (archiver par défaut) ; changer les actions dans Paramètres →
   Options avancées → Gestes ; vérifier l'archive puis le désarchivage.
4. **Son d'envoi** : envoyer un SMS ; entendre le carillon ; le couper dans
   les paramètres ; régler le volume.
5. **Liste noire** : Paramètres → Liste noire → bloquer son propre deuxième
   numéro ; s'envoyer un SMS depuis ce numéro ; vérifier l'absence de
   notification et la ligne dans le journal ; débloquer.
6. **Suppression** : configurer le geste « supprimer », balayer, confirmer ;
   vérifier la disparition du fil.
7. **Langue** : passer l'interface en anglais puis revenir au français
   (Android 13+ ; en dessous, suit le système).

## Nouvelle référence d’interface

La [maquette interactive et ses sources](design/README.md) sont disponibles pour préparer le portage Compose. Le [guide d’intégration](docs/design/INTEGRATION-ANDROID.md) relie chaque parcours au code existant ; la [note sur les thèmes](docs/design/THEMES.md) précise les corrections avant validation Android. Ces fichiers ne modifient pas le comportement de l’APK.
