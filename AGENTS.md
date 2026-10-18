# AGENTS.md — Tinda SMS

Conventions pour tout agent travaillant sur ce dépôt.

## Code
- Kotlin uniquement, style officiel (`kotlin.code.style=official`).
- UI : Jetpack Compose + Material 3, design system dans `theme/`.
- Identifiants (classes, fonctions, variables) en **anglais**.
- Chaînes affichées à l'utilisateur en **français** (`values-fr/`) + anglais (`values/`).
- Commentaires de code en français, concis.

## Interdits
- **Jamais** de contenu SMS, de numéro de téléphone ou de PIN dans les logs, les commits ou les messages.
- **Jamais** de code, d'asset ou de nom repris de YAATA ou d'une autre app existante.
- Pas de tirets cadratins dans le code, les docs ni les réponses.
- Pas de `StrictMode` en production (debug uniquement, déjà en place dans `TindaApp`).
- Pas de clé API, de mot de passe ni de secret dans le repo.

## Méthode
- Vérifier avant d'annoncer : un build qui passe se prouve par `./gradlew assembleDebug`, pas par supposition.
- Respecter le PRD (`~/workspace/your_files/prd-tinda-sms.md`) : décisions DEC-001 à DEC-010, règles RB-01 à RB-06.
- `android:exported` toujours explicite sur chaque composant (obligatoire API 31+).
- Toute fonction « sensible » (coffre, faux SMS, bouclier) repasse par la lentille sécurité avant d'entrer dans un lot.
