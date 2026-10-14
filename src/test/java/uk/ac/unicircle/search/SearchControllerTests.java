package uk.ac.unicircle.search;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class SearchControllerTests {
    @Autowired MockMvc mockMvc;

    @Test
    void searchRequiresAnAuthenticatedCampusIdentity() throws Exception {
        mockMvc.perform(get("/api/search").param("query", "library"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void campusSearchReturnsOnlySupportedPermissionAwareDestinations() throws Exception {
        mockMvc.perform(get("/api/search").param("query", "library").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type", hasItem("SERVICE")))
                .andExpect(jsonPath("$[*].destination", hasItem("#services-page")));
    }

    @Test
    void shortQueriesDoNotLeakDirectoryRecords() throws Exception {
        mockMvc.perform(get("/api/search").param("query", "m").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
