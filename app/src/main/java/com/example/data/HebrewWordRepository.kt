package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HebrewWordRepository(private val dao: HebrewWordDao) {
    val allWords: Flow<List<HebrewWord>> = dao.getAllWords()
    val hardWords: Flow<List<HebrewWord>> = dao.getHardWords()

    fun getDueWords(currentTime: Long): Flow<List<HebrewWord>> = dao.getDueWords(currentTime)

    suspend fun insert(word: HebrewWord) = withContext(Dispatchers.IO) {
        dao.insertWord(word)
    }

    suspend fun update(word: HebrewWord) = withContext(Dispatchers.IO) {
        dao.updateWord(word)
    }

    suspend fun delete(word: HebrewWord) = withContext(Dispatchers.IO) {
        dao.deleteWord(word)
    }

    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        val count = dao.getWordCount()
        if (count == 0) {
            val seedList = getSeedWords()
            dao.insertWords(seedList)
        }
    }

    private fun getSeedWords(): List<HebrewWord> {
        return listOf(
            HebrewWord(
                hebrew = "שָׁלוֹם",
                transliteration = "Shalom",
                english = "Hello / Peace / Goodbye",
                partOfSpeech = "Greeting",
                pronunciationGuide = "shah-LOHM (Emphasis on 'LOHM')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "תּוֹדָה",
                transliteration = "Toda",
                english = "Thank you",
                partOfSpeech = "Phrase",
                pronunciationGuide = "toh-DAH (Emphasis on 'DAH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "בַּיִת",
                transliteration = "Bayit",
                english = "House / Home",
                partOfSpeech = "Noun",
                pronunciationGuide = "BAH-yeet (Emphasis on 'BAH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "מַיִם",
                transliteration = "Mayim",
                english = "Water",
                partOfSpeech = "Noun",
                pronunciationGuide = "MAH-yeem (Emphasis on 'MAH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "לֶחֶם",
                transliteration = "Lechem",
                english = "Bread",
                partOfSpeech = "Noun",
                pronunciationGuide = "LEH-khem (Emphasis on 'LEH'. Sound the 'kh' deeply in the throat)",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "אָהַב",
                transliteration = "Ahav",
                english = "To love",
                partOfSpeech = "Verb",
                pronunciationGuide = "ah-HAV (Emphasis on 'HAV')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "אָכַל",
                transliteration = "Achal",
                english = "To eat",
                partOfSpeech = "Verb",
                pronunciationGuide = "ah-KHAL (Emphasis on 'KHAL'. Deep 'kh' sound)",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "שָׁתָה",
                transliteration = "Shata",
                english = "To drink",
                partOfSpeech = "Verb",
                pronunciationGuide = "shah-TAH (Emphasis on 'TAH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "הָלַךְ",
                transliteration = "Halach",
                english = "To walk / To go",
                partOfSpeech = "Verb",
                pronunciationGuide = "hah-LAKH (Emphasis on 'LAKH'. Deep 'kh' at the end)",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "רָאָה",
                transliteration = "Ra'ah",
                english = "To see",
                partOfSpeech = "Verb",
                pronunciationGuide = "rah-AH (Emphasis on 'AH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "דָּבָר",
                transliteration = "Davar",
                english = "Word / Thing",
                partOfSpeech = "Noun",
                pronunciationGuide = "dah-VAR (Emphasis on 'VAR')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "אִישׁ",
                transliteration = "Ish",
                english = "Man / Husband",
                partOfSpeech = "Noun",
                pronunciationGuide = "Eesh (Single long vowel syllable)",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "אִשָּׁה",
                transliteration = "Isha",
                english = "Woman / Wife",
                partOfSpeech = "Noun",
                pronunciationGuide = "ee-SHAH (Emphasis on 'SHAH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "יֶלֶד",
                transliteration = "Yeled",
                english = "Boy / Child",
                partOfSpeech = "Noun",
                pronunciationGuide = "YEH-led (Emphasis on 'YEH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "יַלְדָּה",
                transliteration = "Yalda",
                english = "Girl",
                partOfSpeech = "Noun",
                pronunciationGuide = "yal-DAH (Emphasis on 'DAH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "סֵפֶר",
                transliteration = "Sefer",
                english = "Book",
                partOfSpeech = "Noun",
                pronunciationGuide = "SEH-fehr (Emphasis on 'SEH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "יוֹם",
                transliteration = "Yom",
                english = "Day",
                partOfSpeech = "Noun",
                pronunciationGuide = "Yohm (Single syllable, round 'o' vowel)",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "לַיְלָה",
                transliteration = "Layla",
                english = "Night",
                partOfSpeech = "Noun",
                pronunciationGuide = "LAY-lah (Emphasis on 'LAY')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "אָב",
                transliteration = "Av",
                english = "Father",
                partOfSpeech = "Noun",
                pronunciationGuide = "Ahv (Single syllable, soft terminal v sound)",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "אֵם",
                transliteration = "Em",
                english = "Mother",
                partOfSpeech = "Noun",
                pronunciationGuide = "Aym (Single syllable)",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "מִשְׁפָּחָה",
                transliteration = "Mishpacha",
                english = "Family",
                partOfSpeech = "Noun",
                pronunciationGuide = "meesh-pah-KHAH (Emphasis on 'KHAH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "שֶׁמֶשׁ",
                transliteration = "Shemesh",
                english = "Sun",
                partOfSpeech = "Noun",
                pronunciationGuide = "SHEH-mesh (Emphasis on 'SHEH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "יָרֵחַ",
                transliteration = "Yare'ach",
                english = "Moon",
                partOfSpeech = "Noun",
                pronunciationGuide = "yah-REH-akh (Emphasis on 'REH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "בֹּקֶר טוֹב",
                transliteration = "Boker Tov",
                english = "Good morning",
                partOfSpeech = "Greeting",
                pronunciationGuide = "BOH-kehr Tohv (Emphasis on 'BOH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "עֶרֶב טוֹב",
                transliteration = "Erev Tov",
                english = "Good evening",
                partOfSpeech = "Greeting",
                pronunciationGuide = "EH-rehv Tohv (Emphasis on 'EH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "מַה נִּשְׁמַע",
                transliteration = "Ma Nishma",
                english = "How's it going?",
                partOfSpeech = "Phrase",
                pronunciationGuide = "mah neesh-MAH (Emphasis on 'MAH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "כֵּן",
                transliteration = "Ken",
                english = "Yes",
                partOfSpeech = "Phrase",
                pronunciationGuide = "Kehn (Single syllable)",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "לֹא",
                transliteration = "Lo",
                english = "No",
                partOfSpeech = "Phrase",
                pronunciationGuide = "Loh (Single syllable)",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "בְּבַקָּשָׁה",
                transliteration = "Bevakasha",
                english = "Please / You're welcome",
                partOfSpeech = "Phrase",
                pronunciationGuide = "beh-vah-kah-SHAH (Emphasis on 'SHAH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "אֶרֶץ",
                transliteration = "Eretz",
                english = "Land / Country / Earth",
                partOfSpeech = "Noun",
                pronunciationGuide = "EH-retz (Emphasis on 'EH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "שָׁמַיִם",
                transliteration = "Shamayim",
                english = "Sky / Heavens",
                partOfSpeech = "Noun",
                pronunciationGuide = "shah-MAH-yeem (Emphasis on 'MAH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "לֵב",
                transliteration = "Lev",
                english = "Heart",
                partOfSpeech = "Noun",
                pronunciationGuide = "Lehv (Single syllable)",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "מֶלֶך",
                transliteration = "Melech",
                english = "King",
                partOfSpeech = "Noun",
                pronunciationGuide = "MEH-lekh (Emphasis on 'MEH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "דֶּרֶךְ",
                transliteration = "Derech",
                english = "Way / Road / Path",
                partOfSpeech = "Noun",
                pronunciationGuide = "DEH-rekh (Emphasis on 'DEH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "קָדוֹשׁ",
                transliteration = "Kadosh",
                english = "Holy",
                partOfSpeech = "Adjective",
                pronunciationGuide = "kah-DOHSH (Emphasis on 'DOHSH')",
                isHardWord = false
            ),
            HebrewWord(
                hebrew = "טוֹב",
                transliteration = "Tov",
                english = "Good",
                partOfSpeech = "Adjective",
                pronunciationGuide = "Tohv (Single syllable)",
                isHardWord = false
            )
        )
    }
}
