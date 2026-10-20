package com.dvp.taskmanager.dto;

import com.dvp.taskmanager.model.TaskPriority;
import com.dvp.taskmanager.model.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные задачи")
public class TaskCommand {

    @NotBlank(message = "Title is required")
    @Schema(description = "Название", example = "Написать отчёт")
    private String title;

    @Schema(description = "Описание", example = "Подготовить квартальный отчёт")
    private String description;

    @Schema(description = "Статус", example = "TODO")
    private TaskStatus status;

    @Schema(description = "Приоритет", example = "MEDIUM")
    private TaskPriority priority;

    @Schema(description = "ID исполнителя (может отсутствовать)", example = "2")
    private Long assigneeId;
}