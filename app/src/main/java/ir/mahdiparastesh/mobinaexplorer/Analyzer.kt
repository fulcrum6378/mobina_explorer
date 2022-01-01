package ir.mahdiparastesh.mobinaexplorer

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
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    fun analyze(img: InputImage, listener: OnFinished) = detector.process(img)
        .addOnSuccessListener { faces -> listener.onFinished(faces) }
        .addOnFailureListener { listener.onFinished(null) }

    fun show(faces: List<Face>, cl: ConstraintLayout, w: Int, h: Int) {
        cl.removeAllViews()
        for (obj in faces) {
            val bound = obj.boundingBox
            val width = relative(abs(bound.left - bound.right), w, cl.width).toInt()
            val height = relative(abs(bound.top - bound.bottom), h, cl.height).toInt()
            val transX = relative(bound.left, w, cl.width)
            val transY = relative(bound.top, h, cl.height)

            cl.addView(View(cl.context).apply {
                layoutParams = ConstraintLayout.LayoutParams(width, height).apply {
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                }
                this.id = id
                translationX = transX
                translationY = transY
                background = ContextCompat.getDrawable(cl.context, R.drawable.detected)
                setOnClickListener { }
            })
        }
    }

    private fun relative(num: Int, older: Int, newer: Int) =
        (newer.toFloat() / older.toFloat()) * num.toFloat()

    fun interface OnFinished {
        fun onFinished(faces: MutableList<Face>?)
    }
}
