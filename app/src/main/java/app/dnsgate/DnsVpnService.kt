package app.dnsgate

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * یک VPN سبک فقط برای DNS: آدرس DNS مجازی 10.77.0.1 به سیستم داده می‌شود،
 * کوئری‌ها از tun خوانده و به DNS سرور شما (UDP/53) فرستاده می‌شوند.
 * بقیه ترافیک اصلاً از VPN رد نمی‌شود.
 */
class DnsVpnService : VpnService() {
    companion object {
        const val ACTION_STOP = "app.dnsgate.STOP"
        const val FAKE = "10.77.0.1"
        private val FAKE_BYTES = byteArrayOf(10, 77, 0, 1)
    }

    private var tun: ParcelFileDescriptor? = null
    private var out: FileOutputStream? = null
    private var pool: ExecutorService? = null
    @Volatile private var running = false
    @Volatile private var stopping = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopping = true; running = false
            try { tun?.close() } catch (e: Exception) { }
            if (VpnState.status.value != Status.ON) { VpnState.status.value = Status.OFF; stopSelf() }
            return START_NOT_STICKY
        }
        if (!running && VpnState.status.value != Status.CONNECTING) {
            stopping = false
            VpnState.status.value = Status.CONNECTING
            Thread { begin() }.start()
        }
        return START_STICKY
    }

    private fun begin() {
        var err: String? = null
        try {
            val p = Store.selected(this) ?: throw IllegalStateException("سروری انتخاب نشده است.")
            val st = Store.settings(this)
            val ip = if (p.ip.isNotEmpty()) p.ip else InetAddress.getByName(p.host).hostAddress!!
            Portal.touch(p)
            if (stopping) throw InterruptedException()
            val pfd = Builder().setSession("kankiDNS").addAddress("10.77.0.2", 32)
                .addDnsServer(FAKE).addRoute(FAKE, 32).setMtu(1500).setBlocking(true).establish()
                ?: throw IllegalStateException("اجازه VPN داده نشد.")
            tun = pfd; out = FileOutputStream(pfd.fileDescriptor)
            if (stopping) throw InterruptedException()
            pool = Executors.newFixedThreadPool(8)
            running = true
            VpnState.reset(); VpnState.startedAt = System.currentTimeMillis()
            VpnState.status.value = Status.ON
            loop(pfd, ip, st)
        } catch (e: Exception) {
            if (!stopping) err = e.message ?: "خطای ناشناخته"
        }
        cleanup()
        if (err != null) { VpnState.error.value = err; VpnState.status.value = Status.ERROR }
        else VpnState.status.value = Status.OFF
        stopSelf()
    }

    private fun loop(pfd: ParcelFileDescriptor, ip: String, st: AppSettings) {
        val input = FileInputStream(pfd.fileDescriptor)
        val buf = ByteArray(32767)
        while (running) {
            val n = input.read(buf)
            if (n < 0) break
            if (n < 28) continue
            val ihl = (buf[0].toInt() and 0x0F) * 4
            if ((buf[0].toInt() shr 4) != 4 || buf[9].toInt() != 17 || n < ihl + 8) continue
            var match = true
            for (i in 0 until 4) if (buf[16 + i] != FAKE_BYTES[i]) match = false
            if (!match) continue
            val dport = ((buf[ihl + 2].toInt() and 0xFF) shl 8) or (buf[ihl + 3].toInt() and 0xFF)
            if (dport != 53) continue
            val pkt = buf.copyOf(n)
            val payload = pkt.copyOfRange(ihl + 8, n)
            pool?.execute { handle(pkt, ihl, payload, ip, st) }
        }
    }

    private fun handle(pkt: ByteArray, ihl: Int, q: ByteArray, ip: String, st: AppSettings) {
        val t0 = System.nanoTime()
        val resp = forward(q, ip) ?: if (st.fallback) forward(q, st.fallbackIp) else null
        val ms = ((System.nanoTime() - t0) / 1_000_000).toInt()
        if (resp != null) reply(pkt, ihl, resp)
        val qq = Dns.question(q)
        val ok = resp != null && resp.size > 3 && (resp[3].toInt() and 0x0F) != 5
        VpnState.push(LogEntry(System.currentTimeMillis(), qq?.first ?: "?", qq?.second ?: "?", ms, ok), st.keepLog)
    }

    private fun forward(q: ByteArray, ip: String): ByteArray? = try {
        DatagramSocket().use { s ->
            protect(s)
            s.soTimeout = 3000
            s.send(DatagramPacket(q, q.size, InetAddress.getByName(ip), 53))
            val b = ByteArray(4096)
            val r = DatagramPacket(b, b.size)
            s.receive(r)
            b.copyOf(r.length)
        }
    } catch (e: Exception) { null }

    private fun reply(pkt: ByteArray, ihl: Int, resp: ByteArray) {
        val total = 28 + resp.size
        val o = ByteArray(total)
        o[0] = 0x45; o[2] = (total shr 8).toByte(); o[3] = total.toByte(); o[8] = 64; o[9] = 17
        System.arraycopy(pkt, 16, o, 12, 4)
        System.arraycopy(pkt, 12, o, 16, 4)
        var sum = 0
        for (i in 0 until 20 step 2) sum += ((o[i].toInt() and 0xFF) shl 8) or (o[i + 1].toInt() and 0xFF)
        while ((sum shr 16) != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        val ck = sum.inv() and 0xFFFF
        o[10] = (ck shr 8).toByte(); o[11] = ck.toByte()
        o[20] = pkt[ihl + 2]; o[21] = pkt[ihl + 3]; o[22] = pkt[ihl]; o[23] = pkt[ihl + 1]
        val ul = 8 + resp.size
        o[24] = (ul shr 8).toByte(); o[25] = ul.toByte()
        System.arraycopy(resp, 0, o, 28, resp.size)
        synchronized(this) { try { out?.write(o) } catch (e: Exception) { } }
    }

    private fun cleanup() {
        running = false
        pool?.shutdownNow(); pool = null
        try { tun?.close() } catch (e: Exception) { }
        tun = null; out = null
    }

    override fun onRevoke() {
        stopping = true; running = false
        try { tun?.close() } catch (e: Exception) { }
        super.onRevoke()
    }

    override fun onDestroy() {
        cleanup()
        if (VpnState.status.value == Status.ON || VpnState.status.value == Status.CONNECTING) VpnState.status.value = Status.OFF
        super.onDestroy()
    }
}
