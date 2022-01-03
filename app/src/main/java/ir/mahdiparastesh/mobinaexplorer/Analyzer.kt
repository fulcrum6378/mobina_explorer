package ir.mahdiparastesh.mobinaexplorer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

class Analyzer {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // PERFORMANCE_MODE_FAST
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    fun analyze(bar: ByteArray, listener: OnFinished) =
        analyze(barToBmp(bar), listener)

    fun analyze(bmp: Bitmap, listener: OnFinished) =
        doAnalyze(InputImage.fromBitmap(bmp, 0), listener)

    private fun doAnalyze(img: InputImage, listener: OnFinished) = detector.process(img)
        .addOnSuccessListener { faces -> listener.onFinished(faces) }
        .addOnFailureListener { listener.onFinished(null) }

    companion object {
        fun barToBmp(bar: ByteArray): Bitmap =
            BitmapFactory.decodeByteArray(bar, 0, bar.size)

        fun show(faces: List<Face>, cl: ConstraintLayout, w: Int, h: Int) {
            cl.removeAllViews()
            for (obj in faces) cl.addView(View(cl.context).apply {
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
        fun onFinished(faces: MutableList<Face>?)
    }
}
