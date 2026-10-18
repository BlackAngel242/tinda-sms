package tech.dmampro.tinda.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import tech.dmampro.tinda.data.local.FtsCallback
import tech.dmampro.tinda.data.local.MIGRATION_3_4
import tech.dmampro.tinda.data.local.MIGRATION_4_5
import tech.dmampro.tinda.data.local.MIGRATION_5_6
import tech.dmampro.tinda.data.local.MIGRATION_6_7
import tech.dmampro.tinda.data.local.MIGRATION_7_8
import tech.dmampro.tinda.data.local.MIGRATION_8_9
import tech.dmampro.tinda.data.local.TindaDatabase
import javax.inject.Singleton

/**
 * Graphe Hilt : seule la base Room est fournie ici (le reste utilise
 * @Inject constructor). destructiveMigration : la base n'est pas encore
 * publiée, le schéma peut être recréé entre les lots de dev.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TindaDatabase =
        Room.databaseBuilder(context, TindaDatabase::class.java, "tinda.db")
            .addMigrations(
                MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
            )
            .fallbackToDestructiveMigration()
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
    fun provideArchiveDao(db: TindaDatabase) = db.archiveDao()

    @Provides
    fun provideSearchDao(db: TindaDatabase) = db.searchDao()
}
