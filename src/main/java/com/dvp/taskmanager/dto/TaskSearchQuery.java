package com.dvp.taskmanager.dto;

import com.dvp.taskmanager.model.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Параметры выборки задач")
public class TaskSearchQuery {

    @Schema(description = "Фильтр по статусу", example = "IN_PROGRESS")
    private TaskStatus status;

    @Schema(description = "Фильтр по исполнителю", example = "1")
    private Long assigneeId;

    @Schema(description = "Фильтр по автору", example = "2")
    private Long authorId;
}