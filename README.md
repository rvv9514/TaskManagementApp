# Task Management App

COMP713 Distributed and Mobile System - Group Project

## Overview
This is an Android application for managing tasks. Users can view tasks, create new tasks, assign an user for a task, and update task statuses. The Android application communicates with a Java HTTP server, which stores and retrieves data from a MySQL database hosted on Amazon RDS.

## Features
- View tasks retrieved from the backend server.
- Create tasks with a title, description, and assigned user.
- Select a user from the available users.
- Update task statuses.
- Store task information in a MySQL database.
- Retrieve user and task data through HTTP API endpoints.
- Display errors messages.

## Technology Used (Technology Stack)

- **Frontend:** 
  - Kotlin
  - Jetpack Compose
- **Backend:** 
  - Java HTTP server
- **Database:** 
  - MySQL 8.4 hosted on Amazon RDS
- **Communication:** 
  - HTTP
  - JSON
- **Development tools:** 
  - Android Studio (tested with Medium Phone API 35)
  - JDK 17
  - Git

## Configuration

### Backend configuration

The Java server listens on port `8080` and connects to the MySQL database.

Please use environment variables when using your database credentials

### Android configuration

The Android application uses the following base URL during local emulator development:

http://10.0.2.2:8080

The address `10.0.2.2` allows the Android emulator to reach the backend running on the host computer.

The application requires Internet permission and currently permits HTTP traffic for local development. Production deployments should use HTTPS and an appropriate network security configuration.

## Setup and Installation

### Prerequisites

Before running this application, install or configure the following:
- Android Studio
- An Android emulator or compatible Android device
- CSS
- HTML
- Java 17
- Java HTTP Server
- JavaScript
- JDBC
- JDK 17
- JUnit 5
- Maven 3.9.16
- MySQL 8.4
- Visual Studio Code

Some optional tools for testing:
- MySQL Workbench to observe database

The backend server and Android application must both be running for the application to retrieve and save data.

## Starting the Application

Before starting the Android application, start the backend (task-management-system) first.

### Start the Java backend server (Using Visual Studio Code)
1. Open task-management-system on Visual Studio Code
2. Create new terminal
3. In the terminal enter credentials into environment variables (run one line at a time):
```text
$env:DB_USERNAME="YOUR_USERNAME"
$env:DB_URL="YOUR_URL"
$env:DB_PASSWORD="YOUR_PASSWORD"
```
4. Clean the project by running:
```text
mvn clean package
```

5. Setup the database by running:
```text
mvn exec:java "-Dexec.mainClass=com.comp713.Database.DatabaseSetup"
```

6. Run the server by running:
```text
mvn exec:java "-Dexec.mainClass=com.comp713.Server"
```

### Launch the Android Application (Android Studio)
1. Open TaskManagementApp on Android Studio
2. Run the project

### Address and Ports Used
Here are the addresses and ports used by components of the app

| Component                   | Address or port                              |
|-----------------------------|----------------------------------------------|
| Java backend                | `http://localhost:8080` on the host computer |
| Android emulator to backend | `http://10.0.2.2:8080`                       |
| Task API                    | `http://localhost:8080/api/tasks`            |
| User API                    | `http://localhost:8080/api/users`            |
| MySQL                       | Port `3306` by default                       |

The backend must remain running while using the Android application.

## API Endpoints
The backend exposes the following endpoints:

| Method | Endpoint                       | Purpose                            |
|--------|--------------------------------|------------------------------------|
| GET    | `/api/tasks`                   | Retrieve all tasks                 |
| GET    | `/api/tasks/by-user?user_id=1` | Retrieve tasks for a specific user |
| POST   | `/api/tasks/create`            | Create a task                      |
| PUT    | `/api/tasks/update`            | Update task details and status     |
| DELETE | `/api/tasks/delete`            | Delete a task                      |
| GET    | `/api/users`                   | Retrieve all users                 |
| POST   | `/api/users/create`            | Create a user                      |

## Test Accounts and Sample Data

### Sample user
| Field                      | Value          |
|----------------------------|----------------|
| User ID (Auto-incremented) | 1              |
| Username                   | John           |
| Email                      | John@email.com |

This is a sample application data, not a login account. The current application does not implement authentication.

### Sample task

| Field                 | Value                             |
|-----------------------|-----------------------------------|
| Title                 | `Finish COMP713 assignment`       |
| Description           | `Test the Android app connection` |
| User ID (foreign key) | 1                                 |
| Initial status        | TODO (default)                    |

Use the form within the app to create new tasks

## Testing

### 1. Verify the backend

With the backend running, open the following URLs in a browser on the host computer to see created users and tasks:

```text
http://localhost:8080/api/tasks
http://localhost:8080/api/users
```

#### Using MySQL Queries
You can also view directly from database using MySQL queries.
For tasks table:
```text
SELECT * FROM tasks;
```

For users table:
```text
SELECT * FROM users;
```

### 2. Testing the Android application

This application should capable with the following:
- The application opens successfully
- Existing tasks are loaded from the backend
- The user dropdown displays available users
- A new task can be created
- The new task appears in the task list
- A task status can be changed to `TODO`, `IN_PROGRESS`, or `DONE`
- The updated status remains correct after refreshing the task list
- Changes are persisted in the MySQL database

## Application Workflow

1. Start the Java backend server (task-management-system).
2. Ensure the MySQL database is accessible.
3. Launch the Android application.
4. The app requests the task list and available users from the backend.
5. Select an user and enter the task title and description.
6. Submit the form to create a task.
7. Select a task's status to change it.
8. The app sends the update to the backend.
9. The backend updates the database, and the app reloads the task list to display the latest data.

## Known Limitations

- The backend must be running and reachable for server-backed features to work.
- The application uses a fixed development base URL (http://10.0.2.2:8080) that may need to change for other environments.
- HTTP cleartext traffic is enabled for local development; If the application were to be published, HTTPS should be configured.
- Authentication and authorisation are not currently implemented.
- Error handling and offline support are limited.
- The backend's JSON parsing implementation may have limitations when handling unexpected input.
- API requests and database access depend on the availability and configuration of the Amazon RDS instance. If the database is unavailable or the backend cannot connect to it, task and user data may not load or save successfully.
- 
- 