package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.response.TopicInTrack;
import com.swayam.pathprep.dto.response.TrackSummary;
import com.swayam.pathprep.service.TrackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;


    @GetMapping("/tracks")
    public List<TrackSummary> list() {
        log.info("Received request to list all role tracks");
        List<TrackSummary> tracks = trackService.listTracks();
        log.debug("Returning {} role tracks", tracks.size());
        return tracks;
    }

    @GetMapping("/tracks/{trackId}/topics")
    public List<TopicInTrack> getTopics(@PathVariable Long trackId) {
        log.info("Received request to fetch topics for trackId={}", trackId);
        List<TopicInTrack> topics = trackService.getTopicsForTrack(trackId);
        log.debug("Returning {} topics for trackId={}", topics.size(), trackId);
        return topics;
    }
}
