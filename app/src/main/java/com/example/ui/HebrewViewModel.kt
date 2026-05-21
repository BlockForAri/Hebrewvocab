package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HebrewWord
import com.example.data.HebrewWordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class StudyMode { SRS, HARD_WORDS, ALL }
enum class Assessment { AGAIN, GOOD, EASY }

class HebrewViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = HebrewWordRepository(database.hebrewWordDao())

    // Base flows
    val allWords = repository.allWords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val hardWords = repository.hardWords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Simulation offset for SRS testing
    private val _timeOffsetMs = MutableStateFlow(0L)
    val timeOffsetMs = _timeOffsetMs.asStateFlow()

    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime = _currentTime.asStateFlow()

    // Due words filtered by current epoch + offset
    val dueWords = _currentTime.flatMapLatest { now ->
        repository.getDueWords(now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active session details
    private val _activeMode = MutableStateFlow<StudyMode?>(null)
    val activeMode = _activeMode.asStateFlow()

    private val _studyList = MutableStateFlow<List<HebrewWord>>(emptyList())
    val studyList = _studyList.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex = _currentCardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped = _isCardFlipped.asStateFlow()

    private val _sessionComplete = MutableStateFlow(false)
    val sessionComplete = _sessionComplete.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
            refreshCurrentTime()
        }
    }

    fun refreshCurrentTime() {
        _currentTime.value = System.currentTimeMillis() + _timeOffsetMs.value
    }

    fun advanceTime(minutes: Long) {
        val ms = minutes * 60 * 1000L
        _timeOffsetMs.value += ms
        refreshCurrentTime()
    }

    fun resetTimeOffset() {
        _timeOffsetMs.value = 0L
        refreshCurrentTime()
    }

    fun startSession(mode: StudyMode) {
        viewModelScope.launch {
            refreshCurrentTime()
            val list = when (mode) {
                StudyMode.SRS -> {
                    // Due words
                    dueWords.value
                }
                StudyMode.HARD_WORDS -> {
                    // All hard words
                    hardWords.value
                }
                StudyMode.ALL -> {
                    // All words in db shuffled
                    allWords.value.shuffled()
                }
            }
            if (list.isNotEmpty()) {
                _studyList.value = list
                _currentCardIndex.value = 0
                _isCardFlipped.value = false
                _sessionComplete.value = false
                _activeMode.value = mode
            }
        }
    }

    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun submitAssessment(assessment: Assessment) {
        val currentIndex = _currentCardIndex.value
        val list = _studyList.value
        if (currentIndex >= list.size) return

        val currentWord = list[currentIndex]
        val now = System.currentTimeMillis() + _timeOffsetMs.value

        val (nextStage, streak, nextTime) = when (assessment) {
            Assessment.AGAIN -> {
                // Reset or drop. Move back to Stage 1.
                Triple(1, 0, now) // review again immediately
            }
            Assessment.GOOD -> {
                // Increments slot slightly, or keeps at current.
                val stage = if (currentWord.srsStage == 0) 1 else currentWord.srsStage
                val newStreak = currentWord.correctStreak + 1
                Triple(stage, newStreak, calculateScheduleTime(stage, now))
            }
            Assessment.EASY -> {
                // Advance level
                val next = minOf(6, if (currentWord.srsStage == 0) 2 else currentWord.srsStage + 1)
                val newStreak = currentWord.correctStreak + 1
                Triple(next, newStreak, calculateScheduleTime(next, now))
            }
        }

        val updatedWord = currentWord.copy(
            srsStage = nextStage,
            correctStreak = streak,
            lastReviewedTime = now,
            nextReviewTime = nextTime
        )

        viewModelScope.launch {
            repository.update(updatedWord)
            
            // Advance screen state
            if (currentIndex + 1 >= list.size) {
                _sessionComplete.value = true
            } else {
                _currentCardIndex.value = currentIndex + 1
                _isCardFlipped.value = false
            }
            refreshCurrentTime()
        }
    }

    private fun calculateScheduleTime(stage: Int, now: Long): Long {
        val intervalMs = when (stage) {
            1 -> 20 * 1000L // 20 seconds (immediate test)
            2 -> 90 * 1000L // 1.5 minutes
            3 -> 5 * 60 * 1000L // 5 minutes
            4 -> 15 * 60 * 1000L // 15 minutes
            5 -> 2 * 60 * 60 * 1000L // 2 hours
            6 -> 24 * 60 * 60 * 1000L // 1 day (Mastered)
            else -> 24 * 60 * 60 * 1000L
        }
        return now + intervalMs
    }

    fun endSession() {
        _activeMode.value = null
        _studyList.value = emptyList()
        _currentCardIndex.value = 0
        _isCardFlipped.value = false
        _sessionComplete.value = false
        refreshCurrentTime()
    }

    fun toggleHardWord(word: HebrewWord) {
        viewModelScope.launch {
            repository.update(word.copy(isHardWord = !word.isHardWord))
            refreshCurrentTime()
        }
    }

    fun deleteWord(word: HebrewWord) {
        viewModelScope.launch {
            repository.delete(word)
            // If in active session and we deleted it, handle index gracefully
            refreshCurrentTime()
        }
    }

    fun addNewWord(
        hebrew: String,
        transliteration: String,
        english: String,
        pos: String,
        pronunciation: String
    ) {
        viewModelScope.launch {
            val newWord = HebrewWord(
                hebrew = hebrew.trim(),
                transliteration = transliteration.trim(),
                english = english.trim(),
                partOfSpeech = pos.trim(),
                pronunciationGuide = pronunciation.trim(),
                nextReviewTime = System.currentTimeMillis() + _timeOffsetMs.value // due immediately
            )
            repository.insert(newWord)
            refreshCurrentTime()
        }
    }
}
