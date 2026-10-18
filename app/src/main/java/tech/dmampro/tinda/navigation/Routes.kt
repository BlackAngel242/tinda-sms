package tech.dmampro.tinda.navigation

/** Routes de navigation (identifiants en anglais, libellés UI en français). */
object Routes {
    const val ONBOARDING = "onboarding"
    const val CONVERSATIONS = "conversations"
    const val CONVERSATION_DETAIL = "conversation/{threadId}"
    const val NEW_CONVERSATION = "new_conversation"
    const val SETTINGS = "settings"
    const val BLOCKLIST = "blocklist"
    const val DIAGNOSTIC = "diagnostic"
    const val VAULT_UNLOCK = "vault_unlock"
    const val VAULT_HOME = "vault_home"
    const val VAULT_CONVERSATION = "vault_conversation/{address}"
    const val VAULT_RULES = "vault_rules"

    fun conversationDetail(threadId: Long) = "conversation/$threadId"
    fun vaultConversation(address: String) =
        "vault_conversation/${android.net.Uri.encode(address)}"
}
