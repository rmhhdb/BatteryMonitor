package com.example.batterymonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatteryScreen()
        }
    }
}

@Composable
fun BatteryScreen() {
    var battery by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(listOf<Int>()) }

    LaunchedEffect(Unit) {
        while (true) {
            val intent = androidx.compose.ui.platform.LocalContext.current
            delay(5000)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            battery = (level * 100) / scale
            history = (history + battery).takeLast(30)
            delay(60000)
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101820))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Battery Monitor", color = Color.White, fontSize = 25.sp)

            Spacer(modifier = Modifier.height(35.dp))

            Text(
                "$battery%",
                color = Color(0xFF00E676),
                fontSize = 65.sp
            )

            Text("Remaining Battery", color = Color.LightGray)

            Spacer(modifier = Modifier.height(35.dp))

            Text("Battery History", color = Color.White, fontSize = 20.sp)

            Spacer(modifier = Modifier.height(15.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFF1D2A35), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                if (history.size > 1) {
                    val path = Path()
                    val stepX = size.width / (history.size - 1)

                    history.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = size.height - (value / 100f * size.height)

                        if (index == 0) path.moveTo(x, y)
                        else path.lineTo(x, y)
                    }

                    drawPath(
                        path,
                        color = Color(0xFF00E676),
                        style = Stroke(width = 4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Updates every 60 seconds",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}