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
	public String[] getRepositories() {
		return repositories;
	}
	public void setRepositories(String[] repositories) {
		this.repositories = repositories;
	}
	public String getBaseUrl() {
		return baseUrl;
	}
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getApiVersion() {
		return apiVersion;
	}
	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}
}
