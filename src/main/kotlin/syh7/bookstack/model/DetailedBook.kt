package syh7.bookstack.model

data class DetailedBook(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String,
    val created_at: String,
    val updated_at: String,
    val created_by: User,
    val updated_by: User,
    val owned_by: User,
    val contents: List<BookContents>,
)