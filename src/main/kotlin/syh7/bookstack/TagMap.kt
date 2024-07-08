package syh7.bookstack

import syh7.bookstack.model.DetailedPage

data class TagMap(val tags: List<String>, val url: String) {
    override fun toString(): String {
        return "$url is seen by $tags"
    }
}

fun createTagMap(page: DetailedPage, bookUrl: String): List<TagMap> {
    if (page.tags.isEmpty()) {
        return emptyList()
    }

    val pageUrl = "$bookUrl/page/${page.slug}"

    return page.tags
        .groupBy { it.value }
        .map { (tagValue, subTags) ->
            val tagNames = subTags.map { it.name }
            if (tagValue.isEmpty()) {
                listOf(TagMap(tagNames, pageUrl))
            } else {
                val tagUrl = "$pageUrl#${tagValue.replace(" ", "%20")}"
                tagNames.map { TagMap(listOf(it), tagUrl) }
            }
        }
        .flatten()

}
