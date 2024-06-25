package syh7.backup

import syh7.util.log
import syh7.util.lowerLogOffset
import syh7.util.upLogOffset
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern
import javax.imageio.ImageIO

class ImageBackupService {

    // complex regex made more complex by escaping characters
    // [(possible link text)![(name of file)](possible thumbnail)(possible second link text)](actual image link)
    private val imagePattern = Pattern.compile("\\[(.+)?!\\[(.+\\..+)](\\(.+\\))?(.+)?]\\((.+)\\)")

    fun downloadAndSaveImage(imageLink: String, backupFolder: Path): Path? {
        upLogOffset()
        try {
            log("Downloading image $imageLink")
            val bufferedImage = ImageIO.read(URI.create(imageLink).toURL())

            val (_, imageFolderAndName) = imageLink.split("/gallery/")
            val (imageFolderName, imageName) = imageFolderAndName.split("/")
            val (_, imageType) = imageName.split(".")

            val imageFolder = backupFolder.resolve(imageFolderName)
            val imagePath = imageFolder.resolve(imageName)

            Files.createDirectories(imageFolder)
            Files.createFile(imagePath)

            val successful = ImageIO.write(bufferedImage, imageType, imagePath.toFile())
            if (successful) {
                log("Wrote image to file $imageFolderAndName")
                return imagePath
            } else {
                log("could not write file somehow")
                return null
            }
        } catch (e: Exception) {
            log("Something went wrong: ${e.message}")
            return null
        } finally {
            lowerLogOffset()
        }
    }

    fun findImageLinks(fullText: String): List<ImageLinkSetup> {
        upLogOffset()
        val matcher = imagePattern.matcher(fullText)
        val foundImages = mutableListOf<ImageLinkSetup>()
        while (matcher.find()) {
            val imageLinkSetup = ImageLinkSetup(
                fullSetup = matcher.group(0),
                brackets = matcher.group(2),
                thumbnail = matcher.group(3),
                linkText = (matcher.group(1) ?: "") + (matcher.group(4) ?: ""),
                fullImage = matcher.group(5),
            )
            log(imageLinkSetup)
            foundImages.add(imageLinkSetup)
        }
        lowerLogOffset()
        return foundImages
    }

    data class ImageLinkSetup(
        val fullSetup: String,
        val brackets: String,
        val thumbnail: String?,
        val linkText: String?,
        val fullImage: String,
    )
}