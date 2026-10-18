package tech.dmampro.tinda.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tech.dmampro.tinda.ui.conversation.ConversationDetailScreen
import tech.dmampro.tinda.ui.conversations.ConversationListScreen
import tech.dmampro.tinda.ui.settings.SettingsScreen

/** Graphe de navigation principal (lot 0 : 3 destinations placeholder). */
@Composable
fun TindaNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.CONVERSATIONS,
    ) {
        composable(Routes.CONVERSATIONS) {
            ConversationListScreen(
                onConversationClick = { threadId ->
                    navController.navigate(Routes.conversationDetail(threadId))
                },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
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
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
