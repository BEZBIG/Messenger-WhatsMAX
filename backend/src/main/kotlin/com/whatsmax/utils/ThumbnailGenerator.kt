/** Генерация JPEG-превью изображений (макс. 320px, качество 0.75). */
package com.whatsmax.utils

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

object ThumbnailGenerator {

    private const val MAX_EDGE_PX = 320
    private const val JPEG_QUALITY = 0.75f

    /** Проверяет, поддерживается ли MIME-тип для превью. */
    fun supports(mimeType: String): Boolean =
        mimeType.startsWith("image/", ignoreCase = true) && !mimeType.contains("svg", ignoreCase = true)

    /** Создаёт JPEG-thumbnail или null при ошибке декодирования. */
    fun generate(source: ByteArray): ByteArray? = runCatching {
        val original = ImageIO.read(ByteArrayInputStream(source)) ?: return null
        val scaled = scale(original)
        ByteArrayOutputStream().use { out ->
            val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
            val param = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = JPEG_QUALITY
            }
            ImageIO.createImageOutputStream(out).use { ios ->
                writer.output = ios
                writer.write(null, IIOImage(scaled, null, null), param)
            }
            writer.dispose()
            out.toByteArray()
        }
    }.getOrNull()

    private fun scale(src: BufferedImage): BufferedImage {
        val w = src.width
        val h = src.height
        if (w <= MAX_EDGE_PX && h <= MAX_EDGE_PX) return ensureRgb(src)
        val ratio = MAX_EDGE_PX.toDouble() / maxOf(w, h)
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        val out = BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(src, 0, 0, newW, newH, null)
        g.dispose()
        return out
    }

    private fun ensureRgb(src: BufferedImage): BufferedImage {
        if (src.type == BufferedImage.TYPE_INT_RGB) return src
        val out = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.color = java.awt.Color.WHITE
        g.fillRect(0, 0, src.width, src.height)
        g.drawImage(src, 0, 0, null)
        g.dispose()
        return out
    }
}
