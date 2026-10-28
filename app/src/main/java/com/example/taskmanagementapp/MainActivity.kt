package com.example.taskmanagementapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmanagementapp.ui.theme.TaskManagementAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import io.grpc.ManagedChannelBuilder
import com.comp713.grpc.TaskEventServiceGrpc
import com.comp713.grpc.TaskStatusChangeRequest

private const val BASE_URL = "http://10.0.2.2:8080"

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val status: String,
    val userId: Int
)

data class AppUser(
    val id: Int,
    val username: String,
    val email: String
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
    var users by remember { mutableStateOf<List<AppUser>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<AppUser?>(null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var userFieldWidth by remember { mutableIntStateOf(0) }

    var isLoading by remember { mutableStateOf(loadFromServer) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    LaunchedEffect(loadFromServer) {
        if (loadFromServer) {
            isLoading = true

            try {
                tasks = withContext(Dispatchers.IO) {
                    fetchTasks()
                }
            } catch (e: Exception) {
                errorMessage = "Could not load tasks: ${e.message}"
            }

            try {
                users = withContext(Dispatchers.IO) {
                    fetchUsers()
                }
            } catch (e: Exception) {
                errorMessage = "Could not load users: ${e.message}"
            }

            isLoading = false
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "Task Management System",
                fontSize = 14.sp,
                style = MaterialTheme.typography.headlineMedium
            )

            Button(
                onClick = {
                    context.startActivity(
                        Intent(context, AddUserActivity::class.java)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add User")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create a new task",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 200) title = it
                },
                label = { Text("Task title") },
                supportingText = { Text("${title.length}/200 characters") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Task description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Assign to user",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.height(4.dp))


            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedUser?.username ?: "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Select a user") },
                    placeholder = { Text("Choose a user") },
                    trailingIcon = { Text("▼") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            userFieldWidth = size.width
                        }
                )

                // Transparent layer that receives taps.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(enabled = users.isNotEmpty()) {
                            dropdownExpanded = true
                        }
                )

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.width(
                        with(LocalDensity.current) {
                            userFieldWidth.toDp()
                        }
                    )
                ) {
                    users.forEach { user ->
                        DropdownMenuItem(
                            text = { Text(user.username) },
                            onClick = {
                                selectedUser = user
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            if (users.isEmpty() && !isLoading) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No users available. Check that users exist in the database.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    when {
                        title.isBlank() ->
                            errorMessage = "Please enter a task title."

                        description.isBlank() ->
                            errorMessage = "Please enter a task description."

                        selectedUser == null ->
                            errorMessage = "Please select a user."

                        else -> {
                            scope.launch {
                                isSaving = true
                                errorMessage = null
                                successMessage = null

                                try {
                                    withContext(Dispatchers.IO) {
                                        createTask(
                                            title = title.trim(),
                                            description = description.trim(),
                                            userId = selectedUser!!.id
                                        )
                                    }

                                    tasks = withContext(Dispatchers.IO) {
                                        fetchTasks()
                                    }

                                    title = ""
                                    description = ""
                                    selectedUser = null
                                    successMessage = "Task created successfully!"
                                } catch (e: Exception) {
                                    errorMessage =
                                        "Could not create task: ${e.message}"
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    }
                },
                enabled = !isSaving && !isLoading && users.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "Creating..." else "Create Task")
            }

            if (isSaving) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator()
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            successMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Tasks",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> Text("Loading tasks and users...")
                tasks.isEmpty() -> Text("No tasks yet.")
                else -> tasks.forEach { task ->
                    TaskItem(
                        task = task,

                        onStatusChange = { newStatus ->
                            scope.launch {
                                val success = updateTaskStatus(task, newStatus)

                                if (success) {
                                    println("About to send gRPC event for task ID: ${task.id}")
                                    val eventLogged = recordTaskStatusChange(task, newStatus)

                                    try {
                                        tasks = fetchTasks()

                                        errorMessage = if (eventLogged) {
                                            ""
                                        } else {
                                            "Task updated, but recording the gRPC event failed."
                                        }
                                    } catch (e: Exception) {
                                        errorMessage =
                                            "Refresh failed: ${e.javaClass.simpleName} - ${e.message}"
                                        e.printStackTrace()
                                    }
                                } else {
                                    errorMessage = "Could not update task status. Please try again."
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                val success = deleteTask(task.id)

                                if (success) {
                                    try {
                                        tasks = fetchTasks()
                                        errorMessage = ""
                                    } catch (e: Exception) {
                                        errorMessage = "Task deleted, but refreshing the task list failed."
                                    }
                                } else {
                                    errorMessage = "Could not delete task. Please try again."
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun TaskItem(
    task: Task,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(text = task.description)
        }

        Box {
            TextButton(
                onClick = { dropdownExpanded = true }
            ) {
                Text(task.status)
                Text(" ▼")
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                listOf("TODO", "IN_PROGRESS", "DONE").forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status) },
                        onClick = {
                            dropdownExpanded = false

                            if (status != task.status) {
                                onStatusChange(status)
                            }
                        }
                    )
                }
            }
        }

        Button(
            onClick = { onDelete() }
        ) {
            Text("Delete")
        }
    }
}


private suspend fun fetchTasks(): List<Task> =
    withContext(Dispatchers.IO) {
        val connection = URL("$BASE_URL/api/tasks")
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode !in 200..299) {
                throw Exception(
                    "Server returned HTTP ${connection.responseCode}"
                )
            }

            val response = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val jsonArray = JSONArray(response)

            (0 until jsonArray.length()).map { i ->
                val item = jsonArray.getJSONObject(i)

                Task(
                    id = item.getInt("id"),
                    title = item.getString("title"),
                    description = item.optString("description", ""),
                    status = item.optString("status", "TODO"),
                    userId = item.getInt("user_id")
                )
            }
        } finally {
            connection.disconnect()
        }
    }


private fun fetchUsers(): List<AppUser> {
    val connection = URL("$BASE_URL/api/users")
        .openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        if (connection.responseCode !in 200..299) {
            throw Exception("Server returned HTTP ${connection.responseCode}")
        }

        val response = connection.inputStream
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val jsonArray = JSONArray(response)

        return (0 until jsonArray.length()).map { i ->
            val item = jsonArray.getJSONObject(i)

            AppUser(
                id = item.getInt("id"),
                username = item.getString("username"),
                email = item.getString("email")
            )
        }
    } finally {
        connection.disconnect()
    }
}

private suspend fun updateTaskStatus(
    task: Task,
    newStatus: String
): Boolean = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null

    try {
        val url = URL("$BASE_URL/api/tasks/update")
        connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "PUT"
        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )
        connection.doOutput = true

        val json = JSONObject().apply {
            put("id", task.id)
            put("title", task.title)
            put("description", task.description)
            put("status", newStatus)
        }

        connection.outputStream.use { output ->
            output.write(json.toString().toByteArray(Charsets.UTF_8))
        }

        connection.responseCode in 200..299
    } catch (e: Exception) {
        false
    } finally {
        connection?.disconnect()
    }
}

