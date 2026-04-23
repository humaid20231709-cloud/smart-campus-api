# Name - Humaid Rifai
# ID - 20231709


# Smart Campus API

A RESTful API for managing university rooms and sensors, built with JAX-RS (Jersey) and Grizzly HTTP server.

This project uses only JAX-RS for the API layer and stores data in memory using Java collections, as required by the coursework brief.

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

By default, JAX-RS creates a new resource object for each incoming request. In other words, resource classes are usually request-scoped rather than singletons. That is helpful because it avoids accidental shared state inside the resource class itself. The downside is that any data stored directly in a resource field would be lost after the request finishes.

Because of that, I used a separate singleton `DataStore` to hold the in-memory data for rooms, sensors, and readings. I also used `ConcurrentHashMap` so multiple requests can safely access and update shared data at the same time. If I had used normal non-thread-safe collections for shared state, concurrent requests could cause race conditions or inconsistent data.

**Q: Why is HATEOAS considered a hallmark of advanced RESTful design? How does it benefit client developers?**

HATEOAS (Hypermedia as the Engine of Application State) means that the server includes navigation information in responses so the client can discover available resources and actions instead of hardcoding every URL. In this project, the discovery endpoint gives the client the main collection links such as `/rooms` and `/sensors`.

This is useful because it makes the API easier to explore and reduces the client’s dependency on fixed documentation. If the structure of the API changes later, the client can still follow links provided by the server. That is why HATEOAS is often seen as a more advanced REST idea: the API becomes more self-describing and easier to navigate.

---

### Part 2 — Room Management

**Q: When returning a list of rooms, what are the implications of returning only IDs versus returning full room objects?**

Returning only IDs saves bandwidth because the response is much smaller. However, it also means the client has to make more requests to get the actual room details, which increases latency and adds extra work on the client side.

Returning full room objects is easier for the client because all the main information comes back in one response, but the payload is larger. In a large campus system with many rooms, that could become inefficient. A sensible compromise would be to return summary room data such as `id`, `name`, and `capacity`, while avoiding unnecessary nested details.

**Q: Is the DELETE operation idempotent in your implementation?**

Yes, the DELETE behaviour is still idempotent in terms of server state. If the client deletes a room that exists and has no sensors, the room is removed and the API returns `204 No Content`. If the exact same request is sent again, the room is already gone, so the API returns `404 Not Found`.

Even though the response code is different on the second request, the important point is that the server state does not change after the first successful delete. The room remains deleted, so the operation is still idempotent.

---

### Part 3 — Sensor Operations & Linking

**Q: Explain the technical consequences if a client sends data in a format other than JSON to a @Consumes(APPLICATION_JSON) endpoint.**

If a client sends something like `text/plain` or `application/xml` to an endpoint marked with `@Consumes(MediaType.APPLICATION_JSON)`, JAX-RS will reject the request before it reaches the resource method. In that case, the framework normally returns `415 Unsupported Media Type`.

This is useful because the client gets a clear message that the endpoint only accepts JSON. It also prevents the application from trying to parse data in a format it does not support.

**Q: Why is @QueryParam considered superior to path-based filtering like /sensors/type/CO2?**

`@QueryParam` is a better fit because filtering is optional. If the client sends `/sensors`, it gets all sensors. If it sends `/sensors?type=CO2`, it gets only matching sensors. That makes query parameters a natural choice for search and filtering.

Using a path like `/sensors/type/CO2` makes the filter look like part of the resource hierarchy, even though it is really just changing how the collection is retrieved. Query parameters are also easier to extend later if more filters are needed, such as `/sensors?type=CO2&status=ACTIVE`.

---

### Part 4 — Sub-Resources

**Q: Discuss the architectural benefits of the Sub-Resource Locator pattern.**

The Sub-Resource Locator pattern helps organise nested endpoints more cleanly. Instead of putting all the logic for sensors and sensor readings into one large class, the reading-related logic can be moved into a dedicated `SensorReadingResource` class.

This improves readability and makes the code easier to maintain because each class has a clearer responsibility. In bigger APIs, that separation becomes even more useful because it reduces clutter and makes it easier to extend individual parts of the system without turning one resource class into a huge controller.

---

### Part 5 — Error Handling & Logging

**Q: Why is HTTP 422 more semantically accurate than 404 when a roomId reference is missing inside a valid JSON payload?**

HTTP `404 Not Found` usually means the requested URL itself does not exist. In this case, the URL `/api/v1/sensors` is valid and the JSON structure can also be valid. The actual problem is that the `roomId` inside the payload refers to a room that is not in the system.

That is why `422 Unprocessable Entity` is more accurate. It tells the client that the server understood the request, but could not process it because the data inside the request was semantically wrong.

**Q: From a cybersecurity standpoint, explain the risks of exposing Java stack traces to external API consumers.**

Exposing stack traces to outside users is risky because it can reveal internal details about the application. For example, it may show package names, class names, file paths, framework details, and other information that could help an attacker understand how the system is built.

That kind of information can make targeted attacks easier, especially if it exposes known libraries or internal structure. For that reason, the API uses a global exception mapper to return a generic `500 Internal Server Error` message instead of exposing raw Java errors to the client.

**Q: Why is it better to use JAX-RS filters for logging rather than inserting Logger.info() in every resource method?**

Using JAX-RS filters is better because logging is a cross-cutting concern. It applies to every request and response, so it makes sense to handle it in one central place instead of repeating logging statements in every resource method.

If `Logger.info()` was written manually inside each method, the code would become repetitive and harder to maintain. Filters keep the logging consistent, reduce duplication, and let the resource methods stay focused on the actual business logic.

---

## Error Handling Summary

The API includes custom exception mapping for the main error scenarios required by the coursework:
- `409 Conflict` when trying to delete a room that still has sensors
- `422 Unprocessable Entity` when a sensor references a room that does not exist
- `403 Forbidden` when a reading is posted to a sensor in `MAINTENANCE`
- `500 Internal Server Error` through a global exception mapper for unexpected failures

---

## Technology Stack
- **Java 11**
- **JAX-RS (Jakarta RESTful Web Services)**
- **Jersey 3.1.3** (JAX-RS implementation)
- **Grizzly HTTP Server** (embedded server)
- **Jackson** (JSON serialisation)
- **Maven** (build tool)
- **In-memory storage** (ConcurrentHashMap)
