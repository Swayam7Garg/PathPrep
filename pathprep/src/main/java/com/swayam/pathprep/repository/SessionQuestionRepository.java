package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.SessionQuestion;
import com.swayam.pathprep.entity.SessionQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionQuestionRepository extends JpaRepository<SessionQuestion, SessionQuestionId> {

    @Query("SELECT sq FROM SessionQuestion sq WHERE sq.id.sessionId = :sessionId ORDER BY sq.id.position ASC")
    List<SessionQuestion> findByIdSessionIdOrderByPositionAsc(@Param("sessionId") Long sessionId);
}