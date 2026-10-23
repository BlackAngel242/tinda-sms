# Tinda SMS : six profils, trente questions et fonctionnalités possibles

Étude PRD Table Ronde, 23 octobre 2026. Périmètre : conception ; aucune modification du prototype ou du dépôt Android.

## Cadrage et statut des informations

[CONFIRMÉ] Six profils demandés : personne infidèle, gérant de tontine, blogueur, responsable politique, journaliste et utilisateur sous pseudonyme « Anonymous ».
[HYPOTHÈSE] « Gérant de tantine » est interprété comme « gérant de tontine ». À corriger si un autre métier était visé.
[CONFIRMÉ] Périmètre explicite : SMS uniquement, sans MMS. La protection recherchée est locale et automatique, avec règles par numéro/mot-clé, lecture protégée, déplacement au coffre ou suppression après lecture.
[CONFIRMÉ] L'accueil doit conserver une palette ardoise, gris et turquoise, sans accumulation de couleurs.
[HYPOTHÈSE] Les besoins ci-dessous sont des personas fictifs pour explorer le produit ; aucun entretien utilisateur n'a été réalisé. Les propositions ne sont pas présentées comme des fonctions déjà livrées.

Niveau RENFORCÉ retenu pour cette étude : contenu intime, informations financières, sources journalistiques et échanges politiques potentiellement sensibles. Cela justifie une attention particulière aux accès, aux exports et à la distinction entre confidentialité locale et sécurité du transport. Il ne s'agit pas d'une validation de sécurité du produit réel.

Résultat recherché : une application SMS simple avec des capacités de classement, de suivi et de discrétion activables selon les besoins. Pas six applications différentes. Les catégories sensibles des personas ne deviennent pas des étiquettes visibles dans l'interface.

Les sources de photos ne servent qu’aux fiches et avatars : aucun envoi de photo par MMS n’est ajouté.

## Les six profils

### 1. La personne infidèle : tester les besoins de discrétion

Situation hypothétique : téléphone parfois visible ou prêté ; des conversations ne doivent pas s'afficher involontairement. Le produit répond à la vie privée sans promettre de faire disparaître toutes les traces d'un échange.

Besoins : masquer les aperçus, séparer les conversations privées, éviter une erreur de SIM et comprendre ce qui reste visible ailleurs.

| Question | Réponse et solution possible |
|---|---|
| 1. Peut-on cacher le texte et le nom dans les notifications ? | Oui, comme réglage proposé : aperçu désactivé globalement ou par conversation, avec notification neutre facultative. Tester aussi les notifications déjà affichées, l'écran verrouillé et les appareils associés. |
| 2. Puis-je protéger certaines conversations sans verrouiller toute l'application ? | Proposer un coffre local chiffré avec code, biométrie facultative et repli par code. Son accès discret ne remplace pas le chiffrement ni une revue de sécurité. |
| 3. Peut-on mettre un SMS à l’abri automatiquement, avant que quelqu’un voie son contenu ? | Oui comme règle proposée : expéditeur ou mot-clé reconnu, entrée directe au coffre et notification sans contenu. Reverrouiller à la sortie et masquer l’aperçu des applications récentes. |
| 4. Comment éviter d'envoyer depuis la mauvaise SIM ? | Mémoriser une SIM par contact et afficher son nom avant l'envoi. Si cette SIM est absente, demander un choix ; ne pas basculer silencieusement vers l'autre. |
| 5. Après lecture du SMS de X, peut-il disparaître ou rejoindre le coffre ? | Oui comme règle proposée : après révélation authentifiée, puis fermeture ou délai choisi, déplacer au coffre ou supprimer localement selon le choix explicite. Un aperçu ne compte pas comme lecture. Aucun effacement chez le destinataire. |

Fonctions à explorer : règles de mise à l’abri, lecture protégée, actions après lecture, notifications privées, coffre, reverrouillage automatique, préférence SIM par contact. Ne pas construire une promesse d'invisibilité ou de suppression à distance.

### 2. Le gérant de tontine : organiser sans confondre SMS et comptabilité

Situation hypothétique : cotisations régulières, plusieurs membres et discussions mêlant rappels, déclarations de versement et contestations.

Besoins : rappeler les échéances, éviter les erreurs de destinataire, distinguer déclaré/vérifié et retrouver une période.

| Question | Réponse et solution possible |
|---|---|
| 1. Puis-je rappeler une échéance à tous les membres sans recopier le message ? | Proposer un groupe de diffusion modifiable : ajouter/retirer un membre, modèle avec prénom/période, SMS individuels et aperçu final. Les envois passés gardent leur liste réelle de destinataires. |
| 2. Comment savoir qui a cotisé et qui doit être relancé ? | Ajouter des statuts manuels « à vérifier », « confirmé » et « à relancer ». Un registre de cotisations complet reste un module distinct, à valider avec les gérants. |
| 3. Un SMS Mobile Money suffit-il pour marquer un paiement confirmé ? | Non automatiquement. Une extraction éventuelle ne crée qu'une proposition à vérifier auprès du service de paiement. Ne jamais afficher « paiement certifié » à partir d'un simple SMS. |
| 4. Qui a reçu mon SMS groupé, et qui faut-il relancer ? | Prévoir une fiche par destinataire : en attente, envoyé, livraison confirmée, échec ou livraison non confirmée. Ne pas assimiler l’absence d’accusé à un échec. La reprise vise les échecs identifiés, avec un avertissement avant tout renvoi à un état incertain. |
| 5. Puis-je sortir le récapitulatif d'un mois pour une réunion ? | Proposer un export filtré par période avec messages, annotations et statuts distincts. Il s'agit d'un relevé de suivi, pas d'une preuve autonome de paiement. |

