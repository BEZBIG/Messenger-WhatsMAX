/** Валидация пользовательского ввода и эскейп LIKE-метасимволов. */
package com.whatsmax.utils

private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,32}$")
private val CONTROL_CHARS  = Regex("[\\p{Cc}\\p{Cf}]")

object Validation {

    /** Эскейпит LIKE-метасимволы (% и _) в поисковом запросе. */
    fun escapeLike(query: String): String =
        query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    /** Проверяет username: 3-32 символа, латиница/цифры/подчёркивание. */
    fun validateUsername(username: String) {
        require(USERNAME_REGEX.matches(username)) {
            "Username must be 3-32 characters, letters/digits/underscore only"
        }
    }

    /** Чистит и валидирует displayName (1-128 видимых символов). */
    fun sanitizeDisplayName(raw: String): String {
        val cleaned = raw.replace(CONTROL_CHARS, "").trim()
        require(cleaned.length in 1..128) { "Display name must be 1-128 visible characters" }
        return cleaned
    }

    /** Чистит и валидирует bio (0-500 символов). */
    fun sanitizeBio(raw: String): String {
        val cleaned = raw.replace(CONTROL_CHARS, "").trim()
        require(cleaned.length <= 500) { "Bio must be at most 500 characters" }
        return cleaned
    }
}
