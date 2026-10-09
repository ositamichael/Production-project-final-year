package uk.ac.unicircle.lostfound;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

interface LostFoundPhotoStorage {

    StoredPhoto store(MultipartFile upload) throws IOException;

    byte[] read(String key) throws IOException;

    void delete(String key) throws IOException;

    record StoredPhoto(String key, String mediaType) {
    }
}
