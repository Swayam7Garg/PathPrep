package com.swayam.pathprep.filter;

import com.swayam.pathprep.config.AdminProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AdminKeyFilterTest {

    private AdminKeyFilter filter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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
    void adminPath_ignoresHeaderAndPassesThrough_whenAdminApiKeyBlank() throws Exception {
        filter = new AdminKeyFilter(new AdminProperties(""), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/questions");
        request.addHeader("X-Admin-Key", "any-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // When key mode is disabled, header is ignored and chain continues to JWT authentication
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void adminPath_passesThroughToJwt_whenHeaderMissing() throws Exception {
        filter = new AdminKeyFilter(new AdminProperties("configured-secret"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/questions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // When header is absent, chain continues to JWT authentication
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void adminPath_rejects401_whenHeaderPresentAndWrong() throws Exception {
        filter = new AdminKeyFilter(new AdminProperties("configured-secret"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/questions");
        request.addHeader("X-Admin-Key", "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("ADMIN_KEY_INVALID");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void adminPath_authenticatesAsAdminKeyWithRoleAdmin_whenHeaderCorrect() throws Exception {
        filter = new AdminKeyFilter(new AdminProperties("configured-secret"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/questions");
        request.addHeader("X-Admin-Key", "configured-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("admin-key");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }
}
