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
        c.resources.assets.openFd("mobina.tflite").apply {
            model = FileInputStream(fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            close()
        }
    }

    inner class Subject(bmp: Bitmap, private val finished: OnFinished) {
        private var results: Results? = null

        constructor(bar: ByteArray, finished: OnFinished) : this(barToBmp(bar), finished)

        init {
            detector.process(InputImage.fromBitmap(bmp, 0)).addOnSuccessListener { wryFaces ->
                results = Results(wryFaces.size, finished, wryFaces)
                var ff = 0
                for (f in wryFaces.indices) TfUtils.rotate(bmp, wryFaces[f].headEulerAngleZ).apply {
                    detector.process(InputImage.fromBitmap(this, 0)).addOnSuccessListener { faces ->
                        if (faces.isNullOrEmpty() || ff >= faces.size)
                            results!!.result(null)
                        else {
                            results!!.cropped = crop(this, faces[ff])
                            results!!.result(Result(faces[ff], compare(results!!.cropped!!)))
                            ff++
                        }
                    }.addOnFailureListener { results!!.result(null) }
                }
            }.addOnFailureListener { finished.onFinished(results) }
        }

        private fun crop(raw: Bitmap, f: Face): Bitmap = Bitmap.createBitmap(
            raw, f.boundingBox.left, f.boundingBox.top,
            f.boundingBox.bottom - f.boundingBox.top,
            f.boundingBox.right - f.boundingBox.left
        )

        private fun compare(cropped: Bitmap): FloatArray {
            val input = arrayOf(TfUtils.tensor(cropped))
            val output = Array(input.size) { FloatArray(2) }
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
        var maxima = -1f
        var qualified = false
        var cropped: Bitmap? = null

        init {
            if (expect == 0) end()
        }

        fun result(e: Result?) {
            if (maxima != -1f) return
            reality++
            if (e != null) add(e)
            if (reality == expect) end()
        }

        private fun end() {
            listener.onFinished(this)
            if (size > 0) {
                maxima = maxOf { it.score[1] }
                qualified = maxima > CANDIDATURE
            }
        }
    }

    class Result(val face: Face, val score: FloatArray)

    companion object {
        const val CANDIDATURE = 0.9f

        fun barToBmp(bar: ByteArray): Bitmap =
            BitmapFactory.decodeByteArray(bar, 0, bar.size)
    }
}
