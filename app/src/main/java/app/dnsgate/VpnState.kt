package app.dnsgate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

enum class Status { OFF, CONNECTING, ON, ERROR }

data class LogEntry(val time: Long, val name: String, val type: String, val ms: Int, val ok: Boolean)

object VpnState {
    val status = MutableStateFlow(Status.OFF)
    val error = MutableStateFlow("")
    val logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val queries = MutableStateFlow(0L)
    val failed = MutableStateFlow(0L)
    @Volatile var startedAt = 0L

    fun push(e: LogEntry, keep: Boolean) {
        queries.update { it + 1 }
        if (!e.ok) failed.update { it + 1 }
        if (keep) logs.update { (listOf(e) + it).take(200) }
    }

    fun reset() {
        queries.value = 0; failed.value = 0; logs.value = emptyList()
    }
}
