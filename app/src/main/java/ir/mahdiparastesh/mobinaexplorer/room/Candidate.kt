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
    @Suppress("unused")
    constructor() : this(-1, 0f, "")

    companion object {
        const val IN_PROFILE = "P"
        const val IN_PROFILE_TEXT = "PT"
        const val IN_POST = "%1\$s_%2\$s"
        const val IN_POST_TEXT = "T_%1\$s_%2\$s"
    }

    class Sort(private val by: Int) : Comparator<Candidate>{
        companion object {
            const val BY_SCORE = 0
            const val BY_REJECTED = 1
            const val BY_NOM_NAME = 2
            const val BY_NOM_USER = 3
        }

        override fun compare(a: Candidate, b: Candidate) = when (by) {
            BY_REJECTED -> comBool(a.rejected) - comBool(b.rejected)
            BY_NOM_NAME -> a.nominee!!.name.compareTo(b.nominee!!.name)
            BY_NOM_USER -> a.nominee!!.user.compareTo(b.nominee!!.user)
            else -> (a.score - b.score).toInt()
        }

        private fun comBool(b: Boolean): Int = if (b) 1 else 0
    }
}
