package ir.mahdiparastesh.mobinaexplorer.misc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

class TfUtils {
    companion object {
        private const val MODEL_W = 800
        private const val MODEL_H = 800

        fun tensor(bmp: Bitmap): Array<Array<FloatArray>> {
            val scaled = Bitmap.createScaledBitmap(bmp, MODEL_W, MODEL_H, true)
            val data = Array(MODEL_W) { Array(MODEL_H) { FloatArray(3) } }
            for (y in 0 until MODEL_W) for (x in 0 until MODEL_H) {
                val c = Color.valueOf(scaled.getPixel(x, y))
                data[y][x][0] = c.red()
                data[y][x][1] = c.green()
                data[y][x][2] = c.blue()
            }
            return data
        }

        fun rotate(bmp: Bitmap, deg: Float): Bitmap = Bitmap.createBitmap(
            bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, false
        )

        fun save(c: Context, bmp: Bitmap, fName: String) {
            c.openFileOutput("$fName.jpg", Context.MODE_PRIVATE).apply {
                write(ByteArrayOutputStream().apply {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, this)
                }.toByteArray())
                bmp.recycle()
                close()
            }
        }
    }
}
