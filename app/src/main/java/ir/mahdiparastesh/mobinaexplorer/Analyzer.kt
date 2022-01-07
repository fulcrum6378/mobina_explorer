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

    inner class Subject(bmp: Bitmap?, private val listener: OnFinished) {
        private var results: Results? = null

        constructor(bar: ByteArray, listener: OnFinished) : this(barToBmp(bar), listener)

        init {
            if (bmp == null) Transit(listener, null)
            else detector.process(InputImage.fromBitmap(bmp, 0)).addOnSuccessListener { wryFaces ->
                results = Results(wryFaces.size, listener, wryFaces)
                var ff = 0
                for (f in wryFaces) TfUtils.rotate(bmp, f.headEulerAngleZ).apply {
                    detector.process(InputImage.fromBitmap(this, 0)).addOnSuccessListener { faces ->
                        if (faces.isNullOrEmpty() || ff >= faces.size)
                            results!!.result(null)
                        else {
                            val cropped = if (faces[ff].boundingBox != f.boundingBox)
                                try {
                                    crop(this, faces[ff])
                                } catch (ignored: IllegalArgumentException) {
                                    c.openFileOutput("fuck.txt", Context.MODE_PRIVATE).apply {
                                        write(Crawler.un.encodeToByteArray()) // TODO
                                        close()
                                    }
                                    null // x + width must be <= bitmap.width()
                                } else this
                            if (cropped != null) {
                                if (results!!.cropped == null) results!!.cropped = cropped
                                results!!.result(Result(compare(cropped))) // faces[ff]
                            } else results!!.result(null)
                            ff++
                        }
                    }.addOnFailureListener { results!!.result(null) }
                }
            }.addOnFailureListener { Transit(listener, null) }
        }

        private fun crop(raw: Bitmap, f: Face): Bitmap = Bitmap.createBitmap(
            raw, f.boundingBox.left, f.boundingBox.top,
            f.boundingBox.width(), f.boundingBox.height()
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

        @Suppress("unused")
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
            if (expect == 0) Transit(listener, this)
        }

        fun result(e: Result?) {
            reality++
            if (e != null) add(e)
            if (reality == expect) Transit(listener, this)
        }

        fun anyQualified() = any { it.qualified }

        @Suppress("NestedLambdaShadowedImplicitParameter")
        fun best() = filter { it.qualified }.maxOf { it.prob[it.like] }
    }

    class Result(
        val prob: FloatArray,
        var like: Int = prob.indexOfFirst { it == max(prob.toList()) },
        var qualified: Boolean = prob[0] > CANDIDATURE // like == 0 &&
    )

    class Transit(val listener: OnFinished, val results: Results?) {
        init {
            /*if (!results.isNullOrEmpty()) Log.println(Log.DEBUG, "ANALYZER",
                "${Crawler.un} =>${Gson().toJson(Models.PLURAL.labels[results[0].like])}: " +
                    "${results[0].prob[results[0].like]}")*/
            Crawler.handler?.obtainMessage(Crawler.HANDLE_ML_KIT, this)?.sendToTarget()
        }
    }

    companion object {
        val MODEL = Models.PLURAL
        const val MODEL_SIZE = 224
        const val CANDIDATURE = 0.08f

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
