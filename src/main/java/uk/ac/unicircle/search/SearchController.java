package uk.ac.unicircle.search;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
class SearchController {
    private final SearchService search;

    SearchController(SearchService search) {
        this.search = search;
    }

    @GetMapping
    List<SearchResult> search(Authentication authentication,
            @RequestParam(defaultValue = "") String query) {
        return search.search(authentication, query);
    }
}
