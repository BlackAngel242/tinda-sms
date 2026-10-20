# Proposition technique L1 : coffre, session et politique de panne

Rédigée le 21 octobre 2026 par DrSmoke (Muse), pour arbitrage de SmokeSama.
Base : constats confirmés C1 à C3 du dossier de passation, points P0-1 à P0-6 du brief fusionné, sections 3 et 6 du plan de reprise (PR nº 1). Aucun changement de modèle de clés n'est appliqué avant arbitrage : ce document prépare la décision, il ne la prend pas.

## Ce qui est déjà corrigé sans arbitrage

- P0-2 : le routage des expéditeurs alphanumériques est corrigé et testé (branche `claude/reprise`, commit `d89c9b6`). Les règles KEYWORD restent applicables à tous les expéditeurs.
- La séparation d'état est identifiée dans le code : `VaultRepository.isUnlocked()` vaut « base ouverte », ce qui confond ouverture technique de fond et session utilisateur. Les tests de transitions (réception de fond sans session, verrouillage, expiration) seront écrits dès le modèle arrêté, car ils dépendent de lui.

## Décision 1 : conservation privée quand le coffre est indisponible

Problème (C1 / P0-1) : si la base du coffre ne s'ouvre pas à l'arrivée d'un SMS de liste jaune, le code actuel écrit le message dans le provider système et le notifie normalement. C'est une violation de RB-01 et le défaut le plus grave du projet.

