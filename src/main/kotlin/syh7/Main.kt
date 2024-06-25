package syh7

import syh7.backup.BackupService
import syh7.bookstack.BookstackService
import syh7.bookstack.CompleteBookSetup
import syh7.bookstack.ExportOptions
import syh7.cache.CacheService
import syh7.parse.ParseService


val bookstackService = BookstackService()
val parseService = ParseService()
val cacheService = CacheService()
val backupService = BackupService()

fun main() {

    val bookName = "Darninia"
    val bookSetup = getBookSetup(bookName)

    println("Start parsing for book $bookName")
    parseService.parseDirectory(bookSetup)
    backupService.backupBook(bookSetup, ExportOptions.MARKDOWN)

    // TODO:
    // replace images in backup text with images from backup folder
    // send sessions to the wiki!
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
        val bookSetup = refreshCache(bookName)
        bookSetup
    }
}

private fun refreshCache(bookName: String): CompleteBookSetup {
    val simpleBook = bookstackService.getBooks().data
        .first { it.name.lowercase() == bookName.lowercase() }
    val bookSetup = bookstackService.retrieveBookSetup(simpleBook.id)
    println("Caching setup")
    cacheService.writeCache(bookSetup)
    return bookSetup
}

