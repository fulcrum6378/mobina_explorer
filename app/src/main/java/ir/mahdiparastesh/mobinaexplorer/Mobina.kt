package ir.mahdiparastesh.mobinaexplorer

import android.content.Context
import android.graphics.Bitmap

@Suppress("unused")
class Mobina(private val c: Context) {

    val first = photo(1)

    val second = photo(2)

    val third = photo(3)

    val fourth = photo(4) // NO FACES

    private fun photo(n: Int): Bitmap {
        var data: ByteArray
        c.resources.assets.open("$n.jfif").apply {
            data = readBytes()
            close()
        }
        return Analyzer.barToBmp(data)
    }
}
