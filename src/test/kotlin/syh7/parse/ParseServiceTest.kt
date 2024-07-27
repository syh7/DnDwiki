package syh7.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ParseServiceTest {

    private val foundWord = "found"

    @ParameterizedTest
    @MethodSource("provideTestWords")
    fun shouldReplaceCorrectly(testWord: String) {
        // given
        val toBeReplacedTexts = createStringsToBeReplaced(testWord)
        val expectedReplacedResults = createStringsToBeReplaced(foundWord)

        // when + then
        val replaceablePatternResults = invokePattern(toBeReplacedTexts, testWord)
        assertIterableEquals(expectedReplacedResults, replaceablePatternResults)
    }

    @ParameterizedTest
    @MethodSource("provideTestWords")
    fun shouldNotReplaceIncorrectly(testWord: String) {
        // given
        val singleSentence = "thisis${testWord}notreplaced"
        val shouldReplaceOnce = "this $testWord occurs twice: see $testWord"
        val shouldReplaceExpected = "this $foundWord occurs twice: see $testWord"

        // when + then
        val noReplaceResult = invokePattern(singleSentence, testWord)
        assertEquals(singleSentence, noReplaceResult)

        // when + then
        val replaceOnceResult = invokePattern(shouldReplaceOnce, testWord)
        assertEquals(shouldReplaceExpected, replaceOnceResult)
    }

    private fun invokePattern(strings: List<String>, testWord: String): List<String> {
        val pattern = ParseService().createPattern(testWord)
        return strings.map { pattern.matcher(it).replaceFirst(foundWord) }
    }

    private fun invokePattern(string: String, testWord: String): String {
        val pattern = ParseService().createPattern(testWord)
        return pattern.matcher(string).replaceFirst(foundWord)
    }


    private fun createStringsToBeReplaced(testWord: String): List<String> {
        return listOf(
            "a $testWord text",
            "\n$testWord\n with newlines",
            "($testWord) between brackets",
            "($testWord with space) between brackets",
            "$testWord's with apostrophe",
            "($testWord's with apostrophe and bracket",
        )
    }

    companion object {
        @JvmStatic
        fun provideTestWords() = listOf(
            "simple",
            "SimPle",
            "dïacrîtícs",
            "Öfakkö", // diacritics at the start and end of word
            "test with space",
        )
    }
}