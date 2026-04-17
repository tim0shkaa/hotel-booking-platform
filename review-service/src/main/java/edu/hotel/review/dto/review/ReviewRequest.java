package edu.hotel.review.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "ID бронирования обязателен")
    private Long bookingId;

    @NotNull(message = "Общая оценка обязательна")
    @Min(value = 1, message = "Оценка не может быть меньше 1")
    @Max(value = 5, message = "Оценка не может быть больше 5")
    private Integer overallRating;

    @NotNull(message = "Оценка чистоты обязательна")
    @Min(value = 1, message = "Оценка не может быть меньше 1")
    @Max(value = 5, message = "Оценка не может быть больше 5")
    private Integer cleanlinessRating;

    @NotNull(message = "Оценка сервиса обязательна")
    @Min(value = 1, message = "Оценка не может быть меньше 1")
    @Max(value = 5, message = "Оценка не может быть больше 5")
    private Integer serviceRating;

    @NotNull(message = "Оценка расположения обязательна")
    @Min(value = 1, message = "Оценка не может быть меньше 1")
    @Max(value = 5, message = "Оценка не может быть больше 5")
    private Integer locationRating;

    @NotNull(message = "Оценка соотношения цена/качество обязательна")
    @Min(value = 1, message = "Оценка не может быть меньше 1")
    @Max(value = 5, message = "Оценка не может быть больше 5")
    private Integer valueRating;

    @NotBlank(message = "Текст отзыва обязателен")
    private String body;
}
