# Correctifs de la revue Codex sur la PR nº 2

Date : 23 octobre 2026. Branche : `claude/reprise`. Point par point,
avec ce qui est prouvé, ce qui est seulement compilé ou contrôlé, et
ce qui reste non exécuté dans cet environnement.

## 1. Accusé tardif sur une programmation AMBIGUOUS

`ScheduledMessageDao.resolveByToken` résout désormais une ligne
`SENDING` ou `AMBIGUOUS` portant le jeton de la tentative : un accusé
confirmé arrivé après le balayage résout la programmation en SENT ou
FAILED, et l'écran des programmés ne propose le renvoi que pour les
lignes encore AMBIGUOUS (`ScheduledListScreen`, inchangé sur ce
point). Garde-fou tentative remplacée : le réenfilage remet
`attemptToken` à NULL (requeueAmbiguous et claim), l'ancien jeton ne
peut donc plus correspondre à la ligne. Couvert par
`DaoQueryContractTest` (contrat SQL lu dans la source des DAO).

## 2. Course du balayage

Le balayage ne fait plus lecture puis écriture inconditionnelle :
`ScheduledMessageDao.markAmbiguousIfStillSending` transitionne en
une seule mise à jour conditionnée par l'id, le statut SENDING, le
début et le jeton de la tentative relue. Une ligne résolue entre la
lecture et la transition touche 0 ligne ; `ScheduledSmsManager` ne
journalise que les transitions réellement effectuées (retour 1).

## 3. Callbacks concurrents

`SmsRepository` sérialise les accusés par un `Mutex` singleton autour
de `recordPartSent`, `recordPartDelivered` et `resolveScheduled`
(lecture des segments, agrégation, écriture et résolution programmée
dans la même section critique). En plus du verrou, l'écriture du
statut du message est conditionnelle au niveau DAO :
`updateStatusAndFailureByTokenIfNotTerminal` n'écrase jamais un
statut DELIVERED ou FAILED déjà persisté, sauf réécriture du même
terminal. Le repli historique sans lignes de segments passe par le
même chemin protégé, et le chemin d'exception de `SmsSender` utilise
aussi la variante conditionnelle. `MultipartStatus` reste la fonction
pure d'agrégation, inchangée.

Preuve : contrat SQL vérifié par `DaoQueryContractTest`, agrégation
pure déjà couverte par `MultipartStatusTest` et `SentVerdictTest`.
Non exécuté : un test de concurrence contre le vrai DAO Room (voir
point 5, mêmes limites d'environnement).

## 4. Politique ROM du code 0

Nouvelle fonction pure `isZeroResultCodeProvenRom` (sms/RomPolicy.kt),
à partir des champs de Build. Elle ne retourne vrai que pour les
appareils prouvés sur le terrain le 18 octobre 2026 : Redmi Note 14
sous HyperOS (fabricant Xiaomi, modèle « Note 14 », indication
HyperOS dans l'affichage ou la version incrémentale, préfixes OS1 à
OS3 acceptés) et Tecno sous HiOS (fabricant Tecno et indication
HiOS). L'ancien critère « tout Xiaomi » est supprimé. La même
politique s'applique au callback de livraison
(`isDeliveryConfirmed`) : auparavant, le code 0 y était accepté sur
toutes les ROM ; désormais, hors ROM prouvée, la livraison est
journalisée comme non confirmée et n'est pas enregistrée. Couvert
par `RomPolicyTest` (cas prouvés, Xiaomi autre modèle, Note 14 sans
HyperOS, Tecno sans HiOS, autre fabricant, verdicts de livraison).

## 5. Migrations : exécuté, contrôlé, non exécuté

Exécuté en JVM : `MigrationExecutionTest` invoque réellement le code
des huit migrations contre une base factice qui enregistre le SQL
émis (16 instructions contrôlées, dont la création de
`message_parts` et les colonnes de tentative de la 10 vers 11), et
vérifie la cohérence des schémas exportés 10 et 11.

Non exécuté, précisément : aucune migration n'a tourné contre une
vraie base SQLite dans cet environnement, et la conservation des
données insérées en version ancienne n'est pas prouvée ici. Il
manque, vérifié le 23 octobre 2026 :

- `androidx.room:room-testing` (MigrationTestHelper) : absent du
  cache Gradle hors ligne ;
- `androidx.test` (contexte cible pour un test instrumenté) :
  absent du même cache, et l'ajouter casserait le build hors ligne ;
- les schémas exportés des versions 3 à 9 : seuls 10.json et 11.json
  existent dans app/schemas ; une migration instrumentée complète
  depuis la version 3 ne peut pas être construite sans eux ;
- un appareil ou un émulateur pour exécuter un test instrumenté.

Ces tests instrumentés restent à écrire et à exécuter dès qu'un
environnement avec ces artefacts et un appareil est disponible.
