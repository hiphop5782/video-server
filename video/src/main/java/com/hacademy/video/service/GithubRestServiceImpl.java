package com.hacademy.video.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacademy.video.configuration.FileProperties;
import com.hacademy.video.configuration.GithubRestProperties;
import com.hacademy.video.vo.GithubCollaboratorVO;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GithubRestServiceImpl implements GithubRestService {
	private final WebClient webClient;
	private final ObjectMapper objectMapper;
	private final FileProperties fileProperties;
	private final GithubRestProperties githubProperties;

	private volatile Map<String, List<GithubCollaboratorVO>> users = Map.of();

	public GithubRestServiceImpl(
			@Qualifier("githubWebClient") WebClient webClient,
			ObjectMapper objectMapper,
			FileProperties fileProperties,
			GithubRestProperties githubProperties) {
		this.webClient = webClient;
		this.objectMapper = objectMapper;
		this.fileProperties = fileProperties;
		this.githubProperties = githubProperties;
	}

	@PostConstruct
	public void init() {
		loadCollaborators();
	}

	@Override
	public List<GithubCollaboratorVO> loadCollaborators(String repository) {
		try {
			String response = webClient.get()
					.uri(String.format("/repos/%s/%s/collaborators", githubProperties.getOwner(), repository))
					.retrieve()
					.bodyToMono(String.class)
					.block();
			if(response == null) {
				return List.of();
			}
			return objectMapper.readValue(response, new TypeReference<List<GithubCollaboratorVO>>() {});
		}
		catch(Exception e) {
			throw new IllegalStateException("GitHub collaborator 조회에 실패했습니다: " + repository, e);
		}
	}

	@Scheduled(cron = "0 0 * * * *")
	@Override
	public synchronized void loadCollaborators() {
		if(!githubProperties.isEnabled()) {
			users = Map.of();
			log.info("GitHub collaborator 조회가 비활성화되어 있습니다.");
			return;
		}
		if(githubProperties.getToken() == null || githubProperties.getToken().isBlank()) {
			users = Map.of();
			log.warn("GITHUB_TOKEN이 없어 GitHub 로그인을 사용할 수 없습니다.");
			return;
		}

		Map<String, List<GithubCollaboratorVO>> refreshed = new LinkedHashMap<>();
		for(String repository : githubProperties.getRepositories()) {
			try {
				List<GithubCollaboratorVO> collaborators = loadCollaborators(repository);
				refreshed.put(repository, List.copyOf(collaborators));
				log.info("저장소({}) collaborator {}명 갱신", repository, collaborators.size());
			}
			catch(Exception e) {
				List<GithubCollaboratorVO> previous = users.get(repository);
				if(previous != null) {
					refreshed.put(repository, previous);
				}
				log.error("저장소({}) collaborator 갱신 실패", repository, e);
			}
		}
		users = Map.copyOf(refreshed);
	}

	@Override
	public boolean isConfiguredRepository(String repository) {
		return repository != null && githubProperties.getRepositories().contains(repository);
	}

	@Override
	public String findRepository(String githubUserId) {
		if(githubUserId == null || githubUserId.isBlank()) {
			return null;
		}
		Map<String, List<GithubCollaboratorVO>> snapshot = users;
		for(String repository : githubProperties.getRepositories()) {
			for(GithubCollaboratorVO collaborator : snapshot.getOrDefault(repository, List.of())) {
				if(githubUserId.equals(collaborator.getId())) {
					return repository;
				}
			}
		}
		return null;
	}

	@Override
	public String[] findRepositoryVideo(String githubUserId) {
		String repository = findRepository(githubUserId);
		return repository == null ? null : getRepositoryVideos(repository);
	}

	@Override
	public String[] getRepositoryVideos(String repository) {
		if(!isConfiguredRepository(repository)) {
			return new String[0];
		}
		Path directory = repositoryDirectory(repository);
		if(!Files.isDirectory(directory)) {
			return new String[0];
		}
		try(var paths = Files.list(directory)) {
			List<String> videos = new ArrayList<>();
			paths.filter(Files::isRegularFile)
					.map(path -> path.getFileName().toString())
					.forEach(videos::add);
			String[] result = videos.toArray(String[]::new);
			Arrays.sort(result, String.CASE_INSENSITIVE_ORDER);
			return result;
		}
		catch(IOException e) {
			throw new IllegalStateException("영상 목록을 읽을 수 없습니다: " + repository, e);
		}
	}

	@Override
	public void streamVideo(HttpHeaders headers, HttpServletResponse response, String repository, String video) throws IOException {
		Path directory = repositoryDirectory(repository);
		Path target = directory.resolve(video).normalize();
		if(!target.startsWith(directory) || !Files.isRegularFile(target)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		Resource resource = new FileSystemResource(target);
		long contentLength = resource.contentLength();
		MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
		HttpRange range = headers.getRange().stream().findFirst().orElse(null);

		response.setContentType(mediaType.toString());
		response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
		if(range == null) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
			try(InputStream inputStream = Files.newInputStream(target);
					OutputStream outputStream = response.getOutputStream()) {
				StreamUtils.copy(inputStream, outputStream);
			}
			return;
		}

		long start = range.getRangeStart(contentLength);
		long end = range.getRangeEnd(contentLength);
		long rangeLength = end - start + 1;
		response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
		response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + contentLength);
		response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(rangeLength));
		try(InputStream inputStream = Files.newInputStream(target);
				OutputStream outputStream = response.getOutputStream()) {
			inputStream.skipNBytes(start);
			StreamUtils.copyRange(inputStream, outputStream, 0, rangeLength - 1);
		}
	}

	private Path repositoryDirectory(String repository) {
		if(!isConfiguredRepository(repository)) {
			throw new IllegalArgumentException("허용되지 않은 저장소입니다.");
		}
		Path root = Path.of(fileProperties.getPath()).toAbsolutePath().normalize();
		Path directory = root.resolve(repository).normalize();
		if(!directory.startsWith(root)) {
			throw new IllegalArgumentException("잘못된 저장소 경로입니다.");
		}
		return directory;
	}
}
