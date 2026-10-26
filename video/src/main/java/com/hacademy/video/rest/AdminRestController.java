package com.hacademy.video.rest;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hacademy.video.access.AccessGrant;
import com.hacademy.video.access.AccessGrantService;
import com.hacademy.video.auth.AdminSessionService;
import com.hacademy.video.auth.AuthenticationException;
import com.hacademy.video.auth.CookieSupport;
import com.hacademy.video.configuration.AccessSecurityProperties;
import com.hacademy.video.configuration.GithubRestProperties;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {
	private final AdminSessionService adminSessionService;
	private final AccessGrantService accessGrantService;
	private final AccessSecurityProperties securityProperties;
	private final GithubRestProperties githubProperties;

	public AdminRestController(
			AdminSessionService adminSessionService,
			AccessGrantService accessGrantService,
			AccessSecurityProperties securityProperties,
			GithubRestProperties githubProperties) {
		this.adminSessionService = adminSessionService;
		this.accessGrantService = accessGrantService;
		this.securityProperties = securityProperties;
		this.githubProperties = githubProperties;
	}

	@PostMapping("/login")
	public ResponseEntity<Void> login(@RequestBody AdminLoginRequest request) {
		String token = adminSessionService.login(request.password());
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, CookieSupport.sessionCookie(
						AdminSessionService.COOKIE_NAME,
						token,
						adminSessionService.sessionDuration(),
						securityProperties).toString())
				.build();
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		adminSessionService.logout(CookieSupport.value(request, AdminSessionService.COOKIE_NAME));
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, CookieSupport.expiredCookie(
						AdminSessionService.COOKIE_NAME, securityProperties).toString())
				.build();
	}

	@GetMapping("/repositories")
	public List<String> repositories(HttpServletRequest request) {
		requireAdmin(request);
		return List.copyOf(githubProperties.getRepositories());
	}

	@GetMapping("/grants")
	public List<AccessGrantResponse> grants(HttpServletRequest request) {
		requireAdmin(request);
		Instant now = Instant.now();
		return accessGrantService.findAll().stream()
				.map(grant -> AccessGrantResponse.from(grant, now))
				.toList();
	}

	@PostMapping("/grants")
	public ResponseEntity<AccessGrantResponse> create(
			@RequestBody CreateAccessGrantRequest body,
			HttpServletRequest request) {
		requireAdmin(request);
		AccessGrant grant = accessGrantService.create(
				body.code(), body.repository(), body.description(), body.expiresAt());
		return ResponseEntity.ok(AccessGrantResponse.from(grant, Instant.now()));
	}

	@DeleteMapping("/grants/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
		requireAdmin(request);
		return accessGrantService.delete(id)
				? ResponseEntity.noContent().build()
				: ResponseEntity.notFound().build();
	}

	private void requireAdmin(HttpServletRequest request) {
		String token = CookieSupport.value(request, AdminSessionService.COOKIE_NAME);
		if(!adminSessionService.isAuthenticated(token)) {
			throw new AuthenticationException("관리자 로그인이 필요합니다.");
		}
	}

	public record AdminLoginRequest(String password) {
	}

	public record CreateAccessGrantRequest(
			String code,
			String repository,
			String description,
			Instant expiresAt) {
	}

	public record AccessGrantResponse(
			Long id,
			String repository,
			String description,
			Instant createdAt,
			Instant expiresAt,
			Instant lastUsedAt,
			boolean active) {
		static AccessGrantResponse from(AccessGrant grant, Instant now) {
			return new AccessGrantResponse(
					grant.getId(),
					grant.getRepository(),
					grant.getDescription(),
					grant.getCreatedAt(),
					grant.getExpiresAt(),
					grant.getLastUsedAt(),
					grant.isActiveAt(now));
		}
	}
}
