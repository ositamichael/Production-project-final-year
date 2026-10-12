package uk.ac.unicircle.config;

import java.net.URI;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
class SignInController {

    private final Environment environment;

    SignInController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/sign-in")
    RedirectView signIn() {
        String destination = environment.acceptsProfiles(Profiles.of("prod"))
                ? "/oauth2/authorization/university"
                : environment.acceptsProfiles(Profiles.of("pilot"))
                    ? "/pilot-login.html" : "/demo-login.html";
        RedirectView redirect = new RedirectView(URI.create(destination).toASCIIString());
        redirect.setExposeModelAttributes(false);
        return redirect;
    }
}
