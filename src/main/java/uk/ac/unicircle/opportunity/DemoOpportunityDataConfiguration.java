package uk.ac.unicircle.opportunity;

import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("!prod")
class DemoOpportunityDataConfiguration {

    private static final UUID UNIVERSITY_ID = UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345101");
    private static final UUID MICHAEL_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be101");

    @Bean
    @Order(50)
    ApplicationRunner seedFictionalOpportunities(JdbcTemplate jdbc) {
        return arguments -> {
            UUID university = publisher(jdbc, "51aca635-b830-4402-a4d8-1322fa83f101",
                    "Campus Experience Team", "UNIVERSITY", "campus.example");
            UUID employer = publisher(jdbc, "51aca635-b830-4402-a4d8-1322fa83f102",
                    "Northstar Digital", "EMPLOYER", "northstar.example");
            UUID library = publisher(jdbc, "51aca635-b830-4402-a4d8-1322fa83f103",
                    "Learning Resources", "UNIVERSITY", "library.example");
            UUID union = publisher(jdbc, "51aca635-b830-4402-a4d8-1322fa83f104",
                    "Students' Union", "STUDENT_UNION", "union.example");

            UUID ambassador = opportunity(jdbc, "72f6bd9e-6ed8-47ef-b10f-ec908f417101", university,
                    "Student Ambassador", "Represent the university at open days and student events.",
                    "Support visitors, share an authentic student perspective and help the campus team deliver welcoming events.",
                    "JOB", "ON_CAMPUS", "City Campus", "8 hours per week", "Paid university role",
                    "Communication,Teamwork,Event support", "Open to currently enrolled students in good academic standing.", 24);
            UUID placement = opportunity(jdbc, "72f6bd9e-6ed8-47ef-b10f-ec908f417102", employer,
                    "Software Engineering Placement", "Join a supervised product team for a structured industry placement.",
                    "Work with Java, web APIs, automated testing and delivery practices while receiving regular technical mentoring.",
                    "PLACEMENT", "HYBRID", "Leeds and remote", "12-month placement", "Paid placement",
                    "Java,Git,Testing,Problem solving", "Suitable for computing students completing their penultimate or final year.", 38);
            opportunity(jdbc, "72f6bd9e-6ed8-47ef-b10f-ec908f417103", library,
                    "Library Assistant", "Help students access learning spaces and library services.",
                    "Provide front-desk support, organise learning materials and help maintain safe, welcoming study spaces.",
                    "JOB", "ON_CAMPUS", "Sheila Silver Library", "Evenings and weekends", "Part-time paid role",
                    "Customer service,Organisation,Digital confidence", "Open to enrolled students able to work agreed evening shifts.", 19);
            opportunity(jdbc, "72f6bd9e-6ed8-47ef-b10f-ec908f417104", union,
                    "Peer Welcome Volunteer", "Help new students settle into university life.",
                    "Join induction activities, answer practical campus questions and signpost students to approved support services.",
                    "VOLUNTEERING", "FLEXIBLE", "Across campus", "Flexible sessions", "Volunteer role",
                    "Listening,Inclusivity,Signposting", "Training is provided; no previous volunteering experience is required.", 31);
            opportunity(jdbc, "72f6bd9e-6ed8-47ef-b10f-ec908f417105", employer,
                    "Junior Data Project Internship", "Contribute to a short evidence-led data improvement project.",
                    "Prepare datasets, document data-quality checks and present findings to a small multidisciplinary team.",
                    "PLACEMENT", "REMOTE", "Remote within the UK", "10 weeks", "Paid internship",
                    "SQL,Data analysis,Documentation", "Open to students with introductory database or data-analysis experience.", 45);

            save(jdbc, ambassador);
            track(jdbc, placement, "APPLIED");
        };
    }

    private UUID publisher(JdbcTemplate jdbc, String idValue, String name, String type, String domain) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "opportunity_publishers", id) == 0) {
            jdbc.update("""
                    INSERT INTO opportunity_publishers
                      (id, university_id, name, publisher_type, website_domain,
                       verification_status, verified_at)
                    VALUES (?, ?, ?, ?, ?, 'VERIFIED', CURRENT_TIMESTAMP)
                    """, id, UNIVERSITY_ID, name, type, domain);
        }
        return id;
    }

    private UUID opportunity(
            JdbcTemplate jdbc,
            String idValue,
            UUID publisherId,
            String title,
            String summary,
            String description,
            String type,
            String workMode,
            String location,
            String hours,
            String compensation,
            String skills,
            String eligibility,
            int closesInDays) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "opportunities", id) == 0) {
            jdbc.update("""
                    INSERT INTO opportunities
                      (id, university_id, publisher_id, title, summary, description,
                       opportunity_type, work_mode, location, hours_label, compensation_label,
                       skills_text, eligibility, closes_at, moderation_status, published_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'APPROVED', CURRENT_TIMESTAMP)
                    """, id, UNIVERSITY_ID, publisherId, title, summary, description, type, workMode,
                    location, hours, compensation, skills, eligibility,
                    Date.valueOf(LocalDate.now().plusDays(closesInDays)));
        }
        return id;
    }

    private void save(JdbcTemplate jdbc, UUID opportunityId) {
        Integer existing = jdbc.queryForObject("""
                SELECT COUNT(*) FROM saved_opportunities WHERE opportunity_id = ? AND user_id = ?
                """, Integer.class, opportunityId, MICHAEL_ID);
        if (existing != null && existing == 0) {
            jdbc.update("INSERT INTO saved_opportunities (opportunity_id, user_id) VALUES (?, ?)",
                    opportunityId, MICHAEL_ID);
        }
    }

    private void track(JdbcTemplate jdbc, UUID opportunityId, String status) {
        Integer existing = jdbc.queryForObject("""
                SELECT COUNT(*) FROM opportunity_tracking WHERE opportunity_id = ? AND user_id = ?
                """, Integer.class, opportunityId, MICHAEL_ID);
        if (existing != null && existing == 0) {
            jdbc.update("""
                    INSERT INTO opportunity_tracking (opportunity_id, user_id, tracking_status)
                    VALUES (?, ?, ?)
                    """, opportunityId, MICHAEL_ID, status);
        }
    }

    private int count(JdbcTemplate jdbc, String table, UUID id) {
        Integer result = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Integer.class, id);
        return result == null ? 0 : result;
    }
}
