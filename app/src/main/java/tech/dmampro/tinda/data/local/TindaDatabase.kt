package tech.dmampro.tinda.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

/**
 * Base locale applicative (Room), version 2 (lot 1 : vraie téléphonie).
 *
 * Rappel RB-01 / RB-03 du PRD : le contenu du coffre-fort et les faux SMS ne
 * sont JAMAIS écrits dans le provider SMS du système. Cette base est le miroir
 * applicatif ; le coffre utilise une base chiffrée séparée (lot 4, VaultCrypto).
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

@Database(
    entities = [Conversation::class, Message::class, Draft::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TindaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun draftDao(): DraftDao
}
