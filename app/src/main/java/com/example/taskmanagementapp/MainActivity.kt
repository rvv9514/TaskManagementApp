
package com.example.taskmanagementapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmanagementapp.ui.theme.TaskManagementAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val status: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TaskManagementAppTheme {
                TaskScreen()
            }
        }
    }
}

@Composable
fun TaskScreen(loadFromServer: Boolean = true) {
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var isLoading by remember { mutableStateOf(loadFromServer) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loadFromServer) {
        if (loadFromServer) {
            isLoading = true
            errorMessage = null

            try {
                tasks = withContext(Dispatchers.IO) {
                    fetchTasks()
                }
            } catch (e: Exception) {
                errorMessage =
                    e.message ?: "Could not connect to the server."
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                text = "My Tasks",
                fontSize = 30.sp,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Stay organised and get things done.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            when {
                isLoading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading tasks...")
                }

                errorMessage != null -> {
                    Text(
                        text = "Could not load tasks: $errorMessage",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                tasks.isEmpty() -> {
                    Text("No tasks yet.")
                }

                else -> {
                    tasks.forEach { task ->
                        TaskItem(task)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: Task) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Checkbox(
            checked = task.status == "DONE",
            onCheckedChange = {}
        )

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium
            )

            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Status: ${task.status}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun fetchTasks(): List<Task> {
    val connection = URL(
        "http://10.0.2.2:8080/api/tasks"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val responseCode = connection.responseCode

        if (responseCode !in 200..299) {
            throw Exception("Server returned HTTP $responseCode")
        }

        val response = connection.inputStream
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val jsonArray = JSONArray(response)
        val result = mutableListOf<Task>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)

            result.add(
                Task(
                    id = item.getInt("id"),
                    title = item.getString("title"),
                    description = item.optString("description", ""),
                    status = item.optString("status", "TODO")
                )
            )
        }

        return result
    } finally {
        connection.disconnect()
    }
}

@Preview(showBackground = true)
@Composable
fun TaskScreenPreview() {
    TaskManagementAppTheme {
        Surface {
            TaskScreen(loadFromServer = false)
        }
    }
}