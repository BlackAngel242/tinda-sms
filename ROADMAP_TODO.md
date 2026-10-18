# Tinda SMS : feuille de route lots 2 à 5

État au 18 oct. 2026 au soir. Lots 0 et 1 terminés et validés sur le terrain (Tecno/HiOS).
Conventions : pas de contenu SMS ni numéro dans les logs/commits, jamais de code YAATA.

## LOT 2 : recherche, thèmes, gestes, liste noire (code fini, reste le terrain)

- [x] F3 recherche plein texte (FTS4)
- [x] F4 6 thèmes + Material You
- [x] F5 gestes swipe
- [x] F6 son d'envoi (validé sur le terrain : OK)
- [x] F7 liste noire + journal
- [x] Écran Paramètres + i18n FR/EN
- [ ] Valider la recherche sur le téléphone
- [ ] Valider les 6 thèmes sur le téléphone
- [ ] Valider les gestes swipe sur le téléphone
- [ ] Valider la liste noire + journal sur le téléphone

## LOT 3 : automatismes (dépend des lots 0 à 2, base validée)

- F9 SMS programmés
  - [ ] Table ScheduledMessage (Room)
  - [ ] UI de planification (sélecteur date/heure)
  - [ ] WorkManager + replanification au redémarrage
  - [ ] Écran de gestion (liste, annuler)
  - [ ] Tests
- F10 Répondeur automatique
  - [ ] Règles (plages horaires, contacts)
  - [ ] Message de réponse configurable
  - [ ] Déclenchement sur SMS entrant
  - [ ] Tests
- F11 Transfert automatique
  - [ ] Règles expéditeur vers destinataire
  - [ ] Protection anti-boucle
  - [ ] Tests
- F14 Envoi groupé
  - [ ] Sélection multiple de contacts
  - [ ] Suivi par destinataire
  - [ ] Tests
- [ ] Build + APK + validation terrain

## LOT 4 : coffre-fort et suppression auto (dépend du lot 3)

- Coffre-fort
  - [ ] Schéma chiffré (SQLCipher déjà intégré)
  - [ ] PIN, oubli = données perdues (DEC-004)
  - [ ] Déverrouillage par SMS #PIN, jamais stocké (DEC-005)
  - [ ] Déplacement des conversations vers le coffre
  - [ ] UI du coffre
  - [ ] Lentille sécurité avant intégration (AGENTS.md)
  - [ ] Tests
- Suppression automatique
  - [ ] Règles (âge des messages, par conversation)
  - [ ] Worker de nettoyage
  - [ ] Tests
- [ ] Build + APK + validation terrain

## LOT 5 : bouclier anti-arnaque (dépend du lot 4 + HYP-02)

- [ ] HYP-02 : collecter le corpus de 200 SMS réels (terrain, bloque le lot)
- [ ] Moteur de signaux on-device (Mobile Money, urgence, liens...)
- [ ] UI : signaux d'alerte uniquement, jamais de blocage (DEC-006)
- [ ] Tests
- [ ] Build + APK + validation terrain

## Chaîne de dépendances

Lot 3 attend : lots 0 à 2 (fondations, validées le 18 oct.).
Lot 4 attend : lot 3 (modèle de données et workers stabilisés).
Lot 5 attend : lot 4 + corpus HYP-02 (200 SMS réels collectés sur le terrain).
