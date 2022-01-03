package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Suppress("SpellCheckingInspection")
@Entity
class Nominee(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "user") var user: String,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "priv") var priv: Boolean,
    @ColumnInfo(name = "step") var step: Byte,
    @ColumnInfo(name = "anal") var anal: Boolean,
) {
    constructor() : this(-1, "", "", false, -1, false)
}
