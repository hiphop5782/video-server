package com.hacademy.video.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.hacademy.video.configuration.AccessSecurityProperties;

@Service
public class AdminSessionService {
	public static final String COOKIE_NAME = "VIDEO_ADMIN_SESSION";

	private final AccessSecurityProperties properties;
	private final Map<String, Instant> sessions = new ConcurrentHashMap<>();

	public AdminSessionService(AccessSecurityProperties properties) {
		this.properties = properties;
	}

	public String login(String password) {
		String configured = properties.getAdminPassword();
		if(configured == null || configured.isBlank()) {
			throw new AuthenticationException("관리자 비밀번호가 서버에 설정되지 않았습니다.");
		}
		byte[] actual = password == null ? new byte[0] : password.getBytes(StandardCharsets.UTF_8);
		byte[] expected = configured.getBytes(StandardCharsets.UTF_8);
		if(!MessageDigest.isEqual(actual, expected)) {
			throw new AuthenticationException("관리자 비밀번호가 올바르지 않습니다.");
		}
		String token = UUID.randomUUID().toString();
		sessions.put(token, Instant.now().plus(sessionDuration()));
		return token;
	}

	public boolean isAuthenticated(String token) {
		if(token == null || token.isBlank()) {
			return false;
		}
		Instant expiresAt = sessions.get(token);
		if(expiresAt == null || !expiresAt.isAfter(Instant.now())) {
			sessions.remove(token);
			return false;
		}
		return true;
	}

	public void logout(String token) {
		if(token != null) {
			sessions.remove(token);
		}
	}

	public Duration sessionDuration() {
		return Duration.ofHours(Math.max(1, properties.getAdminSessionHours()));
	}
}
