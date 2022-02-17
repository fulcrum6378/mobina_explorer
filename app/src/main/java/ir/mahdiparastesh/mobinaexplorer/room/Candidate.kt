package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import ir.mahdiparastesh.mobinaexplorer.Crawler

@Entity
class Candidate(
    @PrimaryKey(autoGenerate = false) var id: Long,
    var score: Float,
    var scope: String,
    var added: Long,
    var obscure: Boolean = false,
    var rejected: Boolean = false,
    @Ignore var nominee: Nominee? = null
) {
    @Suppress("unused")
    constructor() : this(-1, 0f, "", Crawler.now())

    companion object {
        const val IN_PROFILE = "P"
        const val IN_PROFILE_TEXT = "PT"
        const val IN_POST = "%1\$d_%2\$d"
        const val IN_POST_TEXT = "T_%1\$d"

        fun findPosInList(id: Long, list: List<Candidate>): Int {
            var i = -1
            for (can in list.indices) if (list[can].id == id) {
                i = can
                break
            }
            return i
        }
    }
}
