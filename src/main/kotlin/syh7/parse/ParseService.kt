package syh7.parse

import syh7.bookstack.CompleteBookSetup
import syh7.bookstack.TagMap
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

        // original, replaces Tristan for Trista
        tagMap.tags.map { body.indexOf(it, ignoreCase = true) to it }
            .filter { (index, _) -> index != -1 }
            .minByOrNull { it.first }
            ?.let { (_, tag) -> editableBody = editableBody.replaceFirst(tag, "[$tag](${tagMap.url})") }

        // has problem with Öfakkö
//        tagMap.tags.map { ("\\b$it\\b").toRegex() to it }
//            .map { (regex, tag) -> regex.find(body) to tag }
//            .filter { (match, _) -> match != null }
//            .minByOrNull { (match, _) -> match!!.range.first }
//            ?.let { (_, tag) -> editableBody = editableBody.replaceFirst(tag, "[$tag](${tagMap.url})") }


        // only does it on whitespace characters (so no (name) )
//        tagMap.tags
//            .map {
//                val pattern = Pattern.compile("(\\s)($it)(\\s)")
//                val matcher = pattern.matcher(editableBody)
//                matcher to it
//            }
//            .filter { (matcher, tag) -> matcher.find() }
//            .minByOrNull { (matcher, tag) -> matcher.start() }
//            ?.let { (matcher, tag) -> editableBody = matcher.replaceFirst("$1[$tag](${tagMap.url})$3") }

        // still trouble with some ():;
//        tagMap.tags
//            .map {
//                val pattern = Pattern.compile("(\\s|\\b)($it)(\\s|\\b)")
//                val matcher = pattern.matcher(editableBody)
//                matcher to it
//            }
//            .filter { (matcher, tag) -> matcher.find() }
//            .minByOrNull { (matcher, tag) -> matcher.start() }
//            ?.let { (matcher, tag) -> editableBody = matcher.replaceFirst("$1[$tag](${tagMap.url})$3") }

        return editableBody
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
