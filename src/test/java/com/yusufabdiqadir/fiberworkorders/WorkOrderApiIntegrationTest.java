package com.yusufabdiqadir.fiberworkorders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Drives the real HTTP layer, service and H2 database end to end. */
@SpringBootTest
@AutoConfigureMockMvc
class WorkOrderApiIntegrationTest {

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Autowired
    private MockMvc mvc;

    @Test
    void fullWorkOrderLifecycle() throws Exception {
        long technicianId = createTechnician("Sam Tech", "sam.lifecycle@example.com");
        long orderId = createWorkOrder("Jane Doe");

        mvc.perform(post("/api/work-orders/{id}/assign", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianId\": " + technicianId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.technician.name").value("Sam Tech"));

        mvc.perform(post("/api/work-orders/{id}/start", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mvc.perform(post("/api/work-orders/{id}/readings", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\": \"ONT\", \"receivedPowerDbm\": -19.4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passing").value(true));

        mvc.perform(post("/api/work-orders/{id}/complete", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.readings.length()").value(1));

        mvc.perform(get("/api/work-orders").param("status", "COMPLETED").param("technicianId", String.valueOf(technicianId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customerName").value("Jane Doe"));
    }

    @Test
    void completingWithFailingReadingReturnsConflict() throws Exception {
        long technicianId = createTechnician("Alex Tech", "alex.conflict@example.com");
        long orderId = createWorkOrder("John Roe");
        mvc.perform(post("/api/work-orders/{id}/assign", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"technicianId\": " + technicianId + "}"));
        mvc.perform(post("/api/work-orders/{id}/start", orderId));
        mvc.perform(post("/api/work-orders/{id}/readings", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\": \"Drop\", \"receivedPowerDbm\": -32.0}"))
                .andExpect(jsonPath("$.passing").value(false));

        mvc.perform(post("/api/work-orders/{id}/complete", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid state"));
    }

    @Test
    void invalidRequestReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\": \"\", \"address\": \"1 Main\", \"serviceType\": \"RESIDENTIAL\", \"scheduledDate\": \"2000-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerName").exists())
                .andExpect(jsonPath("$.errors.scheduledDate").exists());
    }

    @Test
    void unknownWorkOrderReturnsNotFound() throws Exception {
        mvc.perform(get("/api/work-orders/{id}", 987654))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Work order 987654 was not found"));
    }

    @Test
    void duplicateTechnicianEmailReturnsConflict() throws Exception {
        createTechnician("First", "dup@example.com");

        mvc.perform(post("/api/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Second\", \"email\": \"DUP@example.com\"}"))
                .andExpect(status().isConflict());
    }

    private long createTechnician(String name, String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\", \"email\": \"" + email + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        return idFrom(result);
    }

    private long createWorkOrder(String customer) throws Exception {
        String body = "{\"customerName\": \"" + customer + "\", \"address\": \"123 Main St, Minneapolis, MN\","
                + " \"serviceType\": \"RESIDENTIAL\", \"scheduledDate\": \"" + LocalDate.now().plusDays(3) + "\"}";
        MvcResult result = mvc.perform(post("/api/work-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        return idFrom(result);
    }

    private static long idFrom(MvcResult result) throws Exception {
        Matcher matcher = ID.matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new IllegalStateException("No id in response");
        }
        return Long.parseLong(matcher.group(1));
    }
}
