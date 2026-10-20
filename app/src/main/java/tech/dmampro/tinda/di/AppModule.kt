package tech.dmampro.tinda.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import tech.dmampro.tinda.data.local.ALL_MIGRATIONS
import tech.dmampro.tinda.data.local.FtsCallback
import tech.dmampro.tinda.data.local.TindaDatabase
import javax.inject.Singleton

/**
 * Graphe Hilt : seule la base Room est fournie ici (le reste utilise
 * @Inject constructor).
 *
 * Migrations (P0-5) : aucun repli destructif. Le chemin supporté est
 * la chaîne [ALL_MIGRATIONS] (versions 3 à 10) ; une base d'une autre
 * version fait échouer l'ouverture plutôt que d'être effacée en
 * silence. Les données des utilisateurs (règles, brouillons, SMS
 * programmés, favoris) ne sont jamais sacrifiées pour un schéma.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TindaDatabase =
        Room.databaseBuilder(context, TindaDatabase::class.java, "tinda.db")
            .addMigrations(*ALL_MIGRATIONS)
            .addCallback(FtsCallback())
            .build()

    @Provides
    fun provideConversationDao(db: TindaDatabase) = db.conversationDao()

    @Provides
    fun provideMessageDao(db: TindaDatabase) = db.messageDao()

    @Provides
    fun provideDraftDao(db: TindaDatabase) = db.draftDao()

    @Provides
    fun provideBlockDao(db: TindaDatabase) = db.blockDao()

    @Provides
    fun provideSimAssignmentDao(db: TindaDatabase) = db.simAssignmentDao()

    @Provides
    fun providePinnedThreadDao(db: TindaDatabase) = db.pinnedThreadDao()

    @Provides
    fun provideFavoriteContactDao(db: TindaDatabase) = db.favoriteContactDao()

    @Provides
    fun provideArchiveDao(db: TindaDatabase) = db.archiveDao()

    @Provides
    fun provideSearchDao(db: TindaDatabase) = db.searchDao()
}
