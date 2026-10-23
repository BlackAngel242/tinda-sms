# Thèmes : direction et état réel

## Verdict

La direction visuelle et les sept choix sont cohérents. Le portage Android n'est pas encore validé pour tous les écrans. La maquette change l'accent sur des surfaces communes ; les palettes natives peuvent conserver leurs nuances propres si le résultat garde le même calme visuel et des contrastes suffisants.

| Libellé | Identifiant Android existant | Accent maquette sombre | Accent maquette clair | Contraste texte bouton sombre / clair |
|---|---|---|---|---|
| Radiant | `radiant` | `#00D2B4` | `#00836F` | 6,78 / 4,69 |
| Lagune | `lagoon` | `#8FD8C2` | `#0E7C66` | 7,96 / 5,13 |
| Sahel | `sahel` | `#FFB690` | `#A34A1A` | 7,69 / 5,91 |
| Fleuve | `fleuve` | `#A3C9FF` | `#2E6DA4` | 7,70 / 5,47 |
| Baobab | `baobab` | `#E8B77D` | `#7A4E2B` | 7,16 / 7,11 |
| Nuit | `nuit` | `#BEC2FF` | `#4E5BA6` | 7,69 / 6,23 |
| Orchidée | `orchidee` | `#FFB1C8` | `#8E4A6B` | 7,71 / 6,28 |

Calcul sRGB sur aplats : texte `#00382E` sur l'accent sombre, blanc sur l'accent clair. Tous ces couples dépassent 4,5:1. Ce contrôle de tokens n'est pas un audit complet des composants, opacités, états désactivés ou couleurs natives. Les champs, sous-textes, dialogues et notifications doivent aussi être vérifiés sur Android.

## Écarts relevés dans le code actuel

Au commit `dfddee719be55b4c71fd49cef7f26e92b1bed986` :

1. `theme/TindaThemes.kt` définit bien sept thèmes ; `TindaThemesTest.kt` teste déjà leur nombre, unicité et modes distincts. Les commentaires et le README parlent parfois encore de six thèmes. Les corriger lors du lot 1.
2. `ui/conversations/ConversationListScreen.kt` utilise directement `RadiantBg`, `RadiantText`, `RadiantPrimary`, etc. Un thème choisi via MaterialTheme ne peut pas remplacer ces valeurs. C'est le premier point à corriger pour que les sept thèmes et le mode clair s'appliquent vraiment à l'accueil.
3. `theme/Color.kt` définit `RadiantSim1` rouge et `RadiantSim2` orange ; l'accueil les utilise. Le design retenu demande des badges neutres et des libellés SIM explicites.
4. `data/PrefsRepository.kt` retourne `dynamicColor = true` en l'absence de préférence, et `theme/Theme.kt` donne priorité aux couleurs dynamiques Android 12+. La palette utilisateur peut donc être remplacée par celle du fond d'écran.
5. Certains rôles Material 3 ne sont pas explicitement définis dans les palettes. Auditer notamment `surfaceVariant`, `onSurfaceVariant`, les containers et les outlines en clair avant d'affirmer que tous les états sont prêts.

## Décision d'intégration proposée

- Conserver les sept identifiants persistés, sans réinitialiser les choix existants.
- Pour les nouveaux profils sans préférence, Radiant + mode système + couleurs dynamiques désactivées. Ne pas écraser une préférence Material You déjà choisie.
- Choisir une palette fixe désactive Material You explicitement. L'activer affiche « Couleurs du téléphone » et explique que cette source remplace la palette fixe. Garder le dernier thème fixe pour pouvoir y revenir.
- La maquette mémorise l'interrupteur Material You mais ne lit pas la palette Android : ne pas l'utiliser comme preuve d'intégration de cette fonction.
- Définir les rôles `background`, `surface`, `surfaceContainer`, `onSurface`, `onSurfaceVariant`, `outlineVariant`, `primary` et `onPrimary`. Si un rôle supplémentaire est nécessaire pour les avatars ou badges, créer une petite extension de thème partagée ; éviter les hexadécimaux dispersés dans les écrans.
- Garder avatars et badges neutres dans chaque thème. Une couleur d'état ne doit jamais être le seul signal : texte ou icône l'accompagne.

## Recette native avant validation

Pour chacun des sept thèmes : accueil, conversation, rédaction/clavier, Automatisations, Réglages, dialogues, coffre et messages d'erreur en clair puis sombre. Tester le changement en direct, la restauration après redémarrage, le mode système, la police à 200 %, TalkBack, les labels et les cibles tactiles. Tester Material You sur Android 12+ et le repli avant Android 12.

Les tests actuels de thèmes ne mesurent ni les contrastes ni le rendu des écrans. Ajouter des assertions sur les rôles pertinents et des captures ciblées ; faire relire les captures avec Fixit avant de déclarer les thèmes finalisés.
