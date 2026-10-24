package tech.dmampro.tinda.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
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
import tech.dmampro.tinda.ui.autodelete.AutoDeleteScreen
import tech.dmampro.tinda.ui.automations.AutomationsScreen
import tech.dmampro.tinda.ui.components.TindaBottomBar

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
    // Lot 1 : barre basse sur les trois destinations principales.
    // Choix le moins risqué : un Scaffold extérieur porte la barre et
    // transmet son innerPadding au NavHost ; les écrans gardent leurs
    // propres Scaffold et FAB, qui restent au dessus de la barre.
    // Revue Codex PR nº 4 : ce Scaffold extérieur n'applique AUCUN
    // inset système (contentWindowInsets à zéro). Son innerPadding ne
    // contient que la hauteur de la barre basse : Modifier.padding ne
    // consomme pas les insets, et les Scaffold internes les
    // réappliquaient (double marge haute et basse, espace en trop au
    // dessus du clavier). Chaque écran interne redevient seul
    // responsable de ses insets système, comme avant le lot 1. La
    // NavigationBar gère elle-même l'inset de la barre de navigation
    // système (windowInsets par défaut de Material 3), donc la barre
    // basse reste placée au dessus de la zone système.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val mainRoutes = setOf(Routes.CONVERSATIONS, Routes.AUTOMATIONS, Routes.SETTINGS)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in mainRoutes) {
                TindaBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            // Conversations sert de racine quand elle est
                            // dans la pile (cas normal). Démarrage par
                            // notification ou onboarding : pas de popUpTo
                            // vers une destination absente.
                            val hasConversations = try {
                                navController.getBackStackEntry(Routes.CONVERSATIONS)
                                true
                            } catch (e: IllegalArgumentException) {
                                false
                            }
                            if (route == Routes.CONVERSATIONS && hasConversations) {
                                navController.popBackStack(Routes.CONVERSATIONS, false)
                            } else {
                                navController.navigate(route) {
                                    if (hasConversations) {
                                        popUpTo(Routes.CONVERSATIONS) { saveState = true }
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    },
                )
            }
        },
    ) { outerPadding ->
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(outerPadding),
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
                onOpenAutodelete = { navController.navigate(Routes.AUTODELETE) },
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
        composable(Routes.AUTODELETE) {
            AutoDeleteScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.AUTOMATIONS) {
            AutomationsScreen(
                onOpenScheduled = { navController.navigate(Routes.SCHEDULED) },
                onOpenAutoreply = { navController.navigate(Routes.AUTOREPLY) },
                onOpenForward = { navController.navigate(Routes.FORWARD) },
                onOpenGroupSend = { navController.navigate(Routes.GROUP_SEND) },
                onOpenAutodelete = { navController.navigate(Routes.AUTODELETE) },
            )
        }
    }
    }
}