Cas de panne à distinguer (plan, section 3) : indisponibilité temporaire (processus, mémoire), échec de chargement SQLCipher, corruption de la base, perte de la clé (Keystore invalidé par une réinitialisation de l'écran de verrouillage).

**Recommandation** : file d'attente privée chiffrée, avec une clé dédiée distincte de celle du coffre (clé Keystore propre à la file). À l'arrivée d'un SMS de liste jaune : si le coffre est ouvert ou ouvrable, écriture directe ; sinon, écriture dans la file chiffrée, notification déguisée inchangée, aucun aperçu. Au prochain déverrouillage réussi, la file est versée dans le coffre puis purgée. Si la clé de la file est elle-même indisponible, le message est perdu et l'événement journalisé (compteur seul, jamais de contenu) : c'est la limite explicite, documentée dans l'application.
Borne de la file : nombre maximal de messages et durée maximale de rétention, au-delà desquels les plus anciens sont écartés avec journalisation du compteur.

Pourquoi pas la perte journalisée simple : elle respecte RB-01 mais fait disparaître des messages précisément dans les situations (téléphone réinitialisé, base abîmée) où l'utilisateur compte sur le coffre.
Pourquoi pas une file dépendant de la clé du coffre : si la clé est perdue, la file est illisible aussi ; elle ne couvre que les indisponibilités temporaires, pas la perte de clé.

## Décision 2 : relation entre PIN, chiffrement, biométrie et réception de fond

État réel (C3) : la clé de la base est aléatoire, enveloppée par une clé appareil du Keystore qui n'exige aucune authentification. Le PIN est vérifié par SHA-256(sel + PIN) en une passe ; la biométrie ouvre la même clé appareil. Conséquences : le PIN et la biométrie sont une porte d'interface, pas une condition de déchiffrement ; la réception de fond fonctionne coffre « verrouillé » ; « PIN oublié = données perdues » (DEC-004) est faux au sens cryptographique, il ne vaut que dans l'interface.

Deux modèles :

- **Modèle A (actuel, à documenter)** : on assume la porte d'interface. La promesse produit devient : « vos messages privés sont chiffrés sur l'appareil et protégés par votre PIN à l'écran ». PIN oublié = réinitialisation du coffre possible après effacement. Plus simple, aucun changement de clé ; mais la promesse du PRD doit être réécrite.
- **Modèle B (DEC-004 appliquée)** : la clé de la base est enveloppée par une clé dérivée du PIN (PBKDF2, 120 000 itérations, fonction déjà présente et inutilisée dans `VaultCrypto`) ; la biométrie déverrouille via un objet cryptographique lié à cette enveloppe. PIN oublié = perte réelle, conforme au PRD. Conséquence directe : la réception de fond ne peut plus écrire dans le coffre tant qu'il est verrouillé ; la file chiffrée de la décision 1 devient le sas d'entrée obligatoire, versée au déverrouillage.

**Recommandation** : modèle B, avec la file de la décision 1 comme sas. C'est le seul qui rend la promesse du PRD vraie, et la file supprime son principal inconvénient pratique. Migration : le coffre du téléphone actuel a été effacé le 19 octobre et n'a pas été recréé ; une installation neuve peut donc démarrer directement en modèle B, sans migration de données. Pour d'éventuelles bases existantes ailleurs, la migration se ferait au premier déverrouillage réussi (ré-enveloppement de la clé), jamais par effacement.

Quel que soit le modèle : ajouter une protection contre les essais répétés du PIN (délai progressif après échecs, sans effacement de données), point E2 du brief fusionné ; les valeurs exactes (seuils et délais) seront proposées avec les tests, pas improvisées.

## Décision 3 : déverrouillage par SMS contenant le PIN (DEC-005)

Le PRD prévoit l'envoi à soi-même d'un SMS `#PIN <code>`, consommé sans trace. Le plan de reprise relève le problème : le SMS transite par l'opérateur et peut laisser des traces hors de Tinda (appareil émetteur, journaux opérateur) ; la promesse « aucune trace » n'est pas tenable, et le code ne contient aucune trace de cette fonction.

**Recommandation** : retirer DEC-005 du PRD, ou la reporter sans date. Le déverrouillage local (PIN + biométrie) couvre le besoin sans exposer le code dans un canal externe. Si SmokeSama tient à la fonction, elle sera réexaminée avec une formulation honnête des traces.

## Décision 4 : sémantique de « conserver N messages » en suppression automatique

État réel (M5) : `keepMinimum` protège les N messages les plus récents parmi les candidats à la suppression du fil, pas parmi tous les messages du fil. Le libellé de l'écran laisse croire que le fil conserve toujours au moins N messages au total.

**Recommandation** : aligner le comportement sur le libellé : compter tous les messages du fil ; ne supprimer, parmi les messages plus vieux que le seuil d'âge, que ceux qui dépassent les N plus récents du fil. Aucun changement n'est appliqué avant arbitrage, car il modifie une fonction destructive ; les tests (messages récents et anciens mélangés, règles superposées) seront écrits avant le correctif, au lot L2.

## Décision 5 : périmètre du chiffrement de la base principale

Question (M11) : faut-il chiffrer aussi la base principale (miroir, règles, brouillons) ?
Éléments : les mêmes messages existent en clair dans le provider SMS du système, que Tinda ne contrôle pas ; chiffrer le miroir ne rend donc pas l'historique « protégé » sur un appareil compromis. Le coût est réel : performance sur appareils modestes, migrations, risque supplémentaire autour des données. Les données propres à la base principale qui n'existent pas dans le provider (règles de la liste jaune, programmation, brouillons) sont sensibles mais d'un autre niveau que le contenu du coffre.

**Recommandation** : ne pas chiffrer la base principale à ce stade ; documenter le périmètre de la promesse : seul le coffre est chiffré, et il est le seul endroit où un message privé doit se trouver (d'où l'importance de la décision 1). Réexamen si le modèle de menace change (par exemple si la sauvegarde locale chiffrée du lot 6 couvre la base principale à l'export, ce qui est un autre sujet).

## Ordre d'exécution proposé après arbitrage

1. File chiffrée et suppression du repli provider (décision 1), avec test de panne simulée : aucun contenu privé dans le provider, les notifications, les aperçus ni les journaux.
2. Modèle de clés retenu (décision 2) + séparation session UI / accès de fond, délai de session centralisé, verrouillage au retour et à l'arrière-plan, protection d'essais PIN.
3. Tests de concurrence réception/verrouillage et de reprise de la file après arrêt du processus.
4. Revue par la lentille sécurité (AGENTS.md), build complet, puis recette appareil quand l'accès sera rétabli.
