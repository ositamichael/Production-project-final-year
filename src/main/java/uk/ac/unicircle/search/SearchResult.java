package uk.ac.unicircle.search;

import java.util.UUID;

record SearchResult(String type, UUID id, String title, String subtitle, String destination) {
}
