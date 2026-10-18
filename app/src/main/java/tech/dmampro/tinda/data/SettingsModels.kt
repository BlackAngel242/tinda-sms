package tech.dmampro.tinda.data

/** Action déclenchée par un geste swipe sur une ligne de conversation (F5). */
enum class SwipeAction {
    REPLY,
    ARCHIVE,
    DELETE,
    CALL,
}

/** Langue de l'interface (F8). */
enum class AppLanguage {
    SYSTEM,
    FRENCH,
    ENGLISH,
}
