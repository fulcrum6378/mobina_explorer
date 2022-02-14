package ir.mahdiparastesh.mobinaexplorer.room

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.*
import androidx.room.Database
import ir.mahdiparastesh.mobinaexplorer.Explorer
import java.io.File

@Database(
    entities = [Nominee::class, Candidate::class, Session::class],
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
        fun allNominees(): List<Nominee>

        @Query("SELECT * FROM Nominee WHERE anal = 0 OR fllw = 0")
        fun nominees(): List<Nominee>

        @Query("SELECT * FROM Nominee WHERE anal = 0")
        fun nomineesNf(): List<Nominee>

        @Query("SELECT * FROM Nominee WHERE anal = 0 AND accs = 0")
        fun nomineesPv(): List<Nominee>

        @Query("SELECT * FROM Nominee WHERE anal = 1")
        fun anNominees(): List<Nominee>

        @Query("SELECT * FROM nominee WHERE user LIKE :user LIMIT 1")
        fun nominee(user: String): Nominee

        @Query("SELECT * FROM nominee WHERE id LIKE :id LIMIT 1")
        fun nomineeById(id: Long): Nominee

        @Insert(onConflict = OnConflictStrategy.ABORT)
        fun addNominee(item: Nominee)

        @Update
        fun updateNominee(item: Nominee)

        @Query("DELETE FROM Nominee WHERE id LIKE :id")
        fun deleteNominee(id: Long)


        @Query("SELECT * FROM Candidate")
        fun candidates(): List<Candidate>

        @Query("SELECT * FROM Candidate WHERE rejected = 0")
        fun nrCandidates(): List<Candidate>

        @Query("SELECT * FROM Candidate WHERE rejected = 1")
        fun orCandidates(): List<Candidate>

        @Query("SELECT * FROM candidate WHERE id LIKE :id LIMIT 1")
        fun candidate(id: Long): Candidate

        @Insert(onConflict = OnConflictStrategy.ABORT)
        fun addCandidate(item: Candidate)

        @Update
        fun updateCandidate(item: Candidate)

        @Query("DELETE FROM Candidate WHERE id LIKE :id")
        fun deleteCandidate(id: Long) // throws nothing
    }

    @SuppressLint("SdCardPath")
    class DbFile(which: Triple) : File(
        "/data/data/" + Explorer::class.java.`package`!!.name + "/databases/" + DATABASE + which.s
    ) {
        companion object {
            const val DATABASE = "primary.db"

            fun build(c: Context, mainThread: Boolean = false) = Room.databaseBuilder(
                c, ir.mahdiparastesh.mobinaexplorer.room.Database::class.java, DATABASE
            ).apply { if (mainThread) allowMainThreadQueries() }.build()
        }

        @Suppress("unused")
        enum class Triple(val s: String) {
            MAIN(""), SHARED_MEMORY("-shm"), WRITE_AHEAD_LOG("-wal")
        }
    }
}
