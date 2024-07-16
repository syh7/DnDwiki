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
    private val fileComparator: Comparator<Path> = Comparator { pathA, pathB ->
        val padded1 = digitRegex.replace(pathA.pathString) { it.value.padStart(5, '0') }
        val padded2 = digitRegex.replace(pathB.pathString) { it.value.padStart(5, '0') }
        padded1.compareTo(padded2)
    }

    @OptIn(ExperimentalPathApi::class)
    fun parseDirectory(setup: CompleteBookSetup): List<HandledSession> {
        upLogOffset()
        log("walking $RAW_SESSION_FOLDER")
        val newSessions = Paths.get(RAW_SESSION_FOLDER, setup.name.lowercase())
            .walk()
            .sortedWith(fileComparator)
            .map { rawFilePath ->
                log("walking $rawFilePath")
                val parsedFilePath = Paths.get(rawFilePath.pathString.replace("raw", "parsed"))

                parsedFilePath.parent.createDirectories()


                var (title, body) = rawFilePath.readText().split("\n", limit = 2)
                setup.tagUrlMap.forEach { body = replaceTagsInBody(it, body) }
                val fullText = "$title\n$body"
                writeFile(parsedFilePath, fullText)
            }
            .toList()

        lowerLogOffset()
        return newSessions
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

    private fun writeFile(path: Path, sessionText: String): HandledSession {
        val state: SessionState
        if (path.exists()) {
            val currentParsedText = path.readText()
            if (currentParsedText == sessionText) {
                state = SessionState.IGNORED
            } else {
                state = SessionState.UPDATED
                log("Updating previously parsed $path")
                path.writeText(sessionText)
            }
        } else {
            state = SessionState.NEW
            log("Parsed file, writing to $path")
            path.writeText(sessionText)
        }
        return HandledSession(state, path)
    }

    companion object {
        private const val RAW_SESSION_FOLDER = "src/main/resources/raw"
    }

}

data class HandledSession(
    val state: SessionState,
    val path: Path
)

enum class SessionState {
    NEW, UPDATED, IGNORED
}
