package syh7.bookstack

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import syh7.bookstack.model.*

class BookstackClient {

    private val client = OkHttpClient()
    private val properties = BookstackProperties()
    private val gson = Gson().newBuilder().registerTypeAdapter(BookContents::class.java, BookContentDeserializer()).create()

    fun getBooks(): SimpleBookContainer {
        val apiurl = "${properties.url}/api/books"
        val request = Request.Builder().get().url(apiurl).addHeader("Authorization", properties.tokenString).build()

        return performRequest(request, SimpleBookContainer::class.java)
    }

    fun getChapters(): String {
        val apiurl = "${properties.url}/api/chapters"
        val request = Request.Builder().get().url(apiurl).addHeader("Authorization", properties.tokenString).build()

        return performRequest(request, String::class.java)
    }

    fun getPages(): SimplePageContainer {
        val apiurl = "${properties.url}/api/pages"
        val request = Request.Builder().get().url(apiurl).addHeader("Authorization", properties.tokenString).build()

        return performRequest(request, SimplePageContainer::class.java)
    }

    fun getPage(id: Int): DetailedPage {
        val apiurl = "${properties.url}/api/pages/$id"
        val request = Request.Builder().get().url(apiurl).addHeader("Authorization", properties.tokenString).build()

        return performRequest(request, DetailedPage::class.java)
    }

    fun getBook(id: Int): DetailedBook {
        val apiurl = "${properties.url}/api/books/$id"
        val request = Request.Builder().get().url(apiurl).addHeader("Authorization", properties.tokenString).build()

        return performRequest(request, DetailedBook::class.java)
    }

    fun getBookExport(id: Int, exportOptions: ExportOptions): String {
        val apiurl = "${properties.url}/api/books/$id/export/${exportOptions.name.lowercase()}"
        val request = Request.Builder().get().url(apiurl).addHeader("Authorization", properties.tokenString).build()

        return performRequest(request, String::class.java)
    }

    private fun <T> performRequest(request: Request, clazz: Class<T>): T {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
//                println("successful call to bookstack")
                val body = response.body
                if (body == null) {
                    throw IllegalStateException("empty body after call to ${request.url}")
                } else {
//                    println("received body:")
                    val bodyString = body.string()
//                    println(bodyString)


                    if (clazz == String::class.java) {
                        return bodyString as T
                    }

                    return gson.fromJson(bodyString, clazz)


//                    return bodyString
                }
            } else {
                println("status code: " + response.code)
                println(response.message)
                throw IllegalStateException("Something went wrong trying to call ${request.url}")
            }
        }
    }

}

enum class ExportOptions(val fileExtension: String) {
    HTML(".html"),
    PDF(".pdf"),
    MARKDOWN(".md"),
    PLAINTEXT(".txt"),
}