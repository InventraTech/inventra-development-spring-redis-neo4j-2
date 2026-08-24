package com.inventra.api.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.profile.Profile;

public interface ProfileRepository extends JpaRepository<Profile, Integer> {

    boolean existsByAccessType(String accessType);

}
