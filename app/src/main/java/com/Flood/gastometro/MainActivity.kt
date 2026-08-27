package com.Flood.gastometro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.Flood.gastometro.ui.home.HomeScreen
import com.Flood.gastometro.ui.theme.GastosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GastosTheme {
                HomeScreen()
            }
        }
    }
}