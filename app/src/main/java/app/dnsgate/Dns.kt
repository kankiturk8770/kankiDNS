package app.dnsgate

import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

object Dns {
    private val TYPES = mapOf(1 to "A", 2 to "NS", 5 to "CNAME", 15 to "MX", 16 to "TXT", 28 to "AAAA", 33 to "SRV", 64 to "SVCB", 65 to "HTTPS")

    fun query(name: String, type: Int = 1): ByteArray {
        val o = ByteArrayOutputStream()
        o.write(byteArrayOf(0x12, 0x34, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0))
        name.split('.').forEach { val b = it.toByteArray(); o.write(b.size); o.write(b) }
        o.write(0)
        o.write(type shr 8); o.write(type and 255); o.write(0); o.write(1)
        return o.toByteArray()
    }

    /** نام و نوع کوئری را از بسته DNS بیرون می‌کشد (برای نمایش در لاگ). */
    fun question(p: ByteArray): Pair<String, String>? {
        if (p.size < 13) return null
        var i = 12
        val sb = StringBuilder()
        while (i < p.size) {
            val l = p[i].toInt() and 0xFF
            if (l == 0) { i++; break }
            if (l >= 0xC0 || i + 1 + l > p.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(p, i + 1, l, Charsets.UTF_8))
            i += 1 + l
        }
        if (i + 2 > p.size) return null
        val t = ((p[i].toInt() and 0xFF) shl 8) or (p[i + 1].toInt() and 0xFF)
        return sb.toString() to (TYPES[t] ?: t.toString())
    }

    /** تاخیر پاسخ DNS سرور به میلی‌ثانیه؛ -1 یعنی پاسخ نیامد. */
    fun ping(host: String): Int = try {
        val addr = InetAddress.getByName(host)
        DatagramSocket().use { s ->
            s.soTimeout = 2500
            val q = query("example.com")
            val t = System.nanoTime()
            s.send(DatagramPacket(q, q.size, addr, 53))
            val b = ByteArray(512)
            s.receive(DatagramPacket(b, b.size))
            ((System.nanoTime() - t) / 1_000_000).toInt()
        }
    } catch (e: Exception) { -1 }
}

object Portal {
    /** تلاش بی‌صدا برای باز کردن لینک پورتال کاربر (اگر پنل ثبت IP را از این مسیر انجام بدهد). */
    fun touch(p: Profile) {
        if (p.host.isEmpty() || p.token.isEmpty()) return
        try {
            val c = URL("https://${p.host}/sub/${p.token}").openConnection() as HttpURLConnection
            c.connectTimeout = 3000; c.readTimeout = 3000
            c.responseCode
            c.disconnect()
        } catch (e: Exception) { }
    }
}
