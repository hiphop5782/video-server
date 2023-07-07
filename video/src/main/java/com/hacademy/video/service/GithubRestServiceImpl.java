package com.hacademy.video.service;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacademy.video.configuration.FileProperties;
import com.hacademy.video.configuration.GithubRestProperties;
import com.hacademy.video.vo.GithubCollaboratorVO;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GithubRestServiceImpl implements GithubRestService{
	
	@Autowired
	private WebClient webClient;
	
	private Map<String, List<GithubCollaboratorVO>> users = Collections.synchronizedMap(new HashMap<>());
	
	@Autowired
	private FileProperties fileProps;
	
	@PostConstruct
	public void init() throws JsonMappingException, JsonProcessingException {
		loadCollaborators();
	}

	@Override
	public List<GithubCollaboratorVO> loadCollaborators(String repo) throws JsonMappingException, JsonProcessingException {
		String response = webClient.get()
			.uri(String.format("/repos/%s/%s/collaborators", "hiphop5782", repo))
			.retrieve()
			.bodyToMono(String.class)
			.block();
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(response, new TypeReference<List<GithubCollaboratorVO>>() {});
	}
	
	@Autowired
	private GithubRestProperties githubProps;
	
	@Scheduled(cron = "0 0 * * * *")
	@Override
	public void loadCollaborators() throws JsonMappingException, JsonProcessingException {
		users.clear();
		for(String repositoryName : githubProps.getRepositories()) {
			List<GithubCollaboratorVO> collaborators = loadCollaborators(repositoryName);
			users.put(repositoryName, collaborators);
			log.info("저장소({}) - {} 유저 추가 완료", repositoryName, collaborators.size());
		}
	}
	
	private boolean checkRepository(String repository) {
		String[] repositories = githubProps.getRepositories();
		for(String repo : repositories) {
			if(repo.equals(repository)) {
				return true;
			}
		}
		return false;
	}
	private boolean checkUser(String repository, String user) {
		log.info("users = {}", users.keySet());
		List<GithubCollaboratorVO> userList = users.get(repository);
		log.info("userList = {}", userList);
		for(GithubCollaboratorVO vo : userList) {
			if(vo.getLogin().equals(user)) {
				return true; 
			}
		}
		return false;
	}

	@Override
	public String[] getRepositoryFiles(String repository, String user) {
		if(checkRepository(repository) && checkUser(repository, user)) {
			log.info("repository = {}, user = {}", repository, user);
			File directory = new File(fileProps.getPath(), repository);
			if(directory.exists()) {
				return directory.list((dir, name)->new File(dir, name).isFile());
			}
		}
		return null;
	}

	@Override
	public String findRepository(String user) {
		for(String repository : users.keySet()) {
			List<GithubCollaboratorVO> list = users.get(repository);
			for(GithubCollaboratorVO vo : list) {
				if(vo.getLogin().equals(user)) {
					return repository;
				}
			}
		}
		return null;
	}
	
	@Override
	public String[] findRepositoryVideo(String user) {
		String repo = findRepository(user);
		if(repo != null) {
			File directory = new File(fileProps.getPath(), repo);
			if(directory.exists()) {
				return directory.list((dir, name)->new File(dir, name).isFile());
			}
		}
		return null;
	}
	
	@Override
	public ResponseEntity<ResourceRegion> getVideo(HttpHeaders headers, String user, String video) throws IOException {
		String repo = findRepository(user);
		File directory = new File(fileProps.getPath(), repo);
		File target = new File(directory, video);
		Resource resource = new FileSystemResource(target);	
		ResourceRegion region = createResourceRegion(resource, headers);
		return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
										.contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
										.body(region);
	}
	
	private ResourceRegion createResourceRegion(
			Resource resource, HttpHeaders headers) throws IOException {
		final long chunkSize = 100000L;
		long contentLength = resource.contentLength();
		
		HttpRange range = headers.getRange().stream().findFirst().orElse(null);
		if(range != null) {
			long start = range.getRangeStart(contentLength);
			long end = range.getRangeEnd(contentLength);
			long rangeLength = Long.min(chunkSize, end-start+1);
			return new ResourceRegion(resource, start, rangeLength);
		}
		else {
			long rangeLength = Long.min(chunkSize, contentLength);
			return new ResourceRegion(resource, 0, rangeLength);
		}
	}
	
}
