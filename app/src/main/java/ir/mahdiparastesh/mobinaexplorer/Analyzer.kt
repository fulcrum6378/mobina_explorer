package ir.mahdiparastesh.mobinaexplorer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import ir.mahdiparastesh.mobinaexplorer.misc.TfUtils
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Collections.max
import kotlin.math.abs

class Analyzer(val c: Context) {
    var model: ByteBuffer
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    init {
        c.resources.assets.openFd(MODEL.file).apply {
            model = FileInputStream(fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            close()
        }
    }

    inner class Subject(bmp: Bitmap?, private val finished: OnFinished) {
        private var results: Results? = null

        constructor(bar: ByteArray, finished: OnFinished) : this(barToBmp(bar), finished)

        init {
            if (bmp == null) finished.onFinished(null)
            else detector.process(InputImage.fromBitmap(bmp, 0)).addOnSuccessListener { wryFaces ->
                results = Results(wryFaces.size, finished, wryFaces)
                var ff = 0
                for (f in wryFaces) TfUtils.rotate(bmp, f.headEulerAngleZ).apply {
                    detector.process(InputImage.fromBitmap(this, 0)).addOnSuccessListener { faces ->
                        if (faces.isNullOrEmpty() || ff >= faces.size)
                            results!!.result(null)
                        else {
                            val cropped = if (faces[ff].boundingBox != f.boundingBox)
                                crop(this, faces[ff]) else this
                            if (results!!.cropped == null) results!!.cropped = cropped
                            results!!.result(Result(compare(cropped))) // faces[ff]
                            ff++
                        }
                    }.addOnFailureListener { results!!.result(null) }
                }
            }.addOnFailureListener { finished.onFinished(null) }
        }

        private fun crop(raw: Bitmap, f: Face): Bitmap = Bitmap.createBitmap(
            raw, f.boundingBox.left, f.boundingBox.top,
            f.boundingBox.bottom - f.boundingBox.top,
            f.boundingBox.right - f.boundingBox.left
        )

        private fun compare(cropped: Bitmap): FloatArray {
            val input = arrayOf(TfUtils.tensor(cropped))
            val output = Array(input.size) { FloatArray(MODEL.labels.size) }
            Interpreter(model).use {
                it.run(input, output)
                it.close()
            }
            return output[0]
        }

        fun show(cl: ConstraintLayout, w: Int, h: Int) {
            if (results == null) return
            cl.removeAllViews()
            for (obj in results!!.wryFaces) cl.addView(View(cl.context).apply {
                val bound = obj.boundingBox
                layoutParams = ConstraintLayout.LayoutParams(
                    relative(abs(bound.left - bound.right), w, cl.width).toInt(),
                    relative(abs(bound.top - bound.bottom), h, cl.height).toInt()
                ).apply {
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                }
                this.id = id
                translationX = relative(bound.left, w, cl.width)
                translationY = relative(bound.top, h, cl.height)
                background = ContextCompat.getDrawable(cl.context, R.drawable.detected)
                setOnClickListener { }
            })
        }

        private fun relative(num: Int, older: Int, newer: Int) =
            (newer.toFloat() / older.toFloat()) * num.toFloat()
    }

    fun interface OnFinished {
        fun onFinished(results: Results?)
    }

    class Results(
        private val expect: Int, private val listener: OnFinished, var wryFaces: List<Face>
    ) : ArrayList<Result>() {
        private var reality = 0
        var cropped: Bitmap? = null

        init {
            if (expect == 0) listener.onFinished(this)
        }

        fun result(e: Result?) {
            reality++
            if (e != null) add(e)
            if (reality == expect) listener.onFinished(this)
        }

        fun anyQualified() = any { it.qualified }

        @Suppress("NestedLambdaShadowedImplicitParameter")
        fun best() = filter { it.qualified }.maxOf { it.prob[it.like] }
    }

    class Result(
        val prob: FloatArray,
        var like: Int = prob.indexOfFirst { it == max(prob.toList()) },
        var qualified: Boolean = like == 0 && prob[0] > CANDIDATURE
    )

    companion object {
        val MODEL = Models.PLURAL
        const val MODEL_SIZE = 224
        const val CANDIDATURE = 0.5f

        fun barToBmp(bar: ByteArray?): Bitmap? =
            if (bar != null) BitmapFactory.decodeByteArray(bar, 0, bar.size) else null
    }

    @Suppress("SpellCheckingInspection", "unused")
    enum class Models(val file: String, val labels: Array<String>) {
        PLURAL(
            "mobina.tflite", arrayOf(
                "Mobina", "aimi", "amin", "amir", "amirali", "ava", "dad", "diana", "dominik",
                "elisany", "elizabeth", "emily_feld", "hannah", "hasani", "hssp", "jun", "mahdi",
                "maryam", "mina_vahid", "mohaddeseh", "nana", "natasha", "olivier", "pham",
                "queeny", "sara", "sarah", "vivian", "william"
            )
        )
    }
}
