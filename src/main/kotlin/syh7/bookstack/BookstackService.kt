package syh7.bookstack


import syh7.bookstack.model.*
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.writeText

class BookstackService {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm")

    private val properties = BookstackProperties()
    private val bookstackClient = BookstackClient()

    fun getBooks(): SimpleBookContainer {
        return bookstackClient.getBooks()
    }

    fun retrieveBookSetup(book: SimpleBook): CompleteBookSetup {
        val detailedBook = bookstackClient.getBook(book.id)

        val keyChapterPageMap = createChapterPageMap(detailedBook)
        keyChapterPageMap.forEach { (chapter, pages) ->
            println("chapter ${chapter.name} has pages")
            pages.forEach { println("${it.name} had tags: ${it.tags}") }
            println()
        }

        val tagUrlMap = createTagUrlMap(keyChapterPageMap, detailedBook.name)
        tagUrlMap.forEach { println("${it.key} goes to ${it.value}") }

        return CompleteBookSetup(
            name = detailedBook.name,
            bookstackBook = detailedBook,
//            emptyMap(), emptyMap()
            keyChapterPages = keyChapterPageMap,
            tagUrlMap = tagUrlMap
        )
    }

    fun createBackup(setup: CompleteBookSetup) {
        val backupPath = Paths.get(BACKUP_LOCATION, setup.name, dateTimeFormatter.format(LocalDateTime.now()))

        val bookText = backupPath.resolve(setup.name + ".txt")
        bookText.writeText(setup.bookstackBook.toString())

        val bookFolder = backupPath.resolve(setup.name).toFile()
        bookFolder.mkdir()
    }

    private fun createChapterPageMap(book: DetailedBook): Map<BookContentsChapter, List<DetailedPage>> {
        return book.contents
            .filterIsInstance<BookContentsChapter>()
            .filter { it.name.lowercase() in KEY_CHAPTERS }
            .also { println("key chapter: $it") }
            .associateWith { keyChapter ->
                keyChapter.pages.filterNot {
                    if (it.draft) {
                        println("page ${it.id}: ${it.name} is a draft page")
                    }
                    it.draft
                }.map {
                    println("retrieving page ${it.id}: ${it.name} ")
                    val detailedPage = bookstackClient.getPage(it.id)
                    println("detailed page $detailedPage")
                    detailedPage
                }
            }
    }

    private fun slugToFullUrl(book: String, slug: String): String {
        return "${properties.url}/books/$book/page/$slug"
    }

    private fun createTagUrlMap(keyChapterPageMap: Map<BookContentsChapter, List<DetailedPage>>, book: String): Map<List<String>, String> {
        return keyChapterPageMap.values.flatten()
            .mapNotNull {
                val linkTags = getLinkTags(it)
                if (linkTags.isEmpty()) {
                    null
                } else {
                    val url = slugToFullUrl(book, it.slug)
                    val sortedTags = linkTags.sortedByDescending { tag -> tag.length }
                    sortedTags to url
                }
            }.toMap()
    }

    private fun getLinkTags(page: DetailedPage): List<String> {
        return page.tags.firstOrNull { it.name == LINK_TAG_NAME }?.value?.split(",") ?: emptyList()
    }

    companion object {

        private val KEY_CHAPTERS = listOf("player characters", "important npcs", "important locations", "factions")
        private const val LINK_TAG_NAME = "LinkTags"
        private const val BACKUP_LOCATION = "src/main/resources/backups"

    }

}