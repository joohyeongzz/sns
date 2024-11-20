package com.joohyeong.sns.global;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.joohyeong.sns.global.dto.request.PresignedUrlRequest;
import com.joohyeong.sns.global.dto.response.PresignedUrlResponse;
import com.joohyeong.sns.post.domain.Media;
import com.joohyeong.sns.post.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 s3Client;
    private final MediaRepository mediaRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public PresignedUrlResponse generatePresignedUrls(PresignedUrlRequest request) {
        if (request.getFileNames().size() > 10) {
            throw new IllegalArgumentException("최대 10개의 파일만 업로드할 수 있습니다.");
        }

        List<String> presignedUrls = new ArrayList<>();
        List<String> objectKeys = new ArrayList<>();

        for (int i = 0; i < request.getFileNames().size(); i++) {
            String fileName = request.getFileNames().get(i);
            String contentType = request.getContentTypes().get(i);

            String objectKey = generateUniqueObjectKey(fileName);
            objectKeys.add(objectKey);

            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectKey)
                    .withMethod(HttpMethod.PUT)
                    .withContentType(contentType)
                    .withExpiration(getPreSignedUrlExpiration());

            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
            presignedUrls.add(url.toString());
        }

        PresignedUrlResponse response = new PresignedUrlResponse();
        response.setPresignedUrls(presignedUrls);
        response.setObjectKeys(objectKeys);
        return response;
    }


    private String generateUniqueObjectKey(String fileName) {
        String uuid = UUID.randomUUID().toString();
        String extension = fileName.substring(fileName.lastIndexOf("."));
        return uuid + extension;
    }

    private Date getPreSignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 30; // 30분
        expiration.setTime(expTimeMillis);
        return expiration;
    }

    public String getObjectUrl(String objectKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, s3Client.getRegion(), objectKey);
    }

    public void saveUploadedFileUrls(List<String> s3Urls) {
        Media media = new Media();


        for (int i = 0; i < s3Urls.size(); i++) {
            String methodName = "url_" + (i + 1);
            try {
                Method method = Media.class.getMethod(methodName, String.class);
                method.invoke(media, s3Urls.get(i));
            } catch (Exception e) {
                throw new RuntimeException("Error setting URL for index: " + i, e);
            }
        }

        mediaRepository.save(media);
    }





}