package syh7.bookstack.model

data class SimplePage(
    val id: Int,
    val book_id: Int,
    val chapter_id: Int,
    val name: String,
    val slug: String,
    val priority: Int,
    val draft: Boolean,
    val template: Boolean,
    val created_at: String,
    val updated_at: String,
    val created_by: Int,
    val updated_by: Int,
    val owned_by: Int,
    val markdown: String,
    val html: String,
    val revision_count: Int,
)