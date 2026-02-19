package com.debanshu.xcalendar.data.localDataSource

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import com.debanshu.xcalendar.data.localDataSource.CalendarSourceDao
import com.debanshu.xcalendar.data.localDataSource.GoogleAccountDao
import com.debanshu.xcalendar.data.localDataSource.model.CalendarEntity
import com.debanshu.xcalendar.data.localDataSource.model.CalendarSourceEntity
import com.debanshu.xcalendar.data.localDataSource.model.EventEntity
import com.debanshu.xcalendar.data.localDataSource.model.EventReminderEntity
import com.debanshu.xcalendar.data.localDataSource.model.HolidayAnnotationEntity
import com.debanshu.xcalendar.data.localDataSource.model.HolidayEntity
import com.debanshu.xcalendar.data.localDataSource.model.InboxItemEntity
import com.debanshu.xcalendar.data.localDataSource.model.GoogleAccountEntity
import com.debanshu.xcalendar.data.localDataSource.model.PersonEntity
import com.debanshu.xcalendar.data.localDataSource.model.ProjectEntity
import com.debanshu.xcalendar.data.localDataSource.model.RoutineEntity
import com.debanshu.xcalendar.data.localDataSource.model.SyncFailureEntity
import com.debanshu.xcalendar.data.localDataSource.model.TaskEntity
import com.debanshu.xcalendar.data.localDataSource.model.UserEntity

/**
 * Current database version.
 * Increment this when making schema changes and add a migration.
 */
const val DATABASE_VERSION = 5

const val DATABASE_NAME = "xcalendar.db"

