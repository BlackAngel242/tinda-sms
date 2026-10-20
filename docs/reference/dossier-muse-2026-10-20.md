# Archive de passation de Muse

Source : dossier transmis par SmokeSama le 20 octobre 2026. Document historique, non certification technique ou juridique. Les résultats de tests et de terrain sont rapportés par Muse, non réexécutés par Codex. Les affirmations externes du PRD restent à vérifier avant publication. Les corrections et arbitrages sont dans ../superpowers/plans/2026-10-20-tinda-reprise-muse.md. Seule modification éditoriale : remplacement des tirets cadratins.

# Dossier complet Tinda SMS

**Un seul document de passation, au 20 octobre 2026.**
Dépôt : https://github.com/BlackAngel242/tinda-sms : branche `master` : SHA `dfddee719be55b4c71fd49cef7f26e92b1bed986`.
Agent principal du développement : DrSmoke (Muse). Relecture et planning : Codex. Décideur unique : SmokeSama.

Contenu :

- **Partie 1** : le PRD intégral (table ronde du 18 octobre 2026, décisions DEC-001 à DEC-010).
- **Partie 2** : l'état des lieux factuel du 20 octobre 2026, avec la confrontation point par point aux constats de la revue statique de Codex, et le bloc de passation.
- **Partie 3** : le programme de travail consolidé (lots proposés, backlog des demandes, décisions en attente).

Aucun contenu SMS réel, numéro, PIN, clé ou mot de passe dans ce document.

---

# Partie 1 : PRD « Tinda SMS » (intégral)

# PRD : « Tinda SMS » (nom provisoire)

**Application SMS nouvelle génération, 100 % originale : DMAM PRO TECH**
Version 1.0 : 18 octobre 2026 : Statut : validé par SmokeSama
Méthode : PRD Table Ronde v2.2 : niveau **RENFORCÉ** (données SMS = données personnelles sensibles ; rôle « default SMS app » = criticité système ; politique Play SMS/Call Log = réglementation).

> **Nom validé :** « Tinda SMS » (*tinda* = « envoie ! » en lingala). Validé par SmokeSama le 18 octobre 2026.

---

## 0. Réponse directe à la question posée

**Peut-on avoir les mêmes fonctionnalités, les mêmes designs de base voire mieux ?** Oui. Les fonctionnalités et les principes d'ergonomie **ne sont pas protégeables** ; seuls le sont le code source, les assets graphiques propriétaires (icônes, thèmes `.yth`, images) et la marque « YAATA ». Donc :

- **Parité fonctionnelle : oui** : on réimplémente chaque fonction (thèmes, SMS programmés, répondeur, renvoi auto, liste noire, chatheads, popup, recherche, modèles, raccourcis) à partir de l'analyse, sans copier une ligne de code.
- **Designs de base : oui, en esprit** : on recrée un langage visuel équivalent (listes denses, bulles personnalisables, esprit des thèmes) avec un **design system original**. Pas de reprise des assets YAATA.
- **Base vierge : oui, et c'est le bon choix** : pas de chirurgie du smali décompilé (fragile, juridiquement toxique). Base neuve : **Kotlin + Jetpack Compose**, minSdk 26 (Android 8.0, ta demande), targetSdk 36.
- **Tes idées + les miennes : oui** : coffre-fort, auto-suppression, envoi groupé, gestes, son d'envoi signature, accusés, faux SMS (avec garde-fous, voir DEC-007), bouclier anti-arnaque Mobile Money, double SIM intelligente : tout est spécifié ci-dessous.

---

## 1. Cadrage

### 1.1 Vision
Devenir l'app SMS de référence en Afrique centrale francophone : simple comme l'app native pour Madame Tout-le-monde, redoutablement outillée pour les exigeants, et la seule à protéger vraiment contre les arnaques Mobile Money : le tout sans qu'aucune donnée ne quitte le téléphone.

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
- O4 : zéro donnée personnelle transmise (100 % on-device) : argument marketing vérifiable.

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

**DEC-001 : Base vierge Kotlin, pas de réutilisation du code YAATA.**
Contexte : T6 + risque juridique. Options : (a) chirurgie du smali décompilé, (b) base neuve. Décision : (b). Raison : le smali est fragile, illisible à maintenir, et juridiquement toxique. Contrepartie : réécrire les 266 classes équivalentes prend plus de temps au départ. Confiance : haute. Révision : jamais (principe).

**DEC-002 : Périmètre SMS uniquement, pas de MMS.**
Contexte : demande explicite. Décision : aucune couche MMS (ni réception, ni envoi, ni APN). Raison : complexité opérateur énorme pour un usage en chute libre ; WhatsApp a pris ce créneau. Contrepartie : les rares utilisateurs MMS devront garder une autre app. Confiance : haute.

