package syh7.cache

import com.google.gson.Gson
import syh7.bookstack.CompleteBookSetup
import syh7.bookstack.model.BookContentDeserializer
import syh7.bookstack.model.BookContentSerializer
import syh7.bookstack.model.BookContents
import syh7.util.log
import syh7.util.lowerLogOffset
import syh7.util.upLogOffset
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.readText
import kotlin.io.path.writeText

class CacheService {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
    private val gson = Gson().newBuilder()
        .enableComplexMapKeySerialization() // enables Map<Object, Object> to serialize to Map<Object, Object> instead of Map<String, Object>
        .registerTypeAdapter(BookContents::class.java, BookContentSerializer())
        .registerTypeAdapter(BookContents::class.java, BookContentDeserializer())
        .create()


    fun writeCache(bookSetup: CompleteBookSetup) {
        upLogOffset()
        val folderName = bookSetup.name.lowercase()
        val cacheFolder = Paths.get(CACHE_FOLDER, folderName)
        val now = LocalDateTime.now().format(dateTimeFormatter)
        val cacheFile = cacheFolder.resolve("$now.json")
        log("Writing cache file $cacheFile")

        val jsonString = gson.toJson(bookSetup)
        cacheFile.writeText(jsonString)
        log("Wrote cache file $cacheFile")
        lowerLogOffset()
    }

    fun readCache(bookName: String): CompleteBookSetup {
        upLogOffset()
        val folderName = bookName.lowercase()
        val cacheFolder = Paths.get(CACHE_FOLDER, folderName)
        val latestCache = Files.list(cacheFolder)
            .sorted { path1, path2 -> path2.toFile().lastModified().compareTo(path1.toFile().lastModified()) }
            .peek { log(it) }
            .findFirst().orElseThrow { NoSuchElementException("No cache for book $bookName") }
        log("Reading cache from file $latestCache")

        val bookSetup = gson.fromJson(latestCache.readText(), CompleteBookSetup::class.java)
        log("Parsed file $latestCache to booksetup")
        lowerLogOffset()
        return bookSetup
    }


    companion object {
        private const val CACHE_FOLDER = "src/main/resources/cache"
    }

}