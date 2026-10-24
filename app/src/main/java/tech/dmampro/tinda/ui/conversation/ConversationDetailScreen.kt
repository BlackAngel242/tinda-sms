package tech.dmampro.tinda.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.SimInfo
import tech.dmampro.tinda.data.local.Message
import tech.dmampro.tinda.data.local.MessageStatus
import tech.dmampro.tinda.ui.components.MessageSim
import tech.dmampro.tinda.ui.components.SimBadge
import tech.dmampro.tinda.ui.components.SimChoiceControl
import tech.dmampro.tinda.ui.components.messageSimFor
import tech.dmampro.tinda.ui.schedule.ScheduleDialog
import tech.dmampro.tinda.util.OtpDetection
import tech.dmampro.tinda.util.SmsEncoding
import tech.dmampro.tinda.util.TimeFormat

/**
 * Fil de discussion, couleurs du thème (lot 1) :
 * canvas ardoise profonde, bulles sortantes en dégradé teal, libellé
 * de la SIM historique dans le pied de chaque bulle sortante, carte
 * OTP dédiée, modèles rapides, sélecteur SIM et indicateur
 * d'encodage dans le compositeur.
 * La logique d'envoi (SmsSender) et les ViewModels sont inchangés.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    threadId: Long,
    onBack: () -> Unit,
    viewModel: ConversationDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val partsByToken by viewModel.partsByToken.collectAsState()
    val draft by viewModel.draftText.collectAsState()
    var failedMessage by remember { mutableStateOf<Message?>(null) }
    var showSchedule by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Horloge d'affichage : le mot d'état dépend du délai écoulé
    // depuis l'envoi (livraison non confirmée après une heure sans
    // accusé). Elle se rafraîchit toutes les 30 secondes pour qu'un
    // fil ouvert bascule tout seul au dépassement du délai.
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMillis = System.currentTimeMillis()
        }
    }
    val locale = Locale.getDefault()
    val todayLabel = stringResource(R.string.day_today)

    // Messages groupés par jour pour les pilules de date.
    val dayGroups = remember(messages, locale, todayLabel) {
        groupByDay(messages, locale, todayLabel)
    }
    // Pilule réseau + (pilule date + messages) par jour : index du dernier.
    val totalItems = remember(dayGroups) { 1 + dayGroups.sumOf { 1 + it.messages.size } }

    // Toujours scroller vers le dernier message.
    LaunchedEffect(totalItems) {
        if (totalItems > 0) listState.animateScrollToItem(totalItems - 1)
    }

    val segmentInfo = remember(draft) { SmsEncoding.segmentInfo(draft) }
    val slotBySubId = remember(uiState.sims) {
        uiState.sims.associate { it.subscriptionId to it.slotIndex }
    }
    val selectedSim = uiState.sims.firstOrNull { it.subscriptionId == uiState.selectedSimId }
    val selectedSlot = (slotBySubId[uiState.selectedSimId] ?: 0) + 1

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        if (selectedSim != null) {
                            Text(
                                selectedSim.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (uiState.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = stringResource(R.string.favorite_toggle),
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { showSchedule = draft.isNotBlank() },
                        enabled = draft.isNotBlank(),
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = stringResource(R.string.schedule_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            // Le champ de saisie doit rester au-dessus de la barre de
            // navigation système et remonter au-dessus du clavier.
            Composer(
                draft = draft,
                onDraftChange = viewModel::onDraftChange,
                onSend = viewModel::send,
                // Appui long sur le bouton d'envoi = programmer l'envoi.
                onScheduleRequest = { showSchedule = draft.isNotBlank() },
                sending = uiState.sending,
                sendError = uiState.sendError,
                sims = uiState.sims,
                selectedSimId = uiState.selectedSimId,
                selectedSlot = selectedSlot,
                onSimSelected = viewModel::onSimSelected,
                segmentInfo = segmentInfo,
            )
        },
    ) { innerPadding ->
        if (!uiState.ready) {
            Box(Modifier.fillMaxSize().padding(innerPadding))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                dayGroups.forEach { group ->
                    item(key = "day-${group.startOfDay}") { DatePill(group.label) }
                    items(group.messages, key = { it.id }) { message ->
                        val otpCode = if (message.incoming) {
                            OtpDetection.detectCode(message.body)
                        } else {
                            null
                        }
                        if (otpCode != null) {
                            OtpCard(message = message, code = otpCode)
                        } else {
                            MessageBubble(
                                message = message,
                                // SIM historique du message : emplacement
                                // conservé au moment de l'envoi, jamais
                                // déduit de l'emplacement actuel de la
                                // carte, et pas celle du compositeur.
                                messageSim = if (message.incoming) {
                                    null
                                } else {
                                    messageSimFor(message.simSlot)
                                },
                                statusParts = message.token?.let { partsByToken[it] }
                                    ?: emptyList(),
                                nowMillis = nowMillis,
                                onFailedClick = { failedMessage = it },
                            )
                        }
                    }
                }
            }
        }
    }
    failedMessage?.let { message ->
        FailureDialog(
            message = message,
            onDismiss = { failedMessage = null },
        )
    }
    if (showSchedule) {
        ScheduleDialog(
            onConfirm = { atMillis ->
                showSchedule = false
                viewModel.scheduleMessage(draft, atMillis)
            },
            onDismiss = { showSchedule = false },
        )
    }
}

/** Pilule de date centrée ("AUJOURD'HUI, 19 OCTOBRE"). */
@Composable
private fun DatePill(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    messageSim: MessageSim?,
    statusParts: List<tech.dmampro.tinda.sms.PartOutcome>,
    nowMillis: Long,
    onFailedClick: (Message) -> Unit,
) {
    val isIncoming = message.incoming
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isIncoming) 4.dp else 20.dp,
        bottomEnd = if (isIncoming) 20.dp else 4.dp,
    )
    // Aplat de l'accent du thème pour les sortants (un seul accent, lot 1).
    val outgoingBrush = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary),
        start = Offset.Zero,
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .then(
                    if (isIncoming) {
                        Modifier
                    } else {
                        // Halo teal subtil autour des bulles sortantes.
                        Modifier.shadow(
                            elevation = 6.dp,
                            shape = bubbleShape,
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        )
                    },
                )
                .then(
                    if (isIncoming) {
                        Modifier.background(MaterialTheme.colorScheme.surfaceContainer, bubbleShape)
                    } else {
                        Modifier.background(outgoingBrush, bubbleShape)
                    },
                )
                .let {
                    if (!isIncoming && message.status == MessageStatus.FAILED) {
                        it.clickable { onFailedClick(message) }
                    } else {
                        it
                    }
                },
        ) {
            Text(
                message.body,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isIncoming) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
        ) {
            if (isIncoming) {
                Text(
                    TimeFormat.shortDate(message.date, stringResource(R.string.yesterday)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                // État en mots + heure réelle : l'heure affichée est
                // celle de l'événement nommé (envoi, ou livraison
                // confirmée si son heure est conservée).
                StatusLine(message = message, parts = statusParts, nowMillis = nowMillis)
            }
            // SIM historique de CE message, en texte neutre : « SIM 1 »,
            // « SIM 2 », ou « SIM inconnue » si la SIM a été retirée ou
            // n'a pas été persistée. Jamais la SIM du compositeur.
            if (messageSim != null) {
                SimBadge(
                    label = when (messageSim) {
                        is MessageSim.Known ->
                            stringResource(R.string.sim_chip_label, messageSim.slotNumber)
                        MessageSim.Unknown -> stringResource(R.string.sim_unknown_label)
                    },
                )
            }
        }
    }
}

