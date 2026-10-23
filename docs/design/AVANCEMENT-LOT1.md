# Avancement du lot 1 : design system et thèmes

Date : 23 octobre 2026. Branche : `codex_lot1-design-system-themes`,
base `claude/reprise` (PR nº 2, tête `bcc9f94`) plus le commit
documentation de la PR nº 3 (`fbd57f3`, repris en `c761611`).
Aucune fusion, aucune publication : travail local en attente de relecture.

## Problème corrigé

L'accueil et plusieurs écrans imposaient les couleurs Radiant Slate
en dur. Choisir un autre thème, le mode clair ou Material You ne
changeait donc pas ces écrans. Le mode clair de Radiant était en
outre inversé (fond et texte échangés). Les SIM étaient signalées
par des couleurs vives (rouge, orange) sans libellé suffisant.
Material You était actif par défaut et masquait la palette choisie.
Il n'existait pas de destination Automatisations dans la navigation.

## Changements par fichier

Thèmes :

- `theme/TindaThemes.kt` : la fabrique `tindaScheme()` définit
  maintenant explicitement tous les rôles Material 3 utilisés :
  background, surface et variantes de conteneurs (Lowest à Highest),
  onBackground, onSurface, surfaceVariant, onSurfaceVariant,
  outline, outlineVariant, primary et conteneurs, secondary et
  conteneurs, tertiary, error et conteneurs, inverse, scrim.
  Les neutres sont des ardoises teintées par le fond de chaque thème.
  Accents conformes à la maquette (THEMES.md), notamment Radiant
  clair `#00836F` et sombre `#00D2B4`.
- `theme/Color.kt` : constantes du Radiant clair corrigé (fond
  ardoise très léger `#F3F5F7`, texte foncé, conteneurs) et des
  conteneurs d'erreur partagés.
- Radiant clair corrigé : fond clair sobre et lisible, texte foncé ;
  le sombre d'origine est inchangé. Les sept identifiants persistés
  (`radiant`, `lagoon`, `sahel`, `fleuve`, `baobab`, `nuit`,
  `orchidee`) et `DEFAULT_THEME_ID=radiant` sont inchangés.
- `theme/Theme.kt` : commentaire corrigé (sept thèmes) et paramètre
  `dynamicColor` par défaut à false.

Material You distinct de la palette fixe :

- `data/PrefsRepository.kt` : fonction pure `resolveDynamicColor`
  (absent = false, valeur enregistrée respectée) ; le flux
  `dynamicColor` l'utilise. Les clés DataStore (`theme_id`,
  `theme_mode`, `dynamic_color`) ne changent pas : les préférences
  existantes sont conservées après mise à jour et redémarrage.
- `MainActivity.kt` et `SettingsScreen.kt` : valeur initiale du
  flux à false.
- `SettingsViewModel.setTheme()` : choisir un thème fixe enregistre
  aussi `setDynamicColor(false)`. L'identifiant du thème fixe reste
  mémorisé pour revenir en arrière.
- Réglages, section Apparence : libellé « Couleurs du téléphone »
  avec explication (chaînes `settings_dynamic_color_phone` et
  `settings_dynamic_color_hint`, FR et EN).

Composants partagés (`ui/components/TindaComponents.kt`, nouveau) :

- `TindaAvatar` : cercle neutre `surfaceContainerHigh`, initiales,
  taille uniforme 48 dp par défaut ; `tindaInitials` partagée.
- `SimBadge` : fond `surfaceVariant`, texte `onSurfaceVariant`,
  libellé explicite (« SIM 1 Nom »), jamais la couleur seule.
- `TindaSectionTitle` et `TindaSettingsRow` : versions des anciens
  composants Radiant des Réglages, basées sur MaterialTheme.
- `TindaBottomBar` : barre basse Messages / Automatisations /
  Réglages (chaînes `nav_messages`, `nav_automations`, `nav_settings`).

Navigation :

- `navigation/Routes.kt` : route `AUTOMATIONS`.
- `ui/automations/AutomationsScreen.kt` (nouveau) : écran hôte qui
  liste et ouvre les écrans existants Programmés, Répondeur,
  Transfert, Envoi groupé, Suppression auto, sans toucher aux moteurs.
- `navigation/TindaNavGraph.kt` : décision navigation. Un Scaffold
  extérieur porte la barre basse, visible seulement sur les trois
  destinations principales, et transmet son `innerPadding` au
  NavHost. Choix retenu plutôt qu'une barre par écran : une seule
  implémentation, et les Scaffold/FAB internes restent au dessus de
  la barre grâce au padding transmis. Navigation entre destinations
  avec `launchSingleTop`, `saveState`/`restoreState`, `popUpTo`
  Conversations seulement si elle est dans la pile (démarrages par
  notification ou onboarding protégés).
- Réglages : les entrées d'automatisation restent aussi présentes,
  aucun lien existant n'est retiré.

