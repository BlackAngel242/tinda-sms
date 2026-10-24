# Tinda SMS : feuille de route

Mise à jour le 21 octobre 2026 (lot L0 du plan de reprise, PR nº 1).
État détaillé et preuves : `docs/validation/matrice-fonctionnalites.md`.
Conventions : pas de contenu SMS ni numéro dans les logs/commits, jamais de code YAATA.

## Fait et poussé sur master (au 19 octobre 2026, SHA dfddee7)

- Lot 0 : socle Kotlin/Compose, design system, navigation.
- Lot 1 : téléphonie réelle (rôle SMS par défaut, réception, envoi, accusés, double SIM, onboarding). Validé sur le terrain.
- Lot 2 : recherche FTS4, 6 thèmes, gestes swipe, son d'envoi, liste noire + journal, paramètres FR/EN. Validé sur le terrain (retour global de SmokeSama).
- Lot 3 : F9 SMS programmés (WorkManager), F10 répondeur, F11 transfert auto, F14 envoi groupé. F9/F10/F11 validés sur le terrain ; F14 non validé.
- Lot 4 : coffre secret (SQLCipher, PIN 4 à 6 chiffres, biométrie, liste jaune, notification déguisée) et suppression automatique. Coffre validé sur le terrain le 19 octobre ; suppression automatique non validée.
- Design Radiant Slate (maquettes Stitch) : liste, fil, paramètres, nouveau message ; statistiques SMS, attribution SIM par contact, épinglage (backend), filtre Codes 2FA, favoris, expiration OTP, backend brouillons. Builds correspondants compilés et testés, **non installés** sur le téléphone (accès adb coupé le 19 octobre).

## Reprise en cours (branche claude/reprise)

- L0 : référence reproductible (fichiers dans `docs/validation/`), PRD versé au dépôt.
- Correctifs prioritaires : routage des expéditeurs alphanumériques (coffre), normalisation des numéros et liste noire, collisions de notifications, thème initial, migrations sans perte.
- Puis, selon le plan de la PR nº 1 : L1 coffre et session (arbitrages de SmokeSama requis), L2 migrations et suppression, L3 fiabilité des envois, L4 interface (protocole dans `docs/validation/protocole-ui-ux.md`), L5 recette appareil.

## En attente

- Lot 5 : bouclier anti-arnaque. Bloqué par HYP-02 : corpus de 200 SMS réels à collecter sur le terrain. Moteur de signaux 100 % local, signalement sans blocage (DEC-006).
- Validations terrain en retard : F14, suppression automatique, statistiques, favoris, épinglage (UI à finir), verrouillage des options avancées, recréation du PIN du coffre (effacé lors de la désinstallation du 19 octobre).
- Demandes non planifiées : signature automatique, tags thématiques locaux, sauvegarde/restauration chiffrée, navigation basse 4 onglets (DEC-S6), couleurs SIM personnalisables, faux SMS (DEC-007, lentille sécurité), compagnon web local (DEC-009).
