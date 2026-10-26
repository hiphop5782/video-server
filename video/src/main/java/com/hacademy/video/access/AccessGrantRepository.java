package com.hacademy.video.access;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessGrantRepository extends JpaRepository<AccessGrant, Long> {
	List<AccessGrant> findAllByOrderByCreatedAtDesc();
}
