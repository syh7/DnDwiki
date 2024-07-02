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
    fun parseDirectory(setup: CompleteBookSetup): List<Path> {
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
                    tags.firstOrNull { body.contains(it) }?.let { body = body.replaceFirst(it, "[$it]($url)") }
                }
                val fullText = "$title\n$body"
                writeFile(parsedFilePath, fullText)
            }
            .filterNotNull()
            .toList()

        lowerLogOffset()
        return newSessions
    }

    private fun writeFile(path: Path, sessionText: String): Path? {
        if (path.exists()) {
            val currentParsedText = path.readText()
            if (currentParsedText == sessionText) {
                return null
            }
            log("Updating previously parsed $path")
        } else {
            log("Parsed file, writing to $path")
        }
        path.writeText(sessionText)
        return path
    }

    companion object {
        private const val RAW_SESSION_FOLDER = "src/main/resources/raw"
    }

}