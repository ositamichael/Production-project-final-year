package uk.ac.unicircle.identity;

import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

@Configuration
@Profile("!prod & !pilot")
class DemoIdentityConfiguration {

    private static final UUID UNIVERSITY_ID = UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345101");
    private static final UUID ACCOUNT_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be101");
    private static final UUID STAFF_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be105");
    private static final UUID ADMIN_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be106");

    @Bean
    @Order(10)
    ApplicationRunner seedLocalProfile(UniversityRepository universities, UserAccountRepository accounts) {
        return arguments -> {
            University university = universities.findById(UNIVERSITY_ID)
                    .orElseGet(() -> universities.save(new University(
                            UNIVERSITY_ID,
                            "demo-university",
                            "Leeds Beckett–style Fictional Sandbox",
                            "demo.university.example",
                            "https://identity.demo.university.example")));

            if (accounts.findById(ACCOUNT_ID).isEmpty()) {
                UserAccount account = new UserAccount(
                        ACCOUNT_ID,
                        university,
                        ProfileService.DEMO_SUBJECT,
                        "michael.carter@demo.university.example",
                        "Michael Carter",
                        UserRole.STUDENT);
                account.updateProfile(
                        "Michael Carter",
                        "Computer Science student interested in software engineering and campus communities.",
                        "BSc Computer Science",
                        3,
                        ProfileVisibility.UNIVERSITY_ONLY);
                accounts.save(account);
            }
            if (accounts.findById(STAFF_ID).isEmpty()) {
                UserAccount reviewer = new UserAccount(
                        STAFF_ID,
                        university,
                        "fictional-lost-property-reviewer",
                        "reviewer@demo.university.example",
                        "Alex Morgan",
                        UserRole.STAFF);
                reviewer.updateProfile(
                        "Alex Morgan",
                        "Fictional campus service-desk reviewer for safe workflow demonstrations.",
                        "Campus service desk",
                        null,
                        ProfileVisibility.PRIVATE);
                accounts.save(reviewer);
            }
            if (accounts.findById(ADMIN_ID).isEmpty()) {
                UserAccount administrator = new UserAccount(
                        ADMIN_ID,
                        university,
                        "fictional-test-campus-administrator",
                        "admin@demo.university.example",
                        "Jordan Ellis",
                        UserRole.ADMIN);
                administrator.updateProfile(
                        "Jordan Ellis",
                        "Fictional administrator for the UniCircle Test Campus.",
                        "Test Campus Operations",
                        null,
                        ProfileVisibility.PRIVATE);
                accounts.save(administrator);
            }
        };
    }
}
