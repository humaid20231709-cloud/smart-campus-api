package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {
        Map<String, Object> info = new HashMap<>();
        info.put("version", "1.0");
        info.put("name", "Smart Campus API");
        info.put("contact", "admin@westminster.ac.uk");

        Map<String, String> links = new HashMap<>();
        links.put("rooms", "http://localhost:8080/api/v1/rooms");
        links.put("sensors", "http://localhost:8080/api/v1/sensors");
        info.put("resources", links);

        return Response.ok(info).build();
    }
}