@Database(
    entities = [
        UserEntity::class,
        CalendarEntity::class,
        CalendarSourceEntity::class,
        EventEntity::class,
        EventReminderEntity::class,
        HolidayEntity::class,
        SyncFailureEntity::class,
        PersonEntity::class,
        TaskEntity::class,
        RoutineEntity::class,
        ProjectEntity::class,
        InboxItemEntity::class,
        GoogleAccountEntity::class,
        HolidayAnnotationEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
@ConstructedBy(LocalDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getUserEntityDao(): UserDao

    abstract fun getCalendarEntityDao(): CalendarDao

    abstract fun getCalendarSourceDao(): CalendarSourceDao

    abstract fun getEventEntityDao(): EventDao

    abstract fun getHolidayEntityDao(): HolidayDao

    abstract fun getSyncFailureDao(): SyncFailureDao

    abstract fun getPersonDao(): PersonDao

    abstract fun getTaskDao(): TaskDao

    abstract fun getRoutineDao(): RoutineDao

    abstract fun getProjectDao(): ProjectDao

    abstract fun getInboxItemDao(): InboxItemDao

    abstract fun getGoogleAccountDao(): GoogleAccountDao

    abstract fun getHolidayAnnotationDao(): HolidayAnnotationDao

    companion object {
        /**
         * Array of all migrations.
         * Add new migrations here when incrementing DATABASE_VERSION.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
        )
    }
}

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) = doMigrate { db.execSQL(it) }
        override fun migrate(connection: SQLiteConnection) = doMigrate { connection.prepare(it).use { stmt -> stmt.step() } }
        private fun doMigrate(exec: (String) -> Unit) {
            exec(
                """
                CREATE TABLE IF NOT EXISTS people (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    ageYears INTEGER,
                    color INTEGER NOT NULL,
                    avatarUrl TEXT NOT NULL,
                    isAdmin INTEGER NOT NULL,
                    isDefault INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    isArchived INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            exec("CREATE INDEX IF NOT EXISTS index_people_role ON people(role)")
            exec("CREATE INDEX IF NOT EXISTS index_people_isDefault ON people(isDefault)")
            exec("CREATE INDEX IF NOT EXISTS index_people_isArchived ON people(isArchived)")

            exec(
                """
                CREATE TABLE IF NOT EXISTS routines (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    notes TEXT,
                    timeOfDay TEXT NOT NULL,
                    recurrenceRule TEXT,
                    assignedToPersonId TEXT,
                    isActive INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(assignedToPersonId) REFERENCES people(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            exec("CREATE INDEX IF NOT EXISTS index_routines_assignedToPersonId ON routines(assignedToPersonId)")
            exec("CREATE INDEX IF NOT EXISTS index_routines_isActive ON routines(isActive)")

            exec(
                """
                CREATE TABLE IF NOT EXISTS projects (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    notes TEXT,
                    status TEXT NOT NULL,
                    seasonLabel TEXT,
                    startAt INTEGER,
                    endAt INTEGER,
                    ownerPersonId TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(ownerPersonId) REFERENCES people(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            exec("CREATE INDEX IF NOT EXISTS index_projects_ownerPersonId ON projects(ownerPersonId)")
            exec("CREATE INDEX IF NOT EXISTS index_projects_status ON projects(status)")

            exec(
                """
                CREATE TABLE IF NOT EXISTS tasks (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    notes TEXT,
                    status TEXT NOT NULL,
                    priority TEXT NOT NULL,
                    energy TEXT NOT NULL,
                    type TEXT NOT NULL,
                    scheduledStart INTEGER,
                    scheduledEnd INTEGER,
                    dueAt INTEGER,
                    durationMinutes INTEGER NOT NULL,
                    assignedToPersonId TEXT,
                    affectedPersonIds TEXT NOT NULL,
                    projectId TEXT,
                    routineId TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(assignedToPersonId) REFERENCES people(id) ON DELETE SET NULL,
                    FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE SET NULL,
                    FOREIGN KEY(routineId) REFERENCES routines(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            exec("CREATE INDEX IF NOT EXISTS index_tasks_assignedToPersonId ON tasks(assignedToPersonId)")
            exec("CREATE INDEX IF NOT EXISTS index_tasks_projectId ON tasks(projectId)")
            exec("CREATE INDEX IF NOT EXISTS index_tasks_routineId ON tasks(routineId)")
            exec("CREATE INDEX IF NOT EXISTS index_tasks_status ON tasks(status)")

            exec(
                """
                CREATE TABLE IF NOT EXISTS inbox_items (
                    id TEXT NOT NULL PRIMARY KEY,
                    rawText TEXT NOT NULL,
                    source TEXT NOT NULL,
                    status TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    personId TEXT,
                    linkedTaskId TEXT,
                    FOREIGN KEY(personId) REFERENCES people(id) ON DELETE SET NULL,
                    FOREIGN KEY(linkedTaskId) REFERENCES tasks(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            exec("CREATE INDEX IF NOT EXISTS index_inbox_items_personId ON inbox_items(personId)")
            exec("CREATE INDEX IF NOT EXISTS index_inbox_items_linkedTaskId ON inbox_items(linkedTaskId)")
            exec("CREATE INDEX IF NOT EXISTS index_inbox_items_status ON inbox_items(status)")
        }
    }

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) = doMigrate { db.execSQL(it) }
        override fun migrate(connection: SQLiteConnection) = doMigrate { connection.prepare(it).use { stmt -> stmt.step() } }
        private fun doMigrate(exec: (String) -> Unit) {
            exec(
                """
                ALTER TABLE events ADD COLUMN source TEXT NOT NULL DEFAULT 'LOCAL'
                """.trimIndent()
            )
            exec("ALTER TABLE events ADD COLUMN externalId TEXT")
            exec("ALTER TABLE events ADD COLUMN externalUpdatedAt INTEGER")
            exec("ALTER TABLE events ADD COLUMN lastSyncedAt INTEGER")

            exec(
                """
                CREATE TABLE IF NOT EXISTS google_accounts (
                    id TEXT NOT NULL PRIMARY KEY,
                    email TEXT NOT NULL,
                    displayName TEXT,
                    personId TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(personId) REFERENCES people(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            exec("CREATE INDEX IF NOT EXISTS index_google_accounts_personId ON google_accounts(personId)")

            exec(
                """
                CREATE TABLE IF NOT EXISTS calendar_sources (
                    calendarId TEXT NOT NULL PRIMARY KEY,
                    provider TEXT NOT NULL,
                    providerCalendarId TEXT NOT NULL,
                    providerAccountId TEXT NOT NULL,
                    syncEnabled INTEGER NOT NULL,
                    lastSyncedAt INTEGER,
                    FOREIGN KEY(calendarId) REFERENCES calendars(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            exec("CREATE INDEX IF NOT EXISTS index_calendar_sources_providerAccountId ON calendar_sources(providerAccountId)")
        }
    }

val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) = doMigrate { db.execSQL(it) }
        override fun migrate(connection: SQLiteConnection) = doMigrate { connection.prepare(it).use { stmt -> stmt.step() } }
        private fun doMigrate(exec: (String) -> Unit) {
            // Drop and recreate holidays table to add holidayType and translations fields
            // This clears cached Calendarific (India) data and prepares for Enrico (USA/Utah) API
            exec("DROP TABLE IF EXISTS holidays")
            exec(
                """
                CREATE TABLE IF NOT EXISTS holidays (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    countryCode TEXT NOT NULL,
                    holidayType TEXT NOT NULL,
                    translations TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) = doMigrate { db.execSQL(it) }
        override fun migrate(connection: SQLiteConnection) = doMigrate { connection.prepare(it).use { stmt -> stmt.step() } }
        private fun doMigrate(exec: (String) -> Unit) {
            exec(
                """
                CREATE TABLE IF NOT EXISTS holiday_annotations (
                    holidayId TEXT NOT NULL PRIMARY KEY,
                    description TEXT,
                    location TEXT,
                    reminderMinutes INTEGER,
                    affectedPersonIds TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object LocalDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
