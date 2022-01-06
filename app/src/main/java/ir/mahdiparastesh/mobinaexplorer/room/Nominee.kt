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
    @ColumnInfo(name = "accs") var accs: Boolean, // Is their profile accessible?
    @ColumnInfo(name = "step") var step: Byte,
    @ColumnInfo(name = "anal") var anal: Boolean, // Has their profile been analyzed?
    @ColumnInfo(name = "fllw") var fllw: Boolean, // Are their followers/following fetched?
) {
    @Suppress("unused")
    constructor() : this(-1, "", "", false, -1, false, false)

    fun proximity(): Proximity = when {
        step <= MAX_PROXIMITY.max!! -> MAX_PROXIMITY
        step <= MED_PROXIMITY.max!! -> MED_PROXIMITY
        step <= MIN_PROXIMITY.max!! -> MIN_PROXIMITY
        else -> OUT_OF_REACH
    }

    fun maxPosts() = Crawler.maxPosts(proximity())

    fun maxFollow() = Crawler.maxFollow(proximity())
}
