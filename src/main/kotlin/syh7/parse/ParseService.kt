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
    private val chapterParser = ChapterParser()

    private val chapterRegex = Regex("sessions( arc \\d+)?")
    private val digitRegex = Regex("\\d+")
    private val fileComparator: Comparator<Path> = Comparator { pathA, pathB ->
        val padded1 = digitRegex.replace(pathA.pathString) { it.value.padStart(5, '0') }
        val padded2 = digitRegex.replace(pathB.pathString) { it.value.padStart(5, '0') }
        padded1.compareTo(padded2)
    }

    @OptIn(ExperimentalPathApi::class)
    fun parseArcs(setup: CompleteBookSetup): List<ArcSetup> {
        upLogOffset()
        log("walking $RAW_FOLDER")
        val parsedFiles = Paths.get(RAW_FOLDER, setup.name.lowercase())
            .walk()
            .sortedWith(fileComparator)
            .map { rawFilePath ->
                log("walking $rawFilePath")
                parseAndWriteFile(rawFilePath, setup)
            }
            .toList()

        val arcs = divideInArcs(parsedFiles, setup)

        lowerLogOffset()
        return arcs
    }

    private fun parseAndWriteFile(rawFilePath: Path, setup: CompleteBookSetup): ParsedFile {
        val parsedFilePath = Paths.get(rawFilePath.pathString.replace("raw", "parsed"))

        parsedFilePath.parent.createDirectories()

        val fullText = when (rawFilePath.extension) {
            "md" -> sessionParser.parseSession(rawFilePath, setup)
            "html" -> rawFilePath.readText()
            else -> throw IllegalStateException("unsupported extension for path $rawFilePath")
        }

        val parsedState = writeFile(parsedFilePath, fullText)
        return ParsedFile(parsedState, parsedFilePath)
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
        } else {
            log("Parsed file, writing to $path")
            path.writeText(sessionText)
            return ParseState.NEW
        }
    }

    private fun divideInArcs(parsedFiles: List<ParsedFile>, bookSetup: CompleteBookSetup): List<ArcSetup> {
        val sessionsGroupedPerArc = parsedFiles.groupBy { chapterRegex.find(it.path.pathString)?.value ?: throw IllegalStateException("could not find chapter file") }

        return sessionsGroupedPerArc.map { (_, files) ->
            val chapterSetup = files.first { it.path.parent.name == bookSetup.name.lowercase() }
            val sessions = files.filterNot { it.path.parent.name == bookSetup.name.lowercase() }
            ArcSetup(chapterSetup, sessions)
        }

    }

    companion object {
        private const val RAW_FOLDER = "src/main/resources/raw"
    }
}

data class ArcSetup(
    val chapterSetup: ParsedFile,
    val sessions: List<ParsedFile>
)

data class ParsedFile(
    val state: ParseState,
    val path: Path
)

enum class ParseState {
    NEW, UPDATED, IGNORED
}
