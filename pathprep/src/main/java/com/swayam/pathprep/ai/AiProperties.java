package com.swayam.pathprep.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * Provider mode: GROQ or MOCK
     */
    private String provider = "MOCK";

    /**
     * Groq API key for shared system pool
     */
    private String groqApiKey = "";

    /**
     * Default model name (e.g. llama-3.3-70b-versatile or llama3-8b-8192)
     */
    private String model = "llama-3.3-70b-versatile";

    /**
     * Max free daily interview calls allowed per user on shared pool
     */
    private int dailyFreeCallLimit = 15;

    /**
     * Max free daily tokens per user on shared pool
     */
    private int dailyFreeTokenLimit = 25000;

    /**
     * Global shared daily pool token budget ceiling across all users
     */
    private int sharedPoolDailyTokenBudget = 250000;
}
