package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HebrewWordDao {
    @Query("SELECT * FROM hebrew_words ORDER BY id ASC")
    fun getAllWords(): Flow<List<HebrewWord>>

    @Query("SELECT * FROM hebrew_words WHERE isHardWord = 1 ORDER BY id DESC")
    fun getHardWords(): Flow<List<HebrewWord>>

    @Query("SELECT * FROM hebrew_words WHERE nextReviewTime <= :currentTime ORDER BY srsStage ASC, id ASC")
    fun getDueWords(currentTime: Long): Flow<List<HebrewWord>>

    @Query("SELECT COUNT(*) FROM hebrew_words")
    suspend fun getWordCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: HebrewWord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<HebrewWord>)

    @Update
    suspend fun updateWord(word: HebrewWord)

    @Delete
    suspend fun deleteWord(word: HebrewWord)

    @Query("SELECT * FROM hebrew_words WHERE id = :id")
    suspend fun getWordById(id: Int): HebrewWord?
}
