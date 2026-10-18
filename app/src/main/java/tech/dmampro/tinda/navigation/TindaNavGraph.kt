package tech.dmampro.tinda.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tech.dmampro.tinda.util.EventLog
import tech.dmampro.tinda.onboarding.OnboardingScreen
import tech.dmampro.tinda.ui.conversation.ConversationDetailScreen
import tech.dmampro.tinda.ui.conversations.ConversationListScreen
import tech.dmampro.tinda.ui.newmessage.NewConversationScreen
import tech.dmampro.tinda.ui.settings.BlocklistScreen
import tech.dmampro.tinda.ui.settings.DiagnosticScreen
import tech.dmampro.tinda.ui.settings.SettingsScreen
import tech.dmampro.tinda.ui.vault.VaultConversationScreen
import tech.dmampro.tinda.ui.vault.VaultHomeScreen
import tech.dmampro.tinda.ui.vault.VaultRulesScreen
import tech.dmampro.tinda.ui.vault.VaultUnlockScreen
import tech.dmampro.tinda.ui.schedule.ScheduledListScreen
import tech.dmampro.tinda.ui.autoreply.AutoReplyScreen
import tech.dmampro.tinda.ui.forward.ForwardScreen
import tech.dmampro.tinda.ui.groupsend.GroupSendScreen

/**
 * Graphe de navigation principal (lot 1 : onboarding + vraie téléphonie).
 * La destination de départ est choisie par MainActivity (rôle SMS déjà actif ?
 * fil ouvert depuis une notification ?).
 */
@Composable
fun TindaNavGraph(
    startDestination: String = Routes.CONVERSATIONS,
    onRequestDefaultRole: () -> Unit = {},
) {
    val navController = rememberNavController()
    // Journal d'écrans vus (diagnostic à distance, sans contenu).
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            EventLog.log("screen", destination.route ?: "?")
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Routes.CONVERSATIONS) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onRequestDefaultRole = onRequestDefaultRole,
            )
        }
        composable(Routes.CONVERSATIONS) {
            ConversationListScreen(
                onConversationClick = { threadId ->
                    navController.navigate(Routes.conversationDetail(threadId))
                },
                onNewMessageClick = { navController.navigate(Routes.NEW_CONVERSATION) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onOpenOnboarding = { navController.navigate(Routes.ONBOARDING) },
                onVaultLongPress = { navController.navigate(Routes.VAULT_UNLOCK) },
                onGroupSendClick = { navController.navigate(Routes.GROUP_SEND) },
            )
        }
        composable(Routes.NEW_CONVERSATION) {
            NewConversationScreen(
                onBack = { navController.popBackStack() },
                onThreadOpened = { threadId ->
                    navController.navigate(Routes.conversationDetail(threadId)) {
                        popUpTo(Routes.CONVERSATIONS)
                    }
                },
            )
        }
        composable(
            route = Routes.CONVERSATION_DETAIL,
            arguments = listOf(navArgument("threadId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getLong("threadId") ?: 0L
            ConversationDetailScreen(
                threadId = threadId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenBlocklist = { navController.navigate(Routes.BLOCKLIST) },
                onOpenDiagnostic = { navController.navigate(Routes.DIAGNOSTIC) },
                onOpenScheduled = { navController.navigate(Routes.SCHEDULED) },
                onOpenAutoreply = { navController.navigate(Routes.AUTOREPLY) },
                onOpenForward = { navController.navigate(Routes.FORWARD) },
            )
        }
        composable(Routes.BLOCKLIST) {
            BlocklistScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DIAGNOSTIC) {
            DiagnosticScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.VAULT_UNLOCK) {
            VaultUnlockScreen(
                onUnlocked = {
                    navController.navigate(Routes.VAULT_HOME) {
                        popUpTo(Routes.VAULT_UNLOCK) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.VAULT_HOME) {
            VaultHomeScreen(
                onThreadClick = { address ->
                    navController.navigate(Routes.vaultConversation(address))
                },
                onOpenRules = { navController.navigate(Routes.VAULT_RULES) },
                onLock = {
                    navController.navigate(Routes.CONVERSATIONS) {
                        popUpTo(Routes.VAULT_HOME) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.VAULT_CONVERSATION,
            arguments = listOf(navArgument("address") { type = NavType.StringType }),
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address").orEmpty()
            VaultConversationScreen(
                address = address,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.VAULT_RULES) {
            VaultRulesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SCHEDULED) {
            ScheduledListScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.AUTOREPLY) {
            AutoReplyScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.FORWARD) {
            ForwardScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.GROUP_SEND) {
            GroupSendScreen(onBack = { navController.popBackStack() })
        }
    }
}