Fonctions à explorer : listes de diffusion, modèles à variables, rappels, suivi individuel et annotations. Le mini-registre financier est une extension future, pas une obligation du cœur SMS.

### 3. Le blogueur : répondre et prévenir sans arroser tout le carnet d'adresses

Situation hypothétique : lecteurs, partenaires et clients contactent le même numéro ; les réponses récurrentes prennent du temps.

Besoins : classer les interlocuteurs, préparer des réponses, annoncer à des personnes volontaires et éviter les erreurs de diffusion.

| Question | Réponse et solution possible |
|---|---|
| 1. Comment reconnaître et séparer lecteurs, partenaires et clients ? | Proposer étiquettes et filtres, avec photo du carnet d’adresses ou photo locale choisie. Garder le numéro visible dans la fiche ; une photo n’est pas une preuve d’identité. |
| 2. Puis-je réutiliser une réponse en l'adaptant à la personne ? | Oui : bibliothèque de réponses rapides avec variables simples et aperçu obligatoire. Si une variable manque, bloquer l'envoi jusqu'à sa correction. |
| 3. Puis-je préparer l'annonce de demain dès ce soir ? | Oui : brouillon puis programmation modifiable et annulable. Afficher qu'une disponibilité réseau et les contraintes Android peuvent décaler le départ réel. |
| 4. Les destinataires verront-ils les numéros des autres abonnés ? | Prévoir des SMS individuels, avec cette précision avant envoi. Une liste de diffusion n'est pas une conversation de groupe partagée. |
| 5. Comment arrêter les annonces pour une personne qui ne les veut plus ? | Ajouter une exclusion par liste et une liste de suppression prioritaire sur les programmations futures. La reconnaissance automatique de « STOP » reste à tester et doit être explicite. |

Fonctions à explorer : étiquettes, modèles, programmation et exclusions de diffusion. Pas de CRM complet ni d'analyseur de réseaux sociaux par défaut.

### 4. Le responsable politique : traiter des demandes et maîtriser les communications

Situation hypothétique : sollicitations citoyennes, demandes internes, rendez-vous et annonces se mélangent.

Besoins : suivre les demandes, distinguer réception/traitement, éviter les engagements automatiques et contrôler les accès d'une équipe.

| Question | Réponse et solution possible |
|---|---|
| 1. Comment ne plus perdre une demande dans les conversations ? | Ajouter des étiquettes de suivi « nouveau », « à traiter », « en attente » et « clos », avec rappel facultatif. Conserver le SMS original séparé des annotations. |
| 2. Peut-on accuser réception sans promettre une solution ? | Proposer un répondeur configuré avec un texte sobre, par exemple « Votre message a été reçu ». Afficher sa portée avant activation et éviter les réponses répétées à chaque SMS. |
| 3. Mon assistant peut-il répondre sans voir mes échanges personnels ? | Cela demande des comptes, des autorisations et un journal d'actions. Un coffre sur un téléphone partagé ne suffit pas ; proposer un espace d'équipe séparé seulement si le besoin est confirmé. |
| 4. Puis-je envoyer une annonce aux personnes qui souhaitent la recevoir ? | Proposer des listes volontaires, une prévisualisation, un volume estimé et une validation finale. Inclure une exclusion des personnes qui se sont retirées. |
| 5. Comment savoir si une demande a reçu une réponse et à quelle heure ? | Montrer le dernier envoi, ses horaires connus et le suivi manuel. Appui long puis Détails pour les dates et la SIM ; « livré » ne signifie pas « lu », et un horaire inconnu doit rester signalé comme indisponible. |

Fonctions à explorer : suivi des sollicitations, accusé de réception neutre, listes volontaires, rappels et historique des actions. La collaboration d'équipe est une extension architecturale, pas un petit bouton à ajouter.

### 5. Le journaliste : protéger ses sources et conserver le contexte

Situation hypothétique : informations reçues par SMS, rendez-vous et sources sensibles ; besoin de retrouver le contexte sans altérer les originaux.

Besoins : discrétion locale, chronologie, séparation des notes et des messages, maîtrise des exports et connaissance des limites du SMS.

| Question | Réponse et solution possible |
|---|---|
| 1. Puis-je garantir l'anonymat d'une source qui m'écrit par SMS ? | Non. Un alias dans Tinda ne modifie pas les métadonnées de téléphonie. Ne pas utiliser le SMS comme canal à promesse d'anonymat ; distinguer ce besoin d'une messagerie chiffrée adaptée. |
| 2. Comment empêcher qu’un nom de source apparaisse pendant un entretien ? | Appliquer une règle de mise au coffre dès réception, avec contenu masqué avant lecture authentifiée. Pour conserver une source, choisir déplacement au coffre après lecture plutôt que suppression automatique. |
| 3. Comment retrouver un échange et ses horaires précis ? | Étiquette de dossier, filtre de dates, recherche locale et appui long sur une bulle pour Détails. Séparer les dates fournies par le réseau et celles enregistrées localement ; ne pas inventer une heure absente. |
| 4. Puis-je exporter un échange avec mes notes sans mélanger les deux ? | Oui : export chronologique avec messages d'origine et annotations dans des zones séparées. La présentation ou une empreinte de fichier ne prouve pas, à elle seule, l'identité de l'expéditeur. |
| 5. Si je perds mon téléphone, puis-je récupérer le coffre ? | Seulement avec une stratégie de sauvegarde chiffrée conçue et testée. Le mode sans sauvegarde doit annoncer le risque de perte ; une sauvegarde externe ne doit pas envoyer la clé en clair avec les données. |

