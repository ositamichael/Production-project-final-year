package uk.ac.unicircle.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalProfilePhotoStorageTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAValidatedPhotoAndReturnsAReEncodedImage() throws Exception {
        LocalProfilePhotoStorage storage = new LocalProfilePhotoStorage(temporaryDirectory.toString());
        MockMultipartFile upload = pngUpload(256, 256);

        ProfilePhotoStorage.StoredPhoto stored = storage.store(upload);
        byte[] content = storage.read(stored.key());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(content));

        assertThat(stored.mediaType()).isEqualTo("image/png");
        assertThat(stored.key()).endsWith(".png");
        assertThat(decoded.getWidth()).isEqualTo(256);
        assertThat(decoded.getHeight()).isEqualTo(256);
        assertThat(content).isNotEmpty();
    }

    @Test
    void rejectsFilesThatOnlyPretendToBeImages() throws Exception {
        LocalProfilePhotoStorage storage = new LocalProfilePhotoStorage(temporaryDirectory.toString());
        MockMultipartFile upload = new MockMultipartFile(
                "photo", "profile.png", "image/png", "not an image".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storage.store(upload))
                .isInstanceOf(InvalidProfilePhotoException.class)
                .hasMessageContaining("supported image");
    }

    @Test
    void rejectsLowResolutionProfilePhotos() throws Exception {
        LocalProfilePhotoStorage storage = new LocalProfilePhotoStorage(temporaryDirectory.toString());

        assertThatThrownBy(() -> storage.store(pngUpload(64, 64)))
                .isInstanceOf(InvalidProfilePhotoException.class)
                .hasMessageContaining("at least 128 by 128");
    }

    @Test
    void rejectsStorageKeysThatCouldEscapeTheConfiguredDirectory() throws Exception {
        LocalProfilePhotoStorage storage = new LocalProfilePhotoStorage(temporaryDirectory.toString());

        assertThatThrownBy(() -> storage.read("../private.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid profile photograph key");
    }

    private MockMultipartFile pngUpload(int width, int height) throws Exception {
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
        return new MockMultipartFile("photo", "profile.png", "image/png", output.toByteArray());
    }
}
