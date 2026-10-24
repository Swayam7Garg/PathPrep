package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.UserLlmKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLlmKeyRepository extends JpaRepository<UserLlmKey, Long> {
    Optional<UserLlmKey> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    void deleteByUserId(Long userId);
}
