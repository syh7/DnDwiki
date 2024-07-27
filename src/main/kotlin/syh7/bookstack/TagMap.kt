package syh7.bookstack

import syh7.bookstack.model.DetailedPage
import syh7.util.createPageUrl

data class TagMap(val tags: List<String>, val url: String) {
    override fun toString(): String {
        return "$url is seen by $tags"
    }
}

fun createTagMap(page: DetailedPage, book: String): List<TagMap> {
    if (page.tags.isEmpty()) {
        return emptyList()
    }

    return page.tags
        .groupBy { it.value }
        .map { (tagValue, subTags) ->
            val tagNames = subTags.map { it.name }
            if (tagValue.isEmpty()) {
                listOf(TagMap(tagNames, createPageUrl(book, page.slug)))
            } else {
                val tag = tagValue.replace(" ", "%20")
                val tagUrl = createPageUrl(book, page.slug, tag)
                tagNames.map { TagMap(listOf(it), tagUrl) }
            }
        }
        .flatten()

}
