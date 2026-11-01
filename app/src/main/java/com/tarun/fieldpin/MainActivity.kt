package com.tarun.fieldpin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tarun.fieldpin.navigation.AppNavigation
import com.tarun.fieldpin.ui.theme.FieldPinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FieldPinTheme {
                AppNavigation()
            }
        }
    }
}
