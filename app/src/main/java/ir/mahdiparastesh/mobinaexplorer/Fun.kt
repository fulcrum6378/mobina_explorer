package ir.mahdiparastesh.mobinaexplorer

import android.content.Context
import androidx.core.content.ContextCompat
import java.util.*

class Fun {
    companion object {
        fun now() = Calendar.getInstance().timeInMillis

        fun color(c: Context, res: Int) = ContextCompat.getColor(c, res)
    }
}
