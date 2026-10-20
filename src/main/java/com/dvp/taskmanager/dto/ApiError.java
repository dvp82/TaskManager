package com.dvp.taskmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Описание ошибки")
public class ApiError {

    @Schema(example = "404")
    private int status;

    @Schema(example = "Not Found")
    private String error;

    @Schema(example = "Task not found")
    private String message;

    @Schema(example = "/api/tasks/999")
    private String path;

    @Schema(example = "2026-10-19T14:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "Ошибки по полям при валидации")
    private Map<String, String> fieldErrors;

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, LocalDateTime.now(), null);
    }

    public static ApiError of(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(status, error, message, path, LocalDateTime.now(), fieldErrors);
    }
}