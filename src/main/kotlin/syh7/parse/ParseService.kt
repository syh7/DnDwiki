package syh7.parse

import syh7.bookstack.CompleteBookSetup
import syh7.util.log
import syh7.util.lowerLogOffset
import syh7.util.upLogOffset
import java.nio.file.Paths
import kotlin.io.path.*

class ParseService {

    @OptIn(ExperimentalPathApi::class)
    fun parseDirectory(setup: CompleteBookSetup) {
        upLogOffset()
        log("walking $RAW_SESSION_FOLDER")
        Paths.get(RAW_SESSION_FOLDER, setup.name.lowercase()).walk()
            .forEach { rawFilePath ->
                log("walking $rawFilePath")
                val parsedFilePath = Paths.get(rawFilePath.pathString.replace("raw", "parsed"))

                if (parsedFilePath.exists()) {
                    log("File already parsed, skipping")
                    return@forEach
                }

                parsedFilePath.parent.createDirectories()

                var rawText = rawFilePath.readText()
                setup.tagUrlMap.forEach { (tags, url) ->
                    tags.firstOrNull { rawText.contains(it) }
                        ?.let { rawText = rawText.replaceFirst(it, "[$it]($url)") }
                }

                log("Parsed file, writing to $parsedFilePath")
                parsedFilePath.writeText(rawText)
            }
        lowerLogOffset()
    }

    companion object {
        private const val RAW_SESSION_FOLDER = "src/main/resources/raw"
    }

}