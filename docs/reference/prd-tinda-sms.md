> Provenance : copie du PRD d'origine (table ronde du 18 octobre 2026), conservé jusqu'ici en fichier local hors dépôt et reproduit aussi dans le dossier de passation du 20 octobre 2026. Ajouté au dépôt au titre du lot L0 du plan de reprise, sans modification de contenu. Ses affirmations de marché ou juridiques sont des éléments du dossier d'origine, pas des faits revérifiés. Écarts connus entre ce PRD et le code au 20 octobre 2026 : déverrouillage par SMS `#PIN` (DEC-005) non implémenté ; modèle de clés du coffre différent de DEC-004 (voir dossier de passation, constat C3).

# PRD — « Tinda SMS » (nom provisoire)

**Application SMS nouvelle génération, 100 % originale — DMAM PRO TECH**
Version 1.0 — 18 octobre 2026 — Statut : validé par SmokeSama
Méthode : PRD Table Ronde v2.2 — niveau **RENFORCÉ** (données SMS = données personnelles sensibles ; rôle « default SMS app » = criticité système ; politique Play SMS/Call Log = réglementation).

> **Nom validé :** « Tinda SMS » (*tinda* = « envoie ! » en lingala). Validé par SmokeSama le 18 octobre 2026.

---

## 0. Réponse directe à la question posée

**Peut-on avoir les mêmes fonctionnalités, les mêmes designs de base voire mieux ?** Oui. Les fonctionnalités et les principes d'ergonomie **ne sont pas protégeables** ; seuls le sont le code source, les assets graphiques propriétaires (icônes, thèmes `.yth`, images) et la marque « YAATA ». Donc :

- **Parité fonctionnelle : oui** — on réimplémente chaque fonction (thèmes, SMS programmés, répondeur, renvoi auto, liste noire, chatheads, popup, recherche, modèles, raccourcis) à partir de l'analyse, sans copier une ligne de code.
- **Designs de base : oui, en esprit** — on recrée un langage visuel équivalent (listes denses, bulles personnalisables, esprit des thèmes) avec un **design system original**. Pas de reprise des assets YAATA.
- **Base vierge : oui, et c'est le bon choix** — pas de chirurgie du smali décompilé (fragile, juridiquement toxique). Base neuve : **Kotlin + Jetpack Compose**, minSdk 26 (Android 8.0, ta demande), targetSdk 36.
- **Tes idées + les miennes : oui** — coffre-fort, auto-suppression, envoi groupé, gestes, son d'envoi signature, accusés, faux SMS (avec garde-fous, voir DEC-007), bouclier anti-arnaque Mobile Money, double SIM intelligente : tout est spécifié ci-dessous.

---

## 1. Cadrage

### 1.1 Vision
Devenir l'app SMS de référence en Afrique centrale francophone : simple comme l'app native pour Madame Tout-le-monde, redoutablement outillée pour les exigeants, et la seule à protéger vraiment contre les arnaques Mobile Money — le tout sans qu'aucune donnée ne quitte le téléphone.

### 1.2 Problème
- YAATA, la référence des amateurs de personnalisation, est **morte** (dev absent depuis mai 2024, premium cassé, bugs non corrigés).
- Google Messages impose le cloud et la télémétrie ; les apps natives des constructeurs (Tecno, Infinix, Itel, Samsung) sont pauvres en fonctions.
- Les arnaques par SMS au Mobile Money explosent ; aucune app SMS ne protège.
- Les utilisateurs avancés veulent : confidentialité (coffre), automatismes (programmé, répondeur, suppression auto), et du contrôle fin (gestes, double SIM).

### 1.3 Utilisateurs
- **Lambda novice** : veut lire/envoyer des SMS sans se perdre. (2 regards : novice, pressé)
- **Expert** : customisation, gestes, automatismes, double SIM.
- **Victime potentielle d'arnaque** : a besoin du bouclier sans rien configurer.

### 1.4 Objectifs (12 mois)
- O1 : app « default SMS » stable sur Android 8 → 16, crash-free ≥ 99,5 %.
- O2 : 10 000 installs actives (marché pilote : Congo + diaspora).
- O3 : bouclier anti-arnaque : précision ≥ 95 %, faux positifs < 2 % [À VALIDER sur corpus].
- O4 : zéro donnée personnelle transmise (100 % on-device) — argument marketing vérifiable.

