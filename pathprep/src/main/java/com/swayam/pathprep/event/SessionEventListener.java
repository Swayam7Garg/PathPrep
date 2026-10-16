package com.swayam.pathprep.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class SessionEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionCompleted(SessionCompletedEvent event) {
        log.debug("SessionCompletedEvent received for userId={}, sessionId={}, trackId={}, completedAt={}, topicsCount={}",
                event.userId(), event.sessionId(), event.trackId(), event.completedAt(), event.perTopicResults().size());
    }
}
