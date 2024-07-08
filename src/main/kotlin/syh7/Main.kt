package syh7

import syh7.backup.BackupService
import syh7.bookstack.BookstackService
import syh7.bookstack.CompleteBookSetup
import syh7.cache.CacheService
import syh7.parse.ParseService
import syh7.parse.SessionState
import syh7.util.log
import syh7.util.lowerLogOffset
import syh7.util.upLogOffset


val bookstackService = BookstackService()
val parseService = ParseService()
val cacheService = CacheService()
val backupService = BackupService()

fun main() {

    val bookName = "Darninia"

//    refreshCache(bookName)

    log("Starting handling book $bookName")
    val bookSetup = getBookSetup(bookName)

    parseAndAddSessionsToWiki(bookName, bookSetup)

//    log("start backing up book $bookName")
//    backupService.backupMarkdown(bookSetup)

    // TODO:
    // tests!
}

private fun parseAndAddSessionsToWiki(bookName: String, bookSetup: CompleteBookSetup) {
    log("Start parsing for book $bookName")
    val sessions = parseService.parseDirectory(bookSetup)

    log("parsed ${sessions.size}, updating wiki")
    bookstackService.updateSessions(bookSetup, sessions)

    if (sessions.any { it.state == SessionState.NEW }) {
        refreshCache(bookName)
    }
}

private fun getBookSetup(bookName: String): CompleteBookSetup {
    upLogOffset()
    return try {
        val bookSetup = cacheService.readCache(bookName)
        log("Read setup from cache")
        bookSetup
    } catch (exception: Exception) {
        log("There was an error reading cache or there was no cache")
        log("Retrieving setup from the wiki")
        val bookSetup = refreshCache(bookName)
        bookSetup
    } finally {
        lowerLogOffset()
    }
}

private fun refreshCache(bookName: String): CompleteBookSetup {
    val simpleBook = bookstackService.getBooks().data
        .first { it.name.lowercase() == bookName.lowercase() }
    val bookSetup = bookstackService.retrieveBookSetup(simpleBook.id)
    log("Caching setup")
    cacheService.writeCache(bookSetup)
    return bookSetup
}

