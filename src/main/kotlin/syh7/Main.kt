package syh7

import syh7.bookstack.BookstackService
import syh7.bookstack.CompleteBookSetup
import syh7.cache.CacheService
import syh7.parse.ParseService


val bookstackService = BookstackService()
val parseService = ParseService()
val cacheService = CacheService()

fun main() {

    val bookName = "Darninia"
    val bookSetup = getBookSetup(bookName)

    println("Start parsing for book $bookName")
    parseService.parseDirectory(bookSetup)

    // TODO:
    // backups
    // tests!
}

private fun getBookSetup(bookName: String): CompleteBookSetup {
    return try {
        val bookSetup = cacheService.readCache(bookName)
        println("Read setup from cache")
        bookSetup
    } catch (exception: Exception) {
        println("There was an error reading cache or there was no cache")
        println("Retrieving setup from the wiki")
        val simpleBook = bookstackService.getBooks().data
            .first { it.name.lowercase() == bookName.lowercase() }
        val bookSetup = bookstackService.retrieveBookSetup(simpleBook)
        println("Caching setup")
        cacheService.writeCache(bookSetup)
        bookSetup
    }
}

