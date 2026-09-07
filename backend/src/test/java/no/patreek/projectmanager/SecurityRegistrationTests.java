package no.patreek.projectmanager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "app.security.enabled=true",
    "app.security.username=admin",
    "app.security.password=admin-password"
})
class SecurityRegistrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void usersCanRegisterWithoutExistingCredentials() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "new-user",
                      "email": "new-user@example.com",
                      "password": "long-password"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("new-user"))
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void otherApiRequestsStillRequireCredentialsWithoutBrowserPopupChallenge() throws Exception {
        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void registeredUsersOnlySeeTheirOwnTasks() throws Exception {
        register("alice", "alice@example.com", "alice-password");
        register("bob", "bob@example.com", "bob-password");

        mockMvc.perform(post("/api/tasks")
                .header("Authorization", basicAuth("alice", "alice-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Alice private task",
                      "description": "Only Alice should see this"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tasks")
                .header("Authorization", basicAuth("bob", "bob-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Bob private task",
                      "description": "Only Bob should see this"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks")
                .header("Authorization", basicAuth("alice", "alice-password")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Alice private task"));

        mockMvc.perform(get("/api/tasks")
                .header("Authorization", basicAuth("bob", "bob-password")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Bob private task"));
    }

    private void register(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(username, email, password)))
            .andExpect(status().isCreated());
    }

    private static String basicAuth(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
