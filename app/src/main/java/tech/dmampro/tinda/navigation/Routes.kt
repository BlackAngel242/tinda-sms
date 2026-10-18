package tech.dmampro.tinda.navigation

/** Routes de navigation (identifiants en anglais, libellés UI en français). */
object Routes {
    const val CONVERSATIONS = "conversations"
    const val CONVERSATION_DETAIL = "conversation/{threadId}"
    const val SETTINGS = "settings"

    fun conversationDetail(threadId: Long) = "conversation/$threadId"
}
