package com.poppy.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Builder
public class ReviewReqDto {
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private List<MultipartFile> images;

    @NotNull(message = "평점은 필수입니다.")
    @Min(value = 0, message = "평점은 0점 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
    private Double rating;
}
