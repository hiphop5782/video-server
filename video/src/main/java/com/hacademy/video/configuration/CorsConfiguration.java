package com.hacademy.video.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfiguration implements WebMvcConfigurer{
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
					.allowedOrigins(
							"http://video.sysout.co.kr", 
							"https://video.sysout.co.kr", 
							"http://localhost:5500",
							"http://localhost:5501", 
							"http://127.0.0.1:5500"		
					)
					.allowedMethods("GET", "HEAD", "OPTIONS")
					.exposedHeaders("Content-Range", "Accept-Ranges", "Content-Length", "token");
	}
}

