package com.swayam.pathprep.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
public class QuestionSelector {

    private final Random random;

    public QuestionSelector() {
        this(new Random());
    }

    public QuestionSelector(Random random) {
        this.random = random;
    }

    /**
     * Selects up to `count` question IDs spread fairly across topics favoring higher weights.
     *
     * @param topicIds Ordered list of topic IDs to select from
     * @param topicWeights Weight of each topic
     * @param candidatesByTopic Map of topic ID to candidate question IDs
     * @param count Desired question count
     * @return List of selected question IDs (shuffled, no duplicates)
     */
    public List<Long> select(
            List<Long> topicIds,
            Map<Long, BigDecimal> topicWeights,
            Map<Long, List<Long>> candidatesByTopic,
            int count
    ) {
        log.debug("Starting question selection: requestedCount={}, topicCount={}", count, topicIds.size());

        if (count <= 0 || topicIds.isEmpty() || candidatesByTopic.isEmpty()) {
            log.debug("Empty selection criteria: count={}, topicsEmpty={}, candidatesEmpty={}",
                    count, topicIds.isEmpty(), candidatesByTopic.isEmpty());
            return List.of();
        }

        // Copy and shuffle candidate lists per topic so individual picks are randomized
        Map<Long, List<Long>> availableByTopic = new LinkedHashMap<>();
        int totalAvailable = 0;
        for (Long topicId : topicIds) {
            List<Long> candidates = candidatesByTopic.get(topicId);
            if (candidates != null && !candidates.isEmpty()) {
                List<Long> shuffled = new ArrayList<>(new LinkedHashSet<>(candidates));
                Collections.shuffle(shuffled, random);
                availableByTopic.put(topicId, shuffled);
                totalAvailable += shuffled.size();
            }
        }

        log.debug("Total available candidate questions across topics: totalAvailable={}", totalAvailable);

        if (totalAvailable == 0) {
            log.warn("No available candidate questions found for specified topics: {}", topicIds);
            return List.of();
        }

        if (totalAvailable <= count) {
            log.info("Available questions ({}) <= requested count ({}). Returning all available candidates.",
                    totalAvailable, count);
            List<Long> all = new ArrayList<>();
            for (List<Long> list : availableByTopic.values()) {
                all.addAll(list);
            }
            Collections.shuffle(all, random);
            return all;
        }

        // Weighted quota allocation using largest remainder method
        double totalWeight = availableByTopic.keySet().stream()
                .mapToDouble(t -> topicWeights.getOrDefault(t, BigDecimal.ONE).doubleValue())
                .sum();
        if (totalWeight <= 0) {
            totalWeight = availableByTopic.size();
        }

        Map<Long, Integer> allocation = new LinkedHashMap<>();
        Map<Long, Double> remainders = new LinkedHashMap<>();
        int allocatedCount = 0;

        for (Long topicId : availableByTopic.keySet()) {
            double weight = topicWeights.getOrDefault(topicId, BigDecimal.ONE).doubleValue();
            double exactQuota = (weight / totalWeight) * count;
            int floorQuota = (int) Math.floor(exactQuota);
            int availableCount = availableByTopic.get(topicId).size();
            int take = Math.min(floorQuota, availableCount);
            allocation.put(topicId, take);
            allocatedCount += take;
            remainders.put(topicId, exactQuota - floorQuota);
        }

        // Distribute remaining slots by largest remainder to topics that still have questions
        List<Long> sortedByRemainder = new ArrayList<>(availableByTopic.keySet());
        sortedByRemainder.sort((a, b) -> Double.compare(remainders.get(b), remainders.get(a)));

        while (allocatedCount < count) {
            boolean anyAdded = false;
            for (Long topicId : sortedByRemainder) {
                if (allocatedCount >= count) {
                    break;
                }
                int currentAlloc = allocation.get(topicId);
                int availableCount = availableByTopic.get(topicId).size();
                if (currentAlloc < availableCount) {
                    allocation.put(topicId, currentAlloc + 1);
                    allocatedCount++;
                    anyAdded = true;
                }
            }
            if (!anyAdded) {
                break; // No more available candidates across any topic
            }
        }

        log.debug("Computed weighted question allocation per topic: {}", allocation);

        // Collect allocated questions
        List<Long> selected = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : allocation.entrySet()) {
            Long topicId = entry.getKey();
            int toTake = entry.getValue();
            List<Long> candidates = availableByTopic.get(topicId);
            selected.addAll(candidates.subList(0, Math.min(toTake, candidates.size())));
        }

        Collections.shuffle(selected, random);
        log.info("Successfully selected and shuffled {} questions across topics", selected.size());
        return selected;
    }
}
