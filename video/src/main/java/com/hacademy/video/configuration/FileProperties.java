package com.hacademy.video.configuration;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Data;

@Component
@ConfigurationProperties(prefix = "custom.file")
public class FileProperties {
	private String path;
	public void setPath(String path) {
		this.path = path;
	}
	public String getPath() {
		return path;
	}
	
	@PostConstruct
	public void init() {
		File dir = new File(path);
		dir.mkdirs();
	}
}
