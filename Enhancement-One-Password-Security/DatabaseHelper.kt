package com.stewartpoulnott.weighttracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "WeightTracker.db"
        private const val DATABASE_VERSION = 4

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

        val createWeightsTable = """
            CREATE TABLE $TABLE_WEIGHTS (
                $COL_WEIGHT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WEIGHT_USER_ID INTEGER,
                $COL_WEIGHT_DATE TEXT,
                $COL_WEIGHT_VALUE REAL,
                $COL_WEIGHT_NOTES TEXT
            )
        """.trimIndent()
        db.execSQL(createWeightsTable)

        val createGoalsTable = """
            CREATE TABLE $TABLE_GOALS (
                $COL_GOAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_GOAL_USER_ID INTEGER,
                $COL_GOAL_WEIGHT REAL
            )
        """.trimIndent()
        db.execSQL(createGoalsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_PASSWORD_SALT TEXT")
            migrateExistingPasswords(db)
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

    fun updateUserPhone(userId: Int, phoneNumber: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PHONE_NUMBER, phoneNumber)
        }
        val rowsAffected = db.update(TABLE_USERS, values, "$COL_USER_ID = ?", arrayOf(userId.toString()))
        return rowsAffected > 0
    }

    fun getUserPhone(userId: Int): String {
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

    fun deleteWeight(weightId: Int): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_WEIGHTS, "$COL_WEIGHT_ID = ?", arrayOf(weightId.toString())) > 0
    }

    fun setGoal(userId: Int, goalWeight: Double): Boolean {
        val db = writableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_GOAL_ID FROM $TABLE_GOALS WHERE $COL_GOAL_USER_ID = ?",
            arrayOf(userId.toString())
        )

        val values = ContentValues().apply {
            put(COL_GOAL_USER_ID, userId)
            put(COL_GOAL_WEIGHT, goalWeight)
        }

        val result = if (cursor.count > 0) {
            db.update(TABLE_GOALS, values, "$COL_GOAL_USER_ID = ?", arrayOf(userId.toString())).toLong()
        } else {
            db.insert(TABLE_GOALS, null, values)
        }
        cursor.close()
        return result != -1L
    }

    fun getUserGoal(userId: Int): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_GOAL_WEIGHT FROM $TABLE_GOALS WHERE $COL_GOAL_USER_ID = ?",
            arrayOf(userId.toString())
        )

        var goal = 0.0
        if (cursor.moveToFirst()) {
            goal = cursor.getDouble(0)
        }
        cursor.close()
        return goal
    }

    data class WeightEntry(
        val id: Int,
        val date: String,
        val weight: Double,
        val notes: String
    )
}