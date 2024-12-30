package com.poppy.common.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.poppy.common.entity.Images;
import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.common.repository.ImageRepository;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.review.entity.Review;
import com.poppy.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final AmazonS3Client amazonS3Client;
    private final ImageRepository imageRepository;
    private final PopupStoreRepository popupStoreRepository;
    private final ReviewRepository reviewRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    private String generateCloudFrontUrl(String storedFileName) {
        String encodedFileName = URLEncoder.encode(storedFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return String.format("https://%s/%s", cloudfrontDomain, encodedFileName);
    }

    @Transactional
    public Images uploadImageFromMultipart(MultipartFile file, String entityType, Long entityId) {
        try {
            validateImageFile(file);

            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String storedFileName = generateStoredFileName(originalFileName);

            // S3 업로드 시도
            String cloudFrontUrl = uploadMultipartToS3(file, storedFileName);

            // DB 저장
            return saveImageEntity(originalFileName, storedFileName, cloudFrontUrl, entityType, entityId);
        }
        catch (Exception e) {
            // S3 업로드 실패 시 이미 업로드된 파일 삭제 시도
            log.error("Image upload failed: ", e);
            try {
                amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, generateStoredFileName(file.getOriginalFilename())));
            } catch (Exception ignored) {}

            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    private String uploadMultipartToS3(MultipartFile file, String storedFileName) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        metadata.setCacheControl("public, max-age=31536000"); // 1년 캐시

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucket,
                    storedFileName,
                    inputStream,
                    metadata
            ).withCannedAcl(CannedAccessControlList.PublicRead);

            amazonS3Client.putObject(putObjectRequest);
            return generateCloudFrontUrl(storedFileName);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_IS_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.contains("..")) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
        }
    }

    private String generateStoredFileName(String originalFileName) {
        String ext = Optional.ofNullable(originalFileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(originalFileName.lastIndexOf(".")))
                .orElse("");
        return UUID.randomUUID() + ext;
    }

    private Images saveImageEntity(String originalFileName, String storedFileName, String cloudFrontUrl,
                                   String entityType, Long entityId) {
        Images images = Images.builder()
                .originName(originalFileName)
                .storedName(storedFileName)
                .uploadUrl(cloudFrontUrl)
                .build();

        switch (entityType) {
            case "PopupStore" -> {
                PopupStore popupStore = popupStoreRepository.findById(entityId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
                images.updatePopupStore(popupStore);
            }
            case "Review" -> {
                Review review = reviewRepository.findById(entityId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
                images.updateReview(review);
            }
        }

        return imageRepository.save(images);
    }

    @Transactional
    public void deleteImage(Long imageId) {
        Images image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        try {
            amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, image.getStoredName()));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.IMAGE_DELETE_FAILED);
        }

        imageRepository.delete(image);
    }
}
