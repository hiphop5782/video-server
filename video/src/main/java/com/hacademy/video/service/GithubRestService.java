package com.hacademy.video.service;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;

import com.hacademy.video.vo.GithubCollaboratorVO;

import jakarta.servlet.http.HttpServletResponse;

public interface GithubRestService {
	List<GithubCollaboratorVO> loadCollaborators(String repository);
	void loadCollaborators();
	boolean isConfiguredRepository(String repository);
	String findRepository(String githubUserId);
	String[] findRepositoryVideo(String githubUserId);
	String[] getRepositoryVideos(String repository);
	void streamVideo(HttpHeaders headers, HttpServletResponse response, String repository, String video) throws IOException;
}
