package com.hacademy.video.auth;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hacademy.video.configuration.FirebaseProperties;

@Service
public class FirebaseAuthService {
	private final FirebaseProperties properties;
	private final WebClient webClient;

	public FirebaseAuthService(FirebaseProperties properties) {
		this.properties = properties;
		this.webClient = WebClient.builder().baseUrl(properties.getBaseUrl()).build();
	}

	public String verifyGithubUser(String authorizationHeader) {
		String idToken = bearerToken(authorizationHeader);
		try {
			FirebaseLookupResponse response = webClient.post()
					.uri(uriBuilder -> uriBuilder
							.path("/v1/accounts:lookup")
							.queryParam("key", properties.getApiKey())
							.build())
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(Map.of("idToken", idToken))
					.retrieve()
					.bodyToMono(FirebaseLookupResponse.class)
					.block();

			if(response == null || response.users() == null || response.users().isEmpty()) {
				throw new AuthenticationException("유효하지 않은 Firebase 사용자입니다.");
			}
			return response.users().get(0).providerUserInfo().stream()
					.filter(provider -> "github.com".equals(provider.providerId()))
					.map(ProviderUserInfo::rawId)
					.filter(id -> id != null && !id.isBlank())
					.findFirst()
					.orElseThrow(() -> new AuthenticationException("GitHub 로그인 정보가 없습니다."));
		}
		catch(WebClientResponseException e) {
			throw new AuthenticationException("Firebase 인증 토큰이 유효하지 않습니다.");
		}
	}

	private String bearerToken(String header) {
		if(header == null || !header.startsWith("Bearer ") || header.length() <= 7) {
			throw new AuthenticationException("Firebase 인증 토큰이 필요합니다.");
		}
		return header.substring(7).trim();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record FirebaseLookupResponse(List<FirebaseUser> users) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record FirebaseUser(List<ProviderUserInfo> providerUserInfo) {
		private FirebaseUser {
			providerUserInfo = providerUserInfo == null ? List.of() : providerUserInfo;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ProviderUserInfo(String providerId, String rawId) {
	}
}
