package com.swayam.pathprep.mapper;

import com.swayam.pathprep.dto.OptionView;
import com.swayam.pathprep.dto.QuestionView;
import com.swayam.pathprep.dto.SessionResponse;
import com.swayam.pathprep.entity.PracticeSession;
import com.swayam.pathprep.entity.Question;
import com.swayam.pathprep.entity.QuestionOption;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Converts JPA entities → API DTOs.
 *
 * <ul>
 *   <li>{@link OptionView} – mapped via {@link ModelMapper} (field names match exactly).</li>
 *   <li>{@link QuestionView} – mapped manually because of derived fields (topicId / topicName
 *       from a nested association and the sorted options list).</li>
 *   <li>{@link SessionResponse} – mapped via {@link ModelMapper} for the flat scalar fields;
 *       the nested {@code currentQuestion} is injected separately.</li>
 * </ul>
 */
@Slf4j
@Component
public class QuestionMapper {

    private final ModelMapper modelMapper;

    public QuestionMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    /** Maps a {@link QuestionOption} entity to an {@link OptionView} DTO using ModelMapper. */
    public OptionView toOptionView(QuestionOption option) {
        if (option == null) {
            return null;
        }
        log.trace("Mapping QuestionOption id={} position={} via ModelMapper", option.getId(), option.getPosition());
        return modelMapper.map(option, OptionView.class);
    }

    /**
     * Maps a {@link Question} entity to a {@link QuestionView} DTO.
     * Uses ModelMapper for the option list and manual extraction for nested topic fields.
     */
    public QuestionView toView(Question question) {
        if (question == null) {
            return null;
        }
        log.debug("Mapping Question entity id={} to QuestionView", question.getId());
        List<OptionView> optionViews = question.getOptions() != null
                ? question.getOptions().stream()
                        .sorted(Comparator.comparingInt(QuestionOption::getPosition))
                        .map(this::toOptionView)
                        .toList()
                : List.of();

        return new QuestionView(
                question.getId(),
                question.getType(),
                question.getTopic() != null ? question.getTopic().getId() : 0L,
                question.getTopic() != null ? question.getTopic().getName() : null,
                question.getDifficulty(),
                question.getBody(),
                optionViews
        );
    }

    /**
     * Builds a {@link SessionResponse} from a saved {@link PracticeSession} and the
     * already-mapped first {@link QuestionView}. Java records are immutable and have no
     * no-arg constructor, so ModelMapper cannot instantiate them directly. Scalar fields are
     * read from the entity and passed to the canonical record constructor.
     */
    public SessionResponse toSessionResponse(PracticeSession session, QuestionView firstQuestion, List<String> warnings) {
        log.debug("Mapping PracticeSession id={} to SessionResponse DTO", session.getId());
        return new SessionResponse(
                session.getId(),
                session.getMode(),
                session.getStyle(),
                session.getStatus(),
                session.getTotalQuestions(),
                session.getCurrentIndex(),
                firstQuestion,
                warnings
        );
    }
}
