package syh7.util

import syh7.bookstack.BookstackProperties


private val properties = BookstackProperties()

fun createBookUrl(bookName: String) = "${properties.url}/books/$bookName"

fun createPageUrl(bookName: String, pageSlug: String, heading: String? = null): String {
    val headingSuffix = heading?.let { "#$heading" } ?: ""
    return createBookUrl(bookName) + "/page/$pageSlug" + headingSuffix
}