Fonctions à explorer : dossiers, recherche temporelle, notes séparées, export contrôlé, notifications discrètes. La sauvegarde du coffre demande une étude spécifique ; pas d'activation cloud automatique.

### 6. L'utilisateur « Anonymous » : réduire les traces sans promettre l'impossible

Situation hypothétique : souhaite une messagerie locale, sans compte supplémentaire ni collecte de contenu ; utilise parfois des pseudonymes.

Besoins : minimisation des données, compréhension des permissions, transparence et distinction entre alias et anonymat.

| Question | Réponse et solution possible |
|---|---|
| 1. Tinda peut-il cacher mon numéro au destinataire d'un SMS classique ? | Pas par un simple réglage d'interface. Une application SMS classique n'offre pas l'anonymat de l'expéditeur ; ne pas proposer un bouton qui laisse croire le contraire. |
| 2. Puis-je l'utiliser sans créer un compte Tinda ? | Garder les fonctions locales sans compte comme principe produit. Toute fonction distante éventuelle devra être présentée séparément, avec son besoin de connexion. |
| 3. Peut-on lire un message uniquement avec une phrase secrète commune ? | Comme verrou local de lecture, oui : cette phrase autorise l’affichage dans Tinda sur l’appareil configuré. Une phrase commune doit être configurée par les personnes concernées ; elle ne protège pas à elle seule les copies SMS hors de Tinda. Le chiffrement du texte avant son envoi est une option distincte à étudier seulement si souhaitée. |
| 4. Puis-je choisir ce que l'application a le droit d'accéder ? | Proposer une explication de chaque permission et une demande au moment utile. Le rôle SMS dépend néanmoins d'exigences Android : ne pas promettre un fonctionnement complet sans les accès nécessaires. |
| 5. Comment limiter les données conservées sans supprimer un échange important ? | Prévoir des durées de conservation avec exclusions explicites et aperçu de ce qui sera supprimé. Le coffre et les éléments marqués « à conserver » ne doivent pas être inclus silencieusement. |

Fonctions à explorer : fonctionnement local sans compte, centre de confidentialité, alias locaux, permission à la demande et conservation configurable. Ne pas présenter Tinda comme un service de SMS anonymes.

## Précision utilisateur : protection locale automatique des SMS

Cette précision modifie les phases 3 à 6 de la réflexion (arbitrages, règles et critères), sans remplacer les six profils ni rouvrir les choix visuels. Aucune fonction n’est codée par cette étude.

### Moteur de règles proposé

Une règle comprend : nom, portée (numéro/contact, mot-clé, combinaison), moment du déclenchement et action. Afficher dans l’éditeur si les conditions signifient « au moins une » ou « toutes ». Les expressions régulières et scripts ne sont pas nécessaires au premier périmètre.

| Déclencheur | Action possible | Réglage utile |
|---|---|---|
| Réception d’un SMS du contact X | Entrée directe dans le coffre | Notification neutre ou silencieuse |
| Réception d’un SMS contenant un mot-clé | Coffre ou attente de révélation protégée | Mot entier / contient, insensible à la casse |
| Révélation après déverrouillage | Démarrer la règle après lecture | Ne pas déclencher sur aperçu, recherche ou statut système Lu |
| Fermeture du message révélé | Déplacer vers le coffre | Pas de délai si la sortie du fil est le choix retenu |
| Délai après révélation | Déplacer ou supprimer localement | Exemple 30 s / 5 min, valeur à confirmer |
| Sortie de l’application ou écran verrouillé | Cacher et reverrouiller immédiatement | Le délai d’effacement peut être exécuté plus tard par Android |

« Lecture » signifie ici « contenu révélé volontairement après autorisation », pas « la personne a effectivement compris tout le texte ». L’app ne peut pas prouver la lecture humaine.

### Éditeur de règle proposé

- Quand : À la réception / Après lecture autorisée / À la fermeture.
- Pour : Numéro ou contact / Mot-clé / Les deux.
- Lecture : Accès normal / Code local / Phrase secrète / Biométrie avec repli.
- Faire : Coffre / Supprimer de cet appareil / Masquer et verrouiller.
- Délai : Immédiat à la fermeture ou délai choisi.
- Notifications : Sans aperçu / Neutre / Silencieuses.
- Exceptions : Messages à conserver, règles désactivées et contacts exclus.
- Avant activation : résumé en français et test sur SMS fictif.

Exemple A : « Les SMS du contact X vont directement au coffre. Après lecture autorisée et fermeture, ils restent dans le coffre verrouillé. »
Exemple B : « Les SMS du contact X contenant le mot-clé convenu demandent une phrase secrète. Après révélation, ils seront supprimés de cet appareil au délai choisi. »
Exemple C : « Les SMS marqués justificatif restent conservés ; aucune suppression automatique ne s’applique. »

