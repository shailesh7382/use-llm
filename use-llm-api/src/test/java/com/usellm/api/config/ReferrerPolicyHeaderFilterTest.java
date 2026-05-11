package com.usellm.api.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ReferrerPolicyHeaderFilterTest {

    private final ReferrerPolicyHeaderFilter filter = new ReferrerPolicyHeaderFilter();

    @Test
    void setsNoReferrerPolicyHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(ReferrerPolicyHeaderFilter.REFERRER_POLICY_HEADER))
                .isEqualTo(ReferrerPolicyHeaderFilter.REFERRER_POLICY_VALUE);
    }
}

