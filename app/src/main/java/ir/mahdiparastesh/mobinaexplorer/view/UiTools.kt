package ir.mahdiparastesh.mobinaexplorer.view

import android.content.Context
import androidx.core.content.ContextCompat

class UiTools {
    companion object {
        fun color(c: Context, res: Int) = ContextCompat.getColor(c, res)
    }
}
