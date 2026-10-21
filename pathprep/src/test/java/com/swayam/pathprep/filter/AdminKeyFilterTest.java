package com.swayam.pathprep.filter;

import com.swayam.pathprep.config.AdminProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AdminKeyFilterTest {

    private AdminKeyFilter filter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void nonAdminPath_passesThroughWithoutFilter() throws Exception {
        filter = new AdminKeyFilter(new AdminProperties("secret-key"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tracks");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void adminPath_rejects403_whenAdminApiKeyNotConfigured() throws Exception {
        filter = new AdminKeyFilter(new AdminProperties(""), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/questions");
        request.addHeader("X-Admin-Key", "any-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("ADMIN_DISABLED");
    }

    @Test
    void adminPath_rejects401_whenHeaderMissing() throws Exception {
        filter = new AdminKeyFilter(new AdminProperties("configured-secret"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/questions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("ADMIN_KEY_INVALID");
    }

    @Test
    void adminPath_rejects401_whenHeaderWrong() throws Exception {
        filter = new AdminKeyFilter(new AdminProperties("configured-secret"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/questions");
        request.addHeader("X-Admin-Key", "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("ADMIN_KEY_INVALID");
    }

    @Test
    void adminPath_proceeds_whenHeaderCorrect() throws Exception {
        filter = new AdminKeyFilter(new AdminProperties("configured-secret"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/questions");
        request.addHeader("X-Admin-Key", "configured-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
