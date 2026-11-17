package uk.ac.unicircle.identity;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

interface ProfilePhotoStorage {

    StoredPhoto store(MultipartFile upload) throws IOException;

    byte[] read(String key) throws IOException;

    void delete(String key) throws IOException;

    record StoredPhoto(String key, String mediaType) {
    }
}

