package syh7.bookstack


enum class ExportOptions(val fileExtension: String) {
    HTML(".html"),
    PDF(".pdf"),
    MARKDOWN(".md"),
    PLAINTEXT(".txt"),
}