private suspend fun deleteTask(taskId: Int): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("http://10.0.2.2:8080/api/tasks/delete")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "DELETE"
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )
            connection.doOutput = true

            val json = JSONObject().put("id", taskId)

            connection.outputStream.use { output ->
                output.write(json.toString().toByteArray(Charsets.UTF_8))
            }

            val success = connection.responseCode in 200..299

            connection.disconnect()

            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

private suspend fun recordTaskStatusChange(
    task: Task,
    newStatus: String
): Boolean = withContext(Dispatchers.IO) {
    var channel: io.grpc.ManagedChannel? = null

    try {
        channel = ManagedChannelBuilder
            .forAddress("10.0.2.2", 9090)
            .usePlaintext()
            .build()

        val request = TaskStatusChangeRequest.newBuilder()
            .setTaskId(task.id)
            .setTaskTitle(task.title)
            .setOldStatus(task.status)
            .setNewStatus(newStatus)
            .setUserId(task.userId)
            .build()

        val requestId = java.util.UUID.randomUUID().toString()
        println("Sending gRPC request: $requestId, task ID: ${task.id}")

        val response = TaskEventServiceGrpc
            .newBlockingStub(channel)
            .recordTaskStatusChange(request)

        println("gRPC response received for request: $requestId")

        response.success
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        channel?.shutdownNow()
    }
}

private fun createTask(
    title: String,
    description: String,
    userId: Int
) {
    val body = JSONObject().apply {
        put("title", title)
        put("description", description)
        put("user_id", userId)
    }

    val connection = URL("$BASE_URL/api/tasks/create")
        .openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.doOutput = true
        connection.setRequestProperty(
            "Content-Type",
            "application/json; charset=UTF-8"
        )

        connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
            it.write(body.toString())
        }

        val responseCode = connection.responseCode

        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }

            throw Exception(
                "Server returned HTTP $responseCode" +
                        (errorBody?.let { ": $it" } ?: "")
            )
        }
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