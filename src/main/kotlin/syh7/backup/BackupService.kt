package syh7.backup

import syh7.bookstack.BookstackService
import syh7.bookstack.CompleteBookSetup
import syh7.bookstack.ExportOptions
import syh7.util.log
import syh7.util.lowerLogOffset
import syh7.util.upLogOffset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.writeText

class BackupService {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")

    private val bookstackService = BookstackService()
    private val imageBackupService = ImageBackupService()

    fun backupMarkdown(bookSetup: CompleteBookSetup) {
        upLogOffset()
        val now = LocalDateTime.now().format(dateTimeFormatter)

        val backupFolder = Paths.get(BACKUP_FOLDER, bookSetup.name.lowercase(), now)
        Files.createDirectories(backupFolder)

        log("Retrieving ${ExportOptions.MARKDOWN} export for backup for book ${bookSetup.name}")
        val markdownExport = bookstackService.getExport(bookSetup.bookstackBook.id, ExportOptions.MARKDOWN)

        val plainBackupFile = backupFolder.resolve("plain-export.md")
        log("Saving plain export to backup file $plainBackupFile")
        plainBackupFile.writeText(markdownExport)
        log("Saved text")

        val changedExport = backupImagesAndReplaceLinks(markdownExport, backupFolder)

        val changedBackupFile = backupFolder.resolve("changed-export.md")
        log("Saving plain export to backup file $changedBackupFile")
        changedBackupFile.writeText(changedExport)
        log("Saved text")

        lowerLogOffset()
    }

    private fun backupImagesAndReplaceLinks(markdownExport: String, backupFolder: Path): String {
        var changedExport = markdownExport
        log("Finding all images in text")
        val imageLinks = imageBackupService.findImageLinks(markdownExport)

        imageLinks.forEach { imageLinkSetup ->
            log("Handling image ${imageLinkSetup.fullSetup}")
            val imagePath = imageBackupService.downloadAndSaveImage(imageLinkSetup.fullImage, backupFolder)

            if (imagePath != null) {
                val relativePath = backupFolder.relativize(imagePath)
                val newLink = "${imageLinkSetup.linkText ?: ""}![${imageLinkSetup.brackets}]($relativePath)"
                changedExport = changedExport.replace(imageLinkSetup.fullSetup, newLink)
                log("Replaced ${imageLinkSetup.fullSetup} with $newLink")
            }
        }

        return changedExport
    }

    companion object {
        private const val BACKUP_FOLDER = "src/main/resources/backup"
    }
}