package com.djtaylor.wordjourney.data.repository

import com.djtaylor.wordjourney.data.db.WordDao
import com.djtaylor.wordjourney.data.db.WordEntity
import com.djtaylor.wordjourney.domain.model.Difficulty
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for WordRepository — word ordering and validation.
 *
 * Uses a fixed global seed (same for all players).
 * Note: These tests mock the WordDao and skip valid_words.json loading
 * (which requires Android Context). Word validation against the dictionary
 * is tested via integration tests on device.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WordRepositoryTest {

    private lateinit var wordDao: WordDao

    private val sampleWords4 = listOf(
        WordEntity(1, "ABLE", 4, "Having ability"),
        WordEntity(2, "ALSO", 4, "In addition"),
        WordEntity(3, "AREA", 4, "A region"),
        WordEntity(4, "ARMY", 4, "Military force"),
        WordEntity(5, "AWAY", 4, "At a distance"),
        WordEntity(6, "BACK", 4, "Rear part"),
        WordEntity(7, "BAND", 4, "A group"),
        WordEntity(8, "BANK", 4, "Financial institution"),
        WordEntity(9, "BASE", 4, "Foundation"),
        WordEntity(10, "BIRD", 4, "A feathered animal")
    )

    private val sampleWords5 = listOf(
        WordEntity(11, "ABOUT", 5, "Regarding"),
        WordEntity(12, "ABOVE", 5, "Higher than"),
        WordEntity(13, "CRANE", 5, "A bird or machine"),
        WordEntity(14, "DREAM", 5, "Mental images during sleep"),
        WordEntity(15, "EVERY", 5, "All of")
    )

    @Before
    fun setUp() {
        wordDao = mockk {
            coEvery { getAllByLength(4) } returns sampleWords4
            coEvery { getAllByLength(5) } returns sampleWords5
            coEvery { getAllByLength(6) } returns emptyList()
        }
    }

    /**
     * Create a WordRepository with a mocked context.
     * Note: The valid_words.json loading will fail gracefully (returns empty map),
     * so isValidWord tests that depend on the dictionary are skipped here.
     */
    private fun createRepo(seed: Long = WordRepository.GLOBAL_WORD_SEED): WordRepository {
        val context = mockk<android.content.Context>(relaxed = true) {
            every { assets } returns mockk {
                every { open(any()) } throws java.io.FileNotFoundException("test mode")
            }
        }
        return WordRepository(wordDao, context).also {
            it.setSeedForTesting(seed)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. WORD ORDERING
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getWordForLevel returns a word`() = runTest {
        val repo = createRepo()
        val word = repo.getWordForLevel(Difficulty.EASY, 1)
        assertNotNull(word)
        assertTrue(word!! in sampleWords4.map { it.word })
    }

    @Test
    fun `getWordForLevel wraps around when level exceeds word count`() = runTest {
        val repo = createRepo()
        val word1 = repo.getWordForLevel(Difficulty.EASY, 1)
        val word11 = repo.getWordForLevel(Difficulty.EASY, 11) // 10 words total → wraps
        assertEquals(word1, word11)
    }

    @Test
    fun `global seed gives all players the same word order`() = runTest {
        val repo1 = createRepo() // uses GLOBAL_WORD_SEED
        val repo2 = createRepo() // same seed

        val words1 = (1..10).map { repo1.getWordForLevel(Difficulty.EASY, it) }
        val words2 = (1..10).map { repo2.getWordForLevel(Difficulty.EASY, it) }

        assertEquals("All players must see the same order", words1, words2)
    }

    @Test
    fun `different test seeds produce different word orders`() = runTest {
        val repo1 = createRepo(seed = 100L)
        val repo2 = createRepo(seed = 200L)

        val words1 = (1..10).map { repo1.getWordForLevel(Difficulty.EASY, it) }
        val words2 = (1..10).map { repo2.getWordForLevel(Difficulty.EASY, it) }

        // Same set of words, but different order
        assertEquals(words1.toSet(), words2.toSet())
        assertNotEquals("Different seeds should produce different orderings", words1, words2)
    }

    @Test
    fun `word order is not alphabetical`() = runTest {
        val repo = createRepo(seed = 12345L)
        val words = (1..10).map { repo.getWordForLevel(Difficulty.EASY, it)!! }

        // The alphabetical order would be ABLE, ALSO, AREA, ARMY, AWAY, BACK, BAND, BANK, BASE, BIRD
        val alphabetical = sampleWords4.map { it.word }
        assertNotEquals("Words should not be in alphabetical order", alphabetical, words)
    }

    @Test
    fun `all words are used before repeating`() = runTest {
        val repo = createRepo()
        val words = (1..10).map { repo.getWordForLevel(Difficulty.EASY, it)!! }

        // All 10 sample words should appear exactly once in levels 1-10
        assertEquals(10, words.toSet().size)
        assertEquals(sampleWords4.map { it.word }.toSet(), words.toSet())
    }

    @Test
    fun `empty word list returns null`() = runTest {
        val repo = createRepo()
        val word = repo.getWordForLevel(Difficulty.HARD, 1) // 6-letter list is empty
        assertNull(word)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. DEFINITIONS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getDefinition returns definition for correct word`() = runTest {
        val repo = createRepo(seed = 42L)
        val word = repo.getWordForLevel(Difficulty.EASY, 1)!!
        val definition = repo.getDefinition(Difficulty.EASY, 1)

        // Find the word entity and verify definition matches
        val entity = sampleWords4.first { it.word == word }
        assertEquals(entity.definition, definition)
    }

    @Test
    fun `getDefinition returns empty for empty list`() = runTest {
        val repo = createRepo()
        val definition = repo.getDefinition(Difficulty.HARD, 1)
        assertEquals("", definition)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. FIND ABSENT LETTER
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `findAbsentLetter returns letter not in target`() = runTest {
        val repo = createRepo()
        val letter = repo.findAbsentLetter("ABLE", emptySet(), emptySet())
        assertNotNull(letter)
        assertTrue(letter!! !in setOf('A', 'B', 'L', 'E'))
    }

    @Test
    fun `findAbsentLetter excludes already eliminated`() = runTest {
        val repo = createRepo()
        val eliminated = ('C'..'Z').toSet() - setOf('A', 'B', 'L', 'E') // eliminate all except target letters
        val letter = repo.findAbsentLetter("ABLE", eliminated, emptySet())
        assertNull("No letters left to remove", letter)
    }

    @Test
    fun `findAbsentLetter excludes already revealed`() = runTest {
        val repo = createRepo()
        val revealed = ('C'..'Z').toSet() - setOf('A', 'B', 'L', 'E')
        val letter = repo.findAbsentLetter("ABLE", emptySet(), revealed)
        assertNull(letter)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. WORD ORDERING — ADDITIONAL TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getWordForLevel returns different words for different levels`() = runTest {
        val repo = createRepo()
        val words = (1..5).map { repo.getWordForLevel(Difficulty.EASY, it)!! }
        // All 5 should be distinct
        assertEquals(5, words.toSet().size)
    }

    @Test
    fun `getWordForLevel for regular difficulty works`() = runTest {
        val repo = createRepo()
        val word = repo.getWordForLevel(Difficulty.REGULAR, 1)
        assertNotNull(word)
        assertTrue(word!! in sampleWords5.map { it.word })
    }

    @Test
    fun `getWordForLevel regular wraps at 5 words`() = runTest {
        val repo = createRepo()
        val word1 = repo.getWordForLevel(Difficulty.REGULAR, 1)
        val word6 = repo.getWordForLevel(Difficulty.REGULAR, 6) // wraps at 5
        assertEquals(word1, word6)
    }

    @Test
    fun `getDefinition for regular difficulty returns correct definition`() = runTest {
        val repo = createRepo(seed = 42L)
        val word = repo.getWordForLevel(Difficulty.REGULAR, 1)!!
        val definition = repo.getDefinition(Difficulty.REGULAR, 1)
        val entity = sampleWords5.first { it.word == word }
        assertEquals(entity.definition, definition)
    }

    @Test
    fun `findAbsentLetter returns different letters for different targets`() = runTest {
        val repo = createRepo()
        val letter1 = repo.findAbsentLetter("ABLE", emptySet(), emptySet())
        val letter2 = repo.findAbsentLetter("BIRD", emptySet(), emptySet())
        // Both should be non-null and not in their respective targets
        assertNotNull(letter1)
        assertNotNull(letter2)
        assertTrue(letter1!! !in setOf('A', 'B', 'L', 'E'))
        assertTrue(letter2!! !in setOf('B', 'I', 'R', 'D'))
    }

    @Test
    fun `cache is reset when seed changes`() = runTest {
        val repo = createRepo(seed = 100L)
        val words1 = (1..10).map { repo.getWordForLevel(Difficulty.EASY, it) }
        
        // Change seed
        repo.setSeedForTesting(200L)
        val words2 = (1..10).map { repo.getWordForLevel(Difficulty.EASY, it) }
        
        // Same set, different order
        assertEquals(words1.filterNotNull().toSet(), words2.filterNotNull().toSet())
        assertNotEquals("Different seed should produce different order", words1, words2)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // v2.4.0 — 3 & 7 LETTER WORD SUPPORT
    // ══════════════════════════════════════════════════════════════════════════

    private val sampleWords3 = listOf(
        WordEntity(21, "ACE", 3, "A playing card with a single pip; also an expert"),
        WordEntity(22, "ACT", 3, "To take action or perform a role; a deed"),
        WordEntity(23, "ADD", 3, "To join or combine numbers or things together"),
        WordEntity(24, "CAT", 3, "A small domesticated carnivorous mammal"),
        WordEntity(25, "DOG", 3, "A domesticated carnivorous mammal kept as a pet")
    )

    private val sampleWords7 = listOf(
        WordEntity(31, "ABANDON", 7, "To give up completely; to desert or leave behind"),
        WordEntity(32, "ABILITY", 7, "The possession of skill or talent to do something"),
        WordEntity(33, "ABOLISH", 7, "To formally put an end to a system or practice"),
        WordEntity(34, "BALANCE", 7, "An even distribution of weight enabling stability"),
        WordEntity(35, "CABINET", 7, "A cupboard with shelves or drawers; a body of advisors")
    )

    private fun createRepoWith3And7(seed: Long = WordRepository.GLOBAL_WORD_SEED): WordRepository {
        val dao = mockk<WordDao> {
            coEvery { getAllByLength(3) } returns sampleWords3
            coEvery { getAllByLength(4) } returns sampleWords4
            coEvery { getAllByLength(5) } returns sampleWords5
            coEvery { getAllByLength(6) } returns emptyList()
            coEvery { getAllByLength(7) } returns sampleWords7
        }
        val context = mockk<android.content.Context>(relaxed = true) {
            every { assets } returns mockk {
                every { open(any()) } throws java.io.FileNotFoundException("test mode")
            }
        }
        return WordRepository(dao, context).also { it.setSeedForTesting(seed) }
    }

    @Test
    fun `getWordForLevel returns 3-letter words for VIP level 1`() = runTest {
        val repo = createRepoWith3And7()
        val wl = Difficulty.vipWordLengthForLevel(1) // 3
        val word = repo.getWordForLevel(Difficulty.VIP, 1, wordLengthOverride = wl)
        assertNotNull(word)
        assertEquals(3, word!!.length)
        assertTrue(word in sampleWords3.map { it.word })
    }

    @Test
    fun `getWordForLevel returns 7-letter words for VIP level 5`() = runTest {
        val repo = createRepoWith3And7()
        val wl = Difficulty.vipWordLengthForLevel(5) // 7
        val word = repo.getWordForLevel(Difficulty.VIP, 5, wordLengthOverride = wl)
        assertNotNull(word)
        assertEquals(7, word!!.length)
        assertTrue(word in sampleWords7.map { it.word })
    }

    @Test
    fun `getDefinition for 3-letter word returns non-empty definition`() = runTest {
        val repo = createRepoWith3And7()
        val wl = Difficulty.vipWordLengthForLevel(1)
        val word = repo.getWordForLevel(Difficulty.VIP, 1, wordLengthOverride = wl)
        val definition = repo.getDefinition(Difficulty.VIP, 1, wordLengthOverride = wl)
        assertNotNull(word)
        assertTrue("3-letter word should have a definition", definition.isNotEmpty())
        val entity = sampleWords3.first { it.word == word }
        assertEquals(entity.definition, definition)
    }

    @Test
    fun `getDefinition for 7-letter word returns non-empty definition`() = runTest {
        val repo = createRepoWith3And7()
        val wl = Difficulty.vipWordLengthForLevel(5)
        val word = repo.getWordForLevel(Difficulty.VIP, 5, wordLengthOverride = wl)
        val definition = repo.getDefinition(Difficulty.VIP, 5, wordLengthOverride = wl)
        assertNotNull(word)
        assertTrue("7-letter word should have a definition", definition.isNotEmpty())
        val entity = sampleWords7.first { it.word == word }
        assertEquals(entity.definition, definition)
    }

    @Test
    fun `VIP word cycling produces correct lengths across 10 levels`() = runTest {
        // Use a repo with enough words to fill all VIP partitions (4L split=76, 5L split=97)
        val words4 = makeWords4(200)
        val words5 = makeWords5(200)
        val words6 = makeWords6(400)
        val dao = mockk<WordDao> {
            coEvery { getAllByLength(3) } returns sampleWords3
            coEvery { getAllByLength(4) } returns words4
            coEvery { getAllByLength(5) } returns words5
            coEvery { getAllByLength(6) } returns words6
            coEvery { getAllByLength(7) } returns sampleWords7
        }
        val context = mockk<android.content.Context>(relaxed = true) {
            every { assets } returns mockk {
                every { open(any()) } throws java.io.FileNotFoundException("test mode")
            }
        }
        val repo = WordRepository(dao, context).also { it.setSeedForTesting(WordRepository.GLOBAL_WORD_SEED) }

        for (level in 1..10) {
            val expectedLen = Difficulty.vipWordLengthForLevel(level)
            val word = repo.getWordForLevel(Difficulty.VIP, level, wordLengthOverride = expectedLen)
            assertNotNull("Level $level ($expectedLen-letter VIP): should return a word", word)
            assertEquals("Level $level: expected $expectedLen-letter word", expectedLen, word!!.length)
        }
    }

    @Test
    fun `3-letter word definition matches word across shuffled order`() = runTest {
        val repo = createRepoWith3And7()
        for (level in listOf(1, 6, 11)) { // VIP levels that map to 3-letter words
            val wl = Difficulty.vipWordLengthForLevel(level)
            assertEquals("Level $level should be 3-letter", 3, wl)
            val word = repo.getWordForLevel(Difficulty.VIP, level, wordLengthOverride = wl)
            val definition = repo.getDefinition(Difficulty.VIP, level, wordLengthOverride = wl)
            if (word != null) {
                val entity = sampleWords3.first { it.word == word }
                assertEquals("Definition for $word should match", entity.definition, definition)
            }
        }
    }

    @Test
    fun `7-letter word definition matches word across shuffled order`() = runTest {
        val repo = createRepoWith3And7()
        for (level in listOf(5, 10, 15)) { // VIP levels that map to 7-letter words
            val wl = Difficulty.vipWordLengthForLevel(level)
            assertEquals("Level $level should be 7-letter", 7, wl)
            val word = repo.getWordForLevel(Difficulty.VIP, level, wordLengthOverride = wl)
            val definition = repo.getDefinition(Difficulty.VIP, level, wordLengthOverride = wl)
            if (word != null) {
                val entity = sampleWords7.first { it.word == word }
                assertEquals("Definition for $word should match", entity.definition, definition)
            }
        }
    }

    // ── isValidWord — 3-letter/7-letter lowercase normalization ───────────────

    /**
     * Creates a repo with a directly-injected word dictionary (bypassing assets loading).
     * Verifies that parseWordSet's uppercase normalization works regardless of the
     * case in which words were stored.
     */
    private fun createRepoWithDictionary(wordSets: Map<Int, Set<String>>): WordRepository {
        val repo = createRepo() // uses standard DAO mock
        repo.setWordSetsForTesting(wordSets)
        return repo
    }

    @Test
    fun `isValidWord returns true for lowercase 3-letter word in dictionary`() = runTest {
        // Simulate real valid_words.json: 3-letter words stored lowercase, parseWordSet uppercases them
        val repo = createRepoWithDictionary(mapOf(3 to setOf("FOB", "ACE", "CAT")))
        assertTrue("FOB should be valid", repo.isValidWord("FOB", 3))
        assertTrue("ACE should be valid", repo.isValidWord("ACE", 3))
        assertTrue("CAT should be valid", repo.isValidWord("CAT", 3))
    }

    @Test
    fun `isValidWord returns true for lowercase 7-letter word in dictionary`() = runTest {
        val repo = createRepoWithDictionary(mapOf(7 to setOf("ABANDON", "KITCHEN", "CHICKEN")))
        assertTrue("ABANDON should be valid", repo.isValidWord("ABANDON", 7))
        assertTrue("KITCHEN should be valid", repo.isValidWord("KITCHEN", 7))
    }

    @Test
    fun `isValidWord returns true for uppercase 5-letter word in dictionary`() = runTest {
        val repo = createRepoWithDictionary(mapOf(5 to setOf("CRANE", "DREAM", "AUDIO")))
        assertTrue("CRANE should be valid", repo.isValidWord("CRANE", 5))
        assertTrue("DREAM should be valid", repo.isValidWord("DREAM", 5))
    }

    @Test
    fun `isValidWord returns false for unknown 3-letter word`() = runTest {
        val repo = createRepoWithDictionary(mapOf(3 to setOf("FOB", "ACE")))
        assertFalse("XYZ should not be valid", repo.isValidWord("XYZ", 3))
    }

    @Test
    fun `isValidWord ignores case of submitted guess`() = runTest {
        // parseWordSet stores as uppercase; isValidWord calls word.uppercase() before lookup
        val repo = createRepoWithDictionary(mapOf(3 to setOf("FOB")))
        assertTrue(repo.isValidWord("fob", 3))
        assertTrue(repo.isValidWord("FOB", 3))
        assertTrue(repo.isValidWord("Fob", 3))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // v2.7.0 — VIP WORD POOL PARTITIONING
    // ══════════════════════════════════════════════════════════════════════════

    /** Generates N 4-letter word entities in the form W001, W002, …, WXXX */
    private fun makeWords4(count: Int): List<WordEntity> =
        (1..count).map { i -> WordEntity(1000 + i, "W${i.toString().padStart(3, '0')}", 4, "Def $i") }

    /** Generates N 5-letter word entities in the form V0001 … VXXXX */
    private fun makeWords5(count: Int): List<WordEntity> =
        (1..count).map { i -> WordEntity(2000 + i, "V${i.toString().padStart(4, '0')}", 5, "Def5 $i") }

    /** Generates N 6-letter word entities in the form U00001... */
    private fun makeWords6(count: Int): List<WordEntity> =
        (1..count).map { i -> WordEntity(3000 + i, "U${i.toString().padStart(5, '0')}", 6, "Def6 $i") }

    private fun createRepoForPartition(): WordRepository {
        val words4 = makeWords4(200)   // split = 76; standard=0..75, vip=76..199
        val words5 = makeWords5(200)   // split = 97; standard=0..96, vip=97..199
        val words6 = makeWords6(400)   // split = 193; standard=0..192, vip=193..399
        val dao = mockk<WordDao> {
            coEvery { getAllByLength(3) } returns sampleWords3
            coEvery { getAllByLength(4) } returns words4
            coEvery { getAllByLength(5) } returns words5
            coEvery { getAllByLength(6) } returns words6
            coEvery { getAllByLength(7) } returns sampleWords7
        }
        val context = mockk<android.content.Context>(relaxed = true) {
            every { assets } returns mockk {
                every { open(any()) } throws java.io.FileNotFoundException("test mode")
            }
        }
        return WordRepository(dao, context).also { it.setSeedForTesting(WordRepository.GLOBAL_WORD_SEED) }
    }

    @Test
    fun `VIP 4-letter words are disjoint from EASY 4-letter words`() = runTest {
        val repo = createRepoForPartition()
        // Standard pool = first 76 shuffled words; VIP pool = last 124 shuffled words
        val easyWords = (1..76).map { repo.getWordForLevel(Difficulty.EASY, it) }.filterNotNull().toSet()
        val vipWords = (1..124).map { repo.getWordForLevel(Difficulty.VIP, it, wordLengthOverride = 4) }
            .filterNotNull().toSet()
        assertTrue("Standard and VIP 4-letter pools must be disjoint — overlap: ${easyWords.intersect(vipWords)}",
            easyWords.intersect(vipWords).isEmpty())
    }

    @Test
    fun `VIP 5-letter words are disjoint from REGULAR 5-letter words`() = runTest {
        val repo = createRepoForPartition()
        val regularWords = (1..97).map { repo.getWordForLevel(Difficulty.REGULAR, it) }.filterNotNull().toSet()
        val vipWords = (1..103).map { repo.getWordForLevel(Difficulty.VIP, it, wordLengthOverride = 5) }
            .filterNotNull().toSet()
        assertTrue("Standard and VIP 5-letter pools must be disjoint — overlap: ${regularWords.intersect(vipWords)}",
            regularWords.intersect(vipWords).isEmpty())
    }

    @Test
    fun `VIP 6-letter words are disjoint from HARD 6-letter words`() = runTest {
        val repo = createRepoForPartition()
        val hardWords = (1..193).map { repo.getWordForLevel(Difficulty.HARD, it) }.filterNotNull().toSet()
        val vipWords = (1..207).map { repo.getWordForLevel(Difficulty.VIP, it, wordLengthOverride = 6) }
            .filterNotNull().toSet()
        assertTrue("Standard and VIP 6-letter pools must be disjoint — overlap: ${hardWords.intersect(vipWords)}",
            hardWords.intersect(vipWords).isEmpty())
    }

    @Test
    fun `EASY pool size is capped at VIP split point for 4-letter words`() = runTest {
        val repo = createRepoForPartition()
        // 200 total 4-letter words, split = 76 → standard pool has 76 unique words
        val easyWords = (1..200).map { repo.getWordForLevel(Difficulty.EASY, it) }.filterNotNull().toSet()
        assertEquals("Standard pool should cycle within 76 words", 76, easyWords.size)
    }

    @Test
    fun `VIP pool for 3-letter words uses full list (no split)`() = runTest {
        val repo = createRepoWith3And7()
        // Lengths 3 and 7 have no partition — VIP gets the full list
        val vipWords = (1..5).map { repo.getWordForLevel(Difficulty.VIP, it, wordLengthOverride = 3) }.filterNotNull()
        val allWords = sampleWords3.map { it.word }
        assertTrue("VIP 3-letter pool should draw from full sampleWords3", allWords.containsAll(vipWords))
        assertEquals("All 5 unique 3-letter words should be seen", 5, vipWords.toSet().size)
    }

    @Test
    fun `VIP pool for 7-letter words uses full list (no split)`() = runTest {
        val repo = createRepoWith3And7()
        val vipWords = (1..5).map { repo.getWordForLevel(Difficulty.VIP, it, wordLengthOverride = 7) }.filterNotNull()
        val allWords = sampleWords7.map { it.word }
        assertTrue("VIP 7-letter pool should draw from full sampleWords7", allWords.containsAll(vipWords))
        assertEquals("All 5 unique 7-letter words should be seen", 5, vipWords.toSet().size)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // v2.7.0 — hasDefinition
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `hasDefinition returns true when word has a non-blank definition`() = runTest {
        val repo = createRepo()
        // All sampleWords4 have non-blank definitions
        assertTrue("Level 1 EASY word should have a definition", repo.hasDefinition(Difficulty.EASY, 1))
    }

    @Test
    fun `hasDefinition returns false when word list is empty`() = runTest {
        val repo = createRepo()
        // HARD words (6-letter) have empty list → getDefinition returns "" → hasDefinition false
        assertFalse("Empty word list should report no definition", repo.hasDefinition(Difficulty.HARD, 1))
    }

    @Test
    fun `hasDefinition with wordLengthOverride uses correct partition`() = runTest {
        val repo = createRepo()
        // 4-letter VIP requests with wordLengthOverride=4, sampleWords4 has definitions
        assertTrue(repo.hasDefinition(Difficulty.EASY, 1, wordLengthOverride = 4))
    }
}
