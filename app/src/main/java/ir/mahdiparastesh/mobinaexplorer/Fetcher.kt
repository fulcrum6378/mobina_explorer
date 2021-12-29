package ir.mahdiparastesh.mobinaexplorer

import android.net.Uri
import android.os.CountDownTimer
import android.text.TextUtils
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.InputStreamReader
import java.util.regex.Pattern

class Fetcher(private val c: Explorer, url: String, finish: Response.Listener<String>) :
    StringRequest(Method.GET, encode(url), OnFinished(finish), OnError()) {

    init {
        setShouldCache(false)
        tag = "fetch"
        retryPolicy = DefaultRetryPolicy(
            20 * 1000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        Volley.newRequestQueue(c).add(this)
    }

    override fun getHeaders(): HashMap<String, String> = Gson().fromJson(
        JsonReader(InputStreamReader(c.resources.assets.open("headers.json"))), HashMap::class.java
    )

    companion object {
        const val HUMAN_DELAY = 10000L

        fun encode(uriString: String): String {
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
    }

    abstract class Delayer : CountDownTimer(HUMAN_DELAY, HUMAN_DELAY) {
        override fun onTick(millisUntilFinished: Long) {}
        abstract override fun onFinish()
    }

    class OnFinished(private val listener: Response.Listener<String>) : Response.Listener<String> {
        override fun onResponse(response: String?) {
            Panel.handler?.obtainMessage(Panel.Handle.BYTES.ordinal)?.sendToTarget()
            listener.onResponse(response)
        }
    }

    class OnError : Response.ErrorListener {
        override fun onErrorResponse(error: VolleyError?) {
            // TODO: "${error?.javaClass?.name}: ${error?.message}"
        }
    }

    enum class Type(val url: String) {
        // &rank_token=0.8502732572695122
        SEARCH("https://www.instagram.com/web/search/topsearch/?context=user&query=%s"),

        // ?count=12&search_surface=follow_list_page
        FOLLOWERS("https://i.instagram.com/api/v1/friendships/%1\$s/followers/?max_id=%2\$s"),

        // ?count=12
        FOLLOWING("https://i.instagram.com/api/v1/friendships/%1\$s/following/?max_id=%2\$s"),

        PROFILE("https://www.instagram.com/%s"),

        POSTS(
            "https://www.instagram.com/graphql/query/?query_hash=%1\$s&variables=" +
                    "{\"id\":\"%2\$s\",\"first\":%3\$s,\"after\":\"%4\$s\"}"
        ),

        LIBRARY("https://www.instagram.com/static/bundles/es6/ConsumerLibCommons.js/%s.js"),
    }
}
