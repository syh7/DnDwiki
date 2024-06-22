package syh7

import syh7.bookstack.BookstackService
import syh7.parse.ParseService


fun main() {

    val bookstackService = BookstackService()
    val parseService = ParseService()

    val bookContainer = bookstackService.getBooks()
    println("${bookContainer.data.size} books found ")

    val simpleDarniniaBook = bookContainer.data.first { it.name == "Darninia" }
    val darniniaSetup = bookstackService.retrieveBookSetup(simpleDarniniaBook)

    parseService.parseDirectory(darniniaSetup)

    // TODO:
    // caching
    // backups
    // tests!
}
