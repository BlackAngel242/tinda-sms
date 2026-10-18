# Tinda SMS

Nouvelle application SMS Android, 100 % originale — **DMAM PRO TECH**.
SMS uniquement (pas de MMS). Min Android 8.0 (API 26), cible Android 16 (API 36).

> Spec complète : `~/workspace/your_files/prd-tinda-sms.md` (table ronde, décisions DEC-001 à DEC-010).

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
  theme/                 # Design system Material 3 original « Tinda »
  navigation/            # Routes + NavGraph
  ui/conversations/      # Liste des conversations (lot 1 : données réelles)
  ui/conversation/       # Fil de discussion (lot 1)
  ui/settings/           # Paramètres (lots suivants)
  sms/                   # Receivers + activités du rôle « default SMS app »
  data/local/            # Room + chiffrement du coffre (SQLCipher, lot 4)
  util/                  # Logger sans PII
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
