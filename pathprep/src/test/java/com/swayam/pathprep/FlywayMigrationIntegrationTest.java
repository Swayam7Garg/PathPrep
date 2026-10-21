package com.swayam.pathprep;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayMigrationIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flyway_migratesThroughV10Cleanly() {
        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("10");
        assertThat(current.getState().isApplied()).isTrue();

        // Verify sessions columns added by V7
        Integer answeredCountCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sessions' AND COLUMN_NAME = 'answered_count'",
                Integer.class
        );
        assertThat(answeredCountCol).isEqualTo(1);

        Integer activeMarkerCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sessions' AND COLUMN_NAME = 'active_marker'",
                Integer.class
        );
        assertThat(activeMarkerCol).isEqualTo(1);

        // Verify V8 learning system tables
        Integer skillMasteryTable = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill_mastery'",
                Integer.class
        );
        assertThat(skillMasteryTable).isEqualTo(1);

        Integer reviewQueueTable = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review_queue'",
                Integer.class
        );
        assertThat(reviewQueueTable).isEqualTo(1);

        Integer dailyActivityTable = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_activity'",
                Integer.class
        );
        assertThat(dailyActivityTable).isEqualTo(1);

        // Verify V9 admin content schema
        Integer questionReportsTable = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question_reports'",
                Integer.class
        );
        assertThat(questionReportsTable).isEqualTo(1);

        Integer subtopicCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'questions' AND COLUMN_NAME = 'subtopic'",
                Integer.class
        );
        assertThat(subtopicCol).isEqualTo(1);

        Integer sourceCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'questions' AND COLUMN_NAME = 'source'",
                Integer.class
        );
        assertThat(sourceCol).isEqualTo(1);
    }
}