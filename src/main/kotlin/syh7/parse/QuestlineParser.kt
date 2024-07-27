package syh7.parse

import syh7.bookstack.CompleteBookSetup
import syh7.util.createPageUrl
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

class QuestlineParser {

    fun parseQuestlines(rawFilePath: Path, setup: CompleteBookSetup): String {
        val arc = rawFilePath.nameWithoutExtension.removePrefix("questlines ")
        val sessionChapter = setup.getChapter(arc)
        val nameUrlMap = sessionChapter.pages.associate { it.name to createPageUrl(setup.name, it.slug) }

        return replaceTagsInBody(nameUrlMap, rawFilePath.readText())
    }

    private fun replaceTagsInBody(nameUrlMap: Map<String, String>, body: String): String {
        var editableBody = body

        nameUrlMap.map { (name, url) ->
            val matcher = createPattern(name).matcher(editableBody)
            editableBody = matcher.replaceFirst("- [$name]($url)")
        }

        return editableBody
    }

    fun createPattern(it: String): Pattern = Pattern.compile("(?i)- ($it)")

}
