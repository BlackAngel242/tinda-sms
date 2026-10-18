package tech.dmampro.tinda.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Base locale applicative (Room).
 *
 * Rappel RB-01 / RB-03 du PRD : le contenu du coffre-fort et les faux SMS ne
 * sont JAMAIS écrits dans le provider SMS du système. Cette base est le miroir
 * applicatif ; le coffre utilise une base chiffrée séparée (lot 4, VaultCrypto).
 */
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val threadId: Long,
    val displayName: String,
    val lastSnippet: String,
    val lastDate: Long,
    val unreadCount: Int,
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadId: Long,
    val body: String,
    val date: Long,
    val read: Boolean,
    val incoming: Boolean,
    /** Vrai uniquement pour les simulations locales (RB-03 : jamais dans le provider). */
    val simulated: Boolean = false,
)

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastDate DESC")
    fun observeAll(): Flow<List<Conversation>>

    @Insert
    suspend fun insertAll(items: List<Conversation>)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY date ASC")
    fun observeForThread(threadId: Long): Flow<List<Message>>

    @Insert
    suspend fun insert(message: Message): Long
}

@Database(entities = [Conversation::class, Message::class], version = 1, exportSchema = false)
abstract class TindaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
