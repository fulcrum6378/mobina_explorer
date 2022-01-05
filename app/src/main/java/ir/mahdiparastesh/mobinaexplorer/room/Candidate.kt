package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
class Candidate(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "score") var score: Float,
    @ColumnInfo(name = "photo_type") var photoType: String,
    @Ignore var nominee: Nominee? = null
) {
    constructor() : this(-1, 0f, "")

    companion object {
        const val TYPE_PROFILE = "P"
        const val TYPE_POST = "%1\$s_%2\$s"
    }
}
