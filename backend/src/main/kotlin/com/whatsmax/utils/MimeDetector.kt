/** MIME-детекция по magic bytes для защиты от подделки Content-Type. */
package com.whatsmax.utils

object MimeDetector {

    fun detect(bytes: ByteArray): String? {
        if (bytes.size < 12) return null

        fun b(i: Int) = bytes[i].toInt() and 0xFF

        return when {
            bytes.size >= 3 && b(0) == 0xFF && b(1) == 0xD8 && b(2) == 0xFF -> "image/jpeg"
            b(0) == 0x89 && b(1) == 0x50 && b(2) == 0x4E && b(3) == 0x47 -> "image/png"
            b(0) == 0x47 && b(1) == 0x49 && b(2) == 0x46 -> "image/gif"
            b(0) == 0x52 && b(1) == 0x49 && b(2) == 0x46 && b(3) == 0x46 &&
                    b(8) == 0x57 && b(9) == 0x45 && b(10) == 0x42 && b(11) == 0x50 -> "image/webp"
            b(0) == 0x25 && b(1) == 0x50 && b(2) == 0x44 && b(3) == 0x46 -> "application/pdf"
            bytes.size >= 12 && b(4) == 0x66 && b(5) == 0x74 && b(6) == 0x79 && b(7) == 0x70 -> {
                val brand = String(bytes, 8, 4)
                when (brand) {
                    "M4A ", "mp42" -> "audio/mp4"
                    else -> "video/mp4"
                }
            }
            b(0) == 0x4F && b(1) == 0x67 && b(2) == 0x67 && b(3) == 0x53 -> "audio/ogg"
            b(0) == 0x50 && b(1) == 0x4B && b(2) == 0x03 && b(3) == 0x04 -> "application/zip"
            b(0) == 0x4D && b(1) == 0x5A -> "application/x-msdownload"
            b(0) == 0x7F && b(1) == 0x45 && b(2) == 0x4C && b(3) == 0x46 -> "application/x-elf"
            else -> null
        }
    }

    fun isConsistent(declaredMime: String, bytes: ByteArray): Boolean {
        val detected = detect(bytes) ?: return true
        if (detected == "application/x-msdownload" || detected == "application/x-elf") return false
        if (declaredMime.startsWith("image/") && !detected.startsWith("image/")) return false
        if (declaredMime.startsWith("audio/") && !detected.startsWith("audio/")) return false
        if (declaredMime.startsWith("video/") && !detected.startsWith("video/")) return false
        return true
    }
}
