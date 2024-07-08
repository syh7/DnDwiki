package syh7.bookstack

import syh7.bookstack.model.BookContentsChapter
import syh7.bookstack.model.DetailedBook
import syh7.bookstack.model.DetailedPage

data class CompleteBookSetup(
    val name: String,
    val bookstackBook: DetailedBook,
    val keyChapterPages: Map<BookContentsChapter, List<DetailedPage>>,
    val tagUrlMap: List<TagMap>,
)