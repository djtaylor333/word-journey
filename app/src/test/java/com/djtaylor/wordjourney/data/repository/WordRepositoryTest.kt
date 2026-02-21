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
 * Tests for WordRepository — word ordering randomization and validation.
 *
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
    private fun createRepo(seed: Long = 12345L): WordRepository {
        val context = mockk<android.content.Context>(relaxed = true) {
            every { getSharedPreferences(any(), any()) } returns mockk {
                every { getLong(any(), any()) } returns seed
                every { edit() } returns mockk(relaxed = true)
            }
            every { assets } returns mockk {
                every { open(any()) } throws java.io.FileNotFoundException("test mode")
            }
        }
        return WordRepository(wordDao, context).also {
            it.setPlayerSeedForTesting(seed)
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
    fun `different seeds produce different word orders`() = runTest {
        val repo1 = createRepo(seed = 100L)
        val repo2 = createRepo(seed = 200L)

        val words1 = (1..10).map { repo1.getWordForLevel(Difficulty.EASY, it) }
        val words2 = (1..10).map { repo2.getWordForLevel(Difficulty.EASY, it) }

        // Same set of words, but different order
        assertEquals(words1.toSet(), words2.toSet())
        assertNotEquals("Different seeds should produce different orderings", words1, words2)
    }

    @Test
    fun `same seed always produces same word order`() = runTest {
        val repo1 = createRepo(seed = 42L)
        val repo2 = createRepo(seed = 42L)

        val words1 = (1..10).map { repo1.getWordForLevel(Difficulty.EASY, it) }
        val words2 = (1..10).map { repo2.getWordForLevel(Difficulty.EASY, it) }

        assertEquals("Same seed must produce identical order", words1, words2)
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
        repo.setPlayerSeedForTesting(200L)
        val words2 = (1..10).map { repo.getWordForLevel(Difficulty.EASY, it) }
        
        // Same set, different order
        assertEquals(words1.filterNotNull().toSet(), words2.filterNotNull().toSet())
        assertNotEquals("Different seed should produce different order", words1, words2)
    }
}
