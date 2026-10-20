package com.dvp.taskmanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static class Auth {
        private final String token;
        private final long userId;

        Auth(String token, long userId) {
            this.token = token;
            this.userId = userId;
        }

        String getToken() {
            return token;
        }

        long getUserId() {
            return userId;
        }
    }

    private Auth registerAndLogin(String username, String email) throws Exception {
        MvcResult register = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(String.format(
                "{\"username\": \"%s\", \"email\": \"%s\", \"password\": \"secret123\"}",
                username, email)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value(username))
            .andReturn();

        long userId = objectMapper.readTree(register.getResponse().getContentAsString()).get("id").asLong();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(String.format(
                    "{\"username\": \"%s\", \"password\": \"secret123\"}",
                    username)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn();

        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        return new Auth(body.get("token").asText(), userId);
    }

    private long createTask(String token, String title) throws Exception {
        return createTask(token, title, null);
    }

    private long createTask(String token, String title, Long assigneeId) throws Exception {
        String body = ("{\"title\": \"%s\", \"description\": \"Integration test task\", \"status\": \"TODO\", " +
                "\"priority\": \"HIGH\"}")
                .replace("%s", title);
        if (assigneeId != null) {
            body = body.replace("\"priority\": \"HIGH\"",
                    "\"priority\": \"HIGH\", \"assigneeId\": " + assigneeId);
        }
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void protectedEndpoint_shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void registerLoginCreateAndFilter_shouldWork() throws Exception {
        Auth alice = registerAndLogin("alice", "alice@example.com");
        createTask(alice.getToken(), "Первый таск");
        createTask(alice.getToken(), "Второй таск", alice.getUserId());

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + alice.getToken())
                        .param("status", "TODO")
                        .param("authorId", String.valueOf(alice.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + alice.getToken())
                        .param("assigneeId", String.valueOf(alice.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void taskUpdate_byNonAuthor_shouldReturnForbidden() throws Exception {
        Auth owner = registerAndLogin("owner", "owner@example.com");
        Auth intruder = registerAndLogin("intruder", "intruder@example.com");
        long taskId = createTask(owner.getToken(), "Секретная задача");

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + intruder.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Взломанная задача\", \"status\": \"DONE\", \"priority\": \"LOW\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + intruder.getToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + owner.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Секретная задача"));
    }

    @Test
    void taskUpdate_byAuthor_shouldWorkAndReturnNoContentOnDelete() throws Exception {
        Auth creator = registerAndLogin("creator", "creator@example.com");
        long taskId = createTask(creator.getToken(), "Исходная задача");
        long assigneeId = objectMapper.readTree(
                        mockMvc.perform(get("/api/tasks/{id}", taskId)
                                        .header("Authorization", "Bearer " + creator.getToken()))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString())
                .get("author").get("id").asLong();

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + creator.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"title\": \"Обновлённая задача\", \"description\": \"updated\", \"status\": \"IN_PROGRESS\", \"priority\": \"MEDIUM\", \"assigneeId\": %d}",
                                assigneeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + creator.getToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + creator.getToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void login_withExpiredOrInvalidToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(containsString("Unauthorized")));
    }
}
