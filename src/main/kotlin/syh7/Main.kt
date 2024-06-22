package syh7

import syh7.bookstack.BookstackService
import syh7.cache.CacheService
import syh7.parse.ParseService


fun main() {

    val bookstackService = BookstackService()
    val parseService = ParseService()
    val cacheService = CacheService()

    val bookContainer = bookstackService.getBooks()
    println("${bookContainer.data.size} books found ")

    val simpleDarniniaBook = bookContainer.data.first { it.name == "Darninia" }
    val darniniaSetup = bookstackService.retrieveBookSetup(simpleDarniniaBook)

    parseService.parseDirectory(darniniaSetup)
    cacheService.writeCache(darniniaSetup)
    val cachedBooksetup = cacheService.readCache("Darninia")

    println("read booksetup")


    // TODO:
    // backups
    // tests!
}
