package batterymonitor

import android.app.Activity
import android.os.Bundle
import android.os.BatteryManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private lateinit var batteryText: TextView
    private lateinit var statusText: TextView
    private lateinit var graphView: BatteryGraphView

    private val levels = mutableListOf<Float>()
    private val handler = Handler(Looper.getMainLooper())

    private val updateTask = object : Runnable {
        override fun run() {
            updateBattery()
            handler.postDelayed(this, 60000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(30, 40, 30, 30)
        layout.setBackgroundColor(Color.rgb(245, 247, 250))

        val title = TextView(this)
        title.text = "پایش مصرف باتری"
        title.textSize = 25f
        title.setTextColor(Color.rgb(30, 50, 80))
        title.gravity = Gravity.CENTER
        title.setTypeface(null, Typeface.BOLD)
        layout.addView(title)

        batteryText = TextView(this)
        batteryText.textSize = 48f
        batteryText.setTextColor(Color.rgb(20, 140, 80))
        batteryText.gravity = Gravity.CENTER
        batteryText.setPadding(0, 30, 0, 10)
        layout.addView(batteryText)

        statusText = TextView(this)
        statusText.textSize = 16f
        statusText.setTextColor(Color.DKGRAY)
        statusText.gravity = Gravity.CENTER
        layout.addView(statusText)

        graphView = BatteryGraphView(this)

        val graphParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 450
        )

        graphParams.setMargins(0, 30, 0, 20)
        layout.addView(graphView, graphParams)

        val info = TextView(this)
        info.text = "نمودار تغییرات درصد باتری در طول زمان"
        info.textSize = 14f
        info.gravity = Gravity.CENTER
        info.setTextColor(Color.DKGRAY)
        layout.addView(info)

        val refresh = Button(this)
        refresh.text = "بروزرسانی"
        refresh.setOnClickListener {
            updateBattery()
        }
        layout.addView(refresh)

        setContentView(layout)

        updateBattery()
        handler.postDelayed(updateTask, 60000)
    }

    private fun updateBattery() {

        val intent = registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = intent?.getIntExtra(
            BatteryManager.EXTRA_LEVEL, -1
        ) ?: -1

        val scale = intent?.getIntExtra(
            BatteryManager.EXTRA_SCALE, 100
        ) ?: 100

        if (level >= 0 && scale > 0) {

            val percent = level * 100f / scale

            batteryText.text = "${percent.toInt()}٪"

            val status = intent?.getIntExtra(
                BatteryManager.EXTRA_STATUS, -1
            )

            statusText.text = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "در حال شارژ"
                BatteryManager.BATTERY_STATUS_FULL -> "شارژ کامل"
                else -> "در حال استفاده"
            }

            levels.add(percent)

            if (levels.size > 60) {
                levels.removeAt(0)
            }

            graphView.setData(levels.toList())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTask)
    }
}

class BatteryGraphView(context: Context) : View(context) {

    private val linePaint = Paint().apply {
        color = Color.rgb(20, 140, 80)
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 2f
    }

    private val textPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 28f
        isAntiAlias = true
    }

    private var data = listOf<Float>()

    fun setData(values: List<Float>) {
        data = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val left = 65f
        val right = width - 25f
        val top = 30f
        val bottom = height - 40f

        canvas.drawColor(Color.WHITE)

        for (i in 0..4) {
            val y = top + (bottom - top) * i / 4

            canvas.drawLine(
                left, y, right, y, gridPaint
            )

            canvas.drawText(
                "${100 - i * 25}%",
                5f, y + 8f, textPaint
            )
        }

        if (data.size < 2) {
            canvas.drawText(
                "در حال جمع آوری اطلاعات...",
                left + 10f,
                height / 2f,
                textPaint
            )
            return
        }

        val path = android.graphics.Path()

        for (i in data.indices) {

            val x = left +
                (right - left) * i / (data.size - 1)

            val y = bottom -
                (data[i] / 100f) * (bottom - top)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, linePaint)
    }
}