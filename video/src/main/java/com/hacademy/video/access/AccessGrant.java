package com.hacademy.video.access;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "access_grants")
public class AccessGrant {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String codeHash;

	@Column(nullable = false, length = 100)
	private String repository;

	@Column(length = 200)
	private String description;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant expiresAt;
	private Instant lastUsedAt;

	protected AccessGrant() {
	}

	public AccessGrant(String codeHash, String repository, String description, Instant createdAt, Instant expiresAt) {
		this.codeHash = codeHash;
		this.repository = repository;
		this.description = description;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public boolean isActiveAt(Instant now) {
		return expiresAt == null || expiresAt.isAfter(now);
	}

	public Long getId() {
		return id;
	}
	public String getCodeHash() {
		return codeHash;
	}
	public String getRepository() {
		return repository;
	}
	public String getDescription() {
		return description;
	}
	public Instant getCreatedAt() {
		return createdAt;
	}
	public Instant getExpiresAt() {
		return expiresAt;
	}
	public Instant getLastUsedAt() {
		return lastUsedAt;
	}
	public void markUsed(Instant usedAt) {
		this.lastUsedAt = usedAt;
	}
}