### 1.5 Non-objectifs
- Pas de MMS (décision assumée : périmètre SMS uniquement).
- Pas de RCS, pas de messagerie internet, pas de cloud, pas de compte utilisateur.
- Pas de reprise du code ni des assets YAATA.

### 1.6 Contraintes structurantes
- [CONFIRMÉ] Doit pouvoir devenir « default SMS app » (sinon pas d'envoi/lecture SMS sur Android 4.4+).
- [CONFIRMÉ] Politique Play « SMS/Call Log » : seules les apps gestionnaires par défaut obtiennent ces permissions sur le Play Store.
- [CONFIRMÉ] En SMS pur, **l'accusé de lecture n'existe pas** (seule la livraison est confirmable). Lecture = uniquement app-à-app.
- [HYPOTHÈSE] Les utilisateurs d'Owando accepteront de changer d'app SMS par défaut si le bouclier + la simplicité sont au rendez-vous → à invalider avant le MVP (test terrain).
- [BLOQUANT] Aucun : le projet est finançable en solo + IA.

### 1.7 Données sensibles (lentille sécurité)
Contenu des SMS (conversations privées, codes OTP, infos bancaires), carnet de contacts, numéros du coffre-fort, PIN. **Règle d'or : tout reste sur l'appareil, chiffré au repos pour le coffre.** Aucun serveur DMAM ne voit un SMS.

---

## 2. Table ronde dimensionnée (15 regards, niveau RENFORCÉ)

| # | Regard | Préoccupation principale | Risque s'il est ignoré | Compromis acceptable |
|---|---|---|---|---|
| 1 | SmokeSama (décideur/approver) | App à lui, moderne, qui surpasse YAATA | Dispersion, jamais livré | MVP resserré, vagues ensuite |
| 2 | Lambda novice (ex. maman à Owando) | « Je veux juste lire et répondre » | Rejet dès l'install | Mode simple par défaut, fonctions avancées cachées |
| 3 | Lambda pressé (commerçant double SIM) | Aller vite, pas d'erreur d'envoi | Envoi depuis la mauvaise SIM = coût réel | SIM par défaut + confirmation visuelle |
| 4 | Expert customisation | Thèmes, gestes, réglages fins | « Moins bien que YAATA » | Moteur de thèmes dès la V1 |
| 5 | Professionnel discret (sujets sensibles) | Coffre-fort infaillible et discret | Fuite = trahison de confiance | Chiffrement fort, même contre l'oubli du PIN on tranche (DEC-004) |
| 6 | Victime d'arnaque MoMo | Être protégé sans rien comprendre | Faux positif qui bloque un vrai transfert | Seuil conservateur + liste blanche |
| 7 | Dev Android senior | Faisabilité, dette technique | Promesses intenables | Stack moderne, pas de dette 2018 |
| 8 | Expert sécurité | Chiffrement, surface d'attaque | Coffre cassé = catastrophe | SQLCipher, clés dérivées du PIN, audit du coffre |
| 9 | Expert conformité Play | Politique SMS/Call Log | Rejet du Play Store | Rôle default handler + déclaration propre |
| 10 | Designer UX mobile | Clarté, gestes découvrables | Usine à gaz | Gestes configurables, valeurs par défaut saines |
| 11 | Testeur terrain (Tecno/Infinix/Itel low-end) | Perf sur 2 Go de RAM | Lenteurs = désinstall | Budget perf : démarrage < 1,5 s, APK < 25 Mo |
| 12 | Expert accessibilité | TalkBack, contrastes, tailles | Exclusion | Thèmes à contraste validé |
| 13 | Contraintes réseau/opérateur | Coût SMS, livraison | Envoi groupé qui coûte une fortune | Compteur de segments, alerte au-delà de X |
| 14 | Regard marché (Google Messages) | Pourquoi changer ? | « Autant rester sur Google » | Différenciation : bouclier + thèmes + 100 % local |
| 15 | Support / utilisateur en panne | Récupération, backup | Perte des SMS = drame | Sauvegarde chiffrée locale, restauration simple |

---

## 3. Tensions (6)

| # | Tension | Enjeu |
|---|---|---|
| T1 | Parité YAATA (richesse) **vs** simplicité lambda | Ne pas noyer le novice sous les options de l'expert |
| T2 | Coffre-fort inviolable **vs** récupérabilité | PIN oublié = données perdues à jamais, ou porte dérobée ? |
| T3 | Bouclier agressif **vs** faux positifs | Bloquer un vrai SMS MoMo est pire que rater une arnaque |
| T4 | Faux SMS (demandé) **vs** risque d'abus | Fonction de justification mensongère : garde-fous obligatoires |
| T5 | 100 % on-device **vs** confort cloud | Sauvegarde et compagnon web sans serveur : jusqu'où ? |
| T6 | minSdk 26 (couverture max) **vs** coût de maintenance | Android 8 = 2017, APIs manquantes à compenser |

---

## 4. Arbitrages, Red Team, gate

### 4.1 Décisions (DACI : SmokeSama approver unique, tranche sous 7 jours sauf mention)

**DEC-001 — Base vierge Kotlin, pas de réutilisation du code YAATA.**
Contexte : T6 + risque juridique. Options : (a) chirurgie du smali décompilé, (b) base neuve. Décision : (b). Raison : le smali est fragile, illisible à maintenir, et juridiquement toxique. Contrepartie : réécrire les 266 classes équivalentes prend plus de temps au départ. Confiance : haute. Révision : jamais (principe).

**DEC-002 — Périmètre SMS uniquement, pas de MMS.**
Contexte : demande explicite. Décision : aucune couche MMS (ni réception, ni envoi, ni APN). Raison : complexité opérateur énorme pour un usage en chute libre ; WhatsApp a pris ce créneau. Contrepartie : les rares utilisateurs MMS devront garder une autre app. Confiance : haute.

**DEC-003 — Stack : Kotlin + Jetpack Compose, minSdk 26, targetSdk 36.**
Contexte : modernité + couverture « de 8 à la toute dernière ». Décision : Compose (standard 2026), Room, WorkManager, DataStore, Hilt. Raison : productivité solo + IA maximale, UI déclarative adaptée aux thèmes. Contrepartie : Compose sur Android 8 demande des tests soigneux (fait, c'est supporté). Confiance : haute.

**DEC-004 — Coffre-fort : PIN oublié = données perdues, pas de porte dérobée.**
Contexte : T2. Décision : chiffrement SQLCipher, clé dérivée du PIN (PBKDF2), aucun séquestre. Raison : la promesse « même nous on ne peut pas lire » est la valeur du coffre ; une récupération annulerait la confiance. Contrepartie : perte définitive si oubli — l'onboarding le dit explicitement, avec double saisie du PIN. Confiance : haute. Révision : si tests utilisateurs montrent un rejet massif.

**DEC-005 — Déverrouillage du coffre par « SMS à soi-même ».**
Contexte : demande explicite (« s'envoyer un SMS vers son mot de passe »). Décision : l'utilisateur envoie à son propre numéro un SMS au format `#PIN <code>` ; l'app, en tant que default SMS handler, l'intercepte et déverrouille le coffre **sans écrire ce SMS dans le provider système** (il n'apparaît nulle part). Alternative secours : PIN + biométrie dans l'app. Raison : discret (aucune trace visible), sans écran de saisie suspecte. Contrepartie : coûte 1 SMS ; si l'utilisateur n'est pas default handler, ça ne marche pas → l'onboarding l'exige. Confiance : moyenne (à valider sur ROMs Tecno/Infinix). Expérience d'invalidation : test sur 3 appareils du marché local avant le lot coffre.

**DEC-006 — Bouclier anti-arnaque : mode « signalement » d'abord, jamais de blocage silencieux.**
Contexte : T3. Décision : en V1, le bouclier **signale** (bandeau « suspect : ressemble à une arnaque MoMo ») mais ne bloque ni ne supprime jamais. Raison : un faux positif qui fait rater un vrai transfert d'argent est inacceptable. Contrepartie : l'utilisateur doit lire le bandeau. Confiance : haute. Seuil : conservateur, liste blanche des numéros officiels d'opérateurs.

**DEC-007 — Faux SMS : fonction sensible, marquée et cloisonnée.**
Contexte : T4, demande explicite (« créer un faux SMS pour se justifier »). Décision : autorisé **uniquement** comme entrée locale dans l'app, **marquée « simulé » en base**, **jamais injectée dans le provider système** (invisible aux autres apps), **non exportable** (ni backup, ni partage). Raison : c'est une fonction de mise en scène/prank ; l'injecter dans le système téléphonique serait de la fabrication de preuve. Contrepartie : le faux SMS n'existe que dans Tinda. Confiance : moyenne. Garde-fou : mention visible dans l'UI d'édition (« simulation locale »), et la doc rappelle que s'en servir pour tromper (assurance, justice, employeur) est punissable.

**DEC-008 — Accusés : livraison ON par défaut ; lecture = app-à-app uniquement, dit honnêtement.**
Contexte : demande « confirmation de lecture activée par défaut ». Décision : l'accusé de **livraison** (rapport réseau) est activé par défaut. L'accusé de **lecture** n'existe pas en SMS pur : il n'est proposé que quand les deux correspondants ont Tinda (via SMS de données silencieux sur port applicatif), et l'UI le dit explicitement (« lecture confirmée par l'app », sinon « livré »). Raison : ne jamais promettre l'impossible. Confiance : haute (fait technique).

**DEC-009 — 100 % on-device ; compagnon web en réseau local uniquement.**
Contexte : T5. Décision : aucune donnée SMS ne transite par un serveur (même pas DMAM). Le compagnon web fonctionne en Wi-Fi local (serveur embarqué + QR pairing), la sauvegarde est un fichier chiffré local. Raison : c'est l'argument de confiance n°1 et ça supprime tout coût serveur. Contrepartie : pas de synchro multi-appareils cloud. Confiance : haute.

**DEC-010 — Mode simple par défaut, puissance en option.**
Contexte : T1. Décision : à l'install, l'app ressemble à une app SMS normale et belle. Les fonctions avancées (coffre, automatismes, gestes fins, bouclier réglages) vivent dans un espace « Avancé », désactivées ou discrètes par défaut. Raison : le lambda ne doit jamais se noyer. Confiance : haute.

### 4.2 Options écartées

| Option | Pourquoi écartée | Coût évité |
|---|---|---|
| Rebrander l'APK YAATA (package + assets + signature) | Appropriation du travail d'autrui ; code de 2018 fragile | Risque juridique + dette immédiate |
| Acheter/forker une base open source (ex. QKSMS) | QKSMS est GPLv3 (contamination) et daté ; repartir de zéro est plus propre | Contraintes de licence |
| MMS + RCS | Complexité opérateur, usages marginaux ici | Des mois de dev pour rien |
| Cloud / comptes / synchro | Coûts serveur, confiance, et Railway suspendu de toute façon | Infra + RGPD-like |
| Ne rien faire | Le créneau est ouvert (YAATA mort, Google trop intrusif) | Occasion manquée |

### 4.3 Red Team (3 questions)

1. **Qu'est-ce qui tue le projet en 6 mois ?** Un bug du coffre (perte de messages) ou un faux positif du bouclier qui fait rater de l'argent → d'où DEC-004/DEC-006 et les tests terrain avant chaque lot sensible.
2. **Qu'est-ce qu'on croit savoir mais qu'on n'a pas vérifié ?** Que les ROMs Tecno/Infinix/Itel acceptent bien le rôle default SMS + l'interception du SMS-à-soi-même → expérience d'invalidation prévue (HYP-03).
3. **Quelle fonction sera détournée ?** Le faux SMS (fabrication de preuves) et le coffre (dissimulation) → cloisonnement technique (DEC-007) + mentions légales dans l'app.

### 4.4 Gate : **GO conditionnel**
GO, à condition que les 3 expériences d'invalidation (HYP-01/02/03, §8) soient menées **avant** les lots correspondants. Pas de NO-GO : le risque juridique est neutralisé par la base vierge, le risque technique est borné par les validations terrain.

---

## 5. PRD — fonctionnalités

### 5.1 MVP (lot 1 à 3) : le socle qui doit être parfait
- F1. **Conversations + fil de discussion** : liste, bulles, brouillon auto, accusé de livraison ON par défaut (DEC-008).
- F2. **Envoi/réception SMS** en tant que default handler (les 4 composants requis), **double SIM** : choix par conversation + SIM par défaut visible.
- F3. **Recherche** plein texte dans tout l'historique.
- F4. **Moteur de thèmes V1** : 6 thèmes originaux soignés + couleurs dynamiques (l'esprit YAATA, assets 100 % nouveaux).
- F5. **Gestes** : swipe configurable (répondre / archiver / supprimer / appeler).
- F6. **Son d'envoi signature** : petit son « propre à nous » (asset original DMAM), réglable, désactivable.
- F7. **Liste noire / blocage** avec journal.
- F8. **Français + anglais** ; interface « mode simple » (DEC-010).

### 5.2 Vague 2 : automatismes et discrétion
- F9. **SMS programmés** (unitaires + récurrents), alarmes exactes conformes Android 12+ avec repli WorkManager.
- F10. **Répondeur automatique** (règles : plage horaire, contacts, mot-clé).
- F11. **Renvoi automatique** vers un numéro.
- F12. **Coffre-fort** : numéros marqués « privés » → messages chiffrés (SQLCipher), invisibles partout ailleurs ; déverrouillage par SMS-à-soi-même `#PIN <code>` (DEC-005) ou PIN/biomètrie ; PIN oublié = perdu (DEC-004, dit à l'onboarding).
- F13. **Auto-suppression** : règles par contact et par mots sensibles (« supprime après lecture » / « après 24 h »).
- F14. **Envoi groupé** : listes de diffusion locales, compteur de segments SMS, alerte coût.

### 5.3 Vague 3 : différenciation
- F15. **Bouclier anti-arnaque Mobile Money** : scoring on-device (patterns d'expéditeur, formulations, liens), bandeau « suspect », jamais de blocage (DEC-006), liste blanche opérateurs.
- F16. **Modèles de messages** + réponses rapides.
- F17. **Faux SMS** : simulation locale uniquement, marquée « simulé », non exportable (DEC-007 + garde-fous).
- F18. **Sauvegarde chiffrée locale** (export/import par mot de passe).
- F19. **Compagnon web local** : écrire depuis le PC en Wi-Fi, QR pairing, aucun cloud (DEC-009).
- F20. **Statistiques d'usage** (volumes, top contacts), 100 % locales.
- F21. **Chatheads / popup de réponse rapide** (parité YAATA).
- F22. **Pack linguistique lingala** (interface simplifiée) — clin d'œil au marché.

### 5.4 Hors scope (définitif)
MMS, RCS, messagerie internet, cloud, comptes utilisateurs, reprise d'assets YAATA.

### 5.5 Règles métier critiques
- RB-01 : un message du coffre n'est **jamais** écrit dans le provider système.
- RB-02 : le SMS de déverrouillage `#PIN` est **consommé** (jamais stocké, jamais affiché).
- RB-03 : le faux SMS n'existe que dans la base locale, marqué `simulated=1`, exclu des backups et du partage.
- RB-04 : le bouclier ne supprime ni ne bloque jamais (V1) ; il signale.
- RB-05 : l'accusé de lecture n'est affiché que si les deux parties ont Tinda ; sinon l'UI dit « livré ».
- RB-06 : l'envoi groupé affiche toujours le nombre de segments et une alerte au-delà d'un seuil réglable (défaut : 5).

### 5.6 Exigences non fonctionnelles
- Démarrage à froid < 1,5 s sur appareil 2 Go RAM ; APK < 25 Mo ; crash-free ≥ 99,5 %.
- Android 8.0 → 16 : matrice de test sur 4 profils (ancien low-end, milieu, récent, émulateur API 36).
- Accessibilité : TalkBack fonctionnel sur les parcours critiques, contrastes AA.
- Confidentialité : aucune donnée SMS hors de l'appareil (vérifiable : l'app fonctionne en mode avion, hors réception/envoi bien sûr).

---

## 6. Métriques

**North star :** messages envoyés via Tinda par utilisateur actif hebdomadaire (cible : ≥ 20 à 6 mois [À VALIDER]).
**Entrées :** installs actives (10 000 à 12 mois) ; % qui deviennent default SMS (≥ 60 %) ; rétention J30 (≥ 35 %).
**Garde-fous :** crash-free ≥ 99,5 % ; faux positifs bouclier < 2 % ; 0 incident de perte de données coffre ; taille APK < 25 Mo.
Chaque métrique est mesurable on-device via analytics **locale** (comptes anonymes agrégés, opt-in) — pas de télémétrie externe par défaut.

---

## 7. User stories principales (traçabilité DEC → US → AC)

**US-001 — Envoyer un SMS simple (lambda novice).** [DEC-010]
AC : en 3 taps depuis l'accueil ; accusé « livré » visible par défaut ; aucun réglage demandé.

**US-002 — Choisir la SIM par conversation (lambda pressé).** [DEC-003]
AC : badge SIM visible dans le fil ; mémorisé par contact ; changement en 1 tap avant envoi.

**US-003 — Appliquer un thème (expert).** [DEC-010]
AC : 6 thèmes d'origine ; aperçu instantané ; les couleurs suivent aussi le mode sombre système.

**US-004 — Programmer un SMS récurrent (expert).**
AC : programmé pour une date/heure + répétition ; fonctionne après redémarrage ; si l'alarme exacte est refusée par le système, repli documenté dans l'UI.

**US-005 — Cacher les messages d'un numéro (professionnel discret).** [DEC-004, DEC-005]
AC : numéro marqué « privé » → ses messages n'apparaissent ni dans les listes, ni dans la recherche, ni dans les notifications ; déverrouillage par `#PIN <code>` envoyé à soi-même (SMS consommé, invisible) ou PIN/biomètrie ; sans le PIN, impossible (testé : base illisible hors app).

**US-006 — Auto-supprimer les messages sensibles.**
AC : règle « contact X + mot-clé Y → supprimer après lecture » ; journal de suppression (comptes uniquement, pas le contenu).

**US-007 — Être alerté d'une arnaque MoMo (victime potentielle).** [DEC-006]
AC : SMS frauduleux type → bandeau « suspect » ; vrai SMS opérateur → aucun bandeau (corpus de test 200 SMS, précision ≥ 95 %) ; jamais de suppression auto.

**US-008 — Envoyer à un groupe sans se ruiner (lambda pressé).**
AC : liste de diffusion ; compteur « 3 segments × 12 destinataires » ; alerte avant envoi au-delà du seuil.

**US-009 — Créer un faux SMS (prank).** [DEC-007]
AC : créé dans l'app uniquement, marqué « simulé » ; invisible aux autres apps ; exclu des backups/exports ; mention « simulation locale » à la création.

**US-010 — Lire/écrire depuis le PC en Wi-Fi (expert).** [DEC-009]
AC : QR pairing, aucune donnée vers internet (test : box déconnectée du WAN, ça marche).

**US-011 — Restaurer après changement de téléphone.**
AC : export chiffré par mot de passe → import sur le nouvel appareil → historique complet, coffre inclus (avec le même PIN).

**US-012 — Devenir default SMS app (onboarding).**
AC : parcours guidé en 2 écrans ; si refusé, l'app explique ce qui ne marchera pas (pas d'envoi) sans bloquer la visite.

## 8. Architecture et stack technique

| Couche | Choix | Pourquoi (arbitrage réel) |
|---|---|---|
| Langage | Kotlin 100 % | Standard Android 2026, null-safety (moins de crashs), coroutines |
| UI | Jetpack Compose + Material 3 | Thèmes dynamiques faciles, code 2× plus court qu'en Views ; supporté dès API 21 |
| Données | Room (messages miroir, règles, groupes) + SQLCipher (coffre) | Recherche rapide, chiffrement éprouvé pour le sensible |
| Provider SMS | Telephony provider système (lecture/écriture) | Obligatoire pour une default SMS app ; le coffre et les faux SMS restent hors provider (RB-01/03) |
| Tâches différées | WorkManager (+ `SCHEDULE_EXACT_ALARM` quand accordée) | Conforme Android 12+, survit au redémarrage |
| DI | Hilt | Testabilité |
| Réseau local | Serveur HTTP embarqué (Ktor) + WebSocket, QR pairing | Compagnon web sans cloud |
| Bouclier | Moteur de règles + scoring local (pas de ML lourd en V1) | 0 dépendance, 0 réseau, explicable ; ML embarqué en V2 si besoin |
| Build | Gradle, R8 full mode, Baseline Profiles, App Bundle | Perf + taille |
| Tests | JUnit + Espresso sur 4 profils d'appareils/API | Non négociable vu la criticité |

**Premier lot vertical** = F1+F2+onboarding default-handler+socle sécu (chiffrement du coffre même si le coffre arrive en vague 2 : le socle doit être là dès le début).

## 9. Lots et estimations (fourchettes, confiance)

Travail solo + agents IA. Fourchette = conception + code + revue + tests + docs. Confiance : moyenne (codebase neuve, périmètre bien borné).

| Lot | Contenu | Fourchette | Confiance |
|---|---|---|---|
| 0 | Socle : projet, CI, design system, navigation, socle sécu (chiffre, logs) | 2–3 sem. | haute |
| 1 | Default SMS handler : envoi/réception, conversations, fils, livraison ON, double SIM, onboarding | 4–6 sem. | moyenne |
| 2 | Recherche, thèmes V1, gestes, son signature, liste noire, FR/EN | 3–5 sem. | moyenne |
| 3 | SMS programmés, répondeur, renvoi auto, envoi groupé | 3–4 sem. | moyenne |
| 4 | Coffre-fort + auto-suppression (+ audits sécu dédiés) | 4–6 sem. | basse (sécu = prudence) |
| 5 | Bouclier anti-arnaque (corpus 200 SMS + réglage seuils) | 3–5 sem. | basse (dépend du corpus) |
| 6 | Sauvegarde chiffrée, compagnon web local, stats, faux SMS | 4–6 sem. | moyenne |
| 7 | Durcissement : matrice 4 appareils × API 26→36, accessibilité, beta fermée | 3–4 sem. | moyenne |

**Total MVP + vagues : 26–39 semaines** en solo+IA. Recommandation : viser les lots 0–2 en 9–14 semaines pour une beta publique, puis itérer.

## 10. Risques majeurs

| Risque | Probabilité | Mitigation |
|---|---|---|
| Rejet Play (politique SMS/Call Log) | moyenne | Default handler + déclaration conforme ; à défaut, diffusion directe APK (marché local habitué) |
| ROMs locales (Tecno/Infinix) : gestion agressive de la batterie tue les programmés | haute | Whitelist constructeur guidée + WorkManager + tests terrain |
| Faux positifs du bouclier | moyenne | Mode signalement seul (DEC-006), corpus de validation |
| Oubli du PIN coffre (support) | haute | Onboarding explicite, double saisie, aucune promesse de récupération |
| Détournement du faux SMS | moyenne | Cloisonnement technique + mentions légales (DEC-007) |
| Abandon en cours de route (solo) | moyenne | Lots verticaux livrables, beta tôt pour la motivation |

## 11. spec.json (manifeste machine, le Markdown reste la vérité)

```json
{
  "product": "Tinda SMS",
  "status": "draft",
  "approver": "SmokeSama",
  "level": "RENFORCE",
  "decisions": ["DEC-001", "DEC-002", "DEC-003", "DEC-004", "DEC-005", "DEC-006", "DEC-007", "DEC-008", "DEC-009", "DEC-010"],
  "gate": "GO_CONDITIONNEL",
  "mvp_lots": [0, 1, 2],
  "stack": ["kotlin", "jetpack-compose", "room", "sqlcipher", "workmanager", "hilt", "ktor"],
  "minSdk": 26,
  "targetSdk": 36,
  "hypotheses_to_kill": ["HYP-01", "HYP-02", "HYP-03"],
  "metrics": {
    "north_star": "messages_per_wau",
    "guardrails": ["crash_free_99_5", "shield_false_positive_lt_2pct", "apk_lt_25mo"]
  }
}
```

## 12. Protocole de ré-entrée

- Si HYP-02 (bouclier) est invalidée par le corpus → rejouer phase 4 (DEC-006) : descendre le bouclier en « liste de numéros signalés par la communauté » ou le retirer du MVP.
- Si les ROMs locales bloquent l'interception (HYP-03) → rejouer DEC-005 : déverrouillage par PIN/biomètrie uniquement.
- Si la beta montre un rejet du « mode simple » → rejouer DEC-010 et la phase 5 (parcours).
- Toute nouvelle fonction « sensible » (au sens Red Team) repasse par la lentille sécurité avant d'entrer dans un lot.

---

*PRD généré par table ronde (15 regards, 6 tensions, 10 décisions, Red Team, gate GO conditionnel). Hypothèses marquées [À VALIDER] à tuer avant les lots concernés. Prochaine étape proposée : lot 0 (socle) — génération sur ton feu vert.*
