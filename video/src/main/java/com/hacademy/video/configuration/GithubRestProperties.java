package com.hacademy.video.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "custom.github")
public class GithubRestProperties {
	private String[] repositories;
	private String baseUrl;
	private String token;
	private String apiVersion;
}
