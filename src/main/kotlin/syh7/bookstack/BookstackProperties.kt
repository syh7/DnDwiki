package syh7.bookstack

import java.io.File
import java.nio.file.Paths
import java.util.*

class BookstackProperties {

    val tokenString: String
    val url: String


    init {
        val props = Properties()
        val path = Paths.get("src/main/resources/bookstack.properties")
        props.load(File(path.toUri()).inputStream())
        tokenString = "Token " + props.getProperty("api.token.id") + ":" + props.getProperty("api.token.secret")
        url = props.getProperty("website.url")
    }

}