package ir.mahdiparastesh.mobinaexplorer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.Volley
import java.util.regex.Pattern

class Fetcher(
    private val c: Context,
    url: String,
    private val onSuccess: Listener,
    cache: Boolean = false,
    method: Int = Method.GET,
    private val body: String? = null,
    private val crawler: Crawler? = if (c is Explorer) c.crawler else null,
    onError: Response.ErrorListener? = null,
) : Request<ByteArray>(method, encode(url), onError ?: Response.ErrorListener {
    Panel.handler?.obtainMessage(Panel.Action.WAVE_DOWN.ordinal)?.sendToTarget()
    val code = it.networkResponse?.statusCode

    when (code) {
        404 -> {
            crawler?.signal(Crawler.Signal.PAGE_NOT_FOUND, code.toString())
            Crawler.handler?.obtainMessage(Crawler.HANDLE_NOT_FOUND)?.sendToTarget()
            return@ErrorListener
        }
    }

    if (doesErrorPersist < Crawler.maxTryAgain) {
        crawler?.signal(Crawler.Signal.VOLLEY_ERROR, code.toString())
        Crawler.handler?.obtainMessage(HANDLE_ERROR)?.sendToTarget()
    } else crawler?.signal(Crawler.Signal.VOLLEY_NOT_WORKING, it.message.toString())
    doesErrorPersist++
}) {
    init {
        Panel.handler?.obtainMessage(Panel.Action.WAVE_UP.ordinal)?.sendToTarget()
        setShouldCache(cache)
        tag = "fetch"
        retryPolicy = DefaultRetryPolicy(
            10000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        Volley.newRequestQueue(c).add(this)
    }

    @Suppress("SpellCheckingInspection")
    override fun getHeaders(): HashMap<String, String> =
        (crawler?.headers ?: Crawler.deployHeaders(c)).apply {
            if (method == Method.POST) {
                this["content-type"] = "application/x-www-form-urlencoded"
                this["sec-fetch-site"] = "same-origin"
                this["x-requested-with"] = "XMLHttpRequest"
                if (this["cookie"]!!.contains("csrftoken="))
                    this["x-csrftoken"] = this["cookie"]!!
                        .substringAfter("csrftoken=")
                        .substringBefore(";")
            } else this["sec-fetch-site"] = "same-site"
        }

    override fun getBody(): ByteArray = encode(body)?.encodeToByteArray() ?: super.getBody()

    override fun deliverResponse(response: ByteArray) = onSuccess.onResponse(response)

    override fun parseNetworkResponse(response: NetworkResponse): Response<ByteArray> =
        Response.success(response.data as ByteArray, HttpHeaderParser.parseCacheHeaders(response))

    class Listener(
        private val handler: Handler? = Crawler.handler,
        private val listener: OnFinished
    ) : Response.Listener<ByteArray> {
        override fun onResponse(response: ByteArray) {
            try {
                val res = decode(response)
                Log.println(Log.ASSERT, "MOBINA", res)
                if (!res.contains("Log in • Instagram") || !res.startsWith("<!DOCTYPE html>"))
                    throw Exception("NORMAL")
                if (res.contains("Log in • Instagram"))
                    handler?.obtainMessage(Crawler.HANDLE_SIGNED_OUT)?.sendToTarget()
                else handler?.obtainMessage(HANDLE_ERROR)?.sendToTarget()
            } catch (ignored: Exception) {
                Log.println(Log.ASSERT, "MOBINA", "NORMAL")
                doesErrorPersist = 0
                Panel.handler?.obtainMessage(Panel.Action.WAVE_DOWN.ordinal)?.sendToTarget()
                Transit(handler, listener, response)
            }
        }

        fun interface OnFinished {
            fun onFinished(response: ByteArray)
        }

        class Transit(handler: Handler?, val listener: OnFinished, val response: ByteArray) {
            init {
                handler?.obtainMessage(HANDLE_VOLLEY, this)?.sendToTarget()
            }
        }
    }

    enum class Type(val url: String) {
        SEARCH("https://www.instagram.com/web/search/topsearch/?context=user&query=%s"),

        FOLLOWERS("https://i.instagram.com/api/v1/friendships/%1\$s/followers/?max_id=%2\$s"),
        FOLLOWING("https://i.instagram.com/api/v1/friendships/%1\$s/following/?max_id=%2\$s"),
        FRIENDSHIPS("https://i.instagram.com/api/v1/friendships/show_many/"),

        PROFILE("https://www.instagram.com/%s/?__a=1"),
        POSTS(
            "https://www.instagram.com/graphql/query/?query_hash=$postHash" +
                    "&variables={\"id\":\"%1\$s\",\"first\":%2\$s,\"after\":\"%3\$s\"}"
        ),
        INFO("https://i.instagram.com/api/v1/users/%s/info/"),
        // Browser hover feature, takes ID, gets ~1% of what PROFILE gets
        // Updated 2022.04.29: recently (or maybe it was the case even before)...
        // INFO contains ONLY { is_private, pk, profile_pic_url, username } NOT ENOUGH
    }

    companion object {
        const val HANDLE_VOLLEY = 99
        const val HANDLE_ERROR = 98
        const val postHash = "8c2a529969ee035a5063f2fc8602a0fd"
        var doesErrorPersist = 0

        fun encode(uriString: String?): String? {
            if (uriString == null) return null
            if (TextUtils.isEmpty(uriString)) return uriString
            val allowedUrlCharacters = Pattern.compile(
                "([A-Za-z0-9_.~:/?#\\[\\]@!$&'()*+,;" + "=-]|%[0-9a-fA-F]{2})+"
            )
            val matcher = allowedUrlCharacters.matcher(uriString)
            var validUri: String? = null
            if (matcher.find()) validUri = matcher.group()
            if (TextUtils.isEmpty(validUri) || uriString.length == validUri!!.length)
                return uriString

            val uri = Uri.parse(uriString)
            val uriBuilder = Uri.Builder().scheme(uri.scheme).authority(uri.authority)
            for (path in uri.pathSegments) uriBuilder.appendPath(path)
            for (key in uri.queryParameterNames)
                uriBuilder.appendQueryParameter(key, uri.getQueryParameter(key))
            return uriBuilder.build().toString()
        }

        fun decode(data: ByteArray) = String(data)
    }
}
