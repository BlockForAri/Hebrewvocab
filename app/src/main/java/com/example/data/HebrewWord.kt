package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "hebrew_words")
data class HebrewWord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hebrew: String,
    val transliteration: String,
    val english: String,
    val partOfSpeech: String,
    val pronunciationGuide: String,
    val isHardWord: Boolean = false,
    val srsStage: Int = 0, // 0 = New, 1 = Immediate, 2 = Short, 3 = Medium, 4 = Long, 5 = Mastered
    val nextReviewTime: Long = 0L, // timestamp in ms, 0 means due immediately
    val lastReviewedTime: Long = 0L,
    val correctStreak: Int = 0
) : Serializable
