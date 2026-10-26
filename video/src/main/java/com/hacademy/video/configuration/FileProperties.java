package com.hacademy.video.configuration;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "custom.file")
public class FileProperties {
	private String path;

	public void setPath(String path) {
		try {
			Path directory = Path.of(path).toAbsolutePath().normalize();
			Files.createDirectories(directory);
			this.path = directory.toString();
		}
		catch(Exception e) {
			throw new IllegalStateException("영상 저장 경로를 준비할 수 없습니다: " + path, e);
		}
	}

	public String getPath() {
		return path;
	}
}
