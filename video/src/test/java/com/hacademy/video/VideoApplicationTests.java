package com.hacademy.video;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.hacademy.video.service.GithubRestService;
import com.hacademy.video.vo.GithubCollaboratorVO;

@SpringBootTest
class VideoApplicationTests {

	@Autowired
	private GithubRestService service;
	
	@Test
	void contextLoads() throws JsonMappingException, JsonProcessingException {
		for(GithubCollaboratorVO vo : service.loadCollaborators("kh12")) {
			System.out.println(vo);
		}
	}

}
