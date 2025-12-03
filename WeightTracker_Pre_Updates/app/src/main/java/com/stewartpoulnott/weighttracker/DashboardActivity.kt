package com.stewartpoulnott.weighttracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class DashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var username: String
    private var userId: Int = -1

    private lateinit var tvWelcome: TextView
    private lateinit var tvCurrentWeight: TextView
    private lateinit var tvGoalProgress: TextView
    private lateinit var tvProgress: TextView
    private lateinit var weightHistoryContainer: LinearLayout
    private lateinit var btnAddWeight: Button
    private lateinit var btnSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        dbHelper = DatabaseHelper(this)
        username = intent.getStringExtra("username") ?: ""
        userId = dbHelper.getUserId(username)

        initViews()
        setupListeners()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvCurrentWeight = findViewById(R.id.tvCurrentWeight)
        tvGoalProgress = findViewById(R.id.tvGoalProgress)
        tvProgress = findViewById(R.id.tvProgress)
        weightHistoryContainer = findViewById(R.id.weightHistoryContainer)
        btnAddWeight = findViewById(R.id.btnAddWeight)
        btnSettings = findViewById(R.id.btnSettings)
    }

    private fun setupListeners() {
        btnAddWeight.setOnClickListener {
            val intent = Intent(this, AddWeightActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }
    }

    private fun loadData() {
        tvWelcome.text = "Welcome back, $username!"

        val weights = dbHelper.getUserWeights(userId)
        val goalWeight = dbHelper.getUserGoal(userId)

        if (weights.isNotEmpty()) {
            val currentWeight = weights[0].weight
            tvCurrentWeight.text = "${currentWeight} lbs"

            if (goalWeight > 0) {
                tvGoalProgress.text = "Goal: ${goalWeight} lbs"
                val remaining = kotlin.math.abs(currentWeight - goalWeight)
                val direction = if (currentWeight > goalWeight) "to lose" else "to gain"
                tvProgress.text = "${String.format("%.1f", remaining)} lbs $direction"
            } else {
                tvGoalProgress.text = "No goal set"
                tvProgress.text = "Set a goal in Add Weight"
            }
        } else {
            tvCurrentWeight.text = "-- lbs"
            tvGoalProgress.text = "No weights recorded"
            tvProgress.text = "Add your first weight entry"
        }

        loadWeightHistory(weights)
    }

    private fun loadWeightHistory(weights: List<DatabaseHelper.WeightEntry>) {
        weightHistoryContainer.removeAllViews()

        if (weights.isEmpty()) {
            val noDataView = TextView(this).apply {
                text = "No weight entries yet. Add your first weight!"
                textSize = 16f
                setPadding(12, 20, 12, 20)
            }
            weightHistoryContainer.addView(noDataView)
            return
        }

        for (i in weights.indices) {
            val weight = weights[i]
            val previousWeight = if (i < weights.size - 1) weights[i + 1].weight else weight.weight
            val change = weight.weight - previousWeight

            addWeightRow(weight, change)
        }
    }

    private fun addWeightRow(weight: DatabaseHelper.WeightEntry, change: Double) {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(ContextCompat.getColor(this@DashboardActivity,
                if (weightHistoryContainer.childCount % 2 == 0) android.R.color.white else android.R.color.transparent))
        }

        val dateText = TextView(this).apply {
            text = weight.date
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 14f
        }

        val weightText = TextView(this).apply {
            text = "${weight.weight} lbs"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.primary_green))
        }

        val changeText = TextView(this).apply {
            text = if (change == 0.0) "--" else String.format("%+.1f", change)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@DashboardActivity,
                if (change >= 0) R.color.red else R.color.primary_green))
        }

        val deleteButton = Button(this).apply {
            text = "Delete"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 12f
            setBackgroundColor(ContextCompat.getColor(this@DashboardActivity, R.color.red))
            setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.white))
            setOnClickListener {
                showDeleteConfirmation(weight)
            }
        }

        rowLayout.addView(dateText)
        rowLayout.addView(weightText)
        rowLayout.addView(changeText)
        rowLayout.addView(deleteButton)

        weightHistoryContainer.addView(rowLayout)
    }

    private fun showDeleteConfirmation(weight: DatabaseHelper.WeightEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete Weight Entry")
            .setMessage("Are you sure you want to delete this weight entry?")
            .setPositiveButton("Delete") { _, _ ->
                if (dbHelper.deleteWeight(weight.id)) {
                    Toast.makeText(this, "Weight entry deleted", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(this, "Failed to delete entry", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}