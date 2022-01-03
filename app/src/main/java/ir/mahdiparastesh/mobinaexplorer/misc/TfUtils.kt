package ir.mahdiparastesh.mobinaexplorer.misc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

class TfUtils {
    companion object {
        private const val MODEL_SIZE = 224

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
