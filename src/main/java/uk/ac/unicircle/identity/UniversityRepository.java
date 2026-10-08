package uk.ac.unicircle.identity;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UniversityRepository extends JpaRepository<University, UUID> {
}