Écrans migrés hors Radiant codé en dur :

- `ui/conversations/ConversationListScreen.kt` : toutes les couleurs
  passent par `MaterialTheme.colorScheme`. Avatar remplacé par
  `TindaAvatar`, point SIM coloré remplacé par `SimBadge` explicite,
  liseré SIM superposé conservé (correctif L4 : il n'ajoute aucune
  hauteur) mais en `outlineVariant` neutre. Chips de filtre SIM sans
  point coloré. `simColorFor` supprimé.
- `ui/settings/SettingsScreen.kt` : migration complète, sections et
  lignes remplacées par `TindaSectionTitle`/`TindaSettingsRow`,
  chip renommée `TindaChip`, carte Double SIM à bordures et
  pastilles neutres avec libellés « SIM n ».
- `ui/conversation/ConversationDetailScreen.kt` : migration
  complète. Bulle sortante en aplat de l'accent (texte `onPrimary`),
  bordure SIM neutre `outlineVariant` (booléen de présence au lieu
  d'une couleur par SIM), pastilles SIM du compositeur neutres avec
  libellé « n Nom », accent rouge de la carte OTP remplacé par un
  neutre, textes sur boutons en `onPrimary`.
- `ui/newmessage/NewConversationScreen.kt` : migration complète,
  pastilles SIM neutres avec libellé « SIM n nom », couleurs de champ
  renommées `tindaTextFieldColors`.
- `ui/conversations/ConversationListViewModel.kt` : commentaire
  « Radiant Slate » retiré.

Résultat vérifiable : `grep -rn "Radiant" app/src/main/java
--include=*.kt` ne trouve plus aucun usage hors `theme/` (les
définitions de la palette du thème radiant, `theme/Color.kt` et
`theme/TindaThemes.kt`). Les autres écrans (Vault, AutoReply,
Forward, AutoDelete, Scheduled, Blocklist, Diagnostic, Onboarding,
GroupSend) n'utilisaient déjà aucune couleur Radiant : vérifié par
le même grep avant migration.

Chaînes et documentation :

- `res/values/strings.xml` et `res/values-fr/strings.xml` : huit
  nouvelles chaînes (navigation, automatisations, Material You),
  toutes en FR et EN, utilisées via `stringResource`.
- `README.md` et `theme/Theme.kt` : « six thèmes » corrigé en
  « sept thèmes ». Le document historique
  `docs/validation/matrice-fonctionnalites.md` (état au 21 octobre)
  n'est pas réécrit : il décrit l'état d'alors.

## Vérifié

Commandes exécutées dans l'environnement du dépôt (JDK 21 local,
Gradle 8.14.3 local, `--offline`, voir `docs/validation/fiche-build-l0.md`) :

- `./gradlew testDebugUnitTest` (équivalent local documenté) :
  BUILD SUCCESSFUL. 101 tests, 0 échec, 0 erreur, 0 ignoré
  (base PR nº 2 : 95). Nouveaux : `TindaThemesTest` passe de 6 à 9
  tests (rôles explicites et distincts clair/sombre pour les sept
  thèmes, contraste `onPrimary`/`primary` et
  `onSurface`/`background` au dessus de 4,5 en clair et sombre pour
  les sept thèmes, calcul WCAG) et `ResolveDynamicColorTest` (3
  tests : absent = false, true respecté, false respecté). Le test
  existant thème inconnu vers radiant est conservé.
- `./gradlew assembleDebug` : BUILD SUCCESSFUL, APK debug régénéré
  (`app/build/outputs/apk/debug/app-debug.apk`, 15 924 097 octets).
  Non installé sur un téléphone.
- `./gradlew lintDebug` : non exécutable dans cet environnement,
  avant comme après ce lot : `com.android.tools.lint:lint-gradle:
  31.10.1` est absent du cache hors ligne (même constat que la
  fiche L0). Ce n'est pas un échec du code du lot.

## Ce qui reste à faire

- Validation visuelle sur appareil : aucune capture Android réelle
  n'est fournie. Sans téléphone ni émulateur dans cet environnement,
  restent à vérifier sur appareil : les sept thèmes en clair et
  sombre (accueil, conversation, rédaction et clavier, dialogues,
  erreurs, coffre), le mode système, Material You sur Android 12+,
  le redémarrage avec conservation des préférences, la police
  agrandie (200 %), TalkBack et la barre basse sur les trois
  destinations, FAB compris.
- Le fond des bulles entrantes et certains états désactivés suivent
  les rôles Material par construction, mais leur rendu réel n'est
  pas prouvé sans captures (revue Fixit à faire sur captures).
- Lots suivants du guide (conversations, diffusions, contacts,
  confidentialité, finition) : non commencés, conformément à la
  consigne de terminer et vérifier le lot 1 avant la suite.
- Aucune installation sur téléphone, aucune fusion, aucune release.
