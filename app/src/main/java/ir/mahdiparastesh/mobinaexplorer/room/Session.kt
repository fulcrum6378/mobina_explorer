package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Session(
    @PrimaryKey(autoGenerate = true) var id: Long,
    var start: Long,
    var end: Long,
    var bytes: Long,
    var nominees: Long
)
