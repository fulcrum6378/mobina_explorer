package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Candidate(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "score") var score: Float,
    @ColumnInfo(name = "photo_type") var photoType: String,
) {
    companion object {
        const val TYPE_PROFILE = "P"
        const val TYPE_POST = "%1\$s_%2\$s"
    }
}
