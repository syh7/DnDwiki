package syh7.backup

import syh7.bookstack.BookstackService
import syh7.bookstack.CompleteBookSetup
import syh7.bookstack.ExportOptions
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.io.path.writeText

class BackupService {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
    private val imagePattern = Pattern.compile("!\\[image.png]\\((.+)\\)]\\((.+)\\)")

    private val bookstackService = BookstackService()

    fun backupBook(bookSetup: CompleteBookSetup, exportOptions: ExportOptions) {
        when (exportOptions) {
            ExportOptions.MARKDOWN -> backupMarkdown(bookSetup)
            else -> throw NotImplementedError("backup for type $exportOptions is not yet implemented")
        }
    }

    private fun backupMarkdown(bookSetup: CompleteBookSetup) {
        val now = LocalDateTime.now().format(dateTimeFormatter)

        val backupFolder = Paths.get(BACKUP_FOLDER, bookSetup.name.lowercase(), now)
        Files.createDirectories(backupFolder)

        println("Retrieving ${ExportOptions.MARKDOWN} export for backup for book ${bookSetup.name}")
        val markdownExport = bookstackService.getExport(bookSetup.bookstackBook.id, ExportOptions.MARKDOWN)

        val plainBackupFile = backupFolder.resolve("plain-export.md")
        println("Saving plain export to backup file $plainBackupFile")
        plainBackupFile.writeText(markdownExport)
        println("Saved text")

        var changedExport = markdownExport
        val imageLinks = findImageLinks(markdownExport)

        imageLinks.forEach { link ->
            val (_, fullImageLinkWithTrail) = link.split(")](")
            val fullImageLink = fullImageLinkWithTrail.removeSuffix(")")
            val imagePath = downloadAndSaveImage(fullImageLink, backupFolder)

            if (imagePath != null) {
                // TODO fix find and replace with regular text in the link
                // text between preview link and actual link
                // image does not start with image.png but has an actual name (or is a jpg)
                val (prefix, _) = link.split("](")
                val relativePath = backupFolder.relativize(imagePath)
                val newLink = "$prefix]($relativePath)"
                changedExport = changedExport.replace(link, newLink)
            }
        }

        val changedBackupFile = backupFolder.resolve("changed-export.md")
        println("Saving plain export to backup file $changedBackupFile")
        changedBackupFile.writeText(changedExport)
        println("Saved text")

    }

    private fun findImageLinks(fullText: String): List<String> {
        val matcher = imagePattern.matcher(fullText)
        val foundImages = mutableListOf<String>()
        while (matcher.find()) {
            val group = matcher.group()
            println(group)
            foundImages.add(group)
        }
        return foundImages
    }

    private fun downloadAndSaveImage(imageLink: String, backupFolder: Path): Path? {
        try {
            println("Downloading image $imageLink")
            val bufferedImage = ImageIO.read(URI.create(imageLink).toURL())

            val (_, imageFolderAndName) = imageLink.split("/gallery/")
            val (imageFolderName, imageName) = imageFolderAndName.split("/")
            val (_, imageType) = imageName.split(".")
            println("Split image into folder '$imageFolderName', name '$imageName' with type '$imageType'")

            val imageFolder = backupFolder.resolve(imageFolderName)
            val imagePath = imageFolder.resolve(imageName)

            Files.createDirectories(imageFolder)
            Files.createFile(imagePath)

            val successful = ImageIO.write(bufferedImage, imageType, imagePath.toFile())
            if (successful) {
                println("Wrote image to file")
                return imagePath
            } else {
                println("could not write file somehow")
                return null
            }
        } catch (e: Exception) {
            println("Something went wrong: ${e.message}")
            return null
        }
    }

    companion object {
        private const val BACKUP_FOLDER = "src/main/resources/backup"
    }
}