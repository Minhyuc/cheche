package com.cheche.inspection.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Stores inspection photos in Cloud Storage when the service runs on Cloud Run. */
@Component
@ConditionalOnProperty(name = "cheche.storage.type", havingValue = "gcs")
public class GcsPhotoStorage implements PhotoStorage {
    private final Storage storage;
    private final String bucket;

    public GcsPhotoStorage(@Value("${cheche.storage.bucket:}") String bucket) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("CHECHE_GCS_BUCKET must be set when CHECHE_STORAGE_TYPE=gcs");
        }
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucket = bucket;
    }

    @Override
    public String store(MultipartFile photo) {
        validateImage(photo);

        String original = Path.of(photo.getOriginalFilename() == null ? "inspection.jpg" : photo.getOriginalFilename())
                .getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectName = "inspection-photos/" + UUID.randomUUID() + "-" + original;

        try {
            BlobInfo blob = BlobInfo.newBuilder(BlobId.of(bucket, objectName))
                    .setContentType(photo.getContentType())
                    .build();
            storage.create(blob, photo.getBytes());
            return "https://storage.googleapis.com/" + bucket + "/" + objectName;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사진 저장에 실패했습니다.", exception);
        }
    }

    private void validateImage(MultipartFile photo) {
        if (photo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진이 필요합니다.");
        }
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다.");
        }
    }
}
