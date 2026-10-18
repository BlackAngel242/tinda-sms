package tech.dmampro.tinda.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Base chiffrée du coffre secret (SQLCipher). Séparée de TindaDatabase et
 * JAMAIS mélangée au provider SMS du système (RB-01).
 *
 * Contenu : messages secrets entrants/sortants + liste jaune (règles).
 * La clé de chiffrement (32 octets aléatoires) n'est jamais stockée en
 * clair : enveloppée par une clé dérivée du PIN (PBKDF2) et par une clé
 * du Keystore liée à la biométrie. PIN oublié = données perdues (DEC-004).
 */

/** Message du coffre : jamais dans le provider système. */
@Entity(tableName = "vault_messages")
data class VaultMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Numéro d'origine/destination (chiffré au repos par SQLCipher). */
    val address: String,
    val body: String,
    val date: Long,
    val incoming: Boolean,
    val read: Boolean = false,
)

data class VaultThread(
    val address: String,
    val lastSnippet: String,
    val lastDate: Long,
    val unreadCount: Int,
)

@Dao
interface VaultMessageDao {
    @Insert
    suspend fun insert(message: VaultMessage): Long

    @Query(
        "SELECT address, substr(body, 1, 60) AS lastSnippet, MAX(date) AS lastDate, " +
            "SUM(CASE WHEN incoming = 1 AND `read` = 0 THEN 1 ELSE 0 END) AS unreadCount " +
            "FROM vault_messages GROUP BY address ORDER BY lastDate DESC",
    )
    fun observeThreads(): Flow<List<VaultThread>>

    @Query("SELECT * FROM vault_messages WHERE address = :address ORDER BY date ASC, id ASC")
    fun observeForAddress(address: String): Flow<List<VaultMessage>>

    @Query("UPDATE vault_messages SET `read` = 1 WHERE address = :address")
    suspend fun markAddressRead(address: String)

    @Query("DELETE FROM vault_messages WHERE address = :address")
    suspend fun deleteAddress(address: String)
}

@Database(
    entities = [VaultMessage::class],
    version = 1,
    exportSchema = false,
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultMessageDao(): VaultMessageDao
}
