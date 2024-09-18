package org.jetbrains.skiko.menu

import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon

object IconConverter {

    // Converts a Swing Icon to a byte array in PNG format
    @JvmStatic
    fun iconToByteArray(icon: Icon?): ByteArray? {
        if (icon is ImageIcon) {
            val image: Image = icon.image
            val bufferedImage = BufferedImage(
                image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
            )
            val g2d: Graphics2D = bufferedImage.createGraphics()
            g2d.drawImage(image, 0, 0, null)
            g2d.dispose()

            return try {
                ByteArrayOutputStream().use { baos ->
                    ImageIO.write(bufferedImage, "png", baos)
                    baos.toByteArray()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
        return null
    }
}
