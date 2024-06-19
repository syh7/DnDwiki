package syh7.parser

import syh7.bookstack.CompleteBookSetup
import java.nio.file.Paths
import kotlin.io.path.*

class ParseService {

    @OptIn(ExperimentalPathApi::class)
    fun parseDirectory(setup: CompleteBookSetup) {
        println("walking $RAW_SESSION_FOLDER")
        Paths.get(RAW_SESSION_FOLDER, setup.name).walk()
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

    companion object {
        private const val RAW_SESSION_FOLDER = "src/main/resources/raw"
    }

}