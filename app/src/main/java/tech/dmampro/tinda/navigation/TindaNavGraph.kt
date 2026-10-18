package tech.dmampro.tinda.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tech.dmampro.tinda.onboarding.OnboardingScreen
import tech.dmampro.tinda.ui.conversation.ConversationDetailScreen
import tech.dmampro.tinda.ui.conversations.ConversationListScreen
import tech.dmampro.tinda.ui.newmessage.NewConversationScreen
import tech.dmampro.tinda.ui.settings.BlocklistScreen
import tech.dmampro.tinda.ui.settings.SettingsScreen

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
            )
        }
        composable(Routes.BLOCKLIST) {
            BlocklistScreen(onBack = { navController.popBackStack() })
        }
    }
}
