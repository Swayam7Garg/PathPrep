package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    Optional<Institution> findByEmailDomainIgnoreCase(String emailDomain);
}