/** Carte OTP dédiée : code isolé en grand, bouton copier, texte original en petit. */
@Composable
private fun OtpCard(
    message: Message,
    code: String,
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(MaterialTheme.colorScheme.surfaceContainer, cardShape),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(R.string.otp_card_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "2FA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        OtpDetection.formatCode(code),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                            .clickable {
                                copyToClipboard(context, code)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.otp_copied),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            stringResource(R.string.otp_copy),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    message.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val expiryMinutes = remember(message.body) {
                    OtpDetection.detectExpiryMinutes(message.body)
                }
                if (expiryMinutes != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.otp_expires_in, expiryMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            // Accent neutre sur le bord droit.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 1.dp)
                    .width(3.dp)
                    .fillMaxHeight(0.8f)
                    .background(
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(50),
                    ),
            )
        }
        Text(
            TimeFormat.shortDate(message.date, stringResource(R.string.yesterday)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp),
        )
    }
}

/** Copie via le presse-papiers système (API stable, pas de dépréciation Compose). */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("otp", text))
}

/**
 * Compositeur : modèles rapides, pastilles SIM, indicateur d'encodage,
 * champ pilule et bouton d'envoi circulaire. La logique d'envoi reste
 * celle du ViewModel (SmsSender).
 */
@Composable
private fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onScheduleRequest: () -> Unit,
    sending: Boolean,
    sendError: Boolean,
    sims: List<SimInfo>,
    selectedSimId: Int,
    selectedSlot: Int,
    onSimSelected: (Int) -> Unit,
    segmentInfo: SmsEncoding.SegmentInfo,
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (sendError) {
            Text(
                stringResource(R.string.status_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        QuickTemplatesRow(
            onInsert = { template ->
                onDraftChange(if (draft.isBlank()) template else "$draft $template")
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (sims.size > 1) {
                SimChoiceControl(
                    sims = sims,
                    selectedSimId = selectedSimId,
                    onSelect = onSimSelected,
                )
            }
            Spacer(Modifier.weight(1f))
            if (draft.isNotEmpty()) {
                EncodingIndicator(segmentInfo = segmentInfo, draft = draft)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val canSend = draft.isNotBlank() && !sending
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(28.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(28.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                stringResource(R.string.composer_hint_sim, selectedSlot),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        CircleShape,
                    )
                    // Appui court = envoi immédiat, appui long = programmer.
                    .combinedClickable(
                        enabled = canSend,
                        onClick = onSend,
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onScheduleRequest()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Tiroir de modèles rapides : un tap insère le texte, sans envoyer. */
@Composable
private fun QuickTemplatesRow(onInsert: (String) -> Unit) {
    val templates = listOf(
        stringResource(R.string.quick_template_1),
        stringResource(R.string.quick_template_2),
        stringResource(R.string.quick_template_3),
        stringResource(R.string.quick_template_4),
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        items(templates) { template ->
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                    .clickable { onInsert(template) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    template,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Micro-indicateur d'encodage : "74/160 • GSM-7 (1 SMS)", bascule en UCS-2 si besoin. */
@Composable
private fun EncodingIndicator(
    segmentInfo: SmsEncoding.SegmentInfo,
    draft: String,
) {
    val isGsm7 = segmentInfo.encoding == SmsEncoding.Encoding.GSM_7
    val used = remember(draft, isGsm7) {
        if (isGsm7) SmsEncoding.septetCount(draft) ?: 0 else draft.length
    }
    val limit = if (isGsm7) 160 else 70
    Text(
        stringResource(
            R.string.encoding_indicator,
            used,
            limit,
            if (isGsm7) "GSM-7" else "UCS-2",
            segmentInfo.parts,
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Regroupement des messages par jour calendaire, ordre conservé. */
private data class DayGroup(
    val startOfDay: Long,
    val label: String,
    val messages: List<Message>,
)

private fun groupByDay(
    messages: List<Message>,
    locale: Locale,
    todayLabel: String,
): List<DayGroup> {
    if (messages.isEmpty()) return emptyList()
    val dateFormat = SimpleDateFormat(
        if (locale.language == "fr") "d MMMM" else "MMMM d",
        locale,
    )
    val groups = mutableListOf<DayGroup>()
    var currentDayStart = -1L
    var currentMessages = mutableListOf<Message>()
    for (message in messages) {
        val dayStart = startOfDay(message.date)
        if (dayStart != currentDayStart) {
            if (currentMessages.isNotEmpty()) {
                groups.add(
                    DayGroup(
                        currentDayStart,
                        dayLabel(currentDayStart, dateFormat, todayLabel, locale),
                        currentMessages,
                    ),
                )
            }
            currentDayStart = dayStart
            currentMessages = mutableListOf()
        }
        currentMessages.add(message)
    }
    if (currentMessages.isNotEmpty()) {
        groups.add(
            DayGroup(
                currentDayStart,
                dayLabel(currentDayStart, dateFormat, todayLabel, locale),
                currentMessages,
            ),
        )
    }
    return groups
}

private fun startOfDay(epochMillis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun dayLabel(
    dayStart: Long,
    dateFormat: SimpleDateFormat,
    todayLabel: String,
    locale: Locale,
): String {
    val datePart = dateFormat.format(Date(dayStart))
    return if (isToday(dayStart)) {
        "$todayLabel, $datePart".uppercase(locale)
    } else {
        datePart.uppercase(locale)
    }
}

private fun isToday(dayStart: Long): Boolean = startOfDay(System.currentTimeMillis()) == dayStart

/** Dialogue de diagnostic : pourquoi ce message est en échec (toucher le ⚠️). */
@Composable
private fun FailureDialog(
    message: Message,
    onDismiss: () -> Unit,
) {
    val causeRes = when (message.failureCode) {
        1 -> R.string.send_fail_generic
        2 -> R.string.send_fail_no_service
        3 -> R.string.send_fail_invalid
        4 -> R.string.send_fail_radio_off
        -1 -> R.string.send_fail_app
        else -> R.string.send_fail_unknown
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.send_fail_title)) },
        text = { Text(stringResource(causeRes)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

/**
 * État d'un message sortant, en mots : « En cours », « Envoyé »,
 * « Livré », « Échec » ou « Livraison non confirmée », suivi de
 * l'heure de l'événement nommé. Le mot vient uniquement des états
 * persistés (voir statusWord) ; jamais de « lu » en SMS pur (RB-05),
 * jamais de « Livré » déduit d'autre chose que l'accusé confirmé.
 * L'heure de livraison n'est affichée que si elle a été conservée
 * au moment de l'accusé (colonne deliveredAt) ; sinon, et pour
 * « Livraison non confirmée », l'heure affichée est celle de
 * l'envoi : le mot dit ce qui est su, l'heure dit quand le message
 * est parti.
 */
@Composable
private fun StatusLine(
    message: Message,
    parts: List<tech.dmampro.tinda.sms.PartOutcome>,
    nowMillis: Long,
) {
    val word = statusWord(message.status, parts, message.date, nowMillis) ?: return
    val (labelRes, color) = when (word) {
        StatusWord.EN_COURS ->
            R.string.status_label_in_progress to MaterialTheme.colorScheme.outline
        StatusWord.ENVOYE ->
            R.string.status_label_sent to MaterialTheme.colorScheme.outline
        StatusWord.LIVRE ->
            R.string.status_label_delivered to MaterialTheme.colorScheme.primary
        StatusWord.ECHEC ->
            R.string.status_label_failed to MaterialTheme.colorScheme.error
        StatusWord.NON_CONFIRME ->
            R.string.status_label_unconfirmed to MaterialTheme.colorScheme.outline
    }
    val shownTime = if (word == StatusWord.LIVRE && message.deliveredAt != null) {
        message.deliveredAt
    } else {
        message.date
    }
    Text(
        "${stringResource(labelRes)} · " +
            TimeFormat.shortDate(shownTime, stringResource(R.string.yesterday)),
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}
