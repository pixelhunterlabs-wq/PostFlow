package com.pixelhunter.calorietracker

import java.net.URI
import java.net.URLDecoder

/** Parse only the expected PKCE callback. Never display callback URLs or server descriptions. */
object OAuthCallback {
    private fun parameters(url: String): Map<String, String>? = runCatching {
        val uri = URI(url)
        require(uri.scheme == "calorietracker" && uri.host == "login")
        require(uri.rawAuthority == "login" && uri.path.orEmpty().isEmpty())
        require(uri.rawFragment == null)
        val pairs = uri.rawQuery.orEmpty().split("&").filter { it.isNotEmpty() }.map {
            val pair = it.split("=", limit = 2)
            URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
        }
        require(pairs.map { it.first }.distinct().size == pairs.size)
        pairs.toMap()
    }.getOrNull()

    fun code(url: String): String? = parameters(url)?.let {
        if (it.keys.any { key -> key in setOf("error", "error_code", "error_description") }) null
        else it["code"]?.takeIf(String::isNotBlank)
    }

    fun isValid(url: String): Boolean = code(url) != null

    fun errorMessage(url: String): String = when (parameters(url)?.get("error")) {
        "access_denied" -> "Google hesabına erişim izni verilmedi veya giriş iptal edildi."
        "server_error", "temporarily_unavailable" -> "Google giriş hizmeti şu anda kullanılamıyor. Daha sonra tekrar deneyin."
        else -> "Google dönüş bağlantısı geçersiz veya giriş kodu eksik. Girişi yeniden başlatın."
    }

    fun failureMessage(error: Throwable): String {
        // Use known markers only; raw exception messages may contain tokens or callback URLs.
        val detail = generateSequence(error) { it.cause }.take(8)
            .joinToString(" ") { it.message.orEmpty() }.lowercase()
        return when {
            "provider is not enabled" in detail || "unsupported provider" in detail ->
                "Supabase projesinde Google giriş sağlayıcısı etkin değil."
            "invalid_client" in detail || "client_secret" in detail ->
                "Google OAuth istemci ayarları geçersiz. Sunucudaki Google yapılandırması kontrol edilmeli."
            "redirect" in detail -> "Google dönüş adresi kabul edilmedi. OAuth yönlendirme ayarları kontrol edilmeli."
            "code verifier" in detail || "code_verifier" in detail || "flow_state" in detail || "bad_code" in detail ->
                "Giriş doğrulaması kaybolmuş veya süresi dolmuş. Girişi bu cihazdan yeniden başlatın."
            error is java.io.IOException || "timeout" in detail || "unable to resolve host" in detail ->
                "Google girişi sırasında sunucuya ulaşılamadı. İnternet bağlantınızı kontrol edin."
            else -> "Google oturumu doğrulanamadı. Girişi yeniden başlatın."
        }
    }
}
