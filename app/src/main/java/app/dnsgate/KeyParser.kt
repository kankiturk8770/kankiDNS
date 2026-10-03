package app.dnsgate

import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.util.UUID

/**
 * فرمت‌های پشتیبانی‌شده برای «کد ورود»:
 *  1) dnsgate://HOST/TOKEN?ip=1.2.3.4&name=Office
 *  2) DG1.<base64url(JSON)>   مثال JSON: {"name":"سرور ۱","host":"dns.example.com","ip":"203.0.113.10","token":"abc"}
 * وقتی پنل کد تولید کرد، فقط همین فایل را با فرمت واقعی هماهنگ کنید.
 */
object KeyParser {
    fun parse(raw: String): Profile {
        val s = raw.trim()
        require(s.isNotEmpty()) { "کد خالی است." }
        val id = UUID.randomUUID().toString()
        if (s.startsWith("DG1.")) {
            val bytes = Base64.decode(s.removePrefix("DG1."), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val j = JSONObject(String(bytes, Charsets.UTF_8))
            val host = j.optString("host")
            val ip = j.optString("ip")
            require(host.isNotEmpty() || ip.isNotEmpty()) { "آدرس سرور در کد نیست." }
            return Profile(id, j.optString("name", host.ifEmpty { ip }), host, ip, j.optString("token"))
        }
        if (s.startsWith("dnsgate://")) {
            val u = Uri.parse(s)
            val host = u.host
            require(!host.isNullOrEmpty()) { "آدرس سرور پیدا نشد." }
            val ip = u.getQueryParameter("ip").orEmpty()
            val name = u.getQueryParameter("name") ?: host
            val token = u.pathSegments.firstOrNull().orEmpty()
            return Profile(id, name, host, ip, token)
        }
        throw IllegalArgumentException("فرمت کد پشتیبانی نمی‌شود.")
    }
}
