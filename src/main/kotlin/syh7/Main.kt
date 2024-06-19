package syh7

import syh7.bookstack.BookstackService
import syh7.parser.ParseService


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
    // make sure full names are tagged first (if faramar illitris is written, the whole name should link, not just the first name)
    // make sure tags are done once per page instead of once per tag (if faramar links, faramar illitris should not also link)
    // backups
}