La sauvegarde doit être prise en compte : déplacer/supprimer la copie active ne permet pas de promettre la disparition de copies déjà exportées ou sauvegardées. Aucune promesse d’effacement forensique de la mémoire flash.

### Phrase secrète commune : deux sens à distinguer

1. Verrou de lecture local : le besoin retenu ici. La phrase est un secret d’ouverture dans Tinda. Elle peut être commune si les personnes l’ont configurée volontairement ; cela ne synchronise pas magiquement les appareils. Ne pas envoyer cette phrase dans chaque SMS ni l’inscrire en clair dans les journaux.
2. Texte chiffré transporté par SMS : autre option éventuelle. Les deux côtés auraient besoin d’un format compatible et d’une gestion de clés ; la longueur et le nombre de segments augmenteraient. Ce n’est pas décidé par la présente précision sur la protection locale.

Une simple phrase présente en clair dans le SMS est un déclencheur, pas une authentification. Un numéro d’expéditeur sert au classement ; il ne suffit pas à autoriser une commande de déverrouillage, d’export ou d’effacement.

### Priorités et protection contre les erreurs

- Une règle de confidentialité ne doit pas faire fuiter le texte dans les notifications, la recherche publique ou l’écran des applications récentes.
- Les messages du coffre sont exclus des répondeurs et transferts ordinaires par défaut. Un export ou transfert sensible exige une action explicite après déverrouillage.
- Contradictions détectées dans l’éditeur : ne pas choisir silencieusement entre conserver et supprimer. Un mode « conserver » explicite bloque les règles destructives ; les autres conflits demandent un choix avant activation.
- Entrée au coffre : enregistrer durablement la copie chiffrée et vérifier le succès avant de retirer la copie du stockage SMS ordinaire. En cas d’échec, pas de suppression et pas de faux statut « protégé ».
- Après lecture : protéger et effacer selon un événement propre à Tinda, pas d’après le seul champ Lu du fournisseur SMS Android.
- Les SMS à plusieurs segments doivent être réassemblés avant évaluation du mot-clé et de l’action. Pas d’effacement d’un segment isolé laissant un message incohérent.
- Les délais d’effacement Android ne sont pas promis à la seconde. Lors d’une reprise tardive, masquer le message immédiatement et exécuter l’action en attente dès que possible.
- La suppression reste locale ; elle ne rappelle pas un SMS déjà envoyé.

### Acceptation dédiée

| ID | Cas | Attendu |
|---|---|---|
| PRIV-AC-01 | SMS fictif de X correspondant à une règle de coffre | Contenu non révélé dans liste/notification/recherche publique ; accès dans le coffre après autorisation |
| PRIV-AC-02 | Aperçu de notification ou marquage système Lu | Aucun déclenchement de l’action Après lecture |
| PRIV-AC-03 | Mauvaise phrase ou simple mot-clé transmis | Aucun accès au message ni autorisation d’une commande sensible |
| PRIV-AC-04 | Révélation autorisée puis fermeture | Une seule action après lecture exécutée selon le choix annoncé |
| PRIV-AC-05 | Échec d’écriture ou de chiffrement pendant déplacement | Pas de suppression de la copie source ; erreur compréhensible, pas de réussite annoncée |
| PRIV-AC-06 | App quittée, écran verrouillé, redémarrage ou délai retardé | Aucune révélation spontanée ; règle et échéance conservées, exécution reprise sans doublon |
| PRIV-AC-07 | Règles Conserver et Supprimer sur même message | Conflit visible avant activation, priorité de conservation explicite |
| PRIV-AC-08 | SMS multipart contenant un mot-clé à cheval sur deux segments | Évaluation après réassemblage ; action sur le message complet |
| PRIV-AC-09 | Message du coffre correspondant aussi à un transfert automatique | Pas de transfert ordinaire ni répondeur qui expose le contenu |
| PRIV-AC-10 | Pièce jointe MMS ou demande d’envoi multimédia | Hors périmètre : aucun parcours MMS ajouté |

Ces protections requièrent une validation Android réelle, notamment sur notifications, accès système et reprise des tâches. La maquette navigateur ne les démontre pas.

## Ajouts : photos, groupes évolutifs et détails des SMS

Cette demande enrichit les phases 3 à 6 (arbitrages, fonctionnalités et critères). Les cinq questions de chaque profil sont conservées ; les ajouts deviennent transversaux. Aucun accès réel aux contacts n'est effectué dans cette étude.

### Photos liées aux contacts

Deux portées séparées : « Dans Tinda seulement » par défaut et « Mettre à jour la fiche Contacts » sur choix explicite. L'autorisation de lecture du carnet ne vaut pas autorisation d'y écrire une photo.

| Source de photo | Proposition | Limite |
|---|---|---|
| Photo déjà présente dans Contacts Android | L'afficher dans Tinda et rafraîchir l'affichage quand le carnet change | Nécessite l'accès approprié ; absence de photo = initiales |
| Photo sélectionnée par l'utilisateur | Associer localement au contact choisi | Ne pas écraser automatiquement une photo du carnet |
| Photo transmise volontairement via une fonction Tinda future | Étudier un profil facultatif, distinct du transport SMS | Ajouterait un service, une autorisation et éventuellement Internet ; hors socle actuel |
| Photo provenant d'un autre service | Seulement si ce service fournit un accès autorisé adapté | Un numéro seul ne donne pas un accès universel à la photo de profil |

