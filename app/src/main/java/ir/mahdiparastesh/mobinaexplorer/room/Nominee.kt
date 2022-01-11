package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ir.mahdiparastesh.mobinaexplorer.Crawler
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MAX_PROXIMITY
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MED_PROXIMITY
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MIN_PROXIMITY

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

    fun analyzed(): Nominee {
        anal = true
        return this
    }

    fun followed(): Nominee {
        fllw = true
        return this
    }


    fun proximity(): Byte? = when {
        step <= MIN_PROXIMITY -> MIN_PROXIMITY
        step <= MED_PROXIMITY -> MED_PROXIMITY
        step <= MAX_PROXIMITY -> MAX_PROXIMITY
        else -> null
    }

    fun maxPosts() = Crawler.maxPosts(proximity())

    fun maxFollow() = Crawler.maxFollow(proximity())

    fun maxSlides() = Crawler.maxSlides(proximity())
}
