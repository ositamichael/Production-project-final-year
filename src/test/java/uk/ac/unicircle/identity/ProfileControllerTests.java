package uk.ac.unicircle.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ProfileControllerTests {

    @TempDir
    static Path profilePhotoDirectory;

    @DynamicPropertySource
    static void profilePhotoDirectory(DynamicPropertyRegistry properties) {
        properties.add("unicircle.storage.profile-photos-dir", profilePhotoDirectory::toString);
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void profileCannotBeReadWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/profile/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void browserCanObtainACsrfTokenWithoutExposingProfileData() throws Exception {
        mockMvc.perform(get("/api/security/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").isNotEmpty())
                .andExpect(jsonPath("$.parameterName").isNotEmpty())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void signedInUserCanReadTheirOwnProfile() throws Exception {
        mockMvc.perform(get("/api/profile/me").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Michael Carter"))
                .andExpect(jsonPath("$.email").value("michael.carter@demo.university.example"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.visibility").value("UNIVERSITY_ONLY"));
    }

    @Test
    void profileChangesRequireCsrfProtection() throws Exception {
        mockMvc.perform(patch("/api/profile/me")
                        .with(user("student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProfileUpdate()))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedInUserCanUpdateValidProfileDetails() throws Exception {
        mockMvc.perform(patch("/api/profile/me")
                        .with(user("student"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProfileUpdate()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Michael Carter"))
                .andExpect(jsonPath("$.courseName").value("BSc Computer Science"))
                .andExpect(jsonPath("$.yearOfStudy").value(3))
                .andExpect(jsonPath("$.visibility").value("CONNECTIONS"));
    }

    @Test
    void invalidStudyYearIsRejected() throws Exception {
        mockMvc.perform(patch("/api/profile/me")
                        .with(user("student"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Michael Carter",
                                  "biography": "Student",
                                  "courseName": "BSc Computer Science",
                                  "yearOfStudy": 13,
                                  "visibility": "UNIVERSITY_ONLY"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signedInUserCanUploadAndReadAValidatedProfilePhoto() throws Exception {
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "profile.png", MediaType.IMAGE_PNG_VALUE, pngPhoto(256, 256));

        mockMvc.perform(multipart("/api/profile/me/photo")
                        .file(photo)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasPhoto").value(true))
                .andExpect(jsonPath("$.photoUrl").value("/api/profile/me/photo"));

        mockMvc.perform(get("/api/profile/me/photo").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andDo(result -> assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty());
    }

    private String validProfileUpdate() {
        return """
                {
                  "displayName": " Michael Carter ",
                  "biography": "Computer Science student building safer campus communities.",
                  "courseName": "BSc Computer Science",
                  "yearOfStudy": 3,
                  "visibility": "CONNECTIONS"
                }
                """;
    }

    private byte[] pngPhoto(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int blue = 0xff195294;
        int white = 0xffffffff;
        int centreX = width / 2;
        int centreY = height / 2;
        int radius = Math.min(width, height) / 4;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int distanceSquared = (x - centreX) * (x - centreX) + (y - centreY) * (y - centreY);
                image.setRGB(x, y, distanceSquared <= radius * radius ? white : blue);
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
