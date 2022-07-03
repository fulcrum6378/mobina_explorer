package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import ir.mahdiparastesh.mobinaexplorer.Crawler

@Entity
class Session(
    var start: Long,
    var end: Long,
    var bytes: Long,
    var nominees: Long
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0L

    constructor() : this(Crawler.now(), 0L, 0L, 0L)
}
