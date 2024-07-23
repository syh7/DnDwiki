package syh7.util

import syh7.bookstack.BookstackProperties


private val properties = BookstackProperties()

fun bookUrl(book: String) = "${properties.url}/books/$book"
fun pageUrl(book: String, page: String) = "${bookUrl(book)}/page/$page"