Parcours : fiche du contact -> Photo -> choisir la source -> aperçu -> portée Tinda ou Contacts -> valider. Pour modifier une fiche du carnet, privilégier l’éditeur système Contacts et sa validation. Une éventuelle mise à jour en lot nécessite une étude spécifique des versions Android et droits disponibles : liste avant/après, cases de sélection, photos existantes préservées par défaut, résultat et annulation lorsque possible.

Association : résoudre le numéro avec son contexte pays, relier à l'identité du contact Android et gérer les doublons/fusions. Un numéro recyclé ne doit pas conserver silencieusement une ancienne identité. Ne pas traiter les expéditeurs alphanumériques comme des numéros de personnes. Les contacts en lecture seule doivent être signalés, pas prétendument mis à jour.

La confidentialité a priorité : une photo ou un nom sensible ne doit pas réapparaître dans l'aperçu public d'un fil protégé. L'utilisateur peut choisir des initiales uniformes ; l'accueil garde des cadres neutres et une seule couleur d'accent, même si les photos choisies contiennent naturellement des couleurs.

### Groupes de diffusion modifiables

Nom clair dans l'interface : « Liste de diffusion SMS » ou « Groupe de destinataires ». Ce n'est pas un fil collectif MMS : chaque destinataire reçoit son SMS et répond dans son fil individuel.

- Créer/renommer une liste, ajouter/retirer des contacts, éviter les numéros en double, archiver ou supprimer la liste sans effacer les anciens envois.
- Chaque campagne conserve un instantané des numéros réellement ciblés au départ et de la SIM utilisée. L'historique ne change pas quand le groupe est modifié.
- Au moment de programmer, choix explicite : « liste figée maintenant » ou « membres à l'heure d'envoi ». Par défaut : liste figée, avec nombre de destinataires visible.
- Les retraits volontaires et exclusions de sécurité sont réévalués avant départ, même pour une liste figée. Les nouveaux membres ne sont jamais ajoutés silencieusement à un envoi déjà programmé.
- Une modification du groupe ne provoque pas de renvoi des anciens messages. « Envoyer aux nouveaux membres » est une nouvelle action, avec aperçu et validation.
- Le coût est estimé par nombre de destinataires et segments ; prix monétaire seulement si un tarif ou forfait pertinent a été renseigné.

### Livraison : états honnêtes et individuels

| État | Sens | Action de l'interface |
|---|---|---|
| En attente | Envoi non encore tenté | Annuler ou attendre |
| Envoyé | Envoi confirmé côté appareil/réseau | Attendre l'accusé demandé |
| Livraison confirmée | Accusé réseau de livraison reçu | Afficher la confirmation et sa date connue |
| Échec d'envoi | Erreur d'envoi connue | Motif utile et reprise ciblée |
| Échec de livraison | Échec de livraison explicitement remonté | Motif disponible et action adaptée |
| Livraison non confirmée | Pas d'accusé exploitable à ce stade | Ne pas annoncer « non reçu » ; renvoi avec avertissement de doublon |
| Partiel | Certains segments seulement sont confirmés | Ne pas annoncer livraison complète du SMS long |

Vue de campagne : total, compte par état, filtre, détails par destinataire et reprise des échecs. Les totaux ne doivent pas confondre erreurs d'envoi et absence d'accusé. Exemple illustratif, non test réel : 30 ciblés = 24 livraisons confirmées + 3 envois en attente de confirmation + 2 échecs d'envoi + 1 pas encore envoyé.

Un SMS standard ne fournit pas un accusé de lecture humaine. Les confirmations de livraison dépendent des informations effectivement retournées par le réseau. Lorsqu'un SMS est multipart, conserver les retours par segment et calculer un état global explicite.

### Appui long : Détails du message

L'appui long ouvre un menu avec Détails, Copier, Conserver et les actions autorisées pour ce fil. Un bouton Détails accessible autrement reste disponible pour les personnes qui ne découvrent pas le geste.

Dans Détails, afficher uniquement les champs pertinents et disponibles :
- sens entrant/sortant, contact et numéro ;
- SIM effectivement utilisée ;
- date/heure d'envoi connue et provenance de cette information ;
- date/heure de réception locale pour un SMS entrant ;
- pour un SMS sortant, date/heure de l'accusé ou de son observation, libellée précisément selon ce que fournit le réseau ;
- état d'envoi/livraison ; segments et encodage si utile ;
- règle de confidentialité appliquée et prochaine action locale.

Ne jamais inventer l'heure exacte à laquelle le destinataire a vu le message. Si l'heure d'envoi à l'origine, l'heure de livraison ou le statut est absent, afficher « non disponible » ou « non confirmé ». Les dates doivent inclure le fuseau si une ambiguïté existe. L'appui long sur une bulle verrouillée n'en révèle ni le contenu ni les détails sensibles avant autorisation ; les règles Après lecture ne démarrent pas par un simple appui long sur un aperçu masqué.

### Critères dédiés

