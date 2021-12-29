package ir.mahdiparastesh.mobinaexplorer.room

import androidx.room.*
import androidx.room.Database

@Database(
    entities = [Nominee::class, Session::class],
    version = 1, exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun dao(): DAO

    @Dao
    interface DAO {
        @Query("SELECT * FROM Session")
        fun sessions(): List<Session>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun addSession(item: Session): Long


        @Query("SELECT * FROM Nominee")
        fun nominees(): List<Nominee>

        @Query("SELECT * FROM nominee WHERE user LIKE :user LIMIT 1")
        fun nominee(user: String): Nominee

        @Query("SELECT * FROM nominee WHERE id LIKE :id LIMIT 1")
        fun nomineeById(id: Long): Nominee

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun addNominee(item: Nominee)
    }
}
