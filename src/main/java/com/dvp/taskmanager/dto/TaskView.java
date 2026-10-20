package com.dvp.taskmanager.dto;

import com.dvp.taskmanager.model.TaskPriority;
import com.dvp.taskmanager.model.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Задача для чтения")
public class TaskView {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Написать отчёт")
    private String title;

    @Schema(example = "Подготовить квартальный отчёт")
    private String description;

    @Schema(example = "TODO")
    private TaskStatus status;

    @Schema(example = "MEDIUM")
    private TaskPriority priority;

    @Schema(description = "Автор задачи")
    private UserView author;

    @Schema(description = "Исполнитель (может отсутствовать)")
    private UserView assignee;

    @Schema(example = "2026-10-19T14:00:00")
    private LocalDateTime createdAt;

    @Schema(example = "2026-10-19T14:00:00")
    private LocalDateTime updatedAt;
}