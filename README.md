# Smart Campus API

A RESTful API for managing university rooms and sensors, built with JAX-RS (Jersey) and Grizzly HTTP server.

---

## API Overview

This API provides endpoints to manage:
- **Rooms** — university rooms with capacity and sensor tracking
- **Sensors** — CO2, temperature, occupancy sensors linked to rooms
- **Sensor Readings** — historical readings for each sensor

Base URL: `http://localhost:8080/api/v1`

---

## How to Build and Run

### Prerequisites
- Java JDK 11 or 17
- Maven 3.6+

### Steps

**1. Clone the repository:**
```bash
git clone https://github.com/humaid20231709-cloud/smart-campus-api.git
cd smart-campus-api
```

**2. Build the project:**
```bash
mvn clean package
```

**3. Run the server:**
```bash
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```

**4. Server is running at:**
```
http://localhost:8080/api/v1
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1 | Discovery endpoint |
| GET | /api/v1/rooms | Get all rooms |
| POST | /api/v1/rooms | Create a room |
| GET | /api/v1/rooms/{roomId} | Get a specific room |
| DELETE | /api/v1/rooms/{roomId} | Delete a room |
| GET | /api/v1/sensors | Get all sensors |
| POST | /api/v1/sensors | Create a sensor |
| GET | /api/v1/sensors/{sensorId} | Get a specific sensor |
| GET | /api/v1/sensors?type=CO2 | Filter sensors by type |
| GET | /api/v1/sensors/{sensorId}/readings | Get all readings |
| POST | /api/v1/sensors/{sensorId}/readings | Add a reading |

---

## Sample curl Commands

**1. Discovery endpoint:**
```bash
curl -X GET http://localhost:8080/api/v1
```

**2. Create a room:**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```

**3. Create a sensor:**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'
```

**4. Post a sensor reading:**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":412.5}'
```

**5. Filter sensors by type:**
```bash
curl -X GET http://localhost:8080/api/v1/sensors?type=CO2
```

