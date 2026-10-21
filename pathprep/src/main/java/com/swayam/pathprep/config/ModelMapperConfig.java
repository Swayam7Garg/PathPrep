package com.swayam.pathprep.config;

import com.swayam.pathprep.dto.response.OptionView;
import com.swayam.pathprep.entity.QuestionOption;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a shared {@link ModelMapper} bean with STRICT matching strategy so
 * only explicitly-mapped fields are copied. Custom converters are registered for
 * Java record targets since records lack default no-arg constructors.
 */
@Slf4j
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        log.info("Initializing ModelMapper bean with STRICT strategy and record converters");
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true);

        modelMapper.typeMap(QuestionOption.class, OptionView.class).setConverter(ctx -> {
            QuestionOption src = ctx.getSource();
            if (src == null) {
                return null;
            }
            return new OptionView(src.getId(), src.getPosition(), src.getText());
        });

        return modelMapper;
    }
}
