package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.TopicPrereq;
import com.swayam.pathprep.entity.TopicPrereqId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TopicPrereqRepository extends JpaRepository<TopicPrereq, TopicPrereqId> {

    @Query("SELECT tp FROM TopicPrereq tp WHERE tp.id.topicId IN :topicIds")
    List<TopicPrereq> findByTopicIdIn(@Param("topicIds") Collection<Long> topicIds);
}
