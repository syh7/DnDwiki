package syh7.parse

import syh7.bookstack.CompleteBookSetup
import syh7.util.log
import syh7.util.lowerLogOffset
import syh7.util.upLogOffset
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

class ParseService {

    private val digitRegex = Regex("\\d+")
    private val fileComparator: Comparator<Path> = Comparator { pathA, pathB ->
        val padded1 = digitRegex.replace(pathA.name) { it.value.padStart(5, '0') }
        val padded2 = digitRegex.replace(pathB.name) { it.value.padStart(5, '0') }
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
                setup.tagUrlMap.forEach { (tags, url) ->
                    tags.map { body.indexOf(it, ignoreCase = true) to it }
                        .filter { (index, _) -> index != -1 }
                        .minByOrNull { it.first }
                        ?.let { (_, tag) -> body = body.replaceFirst(tag, "[$tag]($url)") }
                }
                val fullText = "$title\n$body"
                writeFile(parsedFilePath, fullText)
            }
            .toList()

        lowerLogOffset()
        return newSessions
    }

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
