package ir.mahdiparastesh.mobinaexplorer.misc

import android.content.Context
import android.graphics.Bitmap
import ir.mahdiparastesh.mobinaexplorer.Analyzer

@Suppress("unused")
class Mobina(private val c: Context) {

    val first = photo(1)

    val second = photo(2)

    val third = photo(3)

    val fourth = photo(4) // NO FACES

    val fifth = photo(5)

    val sixth = photo(6)

    val seventh = photo(7)

    val eighth = photo(8)

    val ninth = photo(9)

    val tenth = photo(10)

    val eleventh = photo(11) // NO FACES

    val twelfth = photo(12)

    val thirteenth = photo(13)

    private fun photo(n: Int): Bitmap {
        var data: ByteArray
        c.resources.assets.open("mobina/$n.jpg").apply {
            data = readBytes()
            close()
        }
        return Analyzer.barToBmp(data)!!
    }
}
