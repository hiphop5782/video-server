package com.hacademy.video.rest;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.hacademy.video.auth.AuthenticationException;
import com.hacademy.video.auth.CookieSupport;
import com.hacademy.video.auth.ViewerSessionService;
import com.hacademy.video.auth.ViewerSessionService.ViewerAccess;
import com.hacademy.video.service.GithubRestService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class StreamingRestController {
	private final GithubRestService githubRestService;
	private final ViewerSessionService viewerSessionService;

	public StreamingRestController(GithubRestService githubRestService, ViewerSessionService viewerSessionService) {
		this.githubRestService = githubRestService;
		this.viewerSessionService = viewerSessionService;
	}

	@GetMapping("/")
	public String home() {
		return "Video server is running";
	}

	@GetMapping("/api/videos")
	public ResponseEntity<String[]> videos(HttpServletRequest request) {
		ViewerAccess access = requireAccess(request);
		return ResponseEntity.ok(githubRestService.getRepositoryVideos(access.repository()));
	}

	@GetMapping("/api/videos/{video:.+}")
	public void play(
			@RequestHeader HttpHeaders headers,
			@PathVariable String video,
			HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		ViewerAccess access = requireAccess(request);
		githubRestService.streamVideo(headers, response, access.repository(), video);
	}

	private ViewerAccess requireAccess(HttpServletRequest request) {
		String token = CookieSupport.value(request, ViewerSessionService.COOKIE_NAME);
		return viewerSessionService.resolve(token)
				.orElseThrow(() -> new AuthenticationException("로그인이 필요하거나 접근 권한이 만료되었습니다."));
	}
}
