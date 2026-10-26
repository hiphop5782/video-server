package com.hacademy.video.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.hacademy.video.access.AccessGrant;
import com.hacademy.video.access.AccessGrantService;
import com.hacademy.video.configuration.AccessSecurityProperties;
import com.hacademy.video.service.GithubRestService;

@Service
public class ViewerSessionService {
	public static final String COOKIE_NAME = "VIDEO_SESSION";

	private final AccessSecurityProperties properties;
	private final AccessGrantService accessGrantService;
	private final GithubRestService githubRestService;
	private final Map<String, ViewerSession> sessions = new ConcurrentHashMap<>();

	public ViewerSessionService(
			AccessSecurityProperties properties,
			AccessGrantService accessGrantService,
			GithubRestService githubRestService) {
		this.properties = properties;
		this.accessGrantService = accessGrantService;
		this.githubRestService = githubRestService;
	}

	public String createGithubSession(String githubUserId) {
		String repository = githubRestService.findRepository(githubUserId);
		if(repository == null) {
			throw new AuthenticationException("접근 가능한 GitHub 저장소가 없습니다.");
		}
		return create(new ViewerSession(SessionType.GITHUB, githubUserId, null, expiresAt()));
	}

	public String createAccessCodeSession(AccessGrant grant) {
		return create(new ViewerSession(SessionType.ACCESS_CODE, null, grant.getId(), expiresAt()));
	}

	public Optional<ViewerAccess> resolve(String token) {
		if(token == null || token.isBlank()) {
			return Optional.empty();
		}
		ViewerSession session = sessions.get(token);
		if(session == null || !session.expiresAt().isAfter(Instant.now())) {
			sessions.remove(token);
			return Optional.empty();
		}

		if(session.type() == SessionType.GITHUB) {
			String repository = githubRestService.findRepository(session.githubUserId());
			if(repository == null) {
				sessions.remove(token);
				return Optional.empty();
			}
			return Optional.of(new ViewerAccess(repository, "github"));
		}

		return accessGrantService.findActive(session.accessGrantId())
				.map(grant -> new ViewerAccess(grant.getRepository(), "access-code"))
				.or(() -> {
					sessions.remove(token);
					return Optional.empty();
				});
	}

	public void logout(String token) {
		if(token != null) {
			sessions.remove(token);
		}
	}

	public Duration sessionDuration() {
		return Duration.ofHours(Math.max(1, properties.getViewerSessionHours()));
	}

	private String create(ViewerSession session) {
		String token = UUID.randomUUID().toString();
		sessions.put(token, session);
		return token;
	}

	private Instant expiresAt() {
		return Instant.now().plus(sessionDuration());
	}

	private enum SessionType {
		GITHUB,
		ACCESS_CODE
	}

	private record ViewerSession(SessionType type, String githubUserId, Long accessGrantId, Instant expiresAt) {
	}

	public record ViewerAccess(String repository, String authenticationType) {
	}
}
