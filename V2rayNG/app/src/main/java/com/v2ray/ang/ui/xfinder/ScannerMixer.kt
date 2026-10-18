package com.v2ray.ang.ui.xfinder

import org.json.JSONObject
import java.util.Base64

/**
 * Matixray: puts clean IPs from the scanner into a config. Each IP becomes one copy of the config whose address is
 * that IP. The old address (a domain) is kept as the TLS server name and the websocket host when the config does not
 * name them, so a CDN still routes the connection to the right site.
 */
object ScannerMixer {
    private val linkShape = Regex("^([a-zA-Z0-9+.-]+://[^@/?#]*@)(\\[[^\\]]+\\]|[^:/?#]+)(:\\d+)?([^#]*)(#.*)?$")

    /** One copy of [config] per address in [ips] (at most [count]); empty when the config kind cannot be mixed. */
    fun mix(config: String, ips: List<String>, count: Int): List<String> {
        val text = config.trim()
        if (text.isEmpty() || count <= 0) return emptyList()
        val chosen = ips.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(count)
        return chosen.mapNotNull { ip ->
            when {
                text.startsWith("vmess://", ignoreCase = true) -> mixVmess(text, ip)
                text.startsWith("vless://", ignoreCase = true) ||
                    text.startsWith("trojan://", ignoreCase = true) -> mixUri(text, ip)
                else -> null
            }
        }
    }

    private fun hostOf(ip: String) = if (':' in ip && !ip.startsWith("[")) "[$ip]" else ip

    private fun isDomain(host: String) = host.any { it.isLetter() } && !host.startsWith("[")

    private fun mixUri(link: String, ip: String): String? {
        val m = linkShape.matchEntire(link) ?: return null
        val (head, oldHost, port, rest, fragment) = m.destructured.let {
            listOf(it.component1(), it.component2(), it.component3(), it.component4(), it.component5())
        }
        var query = rest
        val params = query.removePrefix("?").split("&").filter { it.isNotEmpty() }.map { it.substringBefore('=') }
        if (isDomain(oldHost)) {
            val add = StringBuilder()
            if ("sni" !in params && params.any { it == "security" } ) add.append("&sni=").append(oldHost)
            if ("host" !in params && ("type=ws" in query || "type=httpupgrade" in query || "type=xhttp" in query)) {
                add.append("&host=").append(oldHost)
            }
            if (add.isNotEmpty()) query = if (query.startsWith("?")) query + add else "?" + add.substring(1)
        }
        val name = fragment.removePrefix("#")
        val label = if (name.isEmpty()) ip else "$name | $ip"
        return head + hostOf(ip) + port + query + "#" + label
    }

    private fun mixVmess(link: String, ip: String): String? {
        return try {
            val body = link.substring("vmess://".length).trim()
            val json = JSONObject(String(Base64.getMimeDecoder().decode(body.replace('-', '+').replace('_', '/')), Charsets.UTF_8))
            val oldHost = json.optString("add")
            if (oldHost.isEmpty()) return null
            if (isDomain(oldHost)) {
                if (json.optString("sni").isEmpty()) json.put("sni", oldHost)
                if (json.optString("host").isEmpty() && json.optString("net") in setOf("ws", "httpupgrade", "xhttp")) {
                    json.put("host", oldHost)
                }
            }
            json.put("add", ip)
            val name = json.optString("ps")
            json.put("ps", if (name.isEmpty()) ip else "$name | $ip")
            "vmess://" + Base64.getEncoder().encodeToString(json.toString().toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }
}
