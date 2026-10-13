package uk.ac.unicircle.config;

final class SecurityHeaders {
    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; base-uri 'none'; object-src 'none'; "
            + "frame-ancestors 'none'; form-action 'self'; img-src 'self' data: blob:; media-src 'self' blob:; "
            + "style-src 'self'; script-src 'self'; connect-src 'self' wss:; worker-src 'self' blob:; "
            + "manifest-src 'self'";

    static final String PERMISSIONS_POLICY = "camera=(self), microphone=(self), geolocation=(self), fullscreen=(self)";

    private SecurityHeaders() {
    }
}
