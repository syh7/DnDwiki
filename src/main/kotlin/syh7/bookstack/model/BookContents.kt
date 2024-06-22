package syh7.bookstack.model

import com.google.gson.*
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

class BookContentSerializer : JsonSerializer<BookContents> {
    override fun serialize(bookContents: BookContents, type: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = context.serialize(bookContents).asJsonObject
        when (bookContents) {
            is BookContentsChapter -> jsonObject.addProperty("type", "chapter")
            is BookContentsPage -> jsonObject.addProperty("type", "page")
        }
        return jsonObject
    }
}