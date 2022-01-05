package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
class Candidate(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "score") var score: Float,
    @ColumnInfo(name = "where") var where: String,
    @ColumnInfo(name = "rejected") var rejected: Boolean = false,
    @Ignore var nominee: Nominee? = null
) {
    constructor() : this(-1, 0f, "")

    companion object {
        const val IN_PROFILE = "P"
        const val IN_PROFILE_TEXT = "PT"
        const val IN_POST = "%1\$s_%2\$s"
        const val IN_POST_TEXT = "T_%1\$s_%2\$s"
    }
}
