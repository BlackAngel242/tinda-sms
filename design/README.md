# Référence visuelle et interactive Tinda SMS

Ce dossier contient la maquette choisie et ses sources. Elle guide le portage Compose ; elle n'est pas le code de l'application Android.

- [Guide d'intégration Android et consigne Muse](../docs/design/INTEGRATION-ANDROID.md)
- [Thèmes et corrections à effectuer](../docs/design/THEMES.md)
- [Six profils et besoins](PROFILS-ET-BESOINS.md)
- [Maquette autonome](preview/Tinda-SMS.html) : télécharger ce fichier depuis GitHub puis l'ouvrir dans un navigateur. GitHub affiche le source HTML, pas l'application interactive.
- [Sources React modifiables](prototype/) et [rapport de validation](prototype/design-qa.md).

Pour développer la référence indépendamment de l'APK :

```sh
cd design/prototype
npm ci
npm run dev -- --host 0.0.0.0
```

Vérifications :

```sh
npm run check:runtime
npm run build
node --test tests/domain.test.mjs
node scripts/export-preview.mjs
```

La maquette inclut sept thèmes, clair/sombre/système, conversations, listes, modèles, suivi par destinataire et parcours de confidentialité. Toutes les données sont fictives et se réinitialisent au rechargement. Les SMS, accusés, permissions, programmation, coffre, biométrie et écriture du carnet y sont simulés. L'import d'une image locale permet uniquement un aperçu dans la session. Aucun helper WhatsApp/Messenger n'est connecté.

L'accueil retenu utilise une palette ardoise et un seul accent ; avatars et badges SIM restent calmes. Les variantes de thème gardent la même structure.

Le manifeste fixe le commit Android examiné et l'empreinte du HTML. Les sources et la maquette sont un instantané de référence ; après modification des sources, reconstruire et réexporter le fichier autonome avant de les présenter comme synchronisés. Les ressources de cadre mobile et de clavier servent uniquement à la démonstration web.