| ID | Scénario | Attendu |
|---|---|---|
| EXT-AC-01 | Photo du carnet modifiée à l'extérieur de Tinda | Rafraîchissement sans changement d'identité du contact |
| EXT-AC-02 | Photo locale choisie | Aucun changement du carnet tant que la portée Contacts n'a pas été validée |
| EXT-AC-03 | Doublon, contact fusionné, numéro ambigu ou fiche en lecture seule | Résolution ou choix explicite ; pas d'écrasement silencieux |
| EXT-AC-04 | Ajout/retrait dans un groupe déjà utilisé | Nouveaux envois actualisés ; historique passé inchangé |
| EXT-AC-05 | Groupe modifié après programmation | Comportement conforme au mode figé/dynamique annoncé ; exclusions respectées |
| EXT-AC-06 | Accusé manquant | État non confirmé, jamais faux statut non reçu ou lu |
| EXT-AC-07 | SMS long, un segment seulement confirmé | État partiel, pas livré intégralement |
| EXT-AC-08 | Appui long sur un SMS | Détails lisibles avec heures connues, SIM et état ; valeurs inconnues explicites |
| EXT-AC-09 | Appui long sur un SMS protégé | Aucun contenu ou détail sensible avant autorisation |
| EXT-AC-10 | Groupe avec doublon de numéro | Un envoi par destinataire prévu ; reprise ne duplique pas les réussites |

Priorités proposées : P1 pour groupes modifiables, états individuels et Détails ; P2 pour rafraîchissement des photos Contacts et photo locale ; P3 pour synchronisation de profils via service externe. Aucune récupération universelle de photos n'est promise.

## Table ronde dimensionnée : quinze regards

Les six profils sont complétés par deux utilisateurs lambda et sept regards de validation. Ce sont des perspectives de travail appliquées à l'étude, pas quinze personnes interrogées ni quinze agents effectivement exécutés.

| Regard | Préoccupation | Attente / compromis | Impact |
|---|---|---|---|
| Personne cherchant la discrétion | Exposition sur écran | Coffre et notification maîtrisée ; limites explicites | Confidentialité locale |
| Gérant de tontine | Rappels et contestations | Suivi humainement vérifié ; pas comptabilité automatique | Modèles et annotations |
| Blogueur | Réponses répétées | Étiquettes et diffusion volontaire ; pas CRM complet | Organisation |
| Responsable politique | Suivi des sollicitations | Statuts neutres ; équipe à part | Suivi et droits futurs |
| Journaliste | Source et provenance | Messages originaux distincts des notes | Export contrôlé |
| Utilisateur sous pseudonyme | Collecte et traçage | Local sans compte ; pas faux anonymat | Transparence |
| Utilisateur novice | Compréhension | Trois entrées stables ; détails progressifs | Navigation |
| Utilisateur pressé | Nombre de manipulations | SIM visible et actions rapides | Envoi et prévisualisation |
| Produit / décideur | Périmètre | Socle commun ; modules activables | Priorisation |
| UX | Charge cognitive | Espaces et étiquettes, pas profils sensibles affichés | Architecture d'information |
| Android / architecture | Faisabilité | Étudier permissions et tâches avant promesse | Validation technique |
| Sécurité / vie privée | Fuite des échanges | Revue coffre, transferts et exports | Conditions bloquantes |
| Accessibilité | Lecture et erreurs | Texte agrandissable ; couleur non exclusive | Contrôles et badges |
| QA | Scénarios d'échec | SIM absente, doublon, clé perdue, mauvais destinataire | Critères d'acceptation |
| Support / exploitation | Erreurs terrain | Statuts honnêtes et récupération documentée | Diagnostic et messages |

## Six tensions et arbitrages proposés

IDs EXP-DEC pour ne pas réutiliser les DEC du PRD Android existant.

| ID | Tension | Décision proposée | Contrepartie | Statut / révision |
|---|---|---|---|---|
| EXP-DEC-01 | Personnalisation / simplicité | Garder Messages, Automatisations, Réglages ; étiquettes facultatives | Certains usages demandent une activation | HYPOTHÈSE ; revoir après test de découverte |
| EXP-DEC-02 | Discrétion / promesse de sécurité | Distinguer coffre local, transport SMS et anonymat | Expliquer les limites sans rassurer à tort | CONFIRMÉ sur les limites ; implementation À VALIDER |
| EXP-DEC-03 | Automatisation / erreur d'envoi | Aperçu final, SIM mémorisée et refus de bascule silencieuse | Un choix humain supplémentaire dans les cas ambigus | PROPOSITION À VALIDER |
| EXP-DEC-04 | Paiement déclaré / paiement vérifié | Les SMS ne confirment pas automatiquement les cotisations | Vérification manuelle ou intégration future | PROPOSITION À VALIDER |
| EXP-DEC-05 | Diffusion / contrôle des destinataires | Exclusion et retrait prioritaires ; reprise par échec | Gestion d'une liste et journal individuel | PROPOSITION À VALIDER |
| EXP-DEC-06 | Équipe / téléphone personnel | Différer le multi-utilisateur jusqu'à étude des droits | Pas de collaboration native dans le premier périmètre | PROPOSITION À VALIDER |

DACI : Driver = assistant pour cette proposition ; Approver unique = DrSmoke ; Contributors = perspectives du tableau ; Informed = Muse/Claude seulement lors d'une transmission demandée. Aucun message n'a été envoyé. Les arbitrages doivent être validés avant tout développement ; en cas de désaccord, délai de tranche proposé de 48 h ouvrées, à convenir.

