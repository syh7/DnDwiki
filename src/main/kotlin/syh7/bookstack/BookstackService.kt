package syh7.bookstack


import syh7.bookstack.model.BookContentsChapter
import syh7.bookstack.model.DetailedBook
import syh7.bookstack.model.DetailedPage
import syh7.bookstack.model.SimpleBookContainer
import syh7.parse.ParseState
import syh7.parse.ParsedFile
import syh7.util.log
import syh7.util.lowerLogOffset
import syh7.util.upLogOffset
import java.nio.file.Path
import kotlin.io.path.name

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

    fun updateSessions(bookSetup: CompleteBookSetup, parsedFiles: List<ParsedFile>) {
        upLogOffset()

        for (parsedFile in parsedFiles) {
            log("handling file ${parsedFile.path}")
            upLogOffset()
            when (parsedFile.state) {
                ParseState.NEW -> handleNewFile(parsedFile, bookSetup)
                ParseState.UPDATED -> handleUpdatedFile(parsedFile, bookSetup)
                ParseState.IGNORED -> log("file is not new nor updated, so skip it")
            }
            lowerLogOffset()
        }

        lowerLogOffset()
    }

    private fun handleUpdatedFile(file: ParsedFile, bookSetup: CompleteBookSetup) {
        log("handling updated session ${file.path}")
        val fileName = file.path.toFile().nameWithoutExtension.lowercase()
        val fileNumber = getFileNumber(fileName)
        val chapter = getRelevantChapter(file.path, bookSetup)
        val currentFilePage = chapter.pages.first { getFileNumber(it.name) == fileNumber }
        log("found page ${currentFilePage.id} with the same number: '$fileName'")

        val markdown = file.path.toFile().readText()
        val requestBody = createPageRequestBody(markdown, chapter)
        val createdPage = bookstackClient.updatePage(currentFilePage.id, requestBody)
        log("updated page `${createdPage.name}` with id ${createdPage.id}")
    }

    private fun getFileNumber(fileName: String): Int {
        return if (fileName.contains(" - ")) {
            fileName.split(" - ")[0].split(" ")[1].toInt()
        } else if (fileName.contains("arc")) {
            fileName.split(" arc ")[1].toInt()
        } else {
            throw IllegalStateException("could not parse $fileName to correct number")
        }
    }

    private fun handleNewFile(file: ParsedFile, bookSetup: CompleteBookSetup) {
        log("handling new file ${file.path}")
        val markdown = file.path.toFile().readText()
        val chapter = getRelevantChapter(file.path, bookSetup)
        val requestBody = createPageRequestBody(markdown, chapter)
        val createdPage = bookstackClient.addPage(requestBody)
        log("created page `${createdPage.name}` with id ${createdPage.id}")
    }

    private fun getRelevantChapter(filePath: Path, bookSetup: CompleteBookSetup): BookContentsChapter {
        val parentDirectory = filePath.parent.name.lowercase()
        return bookSetup.getChapter(parentDirectory)
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