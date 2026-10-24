# Audit L4 : parcours 1 (liste des conversations) et 2 (lecture et réponse)

Rédigé le 21 octobre 2026 par DrSmoke (Muse), selon `protocole-ui-ux.md`.
Sources : captures « avant » du 19 octobre (build `0242b6a`, `~/workspace/shots/radiant-list.png` et `radiant-chat.png`), code actuel de la branche `claude/reprise`, référence scellée Stitch (`radiant_slate/DESIGN.md`). Les captures contiennent des données réelles de l'appareil : ce document ne cite ni contenu de message ni nom de contact.
Limite connue : aucune capture « après » n'est possible sans l'appareil (adb coupé) ; les vérifications clair/sombre, grandes polices, petit écran, clavier ouvert et TalkBack restent dues et sont listées à la fin.

## Inventaire et règles communes (extraits du code et de la référence)

- Couleurs Radiant (jetons `theme/`) : fond `#0B0F14`, surface basse `#121820`, surface `#1A222D`, surface haute `#242E3D`, primaire `#00D2B4`, primaire profond `#00B89C`, textes `#F3F5F7` / `#94A3B8` / `#64748B`, ciel `#38BDF8`, SIM 1 `#F44444`, SIM 2 `#F59E0B`.
- Typographie : échelle MaterialTheme ; la police Plus Jakarta Sans de la maquette n'est pas embarquée (le rendu utilise la police système). Écart assumé à ce stade : embarquer la police alourdit l'APK, arbitrage de SmokeSama si souhaité.
- Formes : cartes rayon 16 dp ; bulles 20 dp avec coin « queue » de 4 dp côté expéditeur ; avatar 48 dp ; pastille de date centrée.
- Espacements : marges d'écran 16 à 20 dp ; contenu de carte 12 dp horizontal, 8 dp vertical.
- États : non lu = nom en gras + pastille teal du compteur ; encodage SIM redondant volontaire (point coloré, libellé texte, liseré de bord) pour rester lisible sans la couleur.
- Interactions validées au terrain et intouchables sans validation : appui long sur l'envoi = programmer, appui long sur « + » = coffre, gestes de balayage configurables, pastilles SIM du composeur.

## Parcours 1 : liste des conversations

**L4-1 IMPORTANT, confirmé dans le code, corrigé.** Le liseré SIM était placé après la ligne de contenu dans la carte : il s'affichait sous le contenu et ajoutait 40 dp de vide à chaque carte (visible sur la capture : cartes très hautes, liseré en bas à droite). Correction présentation seule : le liseré est superposé au bord droit, centré verticalement, sans hauteur ajoutée. Effet attendu : environ 5 à 6 conversations visibles par écran au lieu de 4.

**L4-2 OPTIONNEL, conservé.** Les puces de filtre SIM ont deux lignes (nom + opérateur) contre une pour « Toutes » : hauteurs inégales dans la rangée. Conforme aux maquettes Stitch (cartes SIM avec opérateur), pas de changement.

**L4-3 observation.** Les icônes d'en-tête (groupes, paramètres) n'ont pas de libellé texte ; descriptions TalkBack à vérifier sur l'appareil.

## Parcours 2 : lecture et réponse

**L4-4 CONFORME, vérifié dans le code.** Bulles sortantes : dégradé 135 degrés primaire vers primaire profond et coin queue de 4 dp sont bien implémentés. L'aspect plat sur la capture vient des deux teals de marque, proches en valeur ; ce n'est pas un défaut.

**L4-5 CONFORME dans le code actuel, à re-capturer.** Le pied de bulle affiche l'heure via `TimeFormat.shortDate` (heure le jour même, « Hier HH:mm », sinon date + heure). La capture du 19 octobre, plus ancienne, ne montre que la date : le correctif est postérieur à cette capture. À confirmer par une capture « après ».

**L4-6 NON VÉRIFIABLE sur la capture.** Aucun message entrant visible dans le fil capturé : le rendu des bulles entrantes (surface, coin queue à gauche) n'a pas de preuve visuelle. À capturer sur l'appareil.

**L4-7 CONFORME.** Composeur : rangée de réponses rapides, pastilles SIM avec points colorés cohérents avec la liste, champ avec `navigationBarsPadding` et `imePadding` (le chevauchement Redmi constaté le 18 octobre ne se reproduit pas sur la capture).

## Parcours 3 : nouveau message

Inventaire code seulement : l'écran reprend la structure Stitch (recherche de contact, champs). Aucune capture d'application n'existe pour ce parcours ; l'audit visuel est reporté au retour de l'appareil, conformément à l'ordre du protocole.

## Vérifications restant dues (appareil requis)

Captures avant/après du correctif L4-1 ; modes clair et sombre ; grandes polices ; petit écran ; clavier ouvert sur le parcours 2 ; TalkBack sur les parcours 1 et 2 ; rendu des bulles entrantes (L4-6) ; capture du parcours 3.
