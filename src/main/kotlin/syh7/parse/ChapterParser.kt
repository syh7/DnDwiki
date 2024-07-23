package syh7.parse

import syh7.bookstack.CompleteBookSetup
import syh7.bookstack.model.BookContentsChapter
import syh7.util.pageUrl
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

class ChapterParser {

    private val sessionRegex = Regex("<li>(.+)</li>")

    fun parseChapter(rawFilePath: Path, setup: CompleteBookSetup): String {

        val arcName = rawFilePath.nameWithoutExtension.lowercase()
        val arcChapter = setup.bookstackBook.contents
            .filterIsInstance<BookContentsChapter>()
            .first { it.name.lowercase() == arcName.lowercase() }

        return addSessionLinks(rawFilePath.readText(), arcChapter, setup.name)
    }

    private fun addSessionLinks(body: String, chapter: BookContentsChapter, bookName: String): String {
        var editableBody = body

        val sessionMap = chapter.pages.map { it.name to it.slug }
        for ((sessionName, sessionSlug) in sessionMap) {
            val pageUrl = pageUrl(bookName, sessionSlug)
            editableBody = sessionRegex.replace(body, "<a href=$pageUrl>$sessionName</a>")
        }

        return editableBody
    }

}