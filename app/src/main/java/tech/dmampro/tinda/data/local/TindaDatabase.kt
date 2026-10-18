package tech.dmampro.tinda.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

/**
 * Base locale applicative (Room), version 3 (lot 2 : recherche FTS,
 * liste noire, archive).
 *
 * Rappel RB-01 / RB-03 du PRD : le contenu du coffre-fort et les faux SMS ne
 * sont JAMAIS écrits dans le provider SMS du système. Cette base est le miroir
 * applicatif ; le coffre utilise une base chiffrée séparée (lot 4, VaultCrypto).
 *
 * Recherche plein texte : table virtuelle FTS4 `messages_fts` tenue à jour
 * par triggers sur `messages`. La colonne `is_private` permet d'exclure des
 * messages de la recherche (lot 4 : coffre-fort). FTS4 est disponible sur
 * tout Android dès l'API 1, contrairement à FTS5.
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    RECEIVED,
}

class Converters {
    @TypeConverter
    fun statusToString(status: MessageStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
}

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val threadId: Long,
    val displayName: String,
    val lastSnippet: String,
    val lastDate: Long,
    val unreadCount: Int,
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["providerId"], unique = true)],
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Identifiant de ligne du provider système (null si pas encore écrit). */
    val providerId: Long? = null,
    val threadId: Long,
    val address: String = "",
    val body: String,
    val date: Long,
    val read: Boolean,
    val incoming: Boolean,
    val status: MessageStatus = MessageStatus.RECEIVED,
    /** Jeton de corrélation pour les accusés d'envoi (sortants uniquement). */
    val token: String? = null,
    /** Abonnement utilisé pour l'envoi (-1 = inconnu). */
    val subscriptionId: Int = -1,
    /** Vrai uniquement pour les simulations locales (RB-03 : jamais dans le provider). */
    val simulated: Boolean = false,
)

/** Brouillon auto (F1) : clé "thread:<id>" ou "new". */
@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey val key: String,
    val text: String,
    val updatedAt: Long,
)

/** Numéro bloqué (F7). Seul le numéro est stocké, jamais de contenu. */
@Entity(tableName = "blocked_numbers")
data class BlockedNumber(
    @PrimaryKey val number: String,
    val addedAt: Long,
)

/**
 * Journal des blocages (F7) : événements uniquement (numéro + date),
 * jamais le contenu des messages.
 */
@Entity(tableName = "block_events")
data class BlockEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val date: Long,
)

/** Fil archivé par geste swipe (F5). Un nouveau message le désarchive. */
@Entity(tableName = "archived_threads")
data class ArchivedThread(
    @PrimaryKey val threadId: Long,
    val address: String,
    val archivedAt: Long,
)

/** Résultat de recherche plein texte : extrait avec marqueurs de surlignage. */
data class MessageSearchResult(
    @ColumnInfo(name = "messageId") val messageId: Long,
    @ColumnInfo(name = "threadId") val threadId: Long,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "snippet") val snippet: String,
    @ColumnInfo(name = "date") val date: Long,
)

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastDate DESC")
    fun observeAll(): Flow<List<Conversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<Conversation>)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY date ASC, id ASC")
    fun observeForThread(threadId: Long): Flow<List<Message>>

    @Insert
    suspend fun insert(message: Message): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(messages: List<Message>)

    @Query("UPDATE messages SET status = :status WHERE token = :token")
    suspend fun updateStatusByToken(token: String, status: MessageStatus)

    @Query("UPDATE messages SET providerId = :providerId WHERE id = :id")
    suspend fun updateProviderId(id: Long, providerId: Long)

    @Query("UPDATE messages SET `read` = 1 WHERE threadId = :threadId")
    suspend fun markThreadRead(threadId: Long)

    @Query("SELECT * FROM messages WHERE token = :token LIMIT 1")
    suspend fun findByToken(token: String): Message?

    @Query("DELETE FROM messages WHERE threadId = :threadId")
    suspend fun deleteByThread(threadId: Long)
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): Draft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(draft: Draft)

    @Query("DELETE FROM drafts WHERE `key` = :key")
    suspend fun delete(key: String)
}

