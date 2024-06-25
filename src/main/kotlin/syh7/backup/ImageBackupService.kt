package syh7.backup

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
        try {
            println("Downloading image $imageLink")
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
                println("Wrote image to file $imageFolderAndName")
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

    fun findImageLinks(fullText: String): List<ImageLinkSetup> {
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
            println(imageLinkSetup)
            foundImages.add(imageLinkSetup)
        }
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