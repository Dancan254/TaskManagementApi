# Task Management API with Tags and Google Calendar Sync

Setup Instructions

step-by-step guide to setting up the database (using Docker) and configuring the Google Calendar API credentials for the Task Management API.

## 1. Getting Started

### 1.1. Clone the Repository

First, clone the project from GitHub:

```bash
git clone https://github.com/Dancan254/TaskManagementApi.git
cd TaskManagementApi
```

### 1.2. Prerequisites

Ensure you have the following installed and configured:

- Docker and Docker Compose (for database setup)
- Java 25
- Maven (or Gradle, if your build system uses it)
- An IDE (IntelliJ IDEA, VS Code, or Eclipse)
- A Google Account for Google Cloud Console and Google Calendar access.

## 2. Database Setup (using Docker Compose)

The application uses PostgreSQL. We'll use Docker Compose to easily set up and run the database.

1.  **Start PostgreSQL with Docker Compose**:
    Navigate to the project root directory where `docker-compose.yml` is located and run:
    ```bash
    docker-compose up -d
    ```
    This will start a PostgreSQL container in the background. The database will be accessible at `localhost:5432`.

2.  **Verify Configuration**:
    Ensure your `src/main/resources/application.yml` has the correct settings for connecting to the Dockerized PostgreSQL (these should already be correctly configured):

    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:5432/taskdb
        username: taskuser
        password: taskpass
        # ... other configurations
    ```
> **Note:** If you already have PostgreSQL running on your machine, you have two options:
>
> **Option 1: Stop your local PostgreSQL**
> ```bash
> sudo systemctl stop postgresql
> ```
>
> **Option 2: Remap the Docker port**
> 
> Edit `docker-compose.yml` and change the port mapping:
> ```yaml
> services:
>   postgres:
>     image: postgres:16-alpine
>     ports:
>       - "5433:5432"  # Change from 5432:5432 to 5433:5432
>     # ... rest of config
> ```
>
> Then update `src/main/resources/application.yml`:
> ```yaml
> spring:
>   datasource:
>     url: jdbc:postgresql://localhost:5433/taskdb  # Change port to 5433
>     username: taskuser
>     password: taskpass
> 
## 3. Google Calendar API Setup (Service Account)

The application uses a Service Account for application-level, programmatic sync with a single dedicated calendar.

### 3.1. Create Google Cloud Project & Enable API

