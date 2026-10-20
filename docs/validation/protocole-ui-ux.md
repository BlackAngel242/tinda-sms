# Protocole UI/UX Tinda SMS

Instruction de SmokeSama du 21 octobre 2026. Direction de référence : **Radiant Slate** (fond #0B0F14, surfaces #121820 / #1A222D / #242E3D, primaire sarcelle #00D2B4, textes #F3F5F7 / #94A3B8 / #64748B, bulles sortantes en dégradé sarcelle, coins 20dp avec queue 4dp, formes en pilule). Toute retouche graphique préserve la logique SMS et coffre : aucun changement de logique sensible au titre d'une correction visuelle.

## Compétences : disponibilité réelle dans cet environnement

Ordre prescrit par SmokeSama, avec l'état vérifié le 21 octobre 2026 :

1. **Product Design Audit** : pas de skill portant ce nom. Méthode appliquée via le skill local `local_impeccable` (playbooks critique, polish, layout, typeset, clarify) et l'audit des parcours réels ci-dessous. Ne pas prétendre avoir exécuté un plugin absent.
2. **Fixit** : skill local `local_fixit` disponible et applicable tel quel (double lecture expert et utilisateur ordinaire, défauts BLOQUANT / IMPORTANT / OPTIONNEL, réinspection obligatoire, verdict).
3. **Mobbin** : non accessible (service externe payant, aucun connecteur). Si des références ciblées sont nécessaires, recherche d'images et de pages publiques en indiquant la source réelle, sans attribuer le résultat à Mobbin.
4. **Honey Design** : pas de skill portant ce nom. Méthode appliquée : corriger avec les composants Compose et le design system déjà présents dans `theme/` et les composants existants, sans introduire de bibliothèque ni de composant étranger au projet.
5. **Android QA et Performance** : pas de skill dédié. Méthode appliquée : vérifications adb (captures, grandes polices, TalkBack) et mesures de démarrage quand un appareil est joignable. Sans appareil, ces vérifications sont déclarées non exécutées.

## Avant toute modification d'interface

- Inventaire des styles et composants existants (thème, couleurs, typographies, espacements, composants partagés) dans le périmètre touché.
- Règles communes écrites avant correction : couleurs, typographie, espacements, états (pressé, désactivé, erreur, vide, chargement).
- Défauts classés par impact utilisateur (BLOQUANT / IMPORTANT / OPTIONNEL, convention Fixit).
- Interactions déjà validées sur le terrain préservées : appui long sur le bouton d'envoi pour programmer, appui long sur « + » pour le coffre, gestes swipe configurés, pastilles SIM du composeur.

## Ordre des parcours

1. Liste des conversations.
2. Lecture et réponse (fil de discussion).
3. Nouveau message.

Chaque parcours est terminé et vérifié avant d'étendre aux paramètres et aux automatismes.

## Pour chaque correction

- Capture avant et capture après comparables (même écran, même contenu de démonstration, même appareil).
- Raison de la correction, liée à un défaut classé.
- Vérifications effectuées : thème clair et thème sombre, grandes polices, petit écran, clavier ouvert ; TalkBack sur les parcours critiques.
- Toute animation doit expliquer une action ou un état ; pas d'animation décorative.
- Identité recherchée : originale, cohérente, fluide ; pas de reprise d'assets ou de marque d'une autre application (ligne rouge du projet).

## État au 21 octobre 2026

- Captures « avant » disponibles : liste et fil Radiant Slate du 19 octobre (appareil de SmokeSama).
- Captures « après » : impossibles tant que l'accès adb n'est pas rétabli ; les corrections d'interface du lot L4 prépareront le code et les règles, la preuve visuelle restera à produire à la recette appareil (lot L5).
