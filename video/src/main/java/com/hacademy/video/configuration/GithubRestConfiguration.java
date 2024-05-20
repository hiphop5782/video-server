package com.hacademy.video.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GithubRestConfiguration {
	
	@Autowired
	private GithubRestProperties props;
	
	@Bean
	public WebClient webClient() {
//		System.out.println(props);
//		System.out.println(props.getToken());
		return WebClient.builder()
						.baseUrl(props.getBaseUrl())
						.defaultHeader("Authorization", "Bearer "+props.getToken())
						.defaultHeader("X-GitHub-Api-Version", props.getApiVersion())
						.defaultHeader("Accept", "application/vnd.github+json")
						.build();
	}
	
}