## Red Team et gate

1. Pourquoi ne pas construire davantage ? Six profils ne justifient pas six ensembles de menus. Les cas rares peuvent être servis par étiquettes et modèles.
2. Quelle hypothèse peut invalider le positionnement ? Que les utilisateurs confondent protection locale et anonymat réseau, ou que la richesse des réglages empêche l'envoi simple.
3. Quelle fonction séduisante est prématurée ? Compte d'équipe, registre comptable automatique, cloud de sources sensibles ou SMS prétendument anonymes.

Gate : GO pour tester le socle commun et les parcours sur maquette. PIVOT requis si le produit est positionné comme messagerie anonyme ou confidentielle de bout en bout reposant sur des SMS classiques. Développement du coffre, de l'export sensible et de la sauvegarde conditionné à une revue de sécurité. Pas de décision de mise en production dans cette étude.

Alternatives examinées :
- Ne rien ajouter au prototype : insuffisant pour tester les modèles, le suivi et les erreurs de SIM ; reste utile comme témoin de simplicité.
- Six modes métiers complets : écartés, coût de navigation et maintenance disproportionné.
- Ajouter toutes les options de YAATA : écarté, pas de preuve de besoin pour chaque option et identité originale à conserver.
- Utiliser un registre ou un outil métier distinct : retenu comme alternative pour la comptabilité et le travail d'équipe ; Tinda reste d'abord une application SMS.

## Fonctionnalités possibles et priorité

P1 = prochaine hypothèse à maquetter, pas instruction de développement. P2 = après validation ; P3 = étude indépendante.

| Fonction | Profils servis | Priorité | Condition |
|---|---|---|---|
| Étiquettes et filtres de conversations | Tous | P1 | Même fil, plusieurs étiquettes ; pas de duplication |
| Modèles à variables avec aperçu par destinataire | Tontine, blogueur, politique | P1 | Variable manquante bloque l'envoi |
| SIM préférée par contact et confirmation si indisponible | Tous | P1 | Zéro bascule silencieuse |
| Diffusion individuelle, suivi par destinataire, reprise ciblée | Tontine, blogueur, politique | P1 | Distinguer en attente, envoyé, livré, échec ; pas de lecture supposée |
| Règles par numéro/mot-clé, notifications privées et verrouillage du coffre | Discrétion, journaliste, Anonymous | P1 pour les écrans ; condition sécurité avant production | Vérifier notifications, récents, stockage et exports |
| Statuts manuels et rappels de suivi | Tontine, politique, journaliste | P2 | Aucune déduction de paiement ou de résolution automatique |
| Liste d'exclusion prioritaire | Blogueur, politique | P1 si diffusion ajoutée | Réévaluer les destinataires au moment du départ |
| Recherche par période et export avec notes séparées | Journaliste, tontine, politique | P2 | Portée visible, contrôle d'accès et avertissement avant export |
| Centre de confidentialité | Tous | P1 | Expliquer données, permissions, collecte et effacement |
| Actions après lecture : coffre ou suppression locale, avec exclusions | Tous | P1 pour la conception ; sécurité à valider | Pas de suppression silencieuse du coffre ou des messages conservés |
| Sauvegarde chiffrée facultative | Journaliste, Anonymous | P3 | Modèle de clés et test de restauration avant toute promesse |
| Registre de tontine / espace d'équipe | Tontine / politique | P3 | Besoin et responsabilité validés ; modules distincts |

## Hypothèses à invalider avant extension

| ID | Hypothèse | Risque si fausse | Test peu coûteux | Critère proposé |
|---|---|---|---|---|
| EXP-HYP-01 | Les fonctions communes couvrent les six profils | Trop de modules inutiles | Entretiens avec 2 personnes par segment, puis priorisation par tâches | Au moins 8/12 retrouvent une tâche fréquente dans le socle ; sinon revoir segmentation |
| EXP-HYP-02 | Les utilisateurs comprennent coffre / SMS / anonymat | Fausse assurance pour données sensibles | Expliquer puis faire reformuler à 5 testeurs | 5/5 comprennent que le coffre ne chiffre pas le transport SMS ; sinon revoir les textes avant livraison |
| EXP-HYP-03 | Les modèles et listes réduisent les erreurs | Mauvais nom, mauvaise SIM, mauvais destinataire | 10 envois fictifs avec variables absentes et SIM retirée | Zéro envoi autorisé avec variable manquante ou SIM changée silencieusement |
| EXP-HYP-04 | Le suivi manuel suffit à la tontine | Besoin d'un vrai registre financier | Deux gérants suivent un mois fictif | Ils identifient sans ambiguïté déclaré / vérifié ; sinon sortir le registre du cœur SMS |
| EXP-HYP-05 | Les options restent découvrables | L'accueil se transforme en cockpit | 5 novices cherchent rédaction, programmation, confidentialité | 4/5 réalisent chaque tâche sans aide en moins de 60 s ; revoir menus si échec |

Toutes ces cibles sont proposées, non mesurées. Il n'y a ni statistique de marché ni validation terrain dans les résultats présents.

## Traçabilité et acceptation