**6. Delete a room:**
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/EMPTY-101
```

---

## Report — Question Answers

### Part 1 — Service Architecture & Setup

**Q: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance created per request or is it a singleton?**

By default, JAX-RS creates a new instance of a resource class for every incoming HTTP request. This is called per-request scope. This means each request gets its own fresh object, which avoids issues with shared state between requests. However, because each instance is destroyed after the request, you cannot store data inside resource class fields — it will be lost. This is why we use a separate singleton DataStore class that uses ConcurrentHashMap to store all data. ConcurrentHashMap is thread-safe, meaning multiple requests can read and write to it simultaneously without causing data corruption or race conditions. If we had used a regular HashMap, two simultaneous requests could corrupt the data structure.

**Q: Why is HATEOAS considered a hallmark of advanced RESTful design? How does it benefit client developers?**

HATEOAS (Hypermedia as the Engine of Application State) means that API responses include links to related resources and available actions. For example, when a client fetches a room, the response includes links to its sensors. This benefits client developers in several ways. First, clients do not need to hardcode URLs — they discover them from responses, making the API self-documenting. Second, if the server changes a URL, clients automatically get the new URL from the response without needing to update their code. Third, it reduces the dependency on static documentation, which can become outdated. Compared to static documentation, HATEOAS makes the API navigable and self-describing, similar to how a website works — you follow links rather than memorising URLs.

---

### Part 2 — Room Management

**Q: When returning a list of rooms, what are the implications of returning only IDs versus returning full room objects?**

Returning only IDs uses less network bandwidth because the response payload is much smaller. However, the client must then make additional requests to fetch details for each room, which increases the number of HTTP calls and adds latency. Returning full room objects uses more bandwidth but gives the client everything it needs in a single request, reducing round trips. For a campus system with thousands of rooms, returning full objects could be slow and expensive. A good middle ground is to return a summary object with key fields like id, name, and capacity, but omit deeply nested data like the full sensor list. This balances bandwidth efficiency with client-side usability.

**Q: Is the DELETE operation idempotent in your implementation?**

The DELETE operation is largely idempotent in this implementation. If a client sends a DELETE request for a room that exists and has no sensors, the room is deleted and a 204 No Content is returned. If the same DELETE request is sent again, the room no longer exists and a 404 Not Found is returned. Although the status code changes between the first and second call, the server state remains the same — the room is gone in both cases. REST idempotency means the server state does not change on repeated calls, which holds true here. The 404 on the second call is acceptable and expected behaviour for a DELETE operation.

---

### Part 3 — Sensor Operations & Linking

**Q: Explain the technical consequences if a client sends data in a format other than JSON to a @Consumes(APPLICATION_JSON) endpoint.**

If a client sends a request with a Content-Type header of text/plain or application/xml to an endpoint annotated with @Consumes(MediaType.APPLICATION_JSON), JAX-RS will automatically reject the request before it even reaches the resource method. The framework returns an HTTP 415 Unsupported Media Type response. This is handled entirely by the JAX-RS runtime, not by application code. This is beneficial because it provides a clear and immediate error to the client, indicating that the server only accepts JSON. It also protects the application from attempting to deserialise incompatible data formats, which could cause parsing errors or unexpected behaviour.

**Q: Why is @QueryParam considered superior to path-based filtering like /sensors/type/CO2?**

Query parameters are semantically more appropriate for filtering and searching collections because they are optional by nature. Using @QueryParam("type") means the parameter can be omitted entirely, returning all sensors, or included to filter results. This gives the endpoint flexibility without requiring separate URL mappings. In contrast, embedding the filter in the path like /sensors/type/CO2 implies that type is a mandatory hierarchical resource identifier, which is semantically incorrect — it is a filter, not a resource. Path parameters should identify specific resources, while query parameters should modify how a collection is retrieved. Query parameters are also easier to combine for multiple filters, for example /sensors?type=CO2&status=ACTIVE.

---

### Part 4 — Sub-Resources

**Q: Discuss the architectural benefits of the Sub-Resource Locator pattern.**

The Sub-Resource Locator pattern improves code organisation by delegating responsibility for nested resources to dedicated classes. Instead of putting all endpoint logic in one massive resource class, each sub-resource has its own class with a single responsibility. For example, SensorReadingResource handles only reading-related operations, keeping the code clean and focused. This makes the codebase easier to maintain, test, and extend. In large APIs with many nested resources, having everything in one class would make it difficult to navigate and modify. The pattern also allows sub-resources to be independently tested and reused. It mirrors object-oriented design principles such as separation of concerns and single responsibility, which are considered industry best practices.

---

### Part 5 — Error Handling & Logging

**Q: Why is HTTP 422 more semantically accurate than 404 when a roomId reference is missing inside a valid JSON payload?**

HTTP 404 Not Found means the requested resource URL does not exist on the server. HTTP 422 Unprocessable Entity means the request was well-formed and the URL was valid, but the server could not process the instructions because of semantic errors in the payload. When a client posts a sensor with a roomId that does not exist, the URL /api/v1/sensors is perfectly valid and found by the server. The problem is inside the request body — the referenced room does not exist. Using 404 would mislead the client into thinking the endpoint itself was not found. Using 422 correctly communicates that the endpoint was found, the JSON was parsed successfully, but the content was logically invalid because it references a non-existent resource.

**Q: From a cybersecurity standpoint, explain the risks of exposing Java stack traces to external API consumers.**

Exposing stack traces to external clients is a serious security risk for several reasons. First, stack traces reveal the internal package structure and class names of the application, helping attackers understand the codebase and identify potential targets. Second, they can expose the versions of libraries and frameworks being used, allowing attackers to look up known vulnerabilities for those specific versions. Third, stack traces may reveal database queries, file paths, server configuration details, or other sensitive infrastructure information. Fourth, they make it easier for attackers to craft targeted exploits by understanding exactly where and why an error occurred. The global exception mapper in this API addresses this by catching all unexpected errors and returning a generic 500 Internal Server Error message that reveals nothing about the internal implementation.

**Q: Why is it better to use JAX-RS filters for logging rather than inserting Logger.info() in every resource method?**

Using JAX-RS filters for logging is a much better approach because it implements logging as a cross-cutting concern — something that applies to all requests and responses without modifying individual resource methods. If logging were added manually to every resource method, it would result in duplicated code across dozens of methods, making maintenance difficult. If the logging format needed to change, every method would need to be updated. Filters follow the DRY principle — Don't Repeat Yourself. They are applied automatically to every request and response by the JAX-RS runtime, ensuring consistent logging without any risk of a developer forgetting to add it to a new method. This separation of concerns keeps resource methods focused purely on business logic, making the code cleaner and easier to read.

---

## Technology Stack
- **Java 11**
- **JAX-RS (Jakarta RESTful Web Services)**
- **Jersey 3.1.3** (JAX-RS implementation)
- **Grizzly HTTP Server** (embedded server)
- **Jackson** (JSON serialisation)
- **Maven** (build tool)
- **In-memory storage** (ConcurrentHashMap)
