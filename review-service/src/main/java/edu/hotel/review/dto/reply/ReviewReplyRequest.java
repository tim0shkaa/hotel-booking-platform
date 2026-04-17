package edu.hotel.review.dto.reply;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewReplyRequest {

    @NotBlank(message = "Текст ответа обязателен")
    private String body;
}
