package com.hacademy.video.service;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.hacademy.video.vo.GithubCollaboratorVO;

public interface GithubRestService {
	List<GithubCollaboratorVO> loadCollaborators(String repo) throws JsonMappingException, JsonProcessingException;
	void loadCollaborators() throws JsonMappingException, JsonProcessingException;
	String[] getRepositoryFiles(String repository, String user);
	String findRepository(String user);
	String[] findRepositoryVideo(String user);
	ResponseEntity<ResourceRegion> getVideo(HttpHeaders headers, String user, String video) throws IOException;
}
