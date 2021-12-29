package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Session(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @ColumnInfo(name = "start") var start: Long,
    @ColumnInfo(name = "end") var end: Long,
    @ColumnInfo(name = "bytes") var bytes: Long
)
