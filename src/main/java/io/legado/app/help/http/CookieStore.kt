@file:Suppress("unused")

package io.legado.app.help.http

import com.htmake.reader.utils.getRelativePath
import com.htmake.reader.utils.getStoragePath
import io.legado.app.utils.TextUtils
import io.legado.app.help.http.api.CookieManager
import io.legado.app.utils.NetworkUtils
import io.vertx.core.json.JsonObject
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

object CookieStore : CookieManager {

    private const val COOKIE_DIR = "cookies"
    private const val COOKIE_FILE = "book_sources.json"

    @Volatile
    private var userNameSpace: String? = null

    fun setUserNameSpace(userNameSpace: String?) {
        CookieStore.userNameSpace = userNameSpace?.takeIf { it.isNotBlank() }
    }

    internal fun cookieStoreFile(userNameSpace: String? = CookieStore.userNameSpace): File {
        val storagePath = getStoragePath()
        return if (userNameSpace.isNullOrBlank()) {
            File(getRelativePath(storagePath, COOKIE_DIR, COOKIE_FILE))
        } else {
            File(getRelativePath(storagePath, "data", userNameSpace, COOKIE_DIR, COOKIE_FILE))
        }
    }

    override fun setCookie(url: String, cookie: String?) {
        synchronized(this) {
            val key = cookieKey(url)
            if (key.isEmpty()) {
                return
            }
            val cookies = readCookies()
            if (cookie.isNullOrBlank()) {
                cookies.remove(key)
            } else {
                cookies.put(key, cookie)
            }
            writeCookies(cookies)
        }
    }

    override fun replaceCookie(url: String, cookie: String) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(cookie)) {
            return
        }
        val oldCookie = getCookie(url)
        if (TextUtils.isEmpty(oldCookie)) {
            setCookie(url, cookie)
        } else {
            val cookieMap = cookieToMap(oldCookie)
            cookieMap.putAll(cookieToMap(cookie))
            val newCookie = mapToCookie(cookieMap)
            setCookie(url, newCookie)
        }
    }

    override fun getCookie(url: String): String {
        synchronized(this) {
            val key = cookieKey(url)
            if (key.isEmpty()) {
                return ""
            }
            return readCookies().getString(key) ?: ""
        }
    }

    override fun removeCookie(url: String) {
        synchronized(this) {
            val key = cookieKey(url)
            if (key.isEmpty()) {
                return
            }
            val cookies = readCookies()
            if (cookies.containsKey(key)) {
                cookies.remove(key)
                writeCookies(cookies)
            }
        }
    }

    override fun cookieToMap(cookie: String): MutableMap<String, String> {
        val cookieMap = mutableMapOf<String, String>()
        if (cookie.isBlank()) {
            return cookieMap
        }
        val pairArray = cookie.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (pair in pairArray) {
            val pairs = pair.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (pairs.size == 1) {
                continue
            }
            val key = pairs[0].trim { it <= ' ' }
            val value = pairs[1]
            if (value.isNotBlank() || value.trim { it <= ' ' } == "null") {
                cookieMap[key] = value.trim { it <= ' ' }
            }
        }
        return cookieMap
    }

    override fun mapToCookie(cookieMap: Map<String, String>?): String? {
        if (cookieMap == null || cookieMap.isEmpty()) {
            return null
        }
        val builder = StringBuilder()
        for (key in cookieMap.keys) {
            val value = cookieMap[key]
            if (value?.isNotBlank() == true) {
                builder.append(key)
                    .append("=")
                    .append(value)
                    .append(";")
            }
        }
        return builder.deleteCharAt(builder.lastIndexOf(";")).toString()
    }

    fun clear() {
        synchronized(this) {
            val file = cookieStoreFile()
            if (!file.exists()) {
                return
            }
            try {
                if (!file.delete()) {
                    writeCookies(JsonObject())
                }
            } catch (e: Exception) {
                logger.error(e) { "Clear cookie store failed: ${file.absolutePath}" }
            }
        }
    }

    private fun cookieKey(url: String): String {
        val key = url.trim()
        if (key.isEmpty()) {
            return ""
        }
        if (!key.startsWith("http")) {
            return key
        }
        return NetworkUtils.getSubDomain(key).ifEmpty { key }
    }

    private fun readCookies(): JsonObject {
        val file = cookieStoreFile()
        if (!file.exists()) {
            return JsonObject()
        }
        return try {
            JsonObject(file.readText())
        } catch (e: Exception) {
            logger.error(e) { "Read cookie store failed: ${file.absolutePath}" }
            JsonObject()
        }
    }

    private fun writeCookies(cookies: JsonObject) {
        val file = cookieStoreFile()
        val parentFile = file.parentFile
        val tempFile = File(parentFile, "${file.name}.tmp")
        try {
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs()
            }
            tempFile.writeText(cookies.encode())
            if (file.exists() && !file.delete()) {
                logger.error { "Delete old cookie store failed: ${file.absolutePath}" }
                tempFile.delete()
                return
            }
            if (!tempFile.renameTo(file)) {
                logger.error { "Replace cookie store failed: ${file.absolutePath}" }
                tempFile.delete()
            }
        } catch (e: Exception) {
            logger.error(e) { "Write cookie store failed: ${file.absolutePath}" }
            tempFile.delete()
        }
    }

}
