# Tinda SMS : cinq parcours, décisions et travaux à venir

État de référence : 24 octobre 2026. Ce document est une carte de travail, pas une attestation de recette. Les états des PR et des fonctionnalités doivent être revérifiés sur GitHub avant chaque intervention.

Mise à jour du 24 octobre 2026 : PR nº 2 fusionnée (`d4d2921`), PR nº 4 fusionnée (`9ef1765`), PR nº 6 fusionnée (`f86956c`, onboarding aligné sur la vérification du rôle SMS de l'envoi). La recette appareil de la version fusionnée a prouvé la migration 11 vers 12 sur le Redmi (voir journal de recette local). Le parcours SMS quotidien de l'issue nº 7 est en cours de traitement : recherche de contacts dans le champ destinataire, états d'envoi en mots avec heure de livraison conservée (migration 12 vers 13), choix SIM compact.

Sources : [PRD et décisions](../reference/dossier-muse-2026-10-20.md), [matrice datée du 21 octobre](../validation/matrice-fonctionnalites.md), [plan de reprise](../superpowers/plans/2026-10-20-tinda-reprise-muse.md), [avancement du design](AVANCEMENT-LOT1.md). Les cinq profils ci-dessous correspondent aux regards 2 à 6 du PRD ; ce sont des profils de conception, pas cinq entretiens terrain individuels démontrés.

## Vue des besoins

```mermaid
flowchart TB
    A["Novice à Owando"] --> A1["Lire et répondre sans réglages"]
    B["Commerçant pressé"] --> B1["Choisir la bonne SIM et prévoir le coût"]
    C["Expert"] --> C1["Personnaliser et automatiser"]
    D["Professionnel discret"] --> D1["Protéger les échanges sensibles"]
    E["Personne exposée aux arnaques MoMo"] --> E1["Comprendre une alerte sans perdre un vrai SMS"]
    A1 --> F["Tinda : SMS local, simple et fiable"]
    B1 --> F
    C1 --> F
    D1 --> F
    E1 --> F
```

| Profil et scène | Besoin exprimé | Réponse prévue | Référence | État à confirmer et prochaine preuve |
|---|---|---|---|---|
| Novice : ouvre l'app, lit un SMS, répond | Aucun réglage obligatoire, états compréhensibles | Onboarding du rôle SMS, conversation, réponse, mode simple, « livré » seulement si confirmé | US-001, US-012, DEC-008/010, F1/F2/F8 | Parcours codé en partie et vu sur appareil historiquement. Preuves ajoutées par la PR nº 2 fusionnée : transitions d'envoi testées contre les courses du worker et les pannes après soumission ; une panne après l'appel radio conserve l'état incertain, sans renvoi automatique. Refaire une recette du build actuel, permissions refusées comprises ; vérifier le mode simple réel. |
| Commerçant : répond vite avec deux SIM ou écrit à un groupe | Éviter la mauvaise SIM et une facture surprise | SIM visible par message, choix avant envoi, attribution par contact, compteur segments × destinataires et confirmation du coût | US-002/008, RB-06, F2/F14 | Le badge SIM affiche l'emplacement conservé au moment de l'envoi (PR nº 4, migration 11 vers 12) : déplacer, retirer ou remplacer la carte ne réécrit plus l'historique, et les anciens messages restent « SIM inconnue ». Testé en JVM (déplacement, retrait, ancien message) ; preuve appareil après migration encore à faire. Le compteur groupé et l'alerte de coût restent à livrer. |
| Expert : choisit un thème et programme un rappel | Personnalisation cohérente et automatisations fiables | Sept thèmes, navigation, programmation, récurrence, gestes et modèles | US-003/004, F4/F5/F9/F16 | PR nº 4 vue partiellement sur appareil le 24 octobre (accueil, navigation basse à trois onglets, badges SIM) ; captures des sept thèmes, grandes polices et TalkBack restantes. Programmation unique partielle, récurrence et alarmes exactes absentes dans la matrice. Vérifier la persistance après redémarrage. |
| Professionnel discret : marque un expéditeur privé | Aucun contenu privé dans les listes, la recherche, les notifications ou le fournisseur normal | Coffre chiffré, session UI distincte de la réception de fond, politique de panne et migration | US-005/006, DEC-004/005, RB-01, F12/F13 | Risque de repli vers le fournisseur signalé ; modèle de clés et comportement en panne non tranchés. Décision SmokeSama avant toute promesse nouvelle, puis tests de panne et de conservation. |
| Personne exposée aux arnaques MoMo : reçoit un SMS suspect | Être avertie sans bloquer un vrai transfert | Bandeau d'avertissement local, jamais de blocage silencieux, liste blanche et mesure des faux positifs | US-007, DEC-006, RB-04, F15 | Prévu, sans moteur validé. Constituer un corpus consenti et anonymisé hors dépôt ; mesurer vrais/faux positifs avant activation. |

## Parcours de bout en bout

Ces schémas sont des **comportements cibles**. Une flèche ne signifie pas que le code actuel l'implémente.

```mermaid
flowchart TB
    A["Premier lancement"] --> B{"Rôle SMS accordé ?"}
    B -- Oui --> C["Liste et conversation"]
    B -- Non --> D["Expliquer les limites et proposer de réessayer"]
    C --> E["Réponse : SIM affichée, envoi confirmé ou incertain"]
    E --> F["Historique cohérent avec le fournisseur SMS"]
```

```mermaid
flowchart TB
    A["Commerçant : destinataires et texte"] --> B["Choix explicite de la SIM"]
    B --> C["Calcul segments × destinataires"]
    C --> D{"Seuil de coût dépassé ?"}
    D -- Oui --> E["Alerte et confirmation"]
    D -- Non --> F["Envoi"]
    E --> F
    F --> G["Verdict par segment ou non confirmé"]
```

```mermaid
flowchart TB
    A["SMS entrant d'un expéditeur privé"] --> B{"Coffre disponible ?"}
    B -- Oui --> C["Stockage privé chiffré"]
    B -- Panne --> D["Politique de secours à arbitrer et tester"]
    C --> E["Notification sans aperçu sensible"]
    E --> F{"Session UI autorisée ?"}
    F -- Non --> G["Écran verrouillé"]
    F -- Oui --> H["Lecture privée"]
```

```mermaid
flowchart TB
    A["SMS Mobile Money entrant"] --> B["Analyse locale et explicable"]
    B --> C{"Soupçon étayé ?"}
    C -- Oui --> D["Bandeau suspect, message conservé"]
    C -- Non --> E["Lecture normale"]
    D --> F["Retour utilisateur et contrôle des faux positifs"]
```

## Dépendances de livraison

```mermaid
flowchart TB
    A["PR 2 : statuts SMS et migrations (fusionnée, d4d2921)"] --> B["Tests de comportements et recette appareil (partielle, faite le 24 octobre)"]
    B --> C["PR 4 : thèmes, navigation, SIM historique (vers master, en revue)"]
    C --> D["Captures Android, accessibilité, revue visuelle (à compléter)"]
    D --> E["Fusion après revue"]
    E --> F["PR suivantes par parcours utilisateur"]
```

| Priorité | Travail | Sortie vérifiable |
|---|---|---|
| P0 | PR 2 : limiter l'exception du code SMS `0` aux preuves appareil réelles et empêcher un état refusé par Room d'être inscrit dans le fournisseur | Fait, PR nº 2 fusionnée (`d4d2921`) : empreinte Build exacte avec drapeaux indépendants, alignement du fournisseur sur le statut persisté, courses et pannes après soumission testées, CI verte au SHA final (tests JVM, lint, construction debug, tests Room sur la variante ciTest) |
| P0 | Coffre : trancher le modèle de clés et la panne, séparer réception technique et autorisation UI | Décision explicite, tests de panne, aucune fuite vers le fournisseur/notification/log, migration non destructive |
| P1 | PR 4 : thèmes, navigation, SIM historique et marges | Fait en code et en tests JVM ; vu sur appareil : accueil et navigation (24 octobre). Restent : captures des sept thèmes, FR/EN, clair/sombre, grandes polices, TalkBack, clavier et deux SIM après la migration 11 vers 12 |
| P1 | Parcours SMS quotidien (issue nº 7) : rôle SMS diagnostiqué sur l'installation exacte, contacts cherchés pendant la saisie du destinataire, états en mots, SIM compacte | Verdicts des trois sources du rôle relevés sur le Redmi (RoleManager détient le rôle pour le paquet .debug, Settings.Secure à null sur cette ROM) ; recherche de contacts bornée et hors fil UI, résultats obsolètes ignorés, contact sans numéro explicite ; mots d'état déduits des seuls états persistés, heure de livraison posée par l'accusé (migration 12 vers 13 testée) ; contrôle SIM segmenté à deux choix, distinct du badge historique |
| P1 | Commerçant : coût groupé, attribution SIM et statuts | Calcul visible avant confirmation et essais double SIM/SMS long |
| P1 | Novice : mode simple, refus du rôle SMS, brouillons et recherche complète | Parcours sans impasse ni affirmation trompeuse sur l'historique recherché |
| P2 | Expert : récurrence, exactitude horaire, règles et modèles | Redémarrage, restrictions Android et état incertain démontrés |
| P2 | Protection MoMo | Corpus consenti, protocole de mesure, bandeau sans blocage ; aucun écran « protégé » prématuré |
| P3 | Sauvegarde locale chiffrée, compagnon local, lingala, chatheads et statistiques avancées | Une PR et des preuves distinctes par parcours après arbitrage de la priorité |

Les fonctions futures F17 (SMS simulé), F18 (sauvegarde), F19 (compagnon local), F21 (chatheads) et F22 (lingala) sont des demandes du PRD, **pas des fonctions livrées**. Le SMS simulé exige les garde-fous de DEC-007 ; aucun faux SMS dans le fournisseur système ni dans un export.

## Mode d'emploi pour Muse, Codex et les agents suivants

1. Avant une PR, sélectionner une scène et ses critères US/DEC/RB, puis relire le code et la matrice à la tête actuelle. Corriger le tableau si l'état a évolué ; ne pas transformer un test JVM en preuve sur appareil.
2. Montrer le parcours avant/après, les états de panne et la propriété des données. Quand un arbitre manque (coffre, coût ou protection), proposer des options à SmokeSama et avancer sur les tâches indépendantes.
3. Après une remarque de revue, reproduire, corriger, tester et écrire la leçon : erreur, cause, solution et contrôle préventif. Inclure ce contrôle dans la prochaine PR concernée.
4. Décrire la PR en termes courants : scène, problème, changement visible, bénéfice, preuve et limite. Codex vérifie puis fusionne les PR prêtes ; Muse ne fusionne pas lui-même.
5. Mettre ce graphe à jour lorsque le statut change. Ajouter le lien de PR, le SHA testé et la preuve appareil sans contenu SMS réel, numéro, PIN, secret ni capture privée.
