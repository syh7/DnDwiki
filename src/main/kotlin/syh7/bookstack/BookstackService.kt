package syh7.bookstack


import syh7.bookstack.model.BookContentsChapter
import syh7.bookstack.model.DetailedBook
import syh7.bookstack.model.DetailedPage
import syh7.bookstack.model.SimpleBookContainer
import syh7.parse.HandledSession
import syh7.parse.SessionState
import syh7.util.log
import syh7.util.lowerLogOffset
import syh7.util.upLogOffset

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

    fun updateSessions(bookSetup: CompleteBookSetup, sessions: List<HandledSession>) {
        upLogOffset()
        val sessionsChapter = bookSetup.bookstackBook.contents.filterIsInstance<BookContentsChapter>().first { it.name.lowercase() == "sessions" }
        log("sessions chapter in book ${bookSetup.name} is ${sessionsChapter.id}")

        for (session in sessions) {
            log("handling new session ${session.path}")
            upLogOffset()
            when (session.state) {
                SessionState.NEW -> handleNewSession(session, sessionsChapter)
                SessionState.UPDATED -> handleUpdatedSession(session, sessionsChapter)
                SessionState.IGNORED -> log("session is not new nor updated, so skip it")
            }
            lowerLogOffset()
        }

        lowerLogOffset()
    }

    private fun handleUpdatedSession(session: HandledSession, sessionsChapter: BookContentsChapter) {
        log("handling updated session ${session.path}")
        val sessionName = session.path.toFile().nameWithoutExtension.lowercase()
        val currentSessionPage = sessionsChapter.pages.first { it.name.lowercase() == sessionName }
        log("found page ${currentSessionPage.id} with the same session name '$sessionName'")

        val markdown = session.path.toFile().readText()
        val requestBody = createPageRequestBody(markdown, sessionsChapter)
        val createdPage = bookstackClient.updatePage(currentSessionPage.id, requestBody)
        log("updated page `${createdPage.name}` with id ${createdPage.id}")
    }

    private fun handleNewSession(session: HandledSession, sessionsChapter: BookContentsChapter) {
        log("handling new session ${session.path}")
        val markdown = session.path.toFile().readText()
        val requestBody = createPageRequestBody(markdown, sessionsChapter)
        val createdPage = bookstackClient.addPage(requestBody)
        log("created page `${createdPage.name}` with id ${createdPage.id}")
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