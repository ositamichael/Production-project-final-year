package uk.ac.unicircle.lostfound;

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

class LocalLostFoundPhotoStorageTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAValidatedPhotoAsAReEncodedImage() throws Exception {
        LocalLostFoundPhotoStorage storage = new LocalLostFoundPhotoStorage(temporaryDirectory.toString());
        LostFoundPhotoStorage.StoredPhoto stored = storage.store(pngUpload(640, 480));
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(storage.read(stored.key())));

        assertThat(stored.mediaType()).isEqualTo("image/png");
        assertThat(decoded.getWidth()).isEqualTo(640);
        assertThat(decoded.getHeight()).isEqualTo(480);
    }

    @Test
    void rejectsFakeImagesLowResolutionAndUnsafeKeys() throws Exception {
        LocalLostFoundPhotoStorage storage = new LocalLostFoundPhotoStorage(temporaryDirectory.toString());
        MockMultipartFile fake = new MockMultipartFile(
                "photo", "item.png", "image/png", "not an image".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storage.store(fake))
                .isInstanceOf(InvalidLostFoundPhotoException.class)
                .hasMessageContaining("supported image");
        assertThatThrownBy(() -> storage.store(pngUpload(120, 120)))
                .isInstanceOf(InvalidLostFoundPhotoException.class)
                .hasMessageContaining("at least 240 by 240");
        assertThatThrownBy(() -> storage.read("../private.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid item photograph key");
    }

    private MockMultipartFile pngUpload(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) image.setRGB(x, y, (x + y) % 2 == 0 ? 0xff195294 : 0xffffffff);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("photo", "item.png", "image/png", output.toByteArray());
    }
}
