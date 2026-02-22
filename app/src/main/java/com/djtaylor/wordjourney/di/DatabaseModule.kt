package com.djtaylor.wordjourney.di

import android.content.Context
import com.djtaylor.wordjourney.data.db.AchievementDao
import com.djtaylor.wordjourney.data.db.DailyChallengeDao
import com.djtaylor.wordjourney.data.db.InboxDao
import com.djtaylor.wordjourney.data.db.StarRatingDao
import com.djtaylor.wordjourney.data.db.WordDao
import com.djtaylor.wordjourney.data.db.WordDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWordDatabase(@ApplicationContext context: Context): WordDatabase {
        val db = WordDatabase.buildDatabase(context)
        // Populate on first launch — runs once per install, ~731 words, fast.
        runBlocking(Dispatchers.IO) {
            db.ensurePopulated(context)
        }
        return db
    }

    @Provides
    @Singleton
    fun provideWordDao(database: WordDatabase): WordDao {
        return database.wordDao()
    }

    @Provides
    @Singleton
    fun provideStarRatingDao(database: WordDatabase): StarRatingDao {
        return database.starRatingDao()
    }

    @Provides
    @Singleton
    fun provideDailyChallengeDao(database: WordDatabase): DailyChallengeDao {
        return database.dailyChallengeDao()
    }

    @Provides
    @Singleton
    fun provideAchievementDao(database: WordDatabase): AchievementDao {
        return database.achievementDao()
    }

    @Provides
    @Singleton
    fun provideInboxDao(database: WordDatabase): InboxDao {
        return database.inboxDao()
    }
}
