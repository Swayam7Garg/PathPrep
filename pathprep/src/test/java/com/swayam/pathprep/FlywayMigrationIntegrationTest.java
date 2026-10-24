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
    void flyway_migratesThroughV12Cleanly() {
        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("12");
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

        // Verify V11 Auth Schema
        Integer refreshTokensTable = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'refresh_tokens'",
                Integer.class
        );
        assertThat(refreshTokensTable).isEqualTo(1);

        Integer googleSubCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'google_sub'",
                Integer.class
        );
        assertThat(googleSubCol).isEqualTo(1);

        Integer pictureUrlCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'picture_url'",
                Integer.class
        );
        assertThat(pictureUrlCol).isEqualTo(1);

        Integer lastLoginCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'last_login_at'",
                Integer.class
        );
        assertThat(lastLoginCol).isEqualTo(1);

        // Verify V12 Daily Plan Schema
        Integer dailyPlanTable = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_plan_items'",
                Integer.class
        );
        assertThat(dailyPlanTable).isEqualTo(1);

        Integer itemTypeCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_plan_items' AND COLUMN_NAME = 'item_type'",
                Integer.class
        );
        assertThat(itemTypeCol).isEqualTo(1);

        Integer planDateCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_plan_items' AND COLUMN_NAME = 'plan_date'",
                Integer.class
        );
        assertThat(planDateCol).isEqualTo(1);
    }
}