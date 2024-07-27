package syh7.bookstack


import syh7.bookstack.model.BookContentsChapter
import syh7.bookstack.model.DetailedBook
import syh7.bookstack.model.DetailedPage
import syh7.bookstack.model.SimpleBookContainer
import syh7.parse.ArcSetup
import syh7.parse.ParseState
import syh7.parse.ParsedFile
import syh7.util.log
import syh7.util.lowerLogOffset
import syh7.util.upLogOffset
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

class BookstackService {

    private val properties = BookstackProperties()
    private val bookstackClient = BookstackClient()

    fun getBooks(): SimpleBookContainer {
        return bookstackClient.getBooks()
    }

    fun getExport(bookId: Int, exportOptions: ExportOptions): String {
        return bookstackClient.getBookExport(bookId, exportOptions)
    }

    fun retrieveBookSetup(bookId: Int): CompleteBookSetup {
        upLogOffset()
        val detailedBook = bookstackClient.getBook(bookId)

        val keyChapterPageMap = createChapterPageMap(detailedBook)
        keyChapterPageMap.forEach { (chapter, pages) ->
            log("chapter ${chapter.name} has pages")
            pages.forEach { log("${it.name} had tags: ${it.tags}") }
            println()
        }

        val tagUrlMap = createTagUrlMap(keyChapterPageMap, detailedBook.name)
        tagUrlMap.forEach { log(it) }
        lowerLogOffset()

        return CompleteBookSetup(
            name = detailedBook.name,
            bookstackBook = detailedBook,
            keyChapterPages = keyChapterPageMap,
            tagUrlMap = tagUrlMap
        )
    }

    private fun createChapterPageMap(book: DetailedBook): Map<BookContentsChapter, List<DetailedPage>> {
        upLogOffset()
        val map = book.contents
            .filterIsInstance<BookContentsChapter>()
            .filter { it.name.lowercase() in KEY_CHAPTERS }
            .also { log("key chapter: $it") }
            .associateWith { keyChapter ->
                keyChapter.pages.filterNot {
                    if (it.draft) {
                        log("page ${it.id}: ${it.name} is a draft page")
                    }
                    it.draft
                }.map {
                    log("retrieving page ${it.id}: ${it.name} ")
                    val detailedPage = bookstackClient.getPage(it.id)
                    log("detailed page $detailedPage")
                    detailedPage
                }
            }
        lowerLogOffset()
        return map
    }

    private fun createTagUrlMap(keyChapterPageMap: Map<BookContentsChapter, List<DetailedPage>>, book: String): List<TagMap> {
        val bookUrl = "${properties.url}/books/$book"
        return keyChapterPageMap.values.flatten()
            .map { createTagMap(it, bookUrl) }
            .flatten()
    }

    fun emptyChapter(bookSetup: CompleteBookSetup, chapterName: String) {
        upLogOffset()
        val sessionsChapter = bookSetup.bookstackBook.contents.filterIsInstance<BookContentsChapter>().first { it.name.lowercase() == chapterName }
        val toBeDeletedPageIds = sessionsChapter.pages.map { it.id to it.name }
        toBeDeletedPageIds.forEach { (id, name) ->
            log("deleting page `$name` with id $id")
            bookstackClient.deletePage(id)
        }
        lowerLogOffset()
    }

    fun updateArcs(bookSetup: CompleteBookSetup, arcs: List<ArcSetup>) {
        upLogOffset()
        val sessionChapters = bookSetup.bookstackBook.contents
            .filterIsInstance<BookContentsChapter>()
            .filter { it.name.lowercase().contains("sessions") }
        for (chapter in sessionChapters) {
            log("${chapter.name} in book ${bookSetup.name} is ${chapter.id}")
        }

        for (arc in arcs) {
            log("handling arc ${arc.chapterSetup.path.name}")
            val relevantSessionChapter = getRelevantSessionChapter(arc, sessionChapters)
            log("relevant chapter in wiki: ${relevantSessionChapter.name}")

            log("handling sessions")
            upLogOffset()

            for (session in arc.sessions) {
                when (session.state) {
                    ParseState.NEW -> handleNewSession(session, relevantSessionChapter)
                    ParseState.UPDATED -> handleUpdatedSession(session, relevantSessionChapter)
                    ParseState.IGNORED -> log("session is not new nor updated, so skip it")
                }
            }
            lowerLogOffset()
            log("done handling sessions")

            log("handling chapter")
            upLogOffset()

            updateChapter(arc.chapterSetup, relevantSessionChapter)

            lowerLogOffset()
            log("done handling chapter")

        }

        lowerLogOffset()
    }

    private fun handleUpdatedSession(session: ParsedFile, relevantSessionChapter: BookContentsChapter) {
        log("handling updated session ${session.path}")
        val sessionName = session.path.toFile().nameWithoutExtension.lowercase()
        val sessionNumber = getSessionNumber(sessionName)
        val currentSessionPage = relevantSessionChapter.pages.first { getSessionNumber(it.name) == sessionNumber }
        log("found page ${currentSessionPage.id} with the same session name '$sessionName'")

        val markdown = session.path.toFile().readText()
        val requestBody = createPageRequestBody(markdown, relevantSessionChapter)
        val createdPage = bookstackClient.updatePage(currentSessionPage.id, requestBody)
        log("updated page `${createdPage.name}` with id ${createdPage.id}")
    }

    private fun updateChapter(chapter: ParsedFile, relevantSessionChapter: BookContentsChapter) {
        if (chapter.state != ParseState.IGNORED) {
            log("updating chapter")
            val chapterRequestBody = createChapterRequestBody(chapter, relevantSessionChapter)
            bookstackClient.updateChapter(relevantSessionChapter.id, chapterRequestBody)
        } else {
            log("chapter was not changed")
        }
    }

    private fun getSessionNumber(sessionName: String) = sessionName.split(" - ")[0].split(" ")[1].toInt()

    private fun handleNewSession(session: ParsedFile, relevantSessionChapter: BookContentsChapter) {
        log("handling new session ${session.path}")
        val markdown = session.path.toFile().readText()
        val requestBody = createPageRequestBody(markdown, relevantSessionChapter)
        val createdPage = bookstackClient.addPage(requestBody)
        log("created page `${createdPage.name}` with id ${createdPage.id}")
    }

    private fun getRelevantSessionChapter(arc: ArcSetup, sessionChapters: List<BookContentsChapter>): BookContentsChapter {
        val arcName = arc.chapterSetup.path.nameWithoutExtension.lowercase()
        val relevantChapter = sessionChapters.first { it.name.lowercase() == arcName.lowercase() }
        log("relevant chapter is ${relevantChapter.name}")
        return relevantChapter
    }

    private fun createChapterRequestBody(file: ParsedFile, sessionsChapter: BookContentsChapter): ChapterRequestBody {
        return ChapterRequestBody(
            book_id = sessionsChapter.book_id,
            name = sessionsChapter.name,
            description_html = file.path.readText(),
        )
    }

    private fun createPageRequestBody(markdown: String, sessionsChapter: BookContentsChapter): PageRequestBody {
        val (title, pageText) = markdown.split("\n", limit = 2)
        return PageRequestBody(
            chapter_id = sessionsChapter.id,
            name = title.removePrefix("# "),
            markdown = pageText,
        )
    }

    companion object {

        private val KEY_CHAPTERS = listOf("player characters", "important npcs", "important locations", "factions")

    }

}