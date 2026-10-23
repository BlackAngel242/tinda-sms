# Prototype de référence Tinda

Cette maquette React est une référence interactive indépendante de l’application Kotlin. Elle ne doit pas être embarquée dans l’APK ni servir de couche de sécurité. Lire ../../docs/design/INTEGRATION-ANDROID.md avant une adaptation native.

UI dans src/Prototype.tsx et src/prototype.css ; règles pures dans src/domain.ts. Préserver src/mobile et le runtime de téléphone simulé, vérifier npm run check:runtime puis npm run build et node --test tests/domain.test.mjs. Ne pas copier le cadre Pixel, le clavier simulé ni les données fictives dans Android. Toutes les interactions sont des simulations réinitialisées au rechargement.

Accueil sobre, avatars ardoise uniformes, badges SIM neutres, un seul accent. Sept thèmes et modes clair/sombre/système ; Material You reste une préférence simulée dans la maquette.
