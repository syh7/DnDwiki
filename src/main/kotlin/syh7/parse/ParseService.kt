package syh7.parse

import syh7.bookstack.CompleteBookSetup
import syh7.bookstack.TagMap
import syh7.util.log
import syh7.util.lowerLogOffset
import syh7.util.upLogOffset
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.io.path.*

class ParseService {

    private val digitRegex = Regex("\\d+")
    private val arcRegex = Regex("sessions( arc \\d+)?")
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

        val fullText = if (rawFilePath.name.contains(" - ")) {
            var (title, body) = rawFilePath.readText().split("\n", limit = 2)
            setup.tagUrlMap.forEach { body = replaceTagsInBody(it, body) }

            "$title\n$body"
        } else {
            rawFilePath.readText()
        }

        val parsedState = writeFile(parsedFilePath, fullText)

        return ParsedFile(parsedState, parsedFilePath)
    }

    private fun replaceTagsInBody(tagMap: TagMap, body: String): String {
        var editableBody = body
        tagMap.tags
            .map {
                val matcher = createPattern(it).matcher(editableBody)
                matcher to it
            }
            .filter { (matcher, _) -> matcher.find() }
            .minByOrNull { (matcher, _) -> matcher.start() }
            ?.let { (matcher, tag) -> editableBody = matcher.replaceFirst("$1[$tag](${tagMap.url})$3") }
        return editableBody
    }

    fun createPattern(it: String): Pattern = Pattern.compile("(?U)(?i)(\\b)($it)(\\b)")

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

        val sessionsGroupedPerArc = parsedFiles.groupBy { arcRegex.find(it.path.pathString)?.value ?: throw IllegalStateException("no session in path") }

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
