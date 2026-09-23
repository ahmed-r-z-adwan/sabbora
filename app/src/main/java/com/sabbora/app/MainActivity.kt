package com.sabbora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sabbora.app.ui.navigation.SabboraNavHost
import com.sabbora.app.ui.theme.SabboraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as SabboraApp).repository
        setContent {
            SabboraTheme {
                SabboraNavHost(repository = repository)
            }
        }
    }
}
