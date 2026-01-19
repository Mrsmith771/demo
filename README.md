# AdBlocker API Documentation

Base URL: `http://localhost:8080`

## Authentication Endpoints

### 1. Register User
**POST** `/users/register`

Creates a new user account.

**Request Body:**
```json
{
  "username": "string (required, not blank)",
  "email": "string (required, valid email)",
  "password": "string (required, not blank)"
}
```

**Response Codes:**
- `200 OK` - User created successfully
- `400 Bad Request` - Validation errors (invalid email, missing fields)
- `409 Conflict` - User with this email already exists
- `415 Unsupported Media Type` - Content-Type is not application/json

**Success Response:**
```json
{
  "message": "User created successfully",
  "userId": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

**Error Response:**
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Email is not valid",
  "timestamp": "2025-01-08T12:34:56",
  "path": "/users/register"
}
```

---

### 2. Login User
**POST** `/users/login`

Authenticates a user with email and password.

**Request Body:**
```json
{
  "email": "string (required, valid email)",
  "password": "string (required, not blank)"
}
```

**Request Headers:**
- `Content-Type: application/json`
- `User-Agent: string` (tracked for security)

**Response Codes:**
- `200 OK` - Authentication successful
- `401 Unauthorized` - Invalid credentials
- `400 Bad Request` - Validation errors

**Success Response:**
```json
{
  "message": "Login successful",
  "email": "john@example.com",
  "token": "generated-auth-token"
}
```

---

## User Management Endpoints

### 3. Get User Profile
**GET** `/users/profile`

Returns the authenticated user's profile.

**Request Headers:**
- `Authorization: Bearer {token}` (required)
- `User-Agent: string`

**Response Codes:**
- `200 OK` - Profile retrieved
- `401 Unauthorized` - Not authenticated

**Success Response:**
```json
{
  "email": "john@example.com",
  "username": "john_doe",
  "createdAt": "2025-01-08T10:00:00"
}
```

---

### 4. Get All Users
**GET** `/users`

Returns a list of all users (admin only).

**Query Parameters:**
- `page` (optional, integer, default: 0)
- `size` (optional, integer, default: 10)

**Response Codes:**
- `200 OK` - Users list retrieved
- `403 Forbidden` - Not authorized

**Success Response:**
```json
{
  "users": [
    {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com"
    }
  ],
  "total": 1
}
```

---

### 5. Update User
**PUT** `/users/{id}`

Updates user information.

**Path Parameters:**
- `id` (required, Long) - User ID

**Request Body:**
```json
{
  "username": "string (optional)",
  "email": "string (optional, valid email)",
  "password": "string (optional)"
}
```

**Response Codes:**
- `200 OK` - User updated
- `404 Not Found` - User not found
- `400 Bad Request` - Validation errors

**Success Response:**
```json
{
  "message": "User updated successfully",
  "userId": 1
}
```

---

### 6. Partial Update User
**PATCH** `/users/{id}`

Partially updates user information.

**Path Parameters:**
- `id` (required, Long) - User ID

**Request Body:**
```json
{
  "username": "new_username"
}
```

**Response Codes:**
- `200 OK` - User updated
- `404 Not Found` - User not found
- `400 Bad Request` - Validation errors

---

### 7. Delete User
**DELETE** `/users/{id}`

Deletes a user account.

**Path Parameters:**
- `id` (required, Long) - User ID

**Response Codes:**
- `200 OK` - User deleted
- `404 Not Found` - User not found
- `403 Forbidden` - Not authorized

**Success Response:**
```json
{
  "message": "User deleted successfully",
  "userId": 1
}
```

---

## Test Endpoints

### 8. Hello Endpoint
**GET** `/hello`

Simple test endpoint.

**Response Codes:**
- `200 OK` - Always returns greeting

**Success Response:**
```json
{
  "message": "Hello, user!"
}
```

---

## Error Response Format

All errors follow this structure:

```json
{
  "status": 400,
  "error": "Error Type",
  "message": "Detailed error message",
  "timestamp": "ISO-8601 timestamp",
  "path": "/request/path",
  "validationErrors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ]
}
```

---

## HTTP Status Codes Used

- `200 OK` - Request successful
- `400 Bad Request` - Validation error or malformed request
- `401 Unauthorized` - Authentication required or failed
- `403 Forbidden` - Authenticated but not authorized
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., duplicate email)
- `415 Unsupported Media Type` - Wrong Content-Type
- `500 Internal Server Error` - Server error

---

## Request Headers

### Required Headers
- `Content-Type: application/json` - For POST/PUT/PATCH requests

### Optional Headers
- `User-Agent: string` - Client identification (tracked for security)
- `Authorization: Bearer {token}` - For authenticated endpoints

---

## Examples

### cURL Examples

**Register:**
```bash
curl -X POST http://localhost:8080/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"pass123"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -H "User-Agent: curl/7.68.0" \
  -d '{"email":"john@example.com","password":"pass123"}'
```

**Get Profile:**
```bash
curl -X GET http://localhost:8080/users/profile \
  -H "Authorization: Bearer your-token-here"
```