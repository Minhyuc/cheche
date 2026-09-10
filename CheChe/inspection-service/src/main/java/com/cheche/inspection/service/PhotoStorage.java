package com.cheche.inspection.service;

import org.springframework.web.multipart.MultipartFile;

public interface PhotoStorage {
    String store(MultipartFile photo);
}
