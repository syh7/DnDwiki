package syh7.bookstack.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

interface BookContents {
    val id: Int
    val name: String
    val slug: String
    val book_id: Int
    val created_at: String
    val updated_at: String
    val url: String
    val type: String
}

data class BookContentsPage(
    override val id: Int,
    override val name: String,
    override val slug: String,
    override val book_id: Int,
    override val created_at: String,
    override val updated_at: String,
    override val url: String,
    val draft: Boolean,
    val template: Boolean,
) : BookContents {
    override val type: String
        get() = "page"
}

data class BookContentsChapter(
    override val id: Int,
    override val name: String,
    override val slug: String,
    override val book_id: Int,
    override val created_at: String,
    override val updated_at: String,
    override val url: String,
    val pages: List<BookContentsPage>,
) : BookContents {
    override val type: String
        get() = "chapter"
}

class BookContentDeserializer : JsonDeserializer<BookContents> {
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): BookContents {
        val jsonObject = json.asJsonObject
        val jsonType = jsonObject.get("type").asString
        return when (jsonType) {
            "page" -> context.deserialize<BookContentsPage>(json, BookContentsPage::class.java)
            "chapter" -> context.deserialize<BookContentsChapter>(json, BookContentsChapter::class.java)
            else -> throw IllegalStateException("unknown content type: $type")
        }
    }
}