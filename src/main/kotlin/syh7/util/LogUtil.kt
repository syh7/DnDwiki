package syh7.util

var logOffset = 0
fun upLogOffset() = logOffset++
fun lowerLogOffset() = logOffset--
fun log(message: Any) {
    println("${"  ".repeat(logOffset)}$message")
}