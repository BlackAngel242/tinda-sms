# Matrice des fonctionnalités Tinda SMS

Établie au lot L0 du plan de reprise, le 21 octobre 2026, sur `master` à `dfddee719be55b4c71fd49cef7f26e92b1bed986`.

États distingués, sans les confondre :

- **Prévu** : décrit par le PRD ou une décision, sans code correspondant.
- **Codé** : le code existe et le build passe.
- **Testé JVM** : au moins un test unitaire exécuté couvre la logique.
- **Testé appareil** : validation sur un téléphone réel, avec retour consigné.

Une ligne « codé » ne vaut ni test ni validation terrain.

## Fonctionnalités du PRD (F1 à F22)

| ID | Fonction | État | Testé JVM | Testé appareil | Écarts et notes |
|---|---|---|---|---|---|
| F1 | Conversations + fil, brouillon, accusé par défaut | Codé | Non | Oui (captures 19 oct., retour terrain 18 oct.) | Brouillon de fil codé ; section « Brouillons (N) » du nouvel écran : backend prêt, UI absente |
| F2 | Envoi/réception default handler, double SIM | Codé | Non | Oui (18 et 19 oct.) | Correspondance `sim_id` du provider à revérifier (valeurs -1/1 observées) |
| F3 | Recherche plein texte | Codé | Non | Non | Couverture = miroir Room (messages passés par Tinda), pas l'historique complet du provider système : la recherche n'est pas « réellement complète » au sens du PRD |
| F4 | Thèmes V1 (6 thèmes + dynamique) | Codé | Oui (`TindaThemesTest`) | Partiel (Radiant Slate vu, 6 thèmes non validés un par un) | Défaut incohérent « lagoon » / « radiant » : correctif M4 planifié |
| F5 | Gestes swipe configurables | Codé | Non | Oui (retour global lot 2) | |
| F6 | Son d'envoi signature | Codé | Non | Oui (lot 2) | |
| F7 | Liste noire + journal | Codé | Non | Oui (retour global lot 2) | Normalisation insuffisante (espaces seuls) : correctif P0-7 planifié ; les formats local et international d'un même numéro ne se correspondent pas |
| F8 | Français + anglais, mode simple | Codé | Non | Non | Parité FR/EN des chaînes constatée par l'audit Claude (251 clés) |
| F9 | SMS programmés | Codé partiel | Oui (suite du lot) | Oui (retour SmokeSama) | **Récurrence absente** et **alarmes exactes non utilisées** : seule la voie WorkManager (envoi unique, retard possible en Doze) est implémentée ; la permission `SCHEDULE_EXACT_ALARM` est déclarée sans usage. Statut SENT prématuré et fenêtre de doublon : lot L3 |
| F10 | Répondeur automatique | Codé partiel | Oui (`AutoReplyMatchingTest`) | Oui (retour SmokeSama) | Portées, fenêtres horaires, jours, cooldown et anti ping-pong présents ; **déclenchement par mot-clé absent** alors que le PRD le prévoit |
| F11 | Transfert automatique | Codé | Oui (`ForwardMatchingTest`) | Oui (retour SmokeSama) | Aucun garde-fou sur le transfert de codes OTP ni plafond : proposition au lot L3, décision produit requise |
| F12 | Coffre-fort (liste jaune, SQLCipher, PIN, biométrie) | Codé | Non | Oui (19 oct., avant effacement par désinstallation) | Repli vers le provider si coffre indisponible (P0-1) ; session et base ouverte confondues (P0-4) ; routage des expéditeurs alphanumériques défectueux (P0-2) ; `#PIN` absent (voir DEC-005) ; PIN du téléphone actuel non recréé |
| F13 | Auto-suppression | Codé partiel | Oui (`AutoDeleteSelectionTest`) | Non | Règles par âge et par fil seulement ; **ni mot-clé ni « après lecture »** ; la sémantique de « conserver N messages » compte un sous-ensemble de candidats et non tous les messages du fil (point M5, arbitrage D4 requis avant changement) |
| F14 | Envoi groupé | Codé | Non | Non | **Compteur de segments × destinataires et alerte coût (RB-06) absents de l'écran** ; déduplication sur numéro brut ; ancien design (refonte au lot L4) |
| F15 | Bouclier anti-arnaque | Prévu | Non | Non | Bloqué par HYP-02 : corpus de 200 SMS réels non collecté ; aucun code |
| F16 | Modèles + réponses rapides | Codé partiel | Non | Vu sur captures (fil) | Les modèles rapides qui insèrent du texte existent dans le fil ; un écran de gestion des modèles n'est pas établi |
| F17 | Faux SMS (simulation locale) | Prévu | Non | Non | Aucun code ; fonction sensible, lentille sécurité obligatoire (DEC-007) |
| F18 | Sauvegarde chiffrée locale | Prévu | Non | Non | Aucun code ; lot d'origine 6 |
| F19 | Compagnon web local | Prévu | Non | Non | Aucun code ; DEC-009 |
| F20 | Statistiques d'usage locales | Codé | Non | Non (build non installé) | Compteurs SMS par SIM, total, bloqués, taux de délivrance (`StatsRepository`) ; le dénominateur du taux doit être précisé dans l'UI |
| F21 | Chatheads / popup | Prévu | Non | Non | Aucun code |
| F22 | Pack lingala | Prévu | Non | Non | Aucun code |

## Décisions Stitch (DEC-S1 à DEC-S6, 19 octobre 2026)

| ID | Décision | État | Testé JVM | Testé appareil | Notes |
|---|---|---|---|---|---|
| DEC-S1 | Statistiques calculées en local | Codé | Non | Non | Voir F20 |
| DEC-S2 | Attribution SIM par contact en Room | Codé | Non | Non (build non installé) | Base v10, `sim_assignments` |
| DEC-S3 | Épinglage en base locale | Codé (backend) | Non | Non | UI (geste + section) absente : lot L4 |
| DEC-S4 | Bulles de discussion (option) | Prévu, différé | Non | Non | Aucune API publique testable sans appareil |
| DEC-S5 | SMSC en lecture seule | Prévu, différé | Non | Non | Aucune API publique Android de configuration |
| DEC-S6 | Navigation basse 4 onglets | Prévu | Non | Non | Écran « Protection » à créer ; aucun bouclier actif à afficher tant que F15 n'existe pas |

## Points de contrôle demandés par le plan de reprise

- **Alarmes exactes / récurrence (F9)** : non implémentées. Écart PRD ouvert, à traiter au lot L3 ou à faire arbitrer.
- **Mots-clés du répondeur (F10)** : absents. Écart PRD ouvert.
- **Suppression après lecture (F13)** : absente. Écart PRD ouvert ; s'ajoute à l'arbitrage D4 sur « conserver N ».
- **Recherche réellement complète (F3)** : couverture limitée au miroir Room. À documenter dans l'UI ou à compléter par une synchronisation du provider, décision au lot L4.
- **Alerte coût envoi groupé (RB-06, F14)** : absente de l'écran. À implémenter au lot L4 avec compteur segments × destinataires et seuil par défaut 5.
