package uk.ac.unicircle.identity;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
class LocalProfilePhotoStorage implements ProfilePhotoStorage {

    static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    static final int MIN_DIMENSION = 128;
    static final int MAX_DIMENSION = 4096;

    private final Path root;

    LocalProfilePhotoStorage(@Value("${unicircle.storage.profile-photos-dir:${java.io.tmpdir}/unicircle-profile-photos}") String directory)
            throws IOException {
        root = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public StoredPhoto store(MultipartFile upload) throws IOException {
        if (upload == null || upload.isEmpty()) {
            throw new InvalidProfilePhotoException("Choose a PNG or JPEG photograph.");
        }
        if (upload.getSize() > MAX_UPLOAD_BYTES) {
            throw new InvalidProfilePhotoException("Profile photographs must be 5 MB or smaller.");
        }

        DecodedPhoto photo = decodeAndValidate(upload);
        String extension = photo.mediaType().equals("image/png") ? ".png" : ".jpg";
        String key = UUID.randomUUID() + extension;
        Path destination = resolveKey(key);
        Path temporary = Files.createTempFile(root, "profile-", ".upload");

        try {
            writeWithoutMetadata(photo.image(), photo.mediaType(), temporary);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return new StoredPhoto(key, photo.mediaType());
    }

    @Override
    public byte[] read(String key) throws IOException {
        return Files.readAllBytes(resolveKey(key));
    }

    @Override
    public void delete(String key) throws IOException {
        if (key != null) {
            Files.deleteIfExists(resolveKey(key));
        }
    }

    private DecodedPhoto decodeAndValidate(MultipartFile upload) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(upload.getInputStream())) {
            if (input == null) {
                throw new InvalidProfilePhotoException("The selected file is not a readable image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidProfilePhotoException("The selected file is not a supported image.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase();
                String mediaType = switch (format) {
                    case "png" -> "image/png";
                    case "jpg", "jpeg" -> "image/jpeg";
                    default -> throw new InvalidProfilePhotoException("Only PNG and JPEG photographs are supported.");
                };
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < MIN_DIMENSION || height < MIN_DIMENSION) {
                    throw new InvalidProfilePhotoException("Profile photographs must be at least 128 by 128 pixels.");
                }
                if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new InvalidProfilePhotoException("Profile photographs cannot exceed 4096 by 4096 pixels.");
                }
                BufferedImage image = reader.read(0);
                return new DecodedPhoto(image, mediaType);
            } finally {
                reader.dispose();
            }
        }
    }

    private void writeWithoutMetadata(BufferedImage source, String mediaType, Path destination) throws IOException {
        String format = mediaType.equals("image/png") ? "png" : "jpeg";
        BufferedImage image = format.equals("jpeg") ? toRgb(source) : source;
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new IOException("No image encoder is available for " + format);
        }

        ImageWriter writer = writers.next();
        try (OutputStream output = Files.newOutputStream(destination);
                ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (format.equals("jpeg") && parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(0.9f);
            }
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }

    private BufferedImage toRgb(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        int[] pixels = source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth());
        rgb.setRGB(0, 0, source.getWidth(), source.getHeight(), pixels, 0, source.getWidth());
        return rgb;
    }

    private Path resolveKey(String key) {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}\\.(png|jpg)")) {
            throw new IllegalArgumentException("Invalid profile photograph key.");
        }
        Path resolved = root.resolve(key).normalize();
        if (!resolved.getParent().equals(root)) {
            throw new IllegalArgumentException("Invalid profile photograph location.");
        }
        return resolved;
    }

    private record DecodedPhoto(BufferedImage image, String mediaType) {
    }
}