| Story | Hypothèse / décision | Critère observable |
|---|---|---|
| EXP-US-01 Classer mes échanges | HYP-01 / DEC-01 | Une étiquette peut être ajoutée/retirée sans dupliquer le fil ; retour au filtre Tous disponible |
| EXP-US-02 Préparer une diffusion personnalisée | HYP-03 / DEC-03,05 | Chaque aperçu contient le bon prénom, la SIM et le texte final ; variable absente bloque |
| EXP-US-03 Reprendre après une panne | HYP-03 / DEC-05 | Une reprise ne réenvoie pas aux destinataires déjà confirmés ; les états incertains sont présentés comme tels |
| EXP-US-04 Garder mes notifications privées | HYP-02 / DEC-02 | Aucune fuite du nom/contenu dans les cas Android effectivement pris en charge et testés ; limites documentées |
| EXP-US-05 Suivre une cotisation | HYP-04 / DEC-04 | Un SMS reconnu ne peut créer qu'un statut À vérifier ; confirmation humaine distincte |
| EXP-US-06 Exporter un échange | HYP-02 / DEC-02 | Portée choisie avant export ; messages et notes visuellement séparés ; pas de promesse de preuve certifiée |
| EXP-US-07 Respecter une exclusion | HYP-03 / DEC-05 | Un contact retiré après programmation est exclu avant départ ; aucune réintégration silencieuse |
| EXP-US-08 Préserver un accueil lisible | HYP-05 / DEC-01 | Une seule couleur d'accent à l'accueil ; statut SIM lisible sans couleur ; cinq novices soumis au protocole défini |

Ces IDs EXP- sont propres à l'exploration ; correspondance avec les DEC/RB existants à faire avant toute intégration. Aucun spec.json d'implémentation n'est fourni, car cette demande concerne des profils et idées, pas une consigne de codage.

## Architecture et exploitation : conséquences utiles

- Maintenir la base Android existante. Avant d'ajouter une fonction, vérifier ses modèles et règles métier actuels ; ne pas remplacer les dépôts ou workers sur la seule base de cette étude.
- Étiquettes, modèles et notes : données locales distinctes du SMS original.
- Diffusion : plan d'envoi par destinataire, avec identifiant et état explicite ; reprise sans doublon, contrôle des exclusions et gestion des résultats incertains.
- Coffre : chiffrement, clés, verrouillage et stockage de notification à examiner ensemble. Le prototype navigateur ne valide aucun de ces mécanismes.
- Export : sortie volontaire de données hors de l'application ; sélection et avertissement utiles au bon moment, pas de transfert cloud par défaut.
- Tontine : aucune écriture automatique de paiement confirmé d'après le texte SMS seul.
- Équipe : comptes, rôles, révocation, journal et synchronisation constituent un autre périmètre.
- Observabilité : mesurer les tâches avec scénarios fictifs, pas en collectant le contenu SMS, les numéros ou les codes réels.

## Ordre proposé pour la conception suivante

1. Tester deux parcours transversaux : diffusion personnalisée et protection des notifications.
2. Tester le moteur de confidentialité : numéro/mot-clé, lecture protégée, puis coffre ou suppression locale. Ajouter classement et suivi manuel si les entretiens confirment leur fréquence.
3. Étudier séparément registre de tontine, collaboration et sauvegarde.

Pas d'estimation de développement ni d'engagement de livraison : ils nécessitent d'abord le choix des fonctions et une lecture technique actualisée.

Réentrée : erreur de compréhension sur anonymat -> reprendre messages et DEC-02 ; échec de simplicité -> reprendre DEC-01 et HYP-05 ; besoin de comptabilité/équipe -> cadrer un module séparé ; contrainte Android nouvelle -> revoir faisabilité et critères avant maquette suivante.

## Sources primaires pour les limites techniques

- Google Messages, « How end-to-end encryption ... provides more security » : https://support.google.com/messages/answer/10262381?hl=en ; SMS/MMS non chiffrés de bout en bout.
- Android Developers, SmsManager : https://developer.android.com/reference/android/telephony/SmsManager ; états d'envoi/livraison et choix de souscription. Une livraison n'établit pas une lecture.
- Android Developers, Task scheduling : https://developer.android.com/develop/background-work/background-tasks/persistent ; tâches persistantes et fenêtres de planification flexibles. Un horaire exact de départ ne doit pas être promis sur la seule base d'un worker.

La distinction entre relevé SMS et paiement vérifié est un arbitrage de conception proposé ici, pas une analyse juridique ou un audit d'un opérateur Mobile Money.

- Android Telephony.Sms : https://developer.android.com/reference/android/provider/Telephony.Sms ; distinguer métadonnées système et événements de révélation dans Tinda.
- Android FLAG_SECURE : https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE ; protection des captures/affichages non sécurisés à évaluer sur les appareils pris en charge.

Sources primaires des ajouts :
- ContactsContract.Contacts : https://developer.android.com/reference/android/provider/ContactsContract.Contacts ; lecture de photo de contact.
- ContactsContract.CommonDataKinds.Photo : https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Photo ; données photo.
- Modifier des données Contacts : https://developer.android.com/identity/providers/contacts-provider/modify-data ; conditions d’écriture et données modifiables.
- SmsManager : https://developer.android.com/reference/android/telephony/SmsManager ; retours par message/segment.
- Telephony.TextBasedSmsColumns : https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns ; champs de dates et états disponibles.
