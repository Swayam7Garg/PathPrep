package com.swayam.pathprep.service;

import com.swayam.pathprep.dto.response.InstitutionSummary;
import com.swayam.pathprep.dto.response.UserProfile;
import com.swayam.pathprep.entity.RoleTrack;
import com.swayam.pathprep.entity.User;
import com.swayam.pathprep.repository.InstitutionRepository;
import com.swayam.pathprep.repository.RoleTrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileAssembler {

    private final InstitutionRepository institutionRepository;
    private final RoleTrackRepository roleTrackRepository;

    public UserProfile toProfile(User user) {
        InstitutionSummary institutionSummary = null;
        if (user.getInstitutionId() != null) {
            institutionSummary = institutionRepository.findById(user.getInstitutionId())
                    .map(inst -> new InstitutionSummary(inst.getId(), inst.getName()))
                    .orElse(null);
        }

        String targetTrackName = null;
        if (user.getTargetTrackId() != null) {
            targetTrackName = roleTrackRepository.findById(user.getTargetTrackId())
                    .map(RoleTrack::getName)
                    .orElse(null);
        }

        boolean onboardingCompleted = (user.getBranch() != null && !user.getBranch().isBlank())
                && (user.getGradYear() != null)
                && (user.getTargetTrackId() != null);

        return new UserProfile(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPictureUrl(),
                user.getRole(),
                user.getBranch(),
                user.getGradYear(),
                user.getTargetTrackId(),
                targetTrackName,
                institutionSummary,
                onboardingCompleted,
                user.getCreatedAt()
        );
    }
}
