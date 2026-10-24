package tech.dmampro.tinda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.SimInfo

/**
 * Choix de la SIM du PROCHAIN envoi : contrôle segmenté compact à deux
 * choix explicites (« SIM 1 · nom », « SIM 2 · nom »), à la place des
 * grandes pastilles. Groupe sélectionnable : TalkBack annonce chaque
 * choix comme un bouton radio, sélectionné ou non.
 *
 * Ce contrôle ne dit rien des messages déjà envoyés : leur SIM
 * historique reste portée par le badge de chaque bulle, lu depuis
 * l'emplacement conservé à l'envoi.
 *
 * À n'afficher que si au moins deux SIM sont actives ; avec une SIM
 * unique ou absente, aucun contrôle ne s'affiche et le choix par
 * défaut du système s'applique.
 */
@Composable
fun SimChoiceControl(
    sims: List<SimInfo>,
    selectedSimId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sims.size < 2) return
    Row(
        modifier = modifier
            .selectableGroup()
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sims.forEach { sim ->
            val selected = sim.subscriptionId == selectedSimId
            Row(
                modifier = Modifier
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(sim.subscriptionId) },
                    )
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(
                        R.string.sim_choice_label,
                        sim.slotIndex + 1,
                        sim.displayName,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}
