package com.hacademy.video.access;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hacademy.video.service.GithubRestService;

@Service
public class AccessGrantService {
	private final AccessGrantRepository repository;
	private final GithubRestService githubRestService;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public AccessGrantService(AccessGrantRepository repository, GithubRestService githubRestService) {
		this.repository = repository;
		this.githubRestService = githubRestService;
	}

	@Transactional
	public AccessGrant create(String code, String targetRepository, String description, Instant expiresAt) {
		String normalizedCode = normalizeCode(code);
		if(!githubRestService.isConfiguredRepository(targetRepository)) {
			throw new IllegalArgumentException("허용되지 않은 저장소입니다.");
		}
		if(expiresAt != null && !expiresAt.isAfter(Instant.now())) {
			throw new IllegalArgumentException("만료 시각은 현재보다 이후여야 합니다.");
		}
		String safeDescription = description == null ? null : description.trim();
		if(safeDescription != null && safeDescription.length() > 200) {
			throw new IllegalArgumentException("설명은 200자 이하여야 합니다.");
		}
		return repository.save(new AccessGrant(
				passwordEncoder.encode(normalizedCode),
				targetRepository,
				safeDescription,
				Instant.now(),
				expiresAt));
	}

	@Transactional
	public Optional<AccessGrant> authenticate(String code) {
		String normalizedCode;
		try {
			normalizedCode = normalizeCode(code);
		}
		catch(IllegalArgumentException e) {
			return Optional.empty();
		}
		Instant now = Instant.now();
		for(AccessGrant grant : repository.findAllByOrderByCreatedAtDesc()) {
			if(grant.isActiveAt(now) && passwordEncoder.matches(normalizedCode, grant.getCodeHash())) {
				grant.markUsed(now);
				return Optional.of(repository.save(grant));
			}
		}
		return Optional.empty();
	}

	@Transactional(readOnly = true)
	public Optional<AccessGrant> findActive(Long id) {
		return repository.findById(id).filter(grant -> grant.isActiveAt(Instant.now()));
	}

	@Transactional(readOnly = true)
	public List<AccessGrant> findAll() {
		return repository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional
	public boolean delete(Long id) {
		if(!repository.existsById(id)) {
			return false;
		}
		repository.deleteById(id);
		return true;
	}

	private String normalizeCode(String code) {
		if(code == null) {
			throw new IllegalArgumentException("접근 코드를 입력해 주세요.");
		}
		String normalized = code.trim();
		if(normalized.length() < 6 || normalized.length() > 128) {
			throw new IllegalArgumentException("접근 코드는 6자 이상 128자 이하여야 합니다.");
		}
		return normalized;
	}
}
