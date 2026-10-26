package com.hacademy.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacademy.video.access.AccessGrant;
import com.hacademy.video.access.AccessGrantService;
import com.hacademy.video.auth.AdminSessionService;
import com.hacademy.video.auth.ViewerSessionService;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:video-test;DB_CLOSE_DELAY=-1",
		"custom.github.enabled=false",
		"custom.file.path=target/test-videos",
		"custom.security.admin-password=test-admin",
		"custom.security.cookie-secure=false"
})
class VideoApplicationTests {
	@Autowired
	private AccessGrantService accessGrantService;
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contextLoads() {
		AccessGrant grant = accessGrantService.create(
				"sample-code",
				"kh14",
				"test",
				Instant.now().plusSeconds(3600));

		assertThat(accessGrantService.authenticate("sample-code"))
				.map(AccessGrant::getId)
				.contains(grant.getId());
		assertThat(accessGrantService.delete(grant.getId())).isTrue();
		assertThat(accessGrantService.findActive(grant.getId())).isEmpty();
	}

	@Test
	void deletedGrantImmediatelyInvalidatesViewerSession() throws Exception {
		MockCookie adminCookie = (MockCookie) mockMvc.perform(post("/api/admin/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of("password", "test-admin"))))
				.andExpect(status().isNoContent())
				.andReturn().getResponse().getCookie(AdminSessionService.COOKIE_NAME);
		assertThat(adminCookie).isNotNull();

		String createResponse = mockMvc.perform(post("/api/admin/grants")
					.cookie(adminCookie)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of(
							"code", "temporary-code",
							"repository", "kh14",
							"description", "integration test"))))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Long grantId = objectMapper.readTree(createResponse).get("id").asLong();

		MockCookie viewerCookie = (MockCookie) mockMvc.perform(post("/api/access/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of("code", "temporary-code"))))
				.andExpect(status().isOk())
				.andReturn().getResponse().getCookie(ViewerSessionService.COOKIE_NAME);
		assertThat(viewerCookie).isNotNull();

		mockMvc.perform(get("/api/videos").cookie(viewerCookie))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/admin/grants/{id}", grantId).cookie(adminCookie))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/videos").cookie(viewerCookie))
				.andExpect(status().isUnauthorized());
	}
}
