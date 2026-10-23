# Tinda SMS : prototype interactif

Design original fondé sur la proposition 2 choisie par DrSmoke, avec un accueil ardoise/turquoise épuré.

Le fichier `Tinda-SMS.html` livré à côté du dossier source contient le prototype autonome et ses assets intégrés. L'ouvrir dans un navigateur moderne. Il ne lit ni n'envoie de vrais SMS. Les données et les réglages sont fictifs et restent dans la session.

Pour modifier les sources :

```sh
npm ci
npm run dev -- --host 0.0.0.0 --port 4173
```

`npm run build` vérifie TypeScript et l'intégrité du runtime mobile, puis prépare la version statique.

Fonctions présentées : conversations, recherche, filtres SIM/non-lus/favoris, rédaction, envoi groupé, programmation, répondeur, transfert, suppression automatique, thèmes et lecture, sons, choix SIM, blocage, archives, gestes, diagnostic, coffre et liste jaune, accueil de premier lancement. Le bouclier anti-arnaque est identifié comme prévu.

Le coffre et la biométrie sont des démonstrations sans protection réelle. La langue anglaise est un aperçu partiel. Les gestes se configurent ici ; leur réalisation native reste à vérifier dans l'application Android.

Aucun fichier du dépôt Android BlackAngel242/tinda-sms n'a été modifié. Le prototype ne remplace pas sa base Kotlin/Compose.

Voir `DESIGN-NOTES.md` pour les arbitrages PRD et `design-qa.md` pour la revue Product Design/Fixit. Les captures citées dans le rapport sont des preuves temporaires de travail, non incluses dans cette archive.


## Extension des besoins utilisateurs

Ajouts : listes de diffusion modifiables, personnalisation par variables, aperçu individuel, exclusions, historique par destinataire et relance des seuls échecs avec texte et SIM d'origine. Modèles, étiquettes, notes privées séparées, suivi manuel et rappel fictif sur la fiche contact. Détails SMS au clic ou appui long. Import volontaire d'une image locale, aperçu et choix de portée ; aucune connexion WhatsApp/Messenger.

Les règles de confidentialité combinent contact ET mot-clé et proposent coffre à réception, phrase de lecture fictive, coffre ou suppression après lecture et option conserver. Le bac à sable permet de simuler réception, autorisation et fermeture ; il ne sécurise pas de vrais messages. Les phrases de démonstration sont en mémoire en clair. Aucun secret réel ne doit y être saisi.

Toutes les données restent dans la session : recharger réinitialise la démo. SMS réseau, accusés réels, programmation en arrière-plan, carnet Android, biométrie et chiffrement nécessitent une implémentation native ultérieure. Les photos privées dans les caches d'autres applications ne sont pas accessibles à une application Android ordinaire par simple connaissance du chemin. Un helper d'accessibilité ne contourne pas le sandbox ni les fenêtres sécurisées.
