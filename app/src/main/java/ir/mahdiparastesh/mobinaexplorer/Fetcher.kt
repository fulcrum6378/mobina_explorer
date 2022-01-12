package ir.mahdiparastesh.mobinaexplorer

import android.net.Uri
import android.text.TextUtils
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.Volley
import java.util.regex.Pattern

class Fetcher(
    private val c: Explorer,
    url: String,
    private val listener: Listener,
    cache: Boolean = false,
    method: Int = Method.GET,
    private val body: String? = null
) : Request<ByteArray>(method, encode(url), Response.ErrorListener {
    Panel.handler?.obtainMessage(Panel.Action.WAVE_DOWN.ordinal)?.sendToTarget()

    val code = it.networkResponse.statusCode
    if (code == 404) {
        c.crawler.signal(Crawler.Signal.PAGE_NOT_FOUND, code.toString())
        Crawler.handler?.obtainMessage(Crawler.HANDLE_NOT_FOUND)?.sendToTarget()
        return@ErrorListener
    }

    if (doesErrorPersist < Crawler.maxTryAgain) {
        c.crawler.signal(Crawler.Signal.VOLLEY_ERROR, code.toString())
        Crawler.handler?.obtainMessage(Crawler.HANDLE_ERROR)?.sendToTarget()
    } else c.crawler.signal(Crawler.Signal.VOLLEY_NOT_WORKING, it.message.toString())
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

    override fun getHeaders(): HashMap<String, String> = c.crawler.headers

    override fun getBody(): ByteArray = encode(body)?.encodeToByteArray() ?: super.getBody()

    override fun deliverResponse(response: ByteArray) = listener.onResponse(response)

    override fun parseNetworkResponse(response: NetworkResponse): Response<ByteArray> =
        Response.success(response.data as ByteArray, HttpHeaderParser.parseCacheHeaders(response))

    class Listener(private val listener: OnFinished) : Response.Listener<ByteArray> {
        override fun onResponse(response: ByteArray) {
            doesErrorPersist = 0
            Panel.handler?.obtainMessage(Panel.Action.WAVE_DOWN.ordinal)?.sendToTarget()
            Transit(listener, response)
        }

        fun interface OnFinished {
            fun onFinished(response: ByteArray)
        }

        class Transit(val listener: OnFinished, val response: ByteArray) {
            init {
                Crawler.handler?.obtainMessage(Crawler.HANDLE_VOLLEY, this)?.sendToTarget()
            }
        }
    }

    enum class Type(val url: String) {
        SEARCH("https://www.instagram.com/web/search/topsearch/?context=user&query=%s"),

        FOLLOWERS("https://i.instagram.com/api/v1/friendships/%1\$s/followers/?max_id=%2\$s"),
        FOLLOWING("https://i.instagram.com/api/v1/friendships/%1\$s/following/?max_id=%2\$s"),
        FRIENDSHIPS("https://i.instagram.com/api/v1/friendships/show_many/"),

        PROFILE("https://www.instagram.com/%s"),
        POSTS(
            "https://www.instagram.com/graphql/query/?query_hash=%1\$s&variables=" +
                    "{\"id\":\"%2\$s\",\"first\":%3\$s,\"after\":\"%4\$s\"}"
        ),
    }

    companion object {
        var doesErrorPersist = 0

        fun encode(uriString: String?): String? {
            if (uriString == null) return null
            if (TextUtils.isEmpty(uriString)) return uriString
            val allowedUrlCharacters = Pattern.compile(
                "([A-Za-z0-9_.~:/?\\#\\[\\]@!$&'()*+,;" + "=-]|%[0-9a-fA-F]{2})+"
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
