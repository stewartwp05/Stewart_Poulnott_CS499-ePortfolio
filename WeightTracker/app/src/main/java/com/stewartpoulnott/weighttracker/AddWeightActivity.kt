package com.stewartpoulnott.weighttracker

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class AddWeightActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var username: String
    private var userId: Int = -1

    private lateinit var btnBack: Button
    private lateinit var etDate: EditText
    private lateinit var etWeight: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnSaveWeight: Button
    private lateinit var etGoalWeight: EditText
    private lateinit var btnUpdateGoal: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_weight)

        dbHelper = DatabaseHelper(this)
        username = intent.getStringExtra("username") ?: ""
        userId = dbHelper.getUserId(username)

        initViews()
        setupListeners()
        loadCurrentGoal()
        setTodaysDate()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etDate = findViewById(R.id.etDate)
        etWeight = findViewById(R.id.etWeight)
        etNotes = findViewById(R.id.etNotes)
        btnSaveWeight = findViewById(R.id.btnSaveWeight)
        etGoalWeight = findViewById(R.id.etGoalWeight)
        btnUpdateGoal = findViewById(R.id.btnUpdateGoal)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        etDate.setOnClickListener {
            showDatePicker()
        }

        btnSaveWeight.setOnClickListener {
            saveWeight()
        }

        btnUpdateGoal.setOnClickListener {
            updateGoal()
        }
    }

    private fun setTodaysDate() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        etDate.setText(today)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                etDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun saveWeight() {
        val date = etDate.text.toString().trim()
        val weightText = etWeight.text.toString().trim()
        val notes = etNotes.text.toString().trim()

        if (date.isEmpty() || weightText.isEmpty()) {
            Toast.makeText(this, "Please enter both date and weight", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val weight = weightText.toDouble()

            if (dbHelper.addWeight(userId, date, weight, notes)) {
                Toast.makeText(this, "Weight entry saved successfully!", Toast.LENGTH_SHORT).show()
                checkGoalAchievement(weight)

                etWeight.setText("")
                etNotes.setText("")
                setTodaysDate()
                finish()
            } else {
                Toast.makeText(this, "Failed to save weight entry", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter a valid weight", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateGoal() {
        val goalText = etGoalWeight.text.toString().trim()

        if (goalText.isEmpty()) {
            Toast.makeText(this, "Please enter a goal weight", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val goalWeight = goalText.toDouble()

            if (dbHelper.setGoal(userId, goalWeight)) {
                Toast.makeText(this, "Goal weight updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to update goal weight", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter a valid goal weight", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCurrentGoal() {
        val currentGoal = dbHelper.getUserGoal(userId)
        if (currentGoal > 0) {
            etGoalWeight.setText(currentGoal.toString())
        }
    }

    private fun checkGoalAchievement(currentWeight: Double) {
        val goalWeight = dbHelper.getUserGoal(userId)

        if (goalWeight > 0) {
            val goalAchieved = kotlin.math.abs(currentWeight - goalWeight) <= 0.5

            if (goalAchieved) {
                sendGoalAchievementSMS(currentWeight, goalWeight)
            }
        }
    }

    private fun sendGoalAchievementSMS(currentWeight: Double, goalWeight: Double) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val phoneNumber = dbHelper.getUserPhone(userId)
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "No phone number set. Please add your phone number in Settings.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            val message = "🎉 Congratulations! You've reached your goal weight of $goalWeight lbs! Current weight: $currentWeight lbs. Keep up the great work!"

            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SMS notification sent to $phoneNumber!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS notification: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}