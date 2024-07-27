package syh7.bookstack

import syh7.bookstack.model.BookContentsChapter
import syh7.bookstack.model.DetailedBook

data class CompleteBookSetup(
    val name: String,
    val bookstackBook: DetailedBook,
    val tagUrlMap: List<TagMap>,
) {
    fun getChapter(chapterName: String): BookContentsChapter {
        return bookstackBook.contents
            .filterIsInstance<BookContentsChapter>()
            .first { it.name.lowercase().contains(chapterName.lowercase()) }
    }
}