@Dao
interface BlockDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun block(number: BlockedNumber)

    @Query("DELETE FROM blocked_numbers WHERE number = :number")
    suspend fun unblock(number: String)

    @Query("SELECT COUNT(*) FROM blocked_numbers WHERE number = :number")
    suspend fun isBlocked(number: String): Int

    @Query("SELECT * FROM blocked_numbers ORDER BY addedAt DESC")
    fun observeBlocked(): Flow<List<BlockedNumber>>

    @Insert
    suspend fun logEvent(event: BlockEvent)

    @Query("SELECT * FROM block_events ORDER BY date DESC LIMIT :limit")
    fun observeEvents(limit: Int): Flow<List<BlockEvent>>

    @Query("SELECT COUNT(*) FROM block_events")
    suspend fun eventCount(): Long
}

@Dao
interface ArchiveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun archive(thread: ArchivedThread)

    @Query("DELETE FROM archived_threads WHERE threadId = :threadId")
    suspend fun unarchive(threadId: Long)

    @Query("SELECT threadId FROM archived_threads")
    suspend fun archivedIds(): List<Long>

    @Query("SELECT * FROM archived_threads ORDER BY archivedAt DESC")
    fun observeArchived(): Flow<List<ArchivedThread>>
}

/**
 * Recherche plein texte (F3) : requête brute (Room ne connaît pas la table
 * virtuelle, la validation statique est donc contournée volontairement).
 * Le marqueur de surlignage utilise la zone privée Unicode (jamais dans un SMS).
 */
@Dao
interface SearchDao {
    @RawQuery
    fun searchRaw(query: SupportSQLiteQuery): List<MessageSearchResult>

    @RawQuery
    fun countRaw(query: SupportSQLiteQuery): Long

    /** Nombre de lignes indexées (requête brute : table virtuelle runtime). */
    suspend fun ftsCount(): Long =
        countRaw(SimpleSQLiteQuery("SELECT COUNT(*) FROM messages_fts"))
}

/** Crée la table FTS4 et ses triggers de synchronisation. */
class FtsCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS messages_fts USING fts4(" +
                "body TEXT, address TEXT, is_private INTEGER, tokenize=unicode61)",
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS messages_fts_ai AFTER INSERT ON messages BEGIN " +
                "INSERT INTO messages_fts(rowid, body, address, is_private) " +
                "VALUES (new.rowid, new.body, new.address, new.simulated); END",
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS messages_fts_ad AFTER DELETE ON messages BEGIN " +
                "DELETE FROM messages_fts WHERE rowid = old.rowid; END",
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS messages_fts_au " +
                "AFTER UPDATE OF body, address, simulated ON messages BEGIN " +
                "DELETE FROM messages_fts WHERE rowid = old.rowid; " +
                "INSERT INTO messages_fts(rowid, body, address, is_private) " +
                "VALUES (new.rowid, new.body, new.address, new.simulated); END",
        )
    }
}

/** Construit une requête FTS sûre à partir d'une saisie utilisateur. */
fun searchMatchQuery(raw: String): SimpleSQLiteQuery {
    val match = toFtsMatch(raw)
    return SimpleSQLiteQuery(
        "SELECT m.id AS messageId, m.threadId AS threadId, m.address AS address, " +
            "snippet(messages_fts, '$HIGHLIGHT_START', '$HIGHLIGHT_END', '…', 0, 14) AS snippet, " +
            "m.date AS date " +
            "FROM messages_fts JOIN messages AS m ON m.rowid = messages_fts.rowid " +
            "WHERE messages_fts MATCH ? AND messages_fts.is_private = 0 " +
            "ORDER BY m.date DESC LIMIT 60",
        arrayOf<Any>(match),
    )
}

/** Marqueurs de surlignage (zone privée Unicode, jamais présents dans un SMS). */
const val HIGHLIGHT_START = "\uE000"
const val HIGHLIGHT_END = "\uE001"

/**
 * Convertit une saisie en expression MATCH FTS sûre : jetons alphanumériques,
 * recherche par préfixe, ET implicite. Retourne une chaîne vide si rien
 * d'exploitable (l'appelant n'exécute pas la requête dans ce cas).
 */
fun toFtsMatch(input: String): String {
    val tokens = input.lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.length >= 2 }
        .distinct()
        .take(8)
    if (tokens.isEmpty()) return ""
    return tokens.joinToString(" ") { "\"$it\"*" }
}

@Database(
    entities = [
        Conversation::class,
        Message::class,
        Draft::class,
        BlockedNumber::class,
        BlockEvent::class,
        ArchivedThread::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TindaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun draftDao(): DraftDao
    abstract fun blockDao(): BlockDao
    abstract fun archiveDao(): ArchiveDao
    abstract fun searchDao(): SearchDao
}
