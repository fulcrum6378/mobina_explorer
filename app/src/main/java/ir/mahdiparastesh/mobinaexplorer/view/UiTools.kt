package ir.mahdiparastesh.mobinaexplorer.view

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.core.content.ContextCompat
import ir.mahdiparastesh.mobinaexplorer.R

class UiTools {
    companion object {
        fun color(c: Context, res: Int) = ContextCompat.getColor(c, res)

        @Suppress("DEPRECATION")
        fun shake(c: Context, dur: Long = 55L) {
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                (c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            else c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                .vibrate(VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        fun vis(v: View, bb: Boolean = true) {
            v.visibility = if (bb) View.VISIBLE else View.GONE
        }

        fun bytes(c: Context, l: Long): String {
            val units = c.resources.getStringArray(R.array.bytes)
            var gig = 0L
            var meg = 0L
            var kil = 0L
            var ll = l
            if (ll >= 1073741824L) {
                gig = ll / 1073741824L
                ll %= 1073741824L
            }
            if (ll >= 1048576L) {
                meg = ll / 1048576L
                ll %= 1048576L
            }
            if (ll >= 1024L) {
                kil = ll / 1024L
                ll %= 1024L
            }
            return StringBuilder().apply {
                if (gig > 0) append("$gig ${units[3]}, ")
                if (meg > 0) append("$meg ${units[2]}, ")
                if (kil > 0) append("$kil ${units[1]}, ")
                append("$ll ${units[0]}")
            }.toString()
        }
    }
}
