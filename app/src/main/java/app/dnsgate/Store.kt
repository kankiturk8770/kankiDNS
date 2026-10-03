package app.dnsgate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Profile(val id: String, val name: String, val host: String, val ip: String, val token: String)

data class AppSettings(
    val autoConnect: Boolean = false,
    val fallback: Boolean = true,
    val fallbackIp: String = "1.1.1.1",
    val keepLog: Boolean = true
)

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("dnsgate", Context.MODE_PRIVATE)

    fun profiles(c: Context): List<Profile> {
        val a = JSONArray(sp(c).getString("profiles", "[]"))
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Profile(o.getString("id"), o.getString("name"), o.optString("host"), o.optString("ip"), o.optString("token"))
        }
    }

    private fun write(c: Context, list: List<Profile>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().put("id", it.id).put("name", it.name).put("host", it.host).put("ip", it.ip).put("token", it.token))
        }
        sp(c).edit().putString("profiles", a.toString()).apply()
    }

    fun add(c: Context, p: Profile) {
        val rest = profiles(c).filterNot { it.host == p.host && it.ip == p.ip && it.token == p.token }
        write(c, rest + p)
        select(c, p.id)
    }

    fun remove(c: Context, id: String) {
        write(c, profiles(c).filterNot { it.id == id })
        if (sp(c).getString("sel", "") == id) sp(c).edit().remove("sel").apply()
    }

    fun select(c: Context, id: String) = sp(c).edit().putString("sel", id).apply()

    fun selected(c: Context): Profile? {
        val l = profiles(c)
        return l.firstOrNull { it.id == sp(c).getString("sel", "") } ?: l.firstOrNull()
    }

    fun settings(c: Context): AppSettings {
        val s = sp(c)
        return AppSettings(
            s.getBoolean("auto", false), s.getBoolean("fb", true),
            s.getString("fbip", "1.1.1.1") ?: "1.1.1.1", s.getBoolean("log", true)
        )
    }

    fun saveSettings(c: Context, s: AppSettings) {
        sp(c).edit().putBoolean("auto", s.autoConnect).putBoolean("fb", s.fallback)
            .putString("fbip", s.fallbackIp).putBoolean("log", s.keepLog).apply()
    }
}
