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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class DashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var username: String
    private var userId: Int = -1

    private lateinit var weightChart: LineChart
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
        weightChart = findViewById(R.id.weightChart)
        tvCurrentWeight = findViewById(R.id.tvCurrentWeight)
        tvGoalProgress = findViewById(R.id.tvGoalProgress)
        tvProgress = findViewById(R.id.tvProgress)
        weightHistoryContainer = findViewById(R.id.weightHistoryContainer)
        btnAddWeight = findViewById(R.id.btnAddWeight)
        btnSettings = findViewById(R.id.btnSettings)

        setupChart()
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

    private fun setupChart() {
        weightChart.description.isEnabled = false
        weightChart.setTouchEnabled(true)
        weightChart.setDragEnabled(true)
        weightChart.setScaleEnabled(true)
        weightChart.setPinchZoom(true)
        weightChart.setDrawGridBackground(false)

        val xAxis = weightChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f

        weightChart.axisRight.isEnabled = false
        weightChart.axisLeft.setDrawGridLines(true)
        weightChart.legend.isEnabled = true
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

            updateChart(weights)
        } else {
            tvCurrentWeight.text = "-- lbs"
            tvGoalProgress.text = "No weights recorded"
            tvProgress.text = "Add your first weight entry"
            weightChart.clear()
            weightChart.invalidate()
        }

        loadWeightHistory(weights)
    }

    private fun updateChart(weights: List<DatabaseHelper.WeightEntry>) {
        if (weights.isEmpty()) {
            weightChart.clear()
            weightChart.invalidate()
            return
        }

        val entries = ArrayList<Entry>()
        val dateLabels = ArrayList<String>()

        val reversedWeights = weights.reversed()

        for (i in reversedWeights.indices) {
            val weight = reversedWeights[i]
            entries.add(Entry(i.toFloat(), weight.weight.toFloat()))
            dateLabels.add(weight.date)
        }

        val dataSet = LineDataSet(entries, "Weight Progress")
        dataSet.color = ContextCompat.getColor(this, R.color.primary_green)
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary_green))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = ContextCompat.getColor(this, R.color.light_green)

        val lineData = LineData(dataSet)
        weightChart.data = lineData

        val xAxis = weightChart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < dateLabels.size) {
                    dateLabels[index]
                } else {
                    ""
                }
            }
        }

        weightChart.animateX(1000)
        weightChart.invalidate()
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
                if (dbHelper.deleteWeight(weight.id, userId)) {  // ← ADDED userId
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