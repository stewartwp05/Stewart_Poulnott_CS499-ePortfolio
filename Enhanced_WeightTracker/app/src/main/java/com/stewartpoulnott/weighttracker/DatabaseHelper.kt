package com.stewartpoulnott.weighttracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "WeightTracker.db"
        private const val DATABASE_VERSION = 5  // Incremented from 4 to 5

        private const val TABLE_USERS = "users"
        private const val COL_USER_ID = "id"
        private const val COL_USERNAME = "username"
        private const val COL_PASSWORD = "password"
        private const val COL_PASSWORD_SALT = "password_salt"
        private const val COL_PHONE_NUMBER = "phone_number"

        private const val TABLE_WEIGHTS = "weights"
        private const val COL_WEIGHT_ID = "id"
        private const val COL_WEIGHT_USER_ID = "user_id"
        private const val COL_WEIGHT_DATE = "date"
        private const val COL_WEIGHT_VALUE = "weight"
        private const val COL_WEIGHT_NOTES = "notes"

        private const val TABLE_GOALS = "goals"
        private const val COL_GOAL_ID = "id"
        private const val COL_GOAL_USER_ID = "user_id"
        private const val COL_GOAL_WEIGHT = "goal_weight"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Enable foreign key constraints
        db.execSQL("PRAGMA foreign_keys = ON")

        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USERNAME TEXT UNIQUE,
                $COL_PASSWORD TEXT,
                $COL_PASSWORD_SALT TEXT,
                $COL_PHONE_NUMBER TEXT DEFAULT ''
            )
        """.trimIndent()
        db.execSQL(createUsersTable)

        // Weights table with foreign key constraint
        val createWeightsTable = """
            CREATE TABLE $TABLE_WEIGHTS (
                $COL_WEIGHT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WEIGHT_USER_ID INTEGER NOT NULL,
                $COL_WEIGHT_DATE TEXT NOT NULL,
                $COL_WEIGHT_VALUE REAL NOT NULL,
                $COL_WEIGHT_NOTES TEXT,
                FOREIGN KEY ($COL_WEIGHT_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID) ON DELETE CASCADE
            )
        """.trimIndent()
        db.execSQL(createWeightsTable)

        // Goals table with foreign key constraint
        val createGoalsTable = """
            CREATE TABLE $TABLE_GOALS (
                $COL_GOAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_GOAL_USER_ID INTEGER NOT NULL,
                $COL_GOAL_WEIGHT REAL NOT NULL,
                FOREIGN KEY ($COL_GOAL_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID) ON DELETE CASCADE
            )
        """.trimIndent()
        db.execSQL(createGoalsTable)

        // Create indexes for performance
        createIndexes(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_PASSWORD_SALT TEXT")
            migrateExistingPasswords(db)
        }

        if (oldVersion < 5) {
            // Enable foreign keys
            db.execSQL("PRAGMA foreign_keys = ON")

            // Add indexes for performance
            createIndexes(db)

            // Recreate weights table with foreign key constraint
            recreateWeightsTableWithForeignKey(db)

            // Recreate goals table with foreign key constraint
            recreateGoalsTableWithForeignKey(db)
        }
    }

    private fun createIndexes(db: SQLiteDatabase) {
        // Index on weights.user_id for faster user-specific queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_weights_user_id ON $TABLE_WEIGHTS($COL_WEIGHT_USER_ID)")

        // Index on weights.date for faster date-based queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_weights_date ON $TABLE_WEIGHTS($COL_WEIGHT_DATE)")

        // Index on goals.user_id
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_goals_user_id ON $TABLE_GOALS($COL_GOAL_USER_ID)")
    }

    private fun recreateWeightsTableWithForeignKey(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            // Create new table with foreign key
            db.execSQL("""
                CREATE TABLE weights_new (
                    $COL_WEIGHT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_WEIGHT_USER_ID INTEGER NOT NULL,
                    $COL_WEIGHT_DATE TEXT NOT NULL,
                    $COL_WEIGHT_VALUE REAL NOT NULL,
                    $COL_WEIGHT_NOTES TEXT,
                    FOREIGN KEY ($COL_WEIGHT_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID) ON DELETE CASCADE
                )
            """.trimIndent())

            // Copy existing data
            db.execSQL("INSERT INTO weights_new SELECT * FROM $TABLE_WEIGHTS")

            // Drop old table
            db.execSQL("DROP TABLE $TABLE_WEIGHTS")

            // Rename new table
            db.execSQL("ALTER TABLE weights_new RENAME TO $TABLE_WEIGHTS")

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun recreateGoalsTableWithForeignKey(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            db.execSQL("""
                CREATE TABLE goals_new (
                    $COL_GOAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_GOAL_USER_ID INTEGER NOT NULL,
                    $COL_GOAL_WEIGHT REAL NOT NULL,
                    FOREIGN KEY ($COL_GOAL_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID) ON DELETE CASCADE
                )
            """.trimIndent())

            db.execSQL("INSERT INTO goals_new SELECT * FROM $TABLE_GOALS")
            db.execSQL("DROP TABLE $TABLE_GOALS")
            db.execSQL("ALTER TABLE goals_new RENAME TO $TABLE_GOALS")

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun migrateExistingPasswords(db: SQLiteDatabase) {
        val cursor = db.rawQuery(
            "SELECT $COL_USER_ID, $COL_PASSWORD FROM $TABLE_USERS WHERE $COL_PASSWORD_SALT IS NULL",
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val userId = cursor.getInt(0)
                val plainPassword = cursor.getString(1)

                val salt = PasswordHasher.generateSalt()
                val hashedPassword = PasswordHasher.hashPassword(plainPassword, salt)

                val values = ContentValues().apply {
                    put(COL_PASSWORD, hashedPassword)
                    put(COL_PASSWORD_SALT, salt)
                }

                db.update(TABLE_USERS, values, "$COL_USER_ID = ?", arrayOf(userId.toString()))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    fun addUser(username: String, password: String): Boolean {
        val db = writableDatabase

        val salt = PasswordHasher.generateSalt()
        val hashedPassword = PasswordHasher.hashPassword(password, salt)

        val values = ContentValues().apply {
            put(COL_USERNAME, username)
            put(COL_PASSWORD, hashedPassword)
            put(COL_PASSWORD_SALT, salt)
            put(COL_PHONE_NUMBER, "")
        }
        val result = db.insert(TABLE_USERS, null, values)
        return result != -1L
    }

    fun checkUser(username: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_PASSWORD, COL_PASSWORD_SALT),
            "$COL_USERNAME = ?",
            arrayOf(username),
            null, null, null
        )

        var isValid = false
        if (cursor.moveToFirst()) {
            val storedHash = cursor.getString(0)
            val storedSalt = cursor.getString(1)
            isValid = PasswordHasher.verifyPassword(password, storedHash, storedSalt)
        }
        cursor.close()
        return isValid
    }

    fun getUserId(username: String): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_USER_ID),
            "$COL_USERNAME = ?",
            arrayOf(username),
            null, null, null
        )
        var userId = -1
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0)
        }
        cursor.close()
        return userId
    }

    fun addWeight(userId: Int, date: String, weight: Double, notes: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_WEIGHT_USER_ID, userId)
            put(COL_WEIGHT_DATE, date)
            put(COL_WEIGHT_VALUE, weight)
            put(COL_WEIGHT_NOTES, notes)
        }
        val result = db.insert(TABLE_WEIGHTS, null, values)
        return result != -1L
    }

    fun getUserWeights(userId: Int): List<WeightEntry> {
        val weights = mutableListOf<WeightEntry>()
        val db = readableDatabase

        // Query uses index on user_id for better performance
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_WEIGHTS WHERE $COL_WEIGHT_USER_ID = ? ORDER BY $COL_WEIGHT_DATE DESC",
            arrayOf(userId.toString())
        )

        if (cursor.moveToFirst()) {
            do {
                val entry = WeightEntry(
                    id = cursor.getInt(0),
                    date = cursor.getString(2),
                    weight = cursor.getDouble(3),
                    notes = cursor.getString(4) ?: ""
                )
                weights.add(entry)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return weights
    }

    // Enhanced with authorization check
    fun deleteWeight(weightId: Int, userId: Int): Boolean {
        val db = writableDatabase

        // Verify ownership before deleting
        val cursor = db.query(
            TABLE_WEIGHTS,
            arrayOf(COL_WEIGHT_USER_ID),
            "$COL_WEIGHT_ID = ?",
            arrayOf(weightId.toString()),
            null, null, null
        )

        var canDelete = false
        if (cursor.moveToFirst()) {
            val ownerId = cursor.getInt(0)
            canDelete = (ownerId == userId)
        }
        cursor.close()

        if (!canDelete) return false

        val result = db.delete(TABLE_WEIGHTS, "$COL_WEIGHT_ID = ?", arrayOf(weightId.toString()))
        return result > 0
    }

    // Enhanced with transaction
    fun setUserGoal(userId: Int, goalWeight: Double): Boolean {
        val db = writableDatabase

        db.beginTransaction()
        try {
            val cursor = db.query(
                TABLE_GOALS,
                arrayOf(COL_GOAL_ID),
                "$COL_GOAL_USER_ID = ?",
                arrayOf(userId.toString()),
                null, null, null
            )

            val result = if (cursor.count > 0) {
                cursor.moveToFirst()
                val goalId = cursor.getInt(0)
                cursor.close()

                val values = ContentValues().apply {
                    put(COL_GOAL_WEIGHT, goalWeight)
                }
                db.update(TABLE_GOALS, values, "$COL_GOAL_ID = ?", arrayOf(goalId.toString())) > 0
            } else {
                cursor.close()

                val values = ContentValues().apply {
                    put(COL_GOAL_USER_ID, userId)
                    put(COL_GOAL_WEIGHT, goalWeight)
                }
                db.insert(TABLE_GOALS, null, values) != -1L
            }

            if (result) {
                db.setTransactionSuccessful()
            }
            return result
        } finally {
            db.endTransaction()
        }
    }

    fun getUserGoal(userId: Int): Double {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GOALS,
            arrayOf(COL_GOAL_WEIGHT),
            "$COL_GOAL_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )
        var goalWeight = 0.0
        if (cursor.moveToFirst()) {
            goalWeight = cursor.getDouble(0)
        }
        cursor.close()
        return goalWeight
    }

    fun savePhoneNumber(userId: Int, phoneNumber: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PHONE_NUMBER, phoneNumber)
        }
        val result = db.update(TABLE_USERS, values, "$COL_USER_ID = ?", arrayOf(userId.toString()))
        return result > 0
    }

    fun getPhoneNumber(userId: Int): String {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_PHONE_NUMBER),
            "$COL_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )
        var phoneNumber = ""
        if (cursor.moveToFirst()) {
            phoneNumber = cursor.getString(0) ?: ""
        }
        cursor.close()
        return phoneNumber
    }

    data class WeightEntry(
        val id: Int,
        val date: String,
        val weight: Double,
        val notes: String
    )
}