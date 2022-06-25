package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import ir.mahdiparastesh.mobinaexplorer.Crawler
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.IN_PLACE
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MAX_DISTANCE
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MED_DISTANCE
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MIN_DISTANCE

@Entity
class Nominee(
    @PrimaryKey var id: Long,
    var user: String,
    var name: String,
    var accs: Boolean, // Is their profile accessible (!pv or followed)?
    var step: Byte,
    var anal: Boolean, // Has their profile been analyzed?
    var fllw: Boolean, // Are their followers/following fetched?
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
        step <= IN_PLACE -> IN_PLACE
        step <= MIN_DISTANCE -> MIN_DISTANCE
        step <= MED_DISTANCE -> MED_DISTANCE
        step <= MAX_DISTANCE -> MAX_DISTANCE
        else -> null
    }

    fun maxPosts() = Crawler.maxPosts(proximity())

    fun maxFollow() = Crawler.maxFollow(proximity())

    fun maxSlides() = Crawler.maxSlides(proximity())
}