**DEC-003 : Stack : Kotlin + Jetpack Compose, minSdk 26, targetSdk 36.**
Contexte : modernité + couverture « de 8 à la toute dernière ». Décision : Compose (standard 2026), Room, WorkManager, DataStore, Hilt. Raison : productivité solo + IA maximale, UI déclarative adaptée aux thèmes. Contrepartie : Compose sur Android 8 demande des tests soigneux (fait, c'est supporté). Confiance : haute.

**DEC-004 : Coffre-fort : PIN oublié = données perdues, pas de porte dérobée.**
Contexte : T2. Décision : chiffrement SQLCipher, clé dérivée du PIN (PBKDF2), aucun séquestre. Raison : la promesse « même nous on ne peut pas lire » est la valeur du coffre ; une récupération annulerait la confiance. Contrepartie : perte définitive si oubli : l'onboarding le dit explicitement, avec double saisie du PIN. Confiance : haute. Révision : si tests utilisateurs montrent un rejet massif.

**DEC-005 : Déverrouillage du coffre par « SMS à soi-même ».**
Contexte : demande explicite (« s'envoyer un SMS vers son mot de passe »). Décision : l'utilisateur envoie à son propre numéro un SMS au format `#PIN <code>` ; l'app, en tant que default SMS handler, l'intercepte et déverrouille le coffre **sans écrire ce SMS dans le provider système** (il n'apparaît nulle part). Alternative secours : PIN + biométrie dans l'app. Raison : discret (aucune trace visible), sans écran de saisie suspecte. Contrepartie : coûte 1 SMS ; si l'utilisateur n'est pas default handler, ça ne marche pas → l'onboarding l'exige. Confiance : moyenne (à valider sur ROMs Tecno/Infinix). Expérience d'invalidation : test sur 3 appareils du marché local avant le lot coffre.

**DEC-006 : Bouclier anti-arnaque : mode « signalement » d'abord, jamais de blocage silencieux.**
Contexte : T3. Décision : en V1, le bouclier **signale** (bandeau « suspect : ressemble à une arnaque MoMo ») mais ne bloque ni ne supprime jamais. Raison : un faux positif qui fait rater un vrai transfert d'argent est inacceptable. Contrepartie : l'utilisateur doit lire le bandeau. Confiance : haute. Seuil : conservateur, liste blanche des numéros officiels d'opérateurs.

**DEC-007 : Faux SMS : fonction sensible, marquée et cloisonnée.**
Contexte : T4, demande explicite (« créer un faux SMS pour se justifier »). Décision : autorisé **uniquement** comme entrée locale dans l'app, **marquée « simulé » en base**, **jamais injectée dans le provider système** (invisible aux autres apps), **non exportable** (ni backup, ni partage). Raison : c'est une fonction de mise en scène/prank ; l'injecter dans le système téléphonique serait de la fabrication de preuve. Contrepartie : le faux SMS n'existe que dans Tinda. Confiance : moyenne. Garde-fou : mention visible dans l'UI d'édition (« simulation locale »), et la doc rappelle que s'en servir pour tromper (assurance, justice, employeur) est punissable.

**DEC-008 : Accusés : livraison ON par défaut ; lecture = app-à-app uniquement, dit honnêtement.**
Contexte : demande « confirmation de lecture activée par défaut ». Décision : l'accusé de **livraison** (rapport réseau) est activé par défaut. L'accusé de **lecture** n'existe pas en SMS pur : il n'est proposé que quand les deux correspondants ont Tinda (via SMS de données silencieux sur port applicatif), et l'UI le dit explicitement (« lecture confirmée par l'app », sinon « livré »). Raison : ne jamais promettre l'impossible. Confiance : haute (fait technique).

**DEC-009 : 100 % on-device ; compagnon web en réseau local uniquement.**
Contexte : T5. Décision : aucune donnée SMS ne transite par un serveur (même pas DMAM). Le compagnon web fonctionne en Wi-Fi local (serveur embarqué + QR pairing), la sauvegarde est un fichier chiffré local. Raison : c'est l'argument de confiance n°1 et ça supprime tout coût serveur. Contrepartie : pas de synchro multi-appareils cloud. Confiance : haute.

**DEC-010 : Mode simple par défaut, puissance en option.**
Contexte : T1. Décision : à l'install, l'app ressemble à une app SMS normale et belle. Les fonctions avancées (coffre, automatismes, gestes fins, bouclier réglages) vivent dans un espace « Avancé », désactivées ou discrètes par défaut. Raison : le lambda ne doit jamais se noyer. Confiance : haute.

### 4.2 Options écartées

| Option | Pourquoi écartée | Coût évité |
|---|---|---|
| Rebrander l'APK YAATA (package + assets + signature) | Appropriation du travail d'autrui ; code de 2018 fragile | Risque juridique + dette immédiate |
| Acheter/forker une base open source (ex. QKSMS) | QKSMS est GPLv3 (contamination) et daté ; repartir de zéro est plus propre | Contraintes de licence |
| MMS + RCS | Complexité opérateur, usages marginaux ici | Des mois de dev pour rien |
| Cloud / comptes / synchro | Coûts serveur, confiance | Infra + conformité |
| Ne rien faire | Le créneau est ouvert (YAATA mort, Google trop intrusif) | Occasion manquée |

### 4.3 Red Team (3 questions)

1. **Qu'est-ce qui tue le projet en 6 mois ?** Un bug du coffre (perte de messages) ou un faux positif du bouclier qui fait rater de l'argent → d'où DEC-004/DEC-006 et les tests terrain avant chaque lot sensible.
2. **Qu'est-ce qu'on croit savoir mais qu'on n'a pas vérifié ?** Que les ROMs Tecno/Infinix/Itel acceptent bien le rôle default SMS + l'interception du SMS-à-soi-même → expérience d'invalidation prévue (HYP-03).
3. **Quelle fonction sera détournée ?** Le faux SMS (fabrication de preuves) et le coffre (dissimulation) → cloisonnement technique (DEC-007) + mentions légales dans l'app.

### 4.4 Gate : **GO conditionnel**
GO, à condition que les 3 expériences d'invalidation (HYP-01/02/03, §8) soient menées **avant** les lots correspondants. Pas de NO-GO : le risque juridique est neutralisé par la base vierge, le risque technique est borné par les validations terrain.

---

## 5. PRD : fonctionnalités

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
- F22. **Pack linguistique lingala** (interface simplifiée) : clin d'œil au marché.

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
Chaque métrique est mesurable on-device via analytics **locale** (comptes anonymes agrégés, opt-in) : pas de télémétrie externe par défaut.

---

## 7. User stories principales (traçabilité DEC → US → AC)

**US-001 : Envoyer un SMS simple (lambda novice).** [DEC-010]
AC : en 3 taps depuis l'accueil ; accusé « livré » visible par défaut ; aucun réglage demandé.

**US-002 : Choisir la SIM par conversation (lambda pressé).** [DEC-003]
AC : badge SIM visible dans le fil ; mémorisé par contact ; changement en 1 tap avant envoi.

**US-003 : Appliquer un thème (expert).** [DEC-010]
AC : 6 thèmes d'origine ; aperçu instantané ; les couleurs suivent aussi le mode sombre système.

**US-004 : Programmer un SMS récurrent (expert).**
AC : programmé pour une date/heure + répétition ; fonctionne après redémarrage ; si l'alarme exacte est refusée par le système, repli documenté dans l'UI.

**US-005 : Cacher les messages d'un numéro (professionnel discret).** [DEC-004, DEC-005]
AC : numéro marqué « privé » → ses messages n'apparaissent ni dans les listes, ni dans la recherche, ni dans les notifications ; déverrouillage par `#PIN <code>` envoyé à soi-même (SMS consommé, invisible) ou PIN/biomètrie ; sans le PIN, impossible (testé : base illisible hors app).

**US-006 : Auto-supprimer les messages sensibles.**
AC : règle « contact X + mot-clé Y → supprimer après lecture » ; journal de suppression (comptes uniquement, pas le contenu).

**US-007 : Être alerté d'une arnaque MoMo (victime potentielle).** [DEC-006]
AC : SMS frauduleux type → bandeau « suspect » ; vrai SMS opérateur → aucun bandeau (corpus de test 200 SMS, précision ≥ 95 %) ; jamais de suppression auto.

**US-008 : Envoyer à un groupe sans se ruiner (lambda pressé).**
AC : liste de diffusion ; compteur « 3 segments × 12 destinataires » ; alerte avant envoi au-delà du seuil.

**US-009 : Créer un faux SMS (prank).** [DEC-007]
AC : créé dans l'app uniquement, marqué « simulé » ; invisible aux autres apps ; exclu des backups/exports ; mention « simulation locale » à la création.

**US-010 : Lire/écrire depuis le PC en Wi-Fi (expert).** [DEC-009]
AC : QR pairing, aucune donnée vers internet (test : box déconnectée du WAN, ça marche).

**US-011 : Restaurer après changement de téléphone.**
AC : export chiffré par mot de passe → import sur le nouvel appareil → historique complet, coffre inclus (avec le même PIN).

**US-012 : Devenir default SMS app (onboarding).**
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

*PRD généré par table ronde (15 regards, 6 tensions, 10 décisions, Red Team, gate GO conditionnel). Hypothèses marquées [À VALIDER] à tuer avant les lots concernés. Prochaine étape proposée : lot 0 (socle) : génération sur ton feu vert.*

---

*Fin de la partie 1. La partie 2 (état des lieux) commence ci-dessous.*


---

# Partie 2 : État des lieux factuel (20 octobre 2026)

Rapport établi sans modification de code ni publication. Chaque affirmation repose sur l'état du dépôt vérifié pendant la rédaction (git, lecture des fichiers cités) ou sur les journaux de travail des 18 et 19 octobre 2026.

Contexte de la passation : DrSmoke (Muse) reste l'agent principal chargé du développement de Tinda SMS. Codex a effectué une première revue statique du dépôt et préparera, après cet état des lieux, une PR contenant un planning et des instructions.

## 2.1 Point de reprise exact

- Branche de travail : `master`, seule branche locale et distante.
- SHA de tête : `dfddee719be55b4c71fd49cef7f26e92b1bed986` (vérifié identique à `origin/master` par `git ls-remote` pendant la rédaction).
- Historique : 33 commits, du 18 octobre 2026 (lot 0, `ee836f3`) au 19 octobre 2026.
- Dernier travail terminé : exposition des brouillons dans `NewConversationViewModel` (commit `dfddee7`, précédé de `569270a`, DAO `observeAll`). Le backend brouillons est prêt, l'interface ne l'est pas.
- Prochaine action prévue : section « Brouillons (N) » + contacts récents/fréquents dans l'écran Nouveau message, puis geste d'épinglage et section « Pinned Threads » dans la liste (backend déjà en place, commit `68a5a21`).
- Modifications locales non committées : aucune. `git status` propre, aucun fichier non suivi, aucun fichier non poussé, aucun travail sur une autre branche.
- Écarts entre l'environnement de DrSmoke et GitHub : trois fichiers présents localement, gitignorés, absents du dépôt par conception : `keystore/tinda-debug.keystore` (clé de signature permanente), `keystore.properties` (accès au keystore) et `local.properties` (chemin du SDK). Le PRD était local uniquement avant ce dossier (il est reproduit en partie 1).

## 2.2 Documents de référence

- `AGENTS.md` (dans le dépôt) : à jour. Conventions, interdits (aucune donnée personnelle dans les logs, aucun code repris d'une autre app), renvoi au PRD et aux décisions DEC-001 à DEC-010.
- `README.md` (dans le dépôt) : partiellement obsolète. La structure décrite s'arrête aux lots 1 et 2, le coffre y est encore présenté comme un lot futur, et les checklists terrain ne couvrent que les lots 1 et 2. Aucune mention du design Radiant Slate, des fonctions issues des maquettes Stitch, ni du dépôt GitHub.
- `ROADMAP_TODO.md` (dans le dépôt) : obsolète. Datée du 18 octobre au soir ; les lots 3 et 4 y figurent entièrement non cochés alors qu'ils sont codés et poussés. Ne reflète ni le coffre livré, ni Radiant Slate, ni les fonctions Stitch.
- PRD `prd-tinda-sms.md` : il était **local uniquement**, absent du dépôt GitHub. AGENTS.md et README.md le citent par un chemin local (`~/workspace/your_files/prd-tinda-sms.md`, lien symbolique vers le dossier du projet). Il est reproduit intégralement en partie 1 de ce dossier.
- Écart supplémentaire : les décisions prises pour les maquettes Stitch (DEC-S1 à DEC-S6 : statistiques calculées en local, attribution SIM par contact en base Room, épinglage en base locale, filtre 2FA, favoris, navigation basse à 4 onglets) ne sont écrites dans **aucun** document du dépôt. Elles n'existaient que dans les notes de travail de DrSmoke ; elles sont consolidées en partie 3 de ce dossier.

## 2.3 État des fonctionnalités

Légende : Prévu (PRD ou demande, pas de code) / Codé (code présent et build OK) / Vérifié auto (tests unitaires passés) / Validé tél. (retour terrain de SmokeSama).

| Fonctionnalité | État du code | Tests automatiques | Validation téléphone | Reste à faire | Fichiers concernés |
|---|---|---|---|---|---|
| Rôle SMS par défaut + permissions | Codé (contrôle du rôle par 3 sources) | Aucun test dédié | Validé : app SMS par défaut depuis le 18 oct., rôle conservé après la réinstallation du 19 oct. | Rien | `util/SmsRole.kt`, `util/RoleDiagnostic.kt`, `AndroidManifest.xml` |
| Réception SMS | Codé | Aucun test dédié | Validé le 18 oct. au soir | Rien | `sms/SmsReceiver.kt`, `data/SmsRepository.kt` |
| Envoi + accusés | Codé (envoi, SENT, DELIVERED, échecs codes 1 à 4) | Aucun test dédié | Validé le 18 oct. (21h35) après correction du resultCode propre à HiOS | Agrégation multipart (voir 2.5) | `sms/SmsSender.kt`, `sms/SmsStatusReceiver.kt` |
| SMS longs (multipart) | Codé (`sendMultipartTextMessage`, compteur de segments) | `SmsEncodingTest` (encodage GSM 03.38) | Non documentée | Agrégation des statuts par segment ; validation terrain | `sms/SmsSender.kt`, `util/SmsEncoding.kt` |
| Double SIM | Codé (sélecteur par fil, attribution par contact en base) | Aucun test dédié | Partielle : pastilles SIM vues sur captures le 19 oct. ; envoi réel via SIM 2 non confirmé explicitement | Vérifier la correspondance `sim_id` du provider (-1/1 observés) | `data/SimRepository.kt`, `data/SimAssignmentRepository.kt`, `ui/conversation/` |
| Conversations (liste + fil) | Codé (design Radiant Slate, épinglage backend, compteurs par fil, filtre SIM) | Aucun test dédié | Validé par captures le 19 oct. (liste et fil) | UI d'épinglage (geste + section) ; verdict explicite sur la vitesse de chargement | `ui/conversations/`, `ui/conversation/`, `data/SmsRepository.kt` |
| Brouillons | Codé dans le fil (persistance lot 1) ; backend « Brouillons (N) » prêt pour le nouvel écran | Aucun test dédié | Brouillon de fil : checklist lot 1, pas de verdict consigné | UI « Brouillons (N) » dans Nouveau message | `data/local/DraftDao`, `ui/newconversation/` |
| Recherche plein texte | Codé (FTS4, surlignage) | Aucun test dédié | Non validée de façon consignée | Validation terrain | `data/SearchRepository.kt`, `ui/search/` |
| Contacts | Codé (résolution des noms en parallèle, sélecteur dans l'envoi groupé) | Aucun test dédié | Noms vérifiés sur captures | Rien de bloquant | `data/ContactsRepository.kt` |
| Thèmes | Codé (6 thèmes + Radiant Slate par défaut) | `TindaThemesTest` | Radiant Slate vu sur captures ; les 6 thèmes d'origine non validés un par un | Rien | `theme/` |
| Gestes (swipe) | Codé (actions configurables : répondre, archiver, supprimer) | Aucun test dédié | Validés dans le retour global du lot 2 (18 oct. 22h02) | Rien | `ui/conversations/`, `ui/settings/` |
| Langues FR/EN | Codé (`values-fr/` + `values/`) | Aucun | Non validée | Rien | `res/values*/` |
| Accessibilité | Non auditée | Aucun | Non | Audit à prévoir (tailles tactiles, contrastes, lecteurs d'écran) | Tout l'UI |
| Liste noire | Codé (+ journal des blocages) | Aucun test dédié | Validée dans le retour global du lot 2 | Rien | `data/BlockRepository.kt`, `ui/settings/` |
| F9 SMS programmés | Codé (WorkManager, replanification au boot, écran de gestion) | Inclus dans la suite (25/25 à la livraison du lot) | Validée (retour SmokeSama) | Statut SENT prématuré + fenêtre de doublon (voir 2.5) | `sms/ScheduledSmsWorker.kt`, `sms/ScheduledSmsManager.kt`, `ui/schedule/` |
| F10 Répondeur auto | Codé (portées, fenêtres horaires, cooldown, anti ping-pong) | `AutoReplyMatchingTest` | Validée (retour SmokeSama) | Rien | `sms/AutoReplyManager.kt`, `sms/AutoReplyMatching.kt`, `ui/autoreply/` |
| F11 Transfert auto | Codé (règles numéro/mot-clé, anti-boucle, journal) | `ForwardMatchingTest` | Validée (retour SmokeSama) | Risque résiduel de collision sur suffixe (voir 2.5) | `sms/ForwardManager.kt`, `ui/forward/` |
| F14 Envoi groupé | Codé (saisie manuelle + contacts, choix SIM, envoi séquentiel, suivi par destinataire) | Aucun test dédié | Non validée | Refonte design Radiant Slate (signalée « archaïque » par SmokeSama) ; validation terrain | `ui/groupsend/` |
| Coffre : PIN + biométrie | Codé (PIN 4 à 6 chiffres, BiometricPrompt framework) | Aucun test dédié | Validé le 19 oct. (00h34) après correction des règles R8 SQLCipher | Le coffre de l'appareil a été effacé par la désinstallation du 19 oct. ; PIN non recréé à ce jour | `data/VaultRepository.kt`, `ui/vault/` |
| Coffre : routage privé (liste jaune) | Codé (règles numéro ou mot-clé, base principale) | Aucun test dédié | Routage observé pendant les tests du coffre | **Repli vers la boîte normale si le coffre est indisponible (voir 2.5, point 1)** | `sms/SmsReceiver.kt`, `data/VaultRepository.kt` |
| Coffre : notifications déguisées | Codé (phrase de reconnaissance configurable) | Aucun test dédié | Non validée isolément | Validation terrain | `notifications/NotificationHelper.kt` |
| Coffre : déverrouillage par SMS `#PIN` (DEC-005) | **Non implémenté** (aucune trace dans le code) | Aucun | Non | Décision : implémenter ou retirer du PRD | Aucun fichier |
| Suppression automatique (lot 4) | Codé (règles par âge, worker quotidien, replanification au boot) | `AutoDeleteSelectionTest` | Non validée | Validation terrain | `sms/AutoDeleteWorker.kt`, `sms/AutoDeleteManager.kt`, `ui/autodelete/` |
| Bouclier anti-arnaque (lot 5) | **Prévu uniquement, aucun code** | Aucun | Non | Bloqué par HYP-02 : corpus de 200 SMS réels à collecter ; moteur de signaux 100 % local (DEC-006, DEC-009) | Aucun fichier |
| Statistiques SMS (Stitch) | Codé (totaux, bloqués, taux de délivrance, par SIM) | Aucun test dédié | Non (build non installé) | Installation + validation | `data/StatsRepository.kt`, `ui/settings/` |
| Détection OTP + expiration | Codé (carte OTP, copie, expiration estimée) | `OtpDetectionTest` (8 tests) | Vu sur captures (liste et fil) | Rien | `util/OtpDetection.kt` |
| Favoris contacts | Codé (étoile dans le fil) | Aucun test dédié | Non (build non installé) | Installation + validation | `data/FavoriteRepository.kt`, `ui/conversation/` |
| Journal d'événements + diagnostic | Codé (journal sans donnée personnelle, export, écran diagnostic) | `EventLogTest` | Utilisé en débogage réel | À retirer ou verrouiller à la version finale (instruction de SmokeSama) | `util/EventLog.kt`, `util/SendJournal.kt`, `ui/settings/DiagnosticScreen.kt` |
| Options avancées verrouillées par PIN du coffre | Codé (inactif tant qu'aucun PIN n'existe) | Aucun test dédié | Non testé | Nécessite la recréation du PIN du coffre | `ui/settings/AdvancedLockDialog.kt` |
| Signature automatique, tags thématiques, sauvegarde chiffrée, navigation basse 4 onglets, couleurs SIM personnalisables | Prévu (demandes SmokeSama), aucun code | Aucun | Non | À planifier (voir partie 3) | Aucun fichier |

## 2.4 Preuves de vérification

Commande de build et de test standard, utilisée pour chaque lot :

```
cd ~/workspace/tinda-sms
JAVA_HOME=~/workspace/tools/rea/external/jdk-21
GRADLE_USER_HOME=/home/hatch/.gradle JDK_JAVA_OPTIONS="-Djava.net.preferIPv4Stack=true" ~/workspace/.tools/gradle-8.14.3/bin/gradle testDebugUnitTest assembleDebug --offline
```

`./gradlew` est inutilisable dans cet environnement (mode hors ligne) ; le Gradle 8.14.3 local est appelé directement. Résultats consignés : lot 2 `5746a18` : 23/23 tests. F9 `b9270b8` : 25/25. F10 `47b8db9` : 34/34. F11 `5f0927f` : 41/41. F14 `15e44c4` : 41/41. Lot 4 `0305062` : 46/46. À HEAD (`dfddee7`) : 57 méthodes `@Test` présentes dans `app/src/test` ; la commande standard a passé pour chaque commit Stitch, mais le compteur exact du dernier run complet n'a pas été relevé. Signalé comme non vérifié précisément.

Installations : `adb install -r` via tunnel Tailscale + relais socat, puis captures `adb screencap` relues une par une. Échecs et limites connus : démons Gradle tués par manque de mémoire (7 Go de RAM, échec sans rapport avec le code, simple relance) ; simulation tactile bloquée par HyperOS sans l'option développeur « paramètres de sécurité » (les tests tactiles restent manuels) ; requêtes `content query` lentes sur de gros volumes ; coupures de téléchargement sur la connexion de SmokeSama (APK allégé en réponse, commit `4c93b6b`).

Dernier APK testé sur appareil :

- versionName `0.1.0`, versionCode `1` (jamais incrémentés depuis le lot 0 ; la version réelle se lit au commit).
- Dernier build dont l'installation est **prouvée** (captures datées du 19 oct. 02h49) : commit `0242b6a` (design Radiant Slate, liste et fil).
- Les builds `4c8678d` à `dfddee7` (corrections post-retour, statistiques, attribution SIM, épinglage, filtre 2FA, favoris, brouillons) sont compilés et testés en unitaire, **jamais installés** : adb est coupé depuis le 19 oct. vers 03h30.
- Signature : keystore debug permanent du projet (alias `tinda-debug`, RSA 2048), créé le 19 oct. après la crise de signature ; aucun secret divulgué ici.
- Appareil adb : Redmi Note 14 (24117RN76G), HyperOS ; version Android non relevée dans les notes. Les validations des 18 et 19 oct. au soir sont rapportées sur Tecno/HiOS par le README, la ROADMAP et les commentaires du code (les deux appareils ont servi pendant le projet).

## 2.5 Confrontation aux constats de Codex

Constats issus d'une lecture statique de `master`, vérifiés un par un sur la version actuelle.

1. **Repli d'un SMS du coffre vers la boîte normale : CONFIRMÉ.** `sms/SmsReceiver.kt` lignes 76 à 89 : si `ensureOpenForBackground()` échoue, le message est écrit dans le provider système via `insertIncoming` et notifié normalement, avec journal « coffre indisponible, repli boîte normale ». Cela contredit RB-01 du PRD. Cas d'échec possibles : clé Keystore invalidée (réinitialisation de l'écran de verrouillage), base corrompue, échec de chargement SQLCipher. Correctif à proposer : ne jamais écrire dans le provider ; stocker en attente chiffrée ou perdre le message avec journal, selon décision de SmokeSama.
2. **Confusion base ouverte / session déverrouillée : CONFIRMÉ, avec nuance.** `VaultRepository.isUnlocked()` vaut `db != null`. `ensureOpenForBackground()` ouvre la base sans authentification et rien ne la referme ensuite, sauf le bouton de verrouillage manuel de `VaultHomeScreen`. L'écran d'entrée exige toujours le PIN ou la biométrie (aucun contournement côté UI), mais en interne, « base ouverte pour le routage » et « session utilisateur » sont le même état : tout code de l'app peut lire le coffre une fois le receiver passé.
3. **Écart PBKDF2 / vérification du PIN : CONFIRMÉ.** Le PRD (DEC-004) et les commentaires de `VaultCrypto.kt` et `VaultDatabase.kt` annoncent une clé dérivée du PIN par PBKDF2. Le code réel : clé de base aléatoire de 32 octets, enveloppée AES/GCM par une clé appareil du Keystore ; le PIN est vérifié par `SHA-256(sel + PIN)` en une seule passe (`VaultRepository.pinVerifier`). `VaultCrypto.deriveKey` (PBKDF2, 120 000 itérations) existe mais n'est appelé nulle part. Conséquence double : la documentation surestime la protection du PIN, et « PIN oublié = données perdues » n'est vrai que côté interface, puisque la clé appareil suffit à ouvrir la base.
4. **Statuts multipart sans agrégation : CONFIRMÉ.** `SmsSender` émet un token unique par message et un PendingIntent par segment ; `SmsStatusReceiver` met à jour le statut du message à **chaque** accusé de segment, sans lire l'index du segment ni agréger. Le dernier accusé traité écrase les autres : un échec partiel peut être masqué.
5. **Programmation marquée SENT dès le retour d'appel : CONFIRMÉ.** `ScheduledSmsWorker` lignes 59 à 67 : `send()` retourne après l'appel radio, le statut passe aussitôt à `SENT`, sans attendre l'accusé d'envoi (qui met à jour le message, pas la ligne programmée).
6. **Doublon ou état ambigu après interruption : CONFIRMÉ, fenêtre étroite.** Aucun état intermédiaire de type SENDING. Si le processus meurt entre l'appel radio et `updateStatus(SENT)`, la ligne reste `PENDING` et sera renvoyée (nouvel essai WorkManager ou replanification au redémarrage) : doublon possible. L'idempotence actuelle ne couvre que le cas où le statut a été écrit.
7. **Recréation destructive si migration absente : CONFIRMÉ.** `di/AppModule.kt` ligne 39 : `.fallbackToDestructiveMigration()` sur la base principale. Les migrations explicites 3 vers 10 existent, mais tout écart de schéma recrée la base en silence (règles, brouillons, SMS programmés perdus). Le commentaire du fichier assume une base « pas encore publiée ». La base du coffre, elle, n'a ni migrations ni repli destructeur déclarés (version 1).
8. **Comparaison par suffixe et anti-boucle : CONFIRMÉ comme choix technique documenté.** Normalisation chiffres + `takeLast(9)` dans `ForwardManager`, `AutoDeleteWorker` et `SimAssignmentRepository` ; comparaison `endsWith` bidirectionnelle pour la liste jaune. Protections présentes : transfert (destinataire « répondable », différent de l'expéditeur après normalisation, déduplication 60 s), répondeur (cooldown par règle et par numéro, anti ping-pong). Risque résiduel assumé : collision entre deux numéros partageant les 9 derniers chiffres (numéros étrangers ou courts). Couvert par `ForwardMatchingTest` et `AutoReplyMatchingTest` sur les formats locaux.
9. **Reproductibilité avec keystore externe : CONFIRMÉ.** La signature passe par `keystore.properties` et `keystore/tinda-debug.keystore`, tous deux gitignorés (vérifié avant la première poussée GitHub). Un clone frais ne peut ni signer à l'identique ni mettre à jour l'app installée sur le téléphone ; sans `keystore.properties`, la configuration retombe sur des valeurs vides. Choix assumé pour l'hygiène des secrets, mais à documenter pour toute reprise, et bloquant pour une CI qui produirait l'APK de terrain.
10. **Absence de CI et de tests instrumentés : CONFIRMÉ.** Aucun `.github/workflows`, aucun `app/src/androidTest`. Seuls des tests unitaires JVM existent (57 méthodes). Migrations Room, receiver, workers et flux du coffre n'ont aucune couverture exécutée sur appareil ou émulateur (l'environnement n'a pas de KVM).

## 2.6 Décisions et leçons

- Décisions validées par SmokeSama à préserver : DEC-001 à DEC-010 du PRD (base vierge Kotlin, SMS uniquement, Compose, PIN oublié = perte assumée, bouclier en signaux sans blocage, 100 % sur l'appareil, accusés activés par défaut, mode simple par défaut) ; ligne rouge absolue sur YAATA (aucun code, asset ou nom repris) ; signature debug permanente après la crise du 19 oct. ; journal de diagnostic visible jusqu'à la version finale ; aucune donnée personnelle dans les logs et les commits ; feu vert explicite avant toute modification d'un dépôt (le dépôt tinda-sms lui-même a été créé et poussé à sa demande).
- Approches essayées puis abandonnées : `androidx.biometric` et `hilt-work` (indisponibles dans l'environnement hors ligne, remplacés par BiometricPrompt framework et un EntryPoint Hilt manuel) ; émulateur + Appium pour les tests (pas de KVM, 2 vCPU, 7 Go de RAM) ; envoi de l'APK par lien de stockage distant (refusé deux fois par SmokeSama, installation par adb retenue) ; clé debug auto-générée par AGP (perdue lors d'un redémarrage de l'environnement, remplacée par le keystore permanent).
- Particularités Tecno/HiOS observées, avec preuves : `Telephony.Sms.getDefaultSmsPackage()` renvoie null alors que le rôle est tenu (correctif : contrôle en 3 sources, `util/SmsRole.kt`) ; les accusés SMS_SENT/SMS_DELIVERED reviennent avec resultCode 0 alors que le SMS part réellement (correctif : seuls les codes d'erreur documentés 1 à 4 valent échec, `SmsStatusReceiver`, validé sur le terrain) ; le provider de fils est une UNION maison sans `recipient_ids`, dont les `_id` ne correspondent pas à `thread_id`, mais qui expose `address` et `body` directement (correctif dans `SmsRepository.queryThreads` + repli `legacyScanThreads`) ; champ de saisie masqué par la barre de navigation sur Redmi Note 14 (correctif `navigationBarsPadding` + `imePadding`).
- Régressions déjà rencontrées : manifeste déclarant les **noms** de constantes au lieu de leurs **valeurs** pour SMS_DELIVER (l'app absente de la liste des apps SMS, `7ae4fda`) ; règles R8 gardant le mauvais package SQLCipher (crash du coffre à la création du PIN, `b6abdd3`) ; FAB Material 3 consommant l'appui long (correctif : un seul `combinedClickable`, `0305062`) ; perte de la clé debug après redémarrage (keystore permanent). Moyen d'éviter : tester chaque correctif sur l'appareil réel avant de l'annoncer, et ne jamais conclure d'une extraction statique.
- Blocages et manques : adb du téléphone coupé depuis le 19 oct. (adresse et port du débogage sans fil à relever sur l'appareil) ; coffre à reconfigurer sur le téléphone (PIN effacé le 19 oct.) ; corpus HYP-02 non collecté (bloque le lot 5) ; version Android exacte des appareils de test non consignée.

## PASSATION À CODEX

- Branche / SHA : `master` / `dfddee719be55b4c71fd49cef7f26e92b1bed986`, identique à GitHub, arbre propre.
- Travaux locaux à préserver : `keystore/tinda-debug.keystore` et `keystore.properties` (signature permanente, jamais à committer), `local.properties` ; le PRD local (reproduit en partie 1 de ce dossier) ; les notes de décisions Stitch DEC-S1 à DEC-S6, absentes du dépôt (consolidées en partie 3).
- Documents faisant autorité : PRD (partie 1, décisions DEC-001 à DEC-010) et `AGENTS.md` du dépôt. README et ROADMAP_TODO sont en retard sur le code.
- Prochaine action envisagée : lot A du programme (partie 3, durcissement coffre et données) après décisions de SmokeSama, en commençant par le repli du SMS de liste jaune.
- Blocages : accès adb au téléphone coupé depuis le 19 oct. ; corpus HYP-02 manquant pour le lot 5 ; aucune CI ni test instrumenté.


---

# Partie 3 : Programme de travail consolidé

Ce programme rassemble la proposition de DrSmoke (lots A, B, C), les décisions Stitch prises le 19 octobre 2026 et le backlog complet des demandes en attente. Il sert de base à la PR de planning que Codex préparera. L'ordre d'exécution reste soumis aux décisions de SmokeSama listées en fin de partie.

## 3.1 Décisions Stitch (19 octobre 2026, maquettes « Radiant Slate »)

- DEC-S1 : les statistiques (volumes, bloqués, taux de délivrance, par SIM) sont calculées depuis la base locale et le provider système. Aucun traçage propriétaire.
- DEC-S2 : l'attribution SIM par contact est stockée en base Room (table `sim_assignments`), prioritaire sur le choix par fil.
- DEC-S3 : l'épinglage des conversations est stocké en base locale (table `pinned_threads`), pas dans une colonne du provider.
- DEC-S4 : les bulles de discussion (Bubbles API Android 11+) sont en option activable ; **différées** à ce stade (complexité, pas de test possible sans appareil).
- DEC-S5 : le SMSC n'est exposé qu'en lecture ; **différé** (aucune API publique Android pour le configurer).
- DEC-S6 : navigation basse à 4 onglets : Messages / Protection / Archivés / Paramètres. L'onglet « Protection » regroupera liste noire, bouclier et journal des blocages ; il n'existe pas encore d'écran dédié.

État d'implémentation au 20 octobre : DEC-S1, DEC-S2, DEC-S3 codées ; filtre « Codes 2FA », favoris et expiration OTP codés ; DEC-S6 non commencée ; DEC-S4 et DEC-S5 en attente.

## 3.2 Lot A : durcissement coffre et données (priorité 1)

Objectif : fermer les failles confirmées en 2.5 avant tout nouvel élargissement du coffre.

Travaux :

1. Supprimer le repli d'un SMS de liste jaune vers la boîte normale (`SmsReceiver`). Comportement de remplacement à décider par SmokeSama : file d'attente chiffrée en base principale, ou perte du message avec journal. Dans les deux cas, jamais d'écriture dans le provider (RB-01).
2. Séparer l'état « base ouverte pour routage » de l'état « session utilisateur déverrouillée » dans `VaultRepository` ; verrouillage automatique de la session à la sortie de l'écran du coffre et après un délai d'inactivité.
3. Retirer `.fallbackToDestructiveMigration()` de `AppModule` ; écrire les tests de migration Room pour les versions 3 à 10 et les suivantes.
4. Aligner la documentation sur le modèle de clés réel (PRD DEC-004 et commentaires de `VaultCrypto` / `VaultDatabase`), ou ajouter l'enveloppement de la clé de base par une clé dérivée du PIN (PBKDF2) en plus de la clé appareil. Choix à trancher par SmokeSama, car il change la promesse « PIN oublié = données perdues ».
5. Trancher le sort de DEC-005 (déverrouillage par SMS `#PIN`) : implémenter avec consommation stricte du SMS (RB-02), ou retirer la décision du PRD.

Critères de réussite : tests unitaires verts ; revue par la lentille sécurité (AGENTS.md) ; aucun chemin d'écriture d'un SMS de liste jaune vers le provider ; documentation et code d'accord sur le modèle de clés.
Risques : toucher au coffre impose une revalidation terrain complète ; la migration de la base existante des téléphones déjà installés doit être testée avant diffusion.

## 3.3 Lot B : fiabilité d'envoi (priorité 2)

Objectif : des statuts d'envoi qui disent la vérité, y compris pour les SMS longs et les envois programmés.

Travaux :

1. Agrégation multipart dans `SmsSender` / `SmsStatusReceiver` : statut par segment, puis statut du message (SENT seulement si tous les segments sont confirmés ; FAILED si un segment échoue de façon documentée ; état partiel visible sinon).
2. État intermédiaire pour les SMS programmés (`ScheduledMessage`) : PENDING, puis SENDING à la prise en charge par le worker, puis SENT seulement sur accusé d'envoi ; reprise explicite après interruption (un SENDING orphelin est reprogrammé ou marqué, jamais renvoyé en aveugle).
3. Statut GSM granulaire dans l'interface : émis vers l'antenne, reçu par le SMSC, délivré au terminal, en restant honnête sur ce que le SMS permet de confirmer (RB-05).

Critères de réussite : tests unitaires d'agrégation ; validation terrain SMS long (plusieurs segments) et SMS programmé, y compris un redémarrage du téléphone entre la programmation et l'envoi.
Risques : comportements d'accusés variables selon les ROM (le resultCode 0 constaté sur HiOS impose de ne traiter comme échecs que les codes documentés).

## 3.4 Lot C : finitions d'interface et rattrapage terrain (priorité 3)

Objectif : terminer les interfaces dont le backend est prêt et solder les validations terrain en retard.

Travaux :

1. Écran Nouveau message : section « Brouillons (N) », contacts récents et fréquents avec compteur de SMS (backend livré par `569270a` et `dfddee7`).
2. Liste des conversations : geste d'épinglage/désépinglage et section « Pinned Threads » (backend livré par `68a5a21`).
3. Envoi groupé : refonte au design Radiant Slate (écran encore à l'ancien design, signalé par SmokeSama).
4. Navigation basse à 4 onglets (DEC-S6), avec création de l'écran « Protection ».
5. Couleurs SIM personnalisables par l'utilisateur (défauts actuels codés en dur : SIM 1 rouge, SIM 2 ambre).
6. Installation du build cumulé sur le téléphone et validations une par une : envoi groupé F14, suppression automatique, statistiques, favoris, verrouillage des options avancées par le PIN du coffre, recréation du PIN du coffre (effacé le 19 oct.), verdict sur la vitesse d'affichage de la liste.

Critères de réussite : build installé ; captures vérifiées ; verdicts de SmokeSama consignés fonction par fonction.
Risques : dépend entièrement du retour de l'accès adb (adresse et port du débogage sans fil à relever sur l'appareil).

## 3.5 Backlog (demandes validées, non planifiées en lots)

- **Signature automatique** : texte ajouté automatiquement aux SMS sortants (demande de SmokeSama du 19 oct.). Réglage global + désactivation par fil à prévoir.
- **Tags thématiques automatiques** : étiquettes calculées sur l'appareil (mobile money, OTP, arnaque probable, spam, important) avec filtres dans la liste (demande du 19 oct.). À coordonner avec le bouclier (lot 5) pour ne pas construire deux moteurs de signaux.
- **Sauvegarde / restauration locale chiffrée** (F18 du PRD, lot 6 d'origine) : export/import par mot de passe, coffre inclus ; les faux SMS en sont exclus (RB-03).
- **Faux SMS** (F17, DEC-007) : simulation locale uniquement, marquée « simulé », jamais dans le provider, non exportable. Fonction sensible : passage par la lentille sécurité obligatoire avant développement.
- **Modèles de messages et réponses rapides** (F16).
- **Compagnon web local** (F19, DEC-009) : Wi-Fi local et QR pairing, aucun cloud.
- **Pack linguistique lingala** (F22).
- **Chatheads / popup de réponse rapide** (F21).
- **Bulles de discussion** (DEC-S4) et **SMSC en lecture** (DEC-S5) : différés, à reprogrammer seulement avec un appareil de test disponible.
- **Audit accessibilité** : TalkBack sur les parcours critiques, contrastes AA (exigence du PRD, jamais auditée).
- **Politique de version** : versionName toujours à `0.1.0` et versionCode à 1 depuis le lot 0 ; schéma de numérotation à adopter.
- **Documentation du dépôt** : mettre à jour README et ROADMAP_TODO, qui datent du 18 octobre ; y consigner le modèle de clés réel, la procédure de signature et les décisions Stitch.
- **CI GitHub Actions** : possible pour les tests unitaires et le build non signé ; elle ne peut pas produire l'APK de terrain sans exposer le keystore (décision en attente).

## 3.6 Lot 5 d'origine : bouclier anti-arnaque (bloqué)

Prérequis non satisfait : HYP-02, la collecte d'un corpus de 200 SMS réels sur le terrain. Le moteur de signaux (100 % sur l'appareil, signaux seulement, jamais de blocage, DEC-006 et DEC-009) ne peut pas être réglé ni validé sans ce corpus (précision visée ≥ 95 %, faux positifs < 2 %). La collecte relève de SmokeSama ; aucun développement n'est lancé avant.

## 3.7 Décisions en attente de SmokeSama

1. Comportement quand le coffre est indisponible au moment où un SMS de liste jaune arrive : file d'attente chiffrée ou perte journalisée (lot A, point 1).
2. Modèle de clés du coffre : aligner la documentation sur le réel, ou ajouter l'enveloppement par clé dérivée du PIN (lot A, point 4).
3. DEC-005 (`#PIN`) : à implémenter ou à retirer du PRD.
4. CI GitHub Actions : oui ou non, et avec quel périmètre.
5. Schéma de numérotation de version à partir du prochain build installé.

---

*Fin du dossier. Document unique de passation Tinda SMS, établi le 20 octobre 2026 par DrSmoke (Muse), agent principal du développement, pour SmokeSama (décideur) et Codex (relecture et planning).*
