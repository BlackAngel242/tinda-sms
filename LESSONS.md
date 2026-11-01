# LESSONS.md : leçons mémorisées entre sessions IA

> Toute IA lit ce fichier avant de travailler. Plus récent en haut.

## 2026-11-01 : distinguer maquette, code natif et preuve appareil

- Une maquette HTML peut être valide tout en divergeant de l'APK Android : préciser la source et le commit de chaque conclusion.
- L'absence de capture Android récente impose la mention « vérification visuelle non effectuée » ; une revue du code ne prouve pas un défaut visible.
- Deux références documentaires peuvent être en tension (navigation à trois vs quatre onglets). Ne pas choisir silencieusement une refonte contraire au design scellé ; demander un arbitrage produit.


## 2026-10-29 : actions destructives et branches fusionnées en squash

1. **Commande destructive proposée sans vérifier l'état.** Un `git checkout -- .` aurait effacé des modifications non commitées. Règle : `git status` d'abord ; commiter ou sauvegarder, ne jamais écraser.
2. **Force-push refusé.** Cause : une branche fusionnée en squash n'est plus un ancêtre de la branche principale. Règle : ne jamais forcer ; fusionner la branche principale dans la branche de travail, puis pousser en avance rapide.
3. **Lien `claude.ai/code` dans une PR.** Il fait tourner des workflows d'auto-fusion sur d'autres dépôts. Règle : ne pas le mettre.
4. **« Vérifié » sans preuve.** Règle : écrire « non vérifié » tant que la vérification n'a pas été faite.
