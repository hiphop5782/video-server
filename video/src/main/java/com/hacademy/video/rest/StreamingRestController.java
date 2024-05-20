package com.hacademy.video.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.hacademy.video.configuration.FileProperties;
import com.hacademy.video.repository.FileRepository;
import com.hacademy.video.service.GithubRestService;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class StreamingRestController {
	
	@Autowired
	private FileProperties props;
	
	@Autowired
	private FileRepository repo;
	
	@Autowired
	private GithubRestService service;
	
	private Map<String, String> tokens = Collections.synchronizedMap(new HashMap<>());

	@GetMapping("/")
	public String home() {
		return "Video server is running";
	}
	
	@GetMapping("/data")
	public ResponseEntity<String[]> data(@RequestHeader String user,HttpSession session) {
		String token = UUID.randomUUID().toString();
		tokens.put(token, user);
		log.debug("user = {}, token = {}", user, token);
		return ResponseEntity.ok()
						.header("token", token)
					.body(service.findRepositoryVideo(user));
	}
	
	@GetMapping("/play/{token}/{video:.+}") 
	public ResponseEntity<ResourceRegion> play(
			@RequestHeader HttpHeaders headers,
			@PathVariable String token,
			@PathVariable String video,
			HttpSession session) throws IOException {
		String user = tokens.get(token);
		if(user == null) {
			return ResponseEntity.status(403).build();
		}
		
		return service.getVideo(headers, user, video);
	}
	
}
