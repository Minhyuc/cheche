package com.cheche.inspection.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
@ConditionalOnProperty(name = "cheche.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalPhotoStorage implements PhotoStorage {
    private final Path uploadRoot;

    public LocalPhotoStorage(@Value("${cheche.upload-dir:./uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile photo) {
        if (photo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진이 필요합니다.");
        }
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다.");
        }
        String original = Path.of(photo.getOriginalFilename() == null ? "inspection.jpg" : photo.getOriginalFilename())
                .getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedName = UUID.randomUUID() + "-" + original;
        try {
            Files.createDirectories(uploadRoot);
            photo.transferTo(uploadRoot.resolve(storedName));
            // TODO(storage): production에서는 S3/R2 등 객체 저장소 URL로 교체합니다.
            return "/inspection-photos/" + storedName;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사진 저장에 실패했습니다.", e);
        }
    }
}
