package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ir.mahdiparastesh.mobinaexplorer.Crawler
import ir.mahdiparastesh.mobinaexplorer.Crawler.Proximity
import ir.mahdiparastesh.mobinaexplorer.Crawler.Proximity.*

@Suppress("SpellCheckingInspection")
@Entity
class Nominee(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "user") var user: String,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "accs") var accs: Boolean,
    @ColumnInfo(name = "step") var step: Byte,
    @ColumnInfo(name = "anal") var anal: Boolean,
    @ColumnInfo(name = "fllw") var fllw: Boolean,
) {
    @Suppress("unused")
    constructor() : this(-1, "", "", false, -1, false, false)

    fun proximity(): Proximity = when {
        step <= MIN_PROXIMITY.max!! -> MIN_PROXIMITY
        step <= MED_PROXIMITY.max!! -> MED_PROXIMITY
        step <= MAX_PROXIMITY.max!! -> MAX_PROXIMITY
        else -> OUT_OF_REACH
    }

    fun maxPosts() =
        if (proximity().max != null) Crawler.MAX_POSTS_FACTOR / proximity().max!! else 0
}
