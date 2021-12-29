package ir.mahdiparastesh.mobinaexplorer.room

import android.annotation.SuppressLint
import ir.mahdiparastesh.mobinaexplorer.Explorer
import java.io.File

@SuppressLint("SdCardPath")
class DbFile(which: Triple) : File(
    "/data/data/" + Explorer::class.java.`package`!!.name + "/databases/" + DATABASE + which.s
) {
    companion object {
        const val DATABASE = "primary.db"
    }

    enum class Triple(val s: String) {
        MAIN(""), SHARED_MEMORY("-shm"), WRITE_AHEAD_LOG("-wal")
    }
}
