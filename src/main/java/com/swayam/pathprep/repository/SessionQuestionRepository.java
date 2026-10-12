package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.SessionQuestion;
import com.swayam.pathprep.entity.SessionQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionQuestionRepository extends JpaRepository<SessionQuestion, SessionQuestionId> {
    List<SessionQuestion> findByIdSessionIdOrderByPositionAsc(Long sessionId);
}
