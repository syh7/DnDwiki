package syh7.bookstack.model

data class DetailedPage(
    val id: Int,
    val book_id: Int,
    val chapter_id: Int,
    val name: String,
    val slug: String,
    val priority: Int,
    val created_at: String,
    val updated_at: String,
    val created_by: User,
    val draft: Boolean,
    val revision_count: Long,
    val template: Boolean,
    val owned_by: User,
    val editor: String,
    val tags: List<Tag>,
) {
    // explicitly put the html + markdown in the class to prevent very big toStrings while still having access to them
    var html: String = ""
    var raw_html: String = ""
    var markdown: String = ""
}