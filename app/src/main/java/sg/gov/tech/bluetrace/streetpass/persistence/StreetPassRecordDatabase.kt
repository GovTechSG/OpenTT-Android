package sg.gov.tech.bluetrace.streetpass.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteDao
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteRecord
import sg.gov.tech.bluetrace.logging.persistence.LogRecord
import sg.gov.tech.bluetrace.logging.persistence.LogRecordDao
import sg.gov.tech.bluetrace.status.persistence.StatusRecord
import sg.gov.tech.bluetrace.status.persistence.StatusRecordDao


@Database(
    entities = [StreetPassRecord::class, StatusRecord::class, SafeEntryRecord::class, StreetPassLiteRecord::class,
        FavouriteRecord::class, FamilyMembersRecord::class, LogRecord::class],
    version = 10,
    exportSchema = true
)
abstract class StreetPassRecordDatabase : RoomDatabase() {

    abstract fun recordDao(): StreetPassRecordDao
    abstract fun statusDao(): StatusRecordDao
    abstract fun safeEntryDao(): SafeEntryDao
    abstract fun bleRecordDao(): StreetPassLiteDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun familyMemberDao(): FamilyMembersDao
    abstract fun logRecordDao(): LogRecordDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: StreetPassRecordDatabase? = null

        /* TEST_MODE creates a temporary database that does not touch our main database in the app
         and flushes that database when all test cases are completed.
         So there is no need to handle temporary database and delete them after test cases are finished running.*/
        var TEST_MODE = false

        fun getDatabase(context: Context): StreetPassRecordDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                //SQLCipher  //Temporary comment off first
//                val userEnteredPassphrase = "temp_pass_phrase".toCharArray()
//                val passphrase: ByteArray = SQLiteDatabase.getBytes(userEnteredPassphrase)
//                val factory = SupportFactory(passphrase , null, false)
//                val instance  = Room.databaseBuilder(context, StreetPassRecordDatabase::class.java, "record_database")
//                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
//                    .openHelperFactory(factory)
//                    .build()
//                INSTANCE = instance

                val instance = if (TEST_MODE) {
                    Room.inMemoryDatabaseBuilder(context, StreetPassRecordDatabase::class.java)
                        .allowMainThreadQueries()
                        .build()
                } else {
                    Room.databaseBuilder(
                        context,
                        StreetPassRecordDatabase::class.java,
                        "record_database"
                    )
                        .addMigrations(MIGRATION_1_2)
                        .addMigrations(MIGRATION_2_3)
                        .addMigrations(MIGRATION_3_4)
                        .addMigrations(MIGRATION_4_5)
                        .addMigrations(MIGRATION_5_6)
                        .addMigrations(MIGRATION_6_7)
                        .addMigrations(MIGRATION_7_8)
                        .addMigrations(MIGRATION_8_9)
                        .addMigrations(MIGRATION_9_10)
                        .build()
                }

                INSTANCE = instance
                return instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //adding of status table
                database.execSQL("CREATE TABLE IF NOT EXISTS `status_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `msg` TEXT NOT NULL)")

                if (!isFieldExist(database, "record_table", "v")) {
                    database.execSQL("ALTER TABLE `record_table` ADD COLUMN `v` INTEGER NOT NULL DEFAULT 1")
                }

                if (!isFieldExist(database, "record_table", "org")) {
                    database.execSQL("ALTER TABLE `record_table` ADD COLUMN `org` TEXT NOT NULL DEFAULT 'SG_MOH'")
                }

            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //adding of status table
                database.execSQL("CREATE TABLE IF NOT EXISTS `safe_entry_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `venueName` TEXT NOT NULL, `venueId` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `postalCode` TEXT NOT NULL, `address` TEXT NOT NULL, `checkInTimeMS` INTEGER NOT NULL, `checkOutTimeMS` INTEGER NOT NULL DEFAULT 0, `checkedOut` INTEGER NOT NULL DEFAULT 0)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //adding of status table
                database.execSQL("ALTER TABLE `safe_entry_table` ADD COLUMN `tenantName` TEXT NOT NULL DEFAULT \"\"")

            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //adding of btl record table
                database.execSQL("CREATE TABLE IF NOT EXISTS `btl_record_table` (`id`  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,`msg` TEXT NOT NULL, `rssi` INTEGER NOT NULL, `txPower` INTEGER, `timestamp` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //adding of favourite table
                database.execSQL("CREATE TABLE IF NOT EXISTS `favourite_table` (`venueId` TEXT NOT NULL, `venueName` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `tenantName` TEXT NOT NULL, `address` TEXT NOT NULL, PRIMARY KEY('venueId','tenantId'))")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //adding of 'postalCode' column to favourite table
                if (!isFieldExist(database, "favourite_table", "postalCode")) {
                    database.execSQL("ALTER TABLE `favourite_table` ADD COLUMN `postalCode` TEXT NOT NULL DEFAULT \"\"")
                }
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //adding of family_members_table table
                database.execSQL("CREATE TABLE IF NOT EXISTS  `family_members_table` (`id`  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,`nric` TEXT NOT NULL, `nickName` TEXT NOT NULL)")
                //adding of 'groupMembers' column to SafeEntry table
                if (!isFieldExist(database, "safe_entry_table", "groupMembers")) {
                    database.execSQL("ALTER TABLE `safe_entry_table` ADD COLUMN `groupMembers` TEXT DEFAULT NULL")
                }
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //adding of 'groupMembers count' column to SafeEntry table
                if (!isFieldExist(database, "safe_entry_table", "groupMembersCount")) {
                    database.execSQL("ALTER TABLE `safe_entry_table` ADD COLUMN `groupMembersCount` INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //adding of log_table table
                database.execSQL("CREATE TABLE IF NOT EXISTS `log_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `time` INTEGER NOT NULL, `level` TEXT NOT NULL, `type` TEXT NOT NULL, `tag` TEXT NOT NULL, `message` TEXT NOT NULL, `metaData` TEXT DEFAULT NULL)")
            }
        }

        // This method will check if column exists in your table
        fun isFieldExist(db: SupportSQLiteDatabase, tableName: String, fieldName: String): Boolean {
            var isExist = false
            val res =
                db.query("PRAGMA table_info($tableName)", null)
            res.moveToFirst()
            do {
                val currentColumn = res.getString(1)
                if (currentColumn == fieldName) {
                    isExist = true
                }
            } while (res.moveToNext())
            return isExist
        }

        fun clearDatabase(context: Context) {
            val database = getDatabase(context)
            database.recordDao().nukeDb()
            database.statusDao().nukeDb()
            database.safeEntryDao().nukeDb()
            database.bleRecordDao().nukeDb()
            database.favouriteDao().nukeDb()
            database.familyMemberDao().nukeDb()
            database.logRecordDao().nukeDb()
        }
    }

}
