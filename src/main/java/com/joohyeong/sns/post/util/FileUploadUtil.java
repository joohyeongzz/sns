package com.joohyeong.sns.post.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileUploadUtil {

    public List<byte[]> convertToBytes(List<MultipartFile> files) {
        return files.stream()
                .map(this::convertToBytes)
                .collect(Collectors.toList());
    }

    public byte[] convertToBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert file to byte array", e);
        }
    }
}