package com.swayam.pathprep.event;

import com.swayam.pathprep.entity.Attempt;
import org.springframework.context.ApplicationEvent;

/**
 * Published via {@link org.springframework.context.ApplicationEventPublisher} immediately
 * after a quiz attempt is graded and persisted (Phase 4).
 *
 * <p>This event is a hook for future mastery-tracking logic (Phase 7+). Listeners can
 * react to each graded attempt to update topic-level mastery scores, trigger adaptive
 * difficulty adjustments, or send analytics events.
 *
 * <p>The listener is currently a no-op. The event is published regardless so that
 * adding mastery logic later requires only a new {@code @EventListener} — no changes
 * to the core submission flow.
 */
public class AttemptGradedEvent extends ApplicationEvent {

    /** The persisted attempt that was just graded. */
    private final Attempt attempt;

    /**
     * @param source  the object that published this event (typically the service bean)
     * @param attempt the graded and persisted {@link Attempt}
     */
    public AttemptGradedEvent(Object source, Attempt attempt) {
        super(source);
        this.attempt = attempt;
    }

    public Attempt getAttempt() {
        return attempt;
    }
}