1.  Go to the [Google Cloud Console](https://console.cloud.google.com/).
2.  Create a **New Project** (e.g., `task-management-api-project`).
3.  Navigate to **APIs & Services** -> **Library**, search for "Google Calendar API", and click **Enable**.

### 3.2. Create and Download Service Account Key

1.  In the Google Cloud Console, go to **IAM & Admin** -> **Service Accounts**.
2.  Click **+ CREATE SERVICE ACCOUNT**.
3.  **Service Account Name**: `task-management-sync` (Note the generated email address: `task-management-sync@<project-id>.iam.gserviceaccount.com`).
4.  Follow the prompts to create the account. You can skip granting project roles.
5.  Find your service account in the list, click the three-dot menu under **Actions**, and select **Manage keys**.
6.  Click **ADD KEY** -> **Create new key**.
7.  Select **JSON** as the key type and click **CREATE**.
8.  Rename this downloaded JSON file to `service-account.json`.

## 4. Google Calendar Configuration

You must create a dedicated calendar and grant the Service Account permission to manage it.

### 4.1. Create a Dedicated Google Calendar

1.  Go to [Google Calendar](https://calendar.google.com/).
2.  Create a new calendar (e.g., `Task Management Tasks`).

### 4.2. Share Calendar with Service Account

This grants the API write permission.

1.  In Google Calendar, go to the settings for the new calendar ("**Settings and sharing**").
2.  Scroll to **Share with specific people or groups**.
3.  Click **Add people and groups**.
4.  Paste the **Service Account Email Address** (from Step 3.2).
5.  Set the **Permissions level** to **Make changes to events**.
6.  Click **Send**.

### 4.3. Retrieve the Calendar ID

While in the calendar settings, scroll to the **Integrate calendar** section.

Copy the **Calendar ID**. (It's a long email-like string).

### 4.4. Webhook Setup for Google Calendar Push Notifications

For Google Calendar to push real-time updates to your application (e.g., when an event is updated or deleted directly in Google Calendar), you need a publicly accessible URL that Google can reach. Since your application runs locally, you'll need a tool to expose your local server to the internet.

#### Using ngrok

[ngrok](https://ngrok.com/) creates a secure tunnel to your localhost. This is ideal for testing webhooks during development.

1.  **Download and Install ngrok**: Follow the instructions on the [ngrok website](https://ngrok.com/download).
2.  **Authenticate ngrok**: Obtain your authtoken from the ngrok dashboard and connect it:
    ```bash
    ngrok authtoken <YOUR_NGROK_AUTHTOKEN>
    ```
3.  **Expose your local application**: Run ngrok to tunnel traffic to your application's port (default: 8080):
    ```bash
    ngrok http 8080
    ```
    ngrok will provide a public URL (e.g., `https://<random-subdomain>.ngrok-free.app`). Copy this URL.

4.  **Configure Webhook in Google Cloud Console**:
    Once you have your ngrok URL (or any other publicly accessible URL),
    you'll need to subscribe to push notifications for your calendar.
    Google Calendar push notifications are typically set up programmatically.
    The application exposes a webhook endpoint at `/api/v1/webhooks/google-calendar-sync`.
    
    When your application starts, it will attempt to subscribe to push notifications for the configured calendar.
    Ensure the `google.calendar.webhook-url` in your `application.yml` is set to your public ngrok URL (or equivalent).

    ```yaml
    google:
      calendar:
        # ... existing configurations ...
        webhook-url: YOUR_NGROK_PUBLIC_URL/api/v1/webhooks/google-calendar-sync
    ```
    The application will handle the subscription process using this URL. You do *not* need to manually set up the webhook in the Google Cloud Console or Google Calendar UI; the application does it for you on startup.

## 5. Application Configuration

Integrate the credentials into your Spring Boot project.

### 5.1. Place Service Account File

Move the renamed `service-account.json` file into the project's resource directory: `src/main/resources/`.

### 5.2. Update application.yml

Open `src/main/resources/application.yml`.

Update the `google.calendar` section with the Calendar ID you copied:

```yaml
google:
  calendar:
    # PASTE YOUR CALENDAR ID HERE (from Step 4.3)
    calendar-id: YOUR_CALENDAR_ID_HERE@group.calendar.google.com
    application-name: task-management
    # Path should point to the file placed in Step 5.1
    key-path: src/main/resources/service-account.json
    sync-enabled: true
    # The file to store the sync token for bidirectional sync
    sync-token-path: data/sync-token.txt
```

## 6. Testing the API

### 6.1. Run the Application

Maven: Run `./mvnw spring-boot:run` in the project root.

### 6.2. Access Testing Tools

-   **Swagger UI (API Docs)**: `http://localhost:8080/swagger-ui.html`
-   **Actuator (Health Check)**: `http://localhost:8080/actuator/health`

### 6.3. Test Calendar Sync

1.  Use the `POST /api/v1/tasks` endpoint in Swagger to create a new task with a `dueDate`.
2.  Verify the new event appears in your dedicated **Task Management Tasks** Google Calendar.
3.  Use `PUT /api/v1/tasks/{id}` to update the task's status and verify the color or title reflects the change in the calendar.
4.  Use `DELETE /api/v1/tasks/{id}` and verify the calendar event is removed.

## Sample API Requests

Here are some example `curl` commands to interact with the API.

### 1. Create a Task

```bash
curl -X POST "http://localhost:8080/api/v1/tasks" \
     -H "Content-Type: application/json" \
     -d '{
           "title": "Buy groceries",
           "description": "Milk, Eggs, Bread, Butter",
           "dueDate": "2025-12-25T10:00:00",
           "tags": ["Shopping", "Home"]
         }'
```

### 2. Get All Tasks (No Filters)

```bash
curl -X GET "http://localhost:8080/api/v1/tasks" \
     -H "Accept: application/json"
```

### 3. Get Tasks by Status

```bash
curl -X GET "http://localhost:8080/api/v1/tasks/status/TODO" \
     -H "Accept: application/json"
```

### 4. Get Tasks by Tag

```bash
curl -X GET "http://localhost:8080/api/v1/tasks/tag/Shopping" \
     -H "Accept: application/json"
```

### 5. Get Tasks by Status and Tag

```bash
curl -X GET "http://localhost:8080/api/v1/tasks/filter?status=TODO&tag=Shopping" \
     -H "Accept: application/json"
```

### 6. Get Task by ID

```bash
curl -X GET "http://localhost:8080/api/v1/tasks/{id}" \
     -H "Accept: application/json"
```

### 7. Update a Task

```bash
curl -X PUT "http://localhost:8080/api/v1/tasks/{id}" \
     -H "Content-Type: application/json" \
     -d '{
           "title": "Buy Groceries and prepare dinner",
           "description": "Milk, Eggs, Bread, Butter. Also, plan dinner menu.",
           "status": "IN_PROGRESS",
           "dueDate": "2025-12-25T18:00:00",
           "tags": ["Shopping", "Home", "Cooking"]
         }'
```

### 5. Delete a Task

```bash
curl -X DELETE "http://localhost:8080/api/v1/tasks/{id}"
```

### 9. Get All Tags

```bash
curl -X GET "http://localhost:8080/api/tags" \
     -H "Accept: application/json"
```

### 10. Get Tasks by Tag Name

```bash
curl -X GET "http://localhost:8080/api/tags/{tagName}/tasks" \
     -H "Accept: application/json"
```
