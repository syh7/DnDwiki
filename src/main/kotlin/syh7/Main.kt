package syh7

import syh7.bookstack.BookstackService
import syh7.bookstack.CompleteBookSetup
import java.nio.file.Paths
import kotlin.io.path.*

private const val RAW_SESSION_FOLDER = "src/main/resources/raw"

fun main() {

    val bookstackService = BookstackService()

    val bookContainer = bookstackService.getBooks()
    println("${bookContainer.data.size} books found ")

    val simpleDarniniaBook = bookContainer.data.first { it.name == "Darninia" }
    val darniniaSetup = bookstackService.retrieveBookSetup(simpleDarniniaBook)

    parseDirectory(RAW_SESSION_FOLDER, darniniaSetup)

    // TODO:
    // caching
    // make sure full names are tagged first (if faramar illitris is written, the whole name should link, not just the first name)
    // make sure tags are done once per page instead of once per tag (if faramar links, faramar illitris should not also link)
    // backups
}

@OptIn(ExperimentalPathApi::class)
private fun parseDirectory(baseDirectory: String, setup: CompleteBookSetup) {
    println("walking $baseDirectory")
    Paths.get(baseDirectory, setup.name).walk()
        .forEach { rawFilePath ->
            println("walking $rawFilePath")
            val parsedFilePath = Paths.get(rawFilePath.pathString.replace("raw", "parsed"))
            parsedFilePath.parent.createDirectories()

            var rawText = rawFilePath.readText()
            setup.tagUrlMap.forEach { (tag, url) ->
                rawText = rawText.replaceFirst(tag, "[$tag]($url)")
            }

            parsedFilePath.writeText(rawText)
        }
}




