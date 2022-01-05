package ir.mahdiparastesh.mobinaexplorer.misc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.widget.ImageView
import android.widget.TextView
import com.google.gson.GsonBuilder
import ir.mahdiparastesh.mobinaexplorer.Analyzer
import ir.mahdiparastesh.mobinaexplorer.Analyzer.Companion.MODEL_SIZE
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@Suppress("unused")
class TfUtils {
    companion object {
        fun tensor(rawBmp: Bitmap): Array<Array<FloatArray>> {
            val bmp = Bitmap.createScaledBitmap(rawBmp, MODEL_SIZE, MODEL_SIZE, true)
            val data = Array(MODEL_SIZE) { Array(MODEL_SIZE) { FloatArray(3) } }
            for (y in 0 until MODEL_SIZE) for (x in 0 until MODEL_SIZE)
                Color.valueOf(bmp.getPixel(x, y)).apply {
                    data[y][x][0] = red()
                    data[y][x][1] = green()
                    data[y][x][2] = blue()
                }
            return data
        }

        fun rotate(bmp: Bitmap, deg: Float): Bitmap = Bitmap.createBitmap(
            bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, false
        )

        fun preTrain(c: Context) { // Put in a separate thread
            val an = Analyzer(c)
            File(c.filesDir, "output").apply { if (!exists()) mkdir() }
            c.resources.assets.list("input")?.map { crush ->
                var ci = 0
                File(c.filesDir, "output/$crush").apply { if (!exists()) mkdir() }
                c.resources.assets.list("input/$crush")?.map { photo ->
                    var data: ByteArray?
                    c.resources.assets.open("input/$crush/$photo").apply {
                        data = readBytes()
                        close()
                    }
                    val bmp = Analyzer.barToBmp(data) ?: return
                    an.Subject(bmp) { res ->
                        if (res?.cropped != null) {
                            save(c, res.cropped!!, "output/$crush/$ci.jpg")
                            ci++
                        }
                    }
                }
            }
            //an.Subject(Mobina(c).first) { save(c, it, "1.jfif") }
        }

        private fun save(c: Context, bmp: Bitmap, path: String) {
            FileOutputStream(File(c.filesDir, path)).apply {
                write(ByteArrayOutputStream().apply {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, this)
                }.toByteArray())
                close()
            }
            bmp.recycle()
        }

        fun test(c: Context, iv: ImageView, tv: TextView, file: String = "1.jfif") {
            val an = Analyzer(c)
            var data: ByteArray
            c.resources.assets.open(file).apply {
                data = readBytes()
                close()
            }
            val bmp = Analyzer.barToBmp(data)
            an.Subject(bmp) {
                iv.setImageBitmap(it?.cropped)
                if (it == null) return@Subject
                val sb = StringBuilder()
                for (r in it) sb.append(GsonBuilder().setPrettyPrinting().create().toJson(
                    HashMap<String, Float>().apply {
                        r.prob.forEachIndexed { i, fl ->
                            this[Analyzer.MODEL.labels[i].replaceFirstChar(Char::titlecase)] = fl
                        }
                    }.toList().sortedBy { (_, fl) -> fl }.toMap()
                )
                ).append("\n")
                tv.text = sb.toString()
            }
        }
    }
}
