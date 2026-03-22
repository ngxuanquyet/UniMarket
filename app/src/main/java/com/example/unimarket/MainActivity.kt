package com.example.unimarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.unimarket.presentation.navigation.RootNavGraph
import com.example.unimarket.presentation.theme.UniMarketTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniMarketTheme {
                val rootNavController = rememberNavController()
                RootNavGraph(navController = rootNavController)
            }
        }
    }
}
