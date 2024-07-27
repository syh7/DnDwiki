package syh7.parse

import syh7.bookstack.CompleteBookSetup
import syh7.util.log
import syh7.util.lowerLogOffset
import syh7.util.upLogOffset
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

class ParseService {

    private val sessionParser = SessionParser()
    private val questlineParser = QuestlineParser()

    private val digitRegex = Regex("\\d+")
    private val fileComparator: Comparator<Path> = Comparator { pathA, pathB ->
        val padded1 = digitRegex.replace(pathA.pathString) { it.value.padStart(5, '0') }
        val padded2 = digitRegex.replace(pathB.pathString) { it.value.padStart(5, '0') }
        padded1.compareTo(padded2)
    }

    @OptIn(ExperimentalPathApi::class)
    fun parseDirectory(setup: CompleteBookSetup): List<ParsedFile> {
        upLogOffset()
        log("walking $RAW_FOLDER")
        val newSessions = Paths.get(RAW_FOLDER, setup.name.lowercase())
            .walk()
            .sortedWith(fileComparator)
            .map { rawFilePath ->
                log("walking $rawFilePath")
                val parsedFilePath = Paths.get(rawFilePath.pathString.replace("raw", "parsed"))

                val fullText = if (rawFilePath.pathString.contains("sessions")) {
                    sessionParser.parseSession(rawFilePath, setup)
                } else if (rawFilePath.pathString.contains("questlines")) {
                    questlineParser.parseQuestlines(rawFilePath, setup)
                } else {
                    throw IllegalStateException("could not find parser for file '$rawFilePath'")
                }

                val parseState = writeFile(parsedFilePath, fullText)
                ParsedFile(parseState, parsedFilePath)
            }
            .toList()

        lowerLogOffset()
        return newSessions
    }

    private fun writeFile(path: Path, sessionText: String): ParseState {
        if (path.exists()) {
            val currentParsedText = path.readText()
            if (currentParsedText == sessionText) {
                return ParseState.IGNORED
            } else {
                log("Updating previously parsed $path")
                path.writeText(sessionText)
                return ParseState.UPDATED
            }
        }

        log("Parsed file, writing to $path")
        path.writeText(sessionText)
        return ParseState.NEW
    }

    companion object {
        private const val RAW_FOLDER = "src/main/resources/raw"
    }

}

data class ParsedFile(
    val state: ParseState,
    val path: Path
)

enum class ParseState {
    NEW, UPDATED, IGNORED
}
