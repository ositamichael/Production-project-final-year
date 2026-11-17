package uk.ac.unicircle.lostfound;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Profile("pilot")
class PilotLostFoundPhotoStorage implements LostFoundPhotoStorage {

    private final JdbcTemplate jdbc;

    PilotLostFoundPhotoStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public StoredPhoto store(MultipartFile upload) throws IOException {
        var temporaryDirectory = Files.createTempDirectory("unicircle-pilot-item-");
        try {
            var processed = new LocalLostFoundPhotoStorage(temporaryDirectory.toString());
            StoredPhoto photo = processed.store(upload);
            try {
                jdbc.update("""
                        INSERT INTO pilot_media (media_key, media_kind, media_type, content)
                        VALUES (?, 'LOST_FOUND', ?, ?)
                        """, photo.key(), photo.mediaType(), processed.read(photo.key()));
                return photo;
            } finally {
                processed.delete(photo.key());
            }
        } finally {
            Files.deleteIfExists(temporaryDirectory);
        }
    }

    @Override
    public byte[] read(String key) throws IOException {
        List<byte[]> matches = jdbc.query("""
                SELECT content FROM pilot_media WHERE media_key = ? AND media_kind = 'LOST_FOUND'
                """, (result, row) -> result.getBytes(1), key);
        if (matches.isEmpty()) throw new IOException("Item photograph is unavailable.");
        return matches.getFirst();
    }

    @Override
    public void delete(String key) {
        if (key != null) jdbc.update("DELETE FROM pilot_media WHERE media_key = ? AND media_kind = 'LOST_FOUND'", key);
    }
}
