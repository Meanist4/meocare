package com.meocare.meocare.dto;

public record FileUploadResponse(
        String url,
        String fileName,
        String contentType,
        long size
) {
}
