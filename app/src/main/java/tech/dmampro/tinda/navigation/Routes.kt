package tech.dmampro.tinda.navigation

/** Routes de navigation (identifiants en anglais, libellés UI en français). */
object Routes {
    const val ONBOARDING = "onboarding"
    const val CONVERSATIONS = "conversations"
    const val CONVERSATION_DETAIL = "conversation/{threadId}"
    const val NEW_CONVERSATION = "new_conversation"
    const val SETTINGS = "settings"
    const val BLOCKLIST = "blocklist"

    fun conversationDetail(threadId: Long) = "conversation/$threadId"
}
