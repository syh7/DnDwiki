package syh7.bookstack

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import syh7.bookstack.model.*
import syh7.util.log

class BookstackClient {

    private val client = OkHttpClient()
    private val properties = BookstackProperties()
    private val gson = Gson().newBuilder().registerTypeAdapter(BookContents::class.java, BookContentDeserializer()).create()

    fun getBooks(): SimpleBookContainer {
        val apiurl = "${properties.url}/api/books"
        val request = Request.Builder().get().url(apiurl).addHeader("Authorization", properties.tokenString).build()

        return performRequest(request, SimpleBookContainer::class.java)
    }

    fun getPage(id: Int): DetailedPage {
        val apiurl = "${properties.url}/api/pages/$id"
        val request = Request.Builder().get().url(apiurl).addHeader("Authorization", properties.tokenString).build()

        return performRequest(request, DetailedPage::class.java)
    }

    fun addPage(newPageRequestBody: NewPageRequestBody): DetailedPage {
        val apiurl = "${properties.url}/api/pages"
        val contentType = "application/json".toMediaType()
        val requestBody = gson.toJson(newPageRequestBody).toRequestBody(contentType = contentType)
        val request = Request.Builder().post(requestBody).url(apiurl).addHeader("Authorization", properties.tokenString).build()
        return performRequest(request, DetailedPage::class.java)
    }

    fun deletePage(id: Int): String {
        val apiurl = "${properties.url}/api/pages/$id"
        val request = Request.Builder().delete().url(apiurl).addHeader("Authorization", properties.tokenString).build()

        return performRequest(request, String::class.java)
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
                val body = response.body
                if (body == null) {
                    throw IllegalStateException("empty body after call to ${request.url}")
                } else {
                    val bodyString = body.string()
                    if (clazz == String::class.java) {
                        return bodyString as T
                    }

                    return gson.fromJson(bodyString, clazz)
                }
            } else {
                log("status code: " + response.code)
                log(response.message)
                response.body?.let { log(it.string()) }
                throw IllegalStateException("Something went wrong trying to call ${request.url}")
            }
        }
    }

}

data class NewPageRequestBody(
    val book_id: Int? = null,
    val chapter_id: Int? = null,
    val name: String,
    val html: String? = null,
    val markdown: String? = null,
) {
    init {
        if (book_id == null && chapter_id == null) {
            throw IllegalStateException("either book or chapter id is required")
        }
        if (html == null && markdown == null) {
            throw IllegalStateException("either html or markdown is required")
        }
    }
}
