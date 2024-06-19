package syh7.bookstack.model

data class SimpleBook(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String,
    val created_at: String,
    val updated_at: String,
    val created_by: Int,
    val updated_by: Int,
    val owned_by: Int,
)