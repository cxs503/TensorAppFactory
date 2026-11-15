package dev.tensorappfactory.reference

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = runCatching {
            assets.open("content/demo-reading.json").use(ContentPackageLoader::load)
        }.getOrNull()
        setContent {
            if (content != null) {
                QuizScreen(content)
            } else {
                MaterialTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Content unavailable", style = MaterialTheme.typography.headlineMedium)
                            Text("Add a valid content package at app/src/main/assets/content/demo-reading.json.")
                        }
                    }
                }
            }
        }
    }
}
