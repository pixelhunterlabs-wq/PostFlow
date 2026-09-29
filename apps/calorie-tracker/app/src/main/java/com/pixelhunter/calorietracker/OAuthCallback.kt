package com.pixelhunter.calorietracker

import java.net.URI
import java.net.URLDecoder

/** Only hand successful PKCE callbacks to the SDK; never log callback tokens. */
object OAuthCallback {
    fun isValid(url: String): Boolean = runCatching {
        val uri = URI(url)
        val parameters = uri.rawQuery.orEmpty().split("&").associate {
            val pair = it.split("=", limit = 2)
            URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
        }
        uri.scheme == "calorietracker" && uri.host == "login" &&
            uri.path.orEmpty().isEmpty() && !parameters.containsKey("error") &&
            !parameters["code"].isNullOrBlank()
    }.getOrDefault(false)
}
