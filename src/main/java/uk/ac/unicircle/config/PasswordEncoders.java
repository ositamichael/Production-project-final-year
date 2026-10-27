package uk.ac.unicircle.config;

import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

final class PasswordEncoders {

    private static final int BCRYPT_STRENGTH = 12;

    private PasswordEncoders() {
    }

    static PasswordEncoder bcryptWithUpgradeSupport() {
        return new DelegatingPasswordEncoder(
                "bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder(BCRYPT_STRENGTH)));
    }
}
