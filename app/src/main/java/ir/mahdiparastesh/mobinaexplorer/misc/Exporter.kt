package ir.mahdiparastesh.mobinaexplorer.misc

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import ir.mahdiparastesh.mobinaexplorer.R
import ir.mahdiparastesh.mobinaexplorer.room.Database.DbFile
import java.io.FileInputStream
import java.io.FileOutputStream

class Exporter(that: AppCompatActivity) {
    private var launcher: ActivityResultLauncher<Intent> =
        that.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val bExp = try {
                that.contentResolver.openFileDescriptor(it.data!!.data!!, "w")?.use { des ->
                    var db: ByteArray?
                    FileInputStream(DbFile(DbFile.Triple.MAIN)).use { fis ->
                        db = fis.readBytes()
                    }
                    FileOutputStream(des.fileDescriptor).use { fos ->
                        fos.write(db)
                    }
                }
                true
            } catch (ignored: Exception) {
                false
            }
            Toast.makeText(
                that, if (bExp) R.string.exportDone else R.string.exportUndone, Toast.LENGTH_LONG
            ).show()
        }

    fun launch() {
        launcher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/x-sqlite3"
            putExtra(Intent.EXTRA_TITLE, "Mobina Explorer.db")
        })
    }
}
