package syh7.bookstack.model

data class SimpleChapter(
    val id: Int,
    val book_id: Int,
    val name: String,
    val slug: String,
    val description: String,
    val priority: Int,
    val created_at: String,
    val updated_at: String,
    val created_by: Int,
    val updated_by: Int,
    val owned_by: Int,
    val pages: List<SimplePage>,
)