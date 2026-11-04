package com.example.batterymonitor

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.*
import android.os.BatteryManager
import android.graphics.*
import android.view.View
import android.widget.*
import android.view.Gravity

class MainActivity : Activity() {

    private lateinit var batteryText: TextView
    private lateinit var voltageText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var healthText: TextView
    private lateinit var currentText: TextView
    private lateinit var graph: BatteryGraphView

    private val history = mutableListOf<Int>()
    private val handler = Handler(Looper.getMainLooper())

    private val prefs by lazy {
        getSharedPreferences("battery_data", MODE_PRIVATE)
    }

    private val updateTask = object : Runnable {
        override fun run() {
            readBattery()
            handler.postDelayed(this, 60000)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                showBatteryInfo(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadHistory()
        createScreen()
        readBattery()
    }

    private fun createScreen() {

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 20, 20, 20)
        root.setBackgroundColor(Color.rgb(245, 247, 250))

        val scroll = ScrollView(this)
        scroll.addView(root)

        val title = TextView(this)
        title.text = "پایشگر هوشمند باتری"
        title.textSize = 24f
        title.gravity = Gravity.CENTER
        title.setTextColor(Color.rgb(20, 60, 100))
        title.setPadding(10, 20, 10, 25)

        root.addView(title)

        batteryText = makeText("درصد باتری: --%", 30f)
        root.addView(batteryText)

        voltageText = makeText("ولتاژ: --", 18f)
        root.addView(voltageText)

        temperatureText = makeText("دمای باتری: --", 18f)
        root.addView(temperatureText)

        healthText = makeText("سلامت باتری: --", 18f)
        root.addView(healthText)

        currentText = makeText("جریان باتری: --", 18f)
        root.addView(currentText)

        val graphTitle = makeText("نمودار تغییرات شارژ باتری", 20f)
        root.addView(graphTitle)

        graph = BatteryGraphView(this, history)

        root.addView(
            graph,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                230
            )
        )

        val historyTitle = makeText("تعداد نمونه های ثبت شده", 17f)
        root.addView(historyTitle)

        val historyCount = makeText("", 16f)
        root.addView(historyCount)

        val exportButton = Button(this)
        exportButton.text = "خروجی گرفتن از اطلاعات (CSV)"
        root.addView(exportButton)

        exportButton.setOnClickListener {
            exportCSV()
        }

        setContentView(scroll)
    }

    private fun makeText(value: String, size: Float): TextView {
        val text = TextView(this)
        text.text = value
        text.textSize = size
        text.gravity = Gravity.CENTER
        text.setTextColor(Color.DKGRAY)
        text.setPadding(8, 12, 8, 12)
        return text
    }

    private fun readBattery() {
        val intent = registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        if (intent != null) {
            showBatteryInfo(intent)
        }
    }

    private fun showBatteryInfo(intent: Intent) {

        val level = intent.getIntExtra(
            BatteryManager.EXTRA_LEVEL, -1
        )

        val scale = intent.getIntExtra(
            BatteryManager.EXTRA_SCALE, 100
        )

        val percent = if (scale > 0) {
            level * 100 / scale
        } else {
            0
        }

        val voltage = intent.getIntExtra(
            BatteryManager.EXTRA_VOLTAGE, 0
        ) / 1000f

        val temperature = intent.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE, 0
        ) / 10f

        val health = intent.getIntExtra(
            BatteryManager.EXTRA_HEALTH, 0
        )

        val manager = getSystemService(BATTERY_SERVICE)
                as BatteryManager

        val current = manager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
        )

        batteryText.text = "شارژ باقی مانده: $percent٪"

        voltageText.text = "ولتاژ باتری: $voltage ولت"

        temperatureText.text =
            "دمای باتری: $temperature درجه"

        healthText.text = "وضعیت سلامت: ${healthName(health)}"

        currentText.text =
            "جریان لحظه ای: ${current / 1000} میلی آمپر"

        if (history.isEmpty() || history.last() != percent) {
            history.add(percent)

            if (history.size > 1440) {
                history.removeAt(0)
            }

            saveHistory()
            graph.invalidate()
        }

        val count = findViewById<ScrollView>(android.R.id.content)
        // اطلاعات نمودار با هر تغییر به روز می شود.
    }

    private fun healthName(value: Int): String {
        return when (value) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "خوب"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "بیش از حد گرم"
            BatteryManager.BATTERY_HEALTH_DEAD -> "خراب"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "ولتاژ بالا"
            BatteryManager.BATTERY_HEALTH_COLD -> "سرد"
            else -> "نامشخص"
        }
    }

    private fun saveHistory() {
        prefs.edit().putString(
            "history",
            history.joinToString(",")
        ).apply()
    }

    private fun loadHistory() {
        val saved = prefs.getString("history", "") ?: ""

        if (saved.isNotEmpty()) {
            history.addAll(
                saved.split(",").mapNotNull { it.toIntOrNull() }
            )
        }
    }

    private fun exportCSV() {

        val data = StringBuilder()
        data.append("شماره,درصد باتری\n")

        history.forEachIndexed { index, value ->
            data.append("${index + 1},$value\n")
        }

        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = "text/csv"
        sendIntent.putExtra(
            Intent.EXTRA_TEXT,
            data.toString()
        )

        startActivity(
            Intent.createChooser(sendIntent, "ارسال اطلاعات باتری")
        )
    }

    override fun onResume() {
        super.onResume()

        registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        handler.post(updateTask)
    }

    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(updateTask)
        unregisterReceiver(batteryReceiver)
    }
}

class BatteryGraphView(
    context: Context,
    private val values: List<Int>
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        paint.color = Color.WHITE
        canvas.drawRoundRect(0f, 0f, w, h, 18f, 18f, paint)

        val left = 55f
        val right = w - 20f
        val top = 20f
        val bottom = h - 35f

        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f

        for (i in 0..4) {
            val y = top + (bottom - top) * i / 4
            canvas.drawLine(left, y, right, y, paint)
        }

        paint.color = Color.DKGRAY
        paint.textSize = 24f

        for (i in 0..4) {
            val y = top + (bottom - top) * i / 4
            canvas.drawText(
                "${100 - i * 25}%",
                3f,
                y + 8f,
                paint
            )
        }

        if (values.size < 2) {
            paint.textSize = 26f
            canvas.drawText(
                "در انتظار ثبت اطلاعات...",
                left + 10f,
                h / 2,
                paint
            )
            return
        }

        val path = Path()

        values.forEachIndexed { index, value ->

            val x = left +
                    (right - left) * index /
                    (values.size - 1).toFloat()

            val y = bottom -
                    (bottom - top) * value / 100f

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        paint.color = Color.rgb(20, 130, 220)
        paint.strokeWidth = 5f
        paint.style = Paint.Style.STROKE

        canvas.drawPath(path, paint)

        paint.style = Paint.Style.FILL
    }
}