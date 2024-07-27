package syh7.parse

import syh7.bookstack.CompleteBookSetup
import syh7.bookstack.TagMap
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.io.path.readText

class SessionParser {

    fun parseSession(rawFilePath: Path, setup: CompleteBookSetup): String {
        var (title, body) = rawFilePath.readText().split("\n", limit = 2)
        setup.tagUrlMap.forEach { body = replaceTagsInBody(it, body) }
        return "$title\n$body"
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

}
