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

## Correctifs revue Codex PR nº 4

Date : 23 octobre 2026. Branche `codex_lot1-design-system-themes`,
après fusion de `claude/reprise` corrigée (correctifs PR nº 2) et de
`master` (PR nº 3 fusionnée, documentation non dupliquée).
Baseline de la branche fusionnée avant ces correctifs : 116 tests
unitaires, assembleDebug OK, lintDebug non exécutable (voir plus bas).

### Problème 1 : SIM historique invisible dans les bulles

Le lot 1 avait remplacé la couleur par SIM par une bordure neutre
identique pour les deux SIM, sans libellé par message : dans un fil
qui alterne SIM 1 et SIM 2, on ne savait plus de quelle SIM chaque
message était parti. Seuls l'en-tête et le compositeur nomment une
SIM, et c'est celle du prochain envoi, pas celle des anciens
messages (commit de correction : `8c71040`).

Changements :

- `ui/components/MessageSim.kt` (nouveau) : type scellé `MessageSim`
  (`Known(slotNumber)` ou `Unknown`) et fonction pure
  `messageSimFor(subscriptionId, slotBySubscriptionId)`. Source :
  le `subscriptionId` persisté de chaque message. `-1` ou
  identifiant absent des SIM actuelles (SIM retirée ou remplacée)
  donne `Unknown`, sans numéro d'emplacement inventé.
- `ui/conversation/ConversationDetailScreen.kt` : `MessageBubble`
  reçoit `messageSim` (null pour les entrants) au lieu de
  `showSimMark` ; le pied de bulle (heure, statut) affiche un
  `SimBadge` textuel : « SIM 1 », « SIM 2 » ou « SIM inconnue »
  (chaîne `sim_unknown_label`, FR et EN). Le libellé inconnu est
  visible comme les autres, jamais masqué. La bordure latérale
  neutre est retirée : le texte porte seul l'information, la garder
  aurait été un signal redondant sans valeur. L'en-tête et le
  compositeur gardent la SIM sélectionnée pour le prochain envoi,
  c'est leur rôle.
- `ui/components/TindaComponents.kt` : `SimBadge` expose son
  libellé comme description d'accessibilité (sémantique fusionnée) :
  TalkBack lit « SIM 1 », pas une couleur.
- Vérifié par grep : aucun autre écran ne présente la SIM du
  compositeur comme celle d'anciens messages. La liste des
  conversations dérive son badge du `subscriptionId` enregistré du
  fil, pas du compositeur.

### Problème 2 : doubles insets du Scaffold extérieur

Le Scaffold extérieur de la navigation appliquait les insets
système et les transmettait au NavHost via `Modifier.padding`, qui
ne les consomme pas : les Scaffold internes les réappliquaient.
Double marge haute et basse, et espace en trop au dessus du clavier
dans le détail (commit de correction : `28cf331`).

Changements :

- `navigation/TindaNavGraph.kt` : `contentWindowInsets =
  WindowInsets(0, 0, 0, 0)` sur le Scaffold extérieur. Son
  innerPadding ne contient plus que la hauteur de la barre basse,
  présente seulement sur les trois destinations principales. La
  `NavigationBar` (Material 3) gère elle-même l'inset bas des barres
  système, la barre basse reste donc placée au dessus de la zone
  système.
- Écrans internes revus un par un : liste des conversations,
  Réglages et Automatisations appliquent leurs insets via leur
  propre Scaffold et son `innerPadding`, une seule fois. Dans le
  détail, le compositeur conserve `navigationBarsPadding()` et
  `imePadding()` : c'est désormais la seule application, le champ
  de rédaction reste au dessus du clavier (comportement d'avant le
  lot 1, validé sur le terrain, commit historique `a4951cf`).

### Vérifié

- `testDebugUnitTest` (équivalent local documenté, JDK 21, Gradle
  8.14.3, `--offline`) : BUILD SUCCESSFUL. 121 tests, 0 échec,
  0 erreur, 0 ignoré (comptes lus dans les rapports XML). Base
  fusionnée : 116 ; nouveau : `MessageSimTest`, 5 cas (emplacement
  0 donne SIM 1, emplacement 1 donne SIM 2, identifiant absent
  donne inconnue, -1 donne inconnue, aucune SIM actuelle donne
  inconnue).
- `assembleDebug` : BUILD SUCCESSFUL, APK debug régénéré
  (15 926 641 octets), non installé.
- `lintDebug` : non exécutable dans cet environnement, avant comme
  après ces correctifs : `com.android.tools.lint:lint-gradle:
  31.10.1` absent du cache hors ligne. Constat d'environnement, pas
  un échec du code.

### Non vérifié

- Validation visuelle sur appareil : NON faite, aucune capture
  Android réelle (ni appareil ni émulateur dans cet environnement).
  Restent à contrôler sur téléphone : le libellé SIM dans les
  bulles en usage réel et sa lecture TalkBack, le champ de
  rédaction et le clavier dans le détail, l'absence de doubles
  marges sur les trois destinations, l'espace entre le contenu
  (et le FAB de la liste) et la barre basse, la police agrandie.
- Aucune installation, aucune fusion, aucune publication : la
  branche attend la poussée par le parent après relecture.
