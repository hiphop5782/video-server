package com.hacademy.video.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hacademy.video.access.AccessGrant;
import com.hacademy.video.access.AccessGrantService;
import com.hacademy.video.auth.CookieSupport;
import com.hacademy.video.auth.FirebaseAuthService;
import com.hacademy.video.auth.ViewerSessionService;
import com.hacademy.video.configuration.AccessSecurityProperties;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class AuthenticationRestController {
	private final FirebaseAuthService firebaseAuthService;
	private final AccessGrantService accessGrantService;
	private final ViewerSessionService viewerSessionService;
	private final AccessSecurityProperties securityProperties;

	public AuthenticationRestController(
			FirebaseAuthService firebaseAuthService,
			AccessGrantService accessGrantService,
			ViewerSessionService viewerSessionService,
			AccessSecurityProperties securityProperties) {
		this.firebaseAuthService = firebaseAuthService;
		this.accessGrantService = accessGrantService;
		this.viewerSessionService = viewerSessionService;
		this.securityProperties = securityProperties;
	}

	@PostMapping("/auth/github")
	public ResponseEntity<SessionResponse> githubLogin(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
		String githubUserId = firebaseAuthService.verifyGithubUser(authorization);
		String token = viewerSessionService.createGithubSession(githubUserId);
		return viewerSessionResponse(token, "github");
	}

	@PostMapping("/access/login")
	public ResponseEntity<SessionResponse> accessCodeLogin(@RequestBody AccessCodeRequest request) {
		AccessGrant grant = accessGrantService.authenticate(request.code())
				.orElseThrow(() -> new com.hacademy.video.auth.AuthenticationException("접근 코드가 올바르지 않거나 만료되었습니다."));
		String token = viewerSessionService.createAccessCodeSession(grant);
		return viewerSessionResponse(token, "access-code");
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		viewerSessionService.logout(CookieSupport.value(request, ViewerSessionService.COOKIE_NAME));
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, CookieSupport.expiredCookie(
						ViewerSessionService.COOKIE_NAME, securityProperties).toString())
				.build();
	}

	private ResponseEntity<SessionResponse> viewerSessionResponse(String token, String type) {
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, CookieSupport.sessionCookie(
						ViewerSessionService.COOKIE_NAME,
						token,
						viewerSessionService.sessionDuration(),
						securityProperties).toString())
				.body(new SessionResponse(type));
	}

	public record AccessCodeRequest(String code) {
	}

	public record SessionResponse(String authenticationType) {
	}
}
