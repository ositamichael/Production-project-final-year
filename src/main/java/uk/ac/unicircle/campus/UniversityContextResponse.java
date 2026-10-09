package uk.ac.unicircle.campus;

import java.util.List;
import java.util.UUID;

record UniversityContextResponse(
        String dataSource,
        String dataNotice,
        UniversitySummary university,
        StudentSummary student,
        List<ModuleSummary> modules,
        List<CoursemateSummary> coursemates) {

    record UniversitySummary(String name) {
    }

    record StudentSummary(
            String displayName,
            String email,
            String programmeCode,
            String programmeTitle,
            String qualification,
            int yearOfStudy) {
    }

    record ModuleSummary(String code, String title, int studyLevel) {
    }

    record CoursemateSummary(
            UUID accountId,
            String displayName,
            String programmeTitle,
            int yearOfStudy,
            String connectionState,
            UUID connectionId,
            String connectionDirection,
            List<String> sharedModuleCodes) {
    }
}
