package com.dvp.taskmanager.controller;

import com.dvp.taskmanager.dto.TaskCommand;
import com.dvp.taskmanager.dto.TaskSearchQuery;
import com.dvp.taskmanager.dto.TaskView;
import com.dvp.taskmanager.security.AuthenticatedUserResolver;
import com.dvp.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Управление задачами")
public class TaskController {

    private final TaskService taskService;
    private final AuthenticatedUserResolver actorProvider;

    public TaskController(TaskService taskService, AuthenticatedUserResolver actorProvider) {
        this.taskService = taskService;
        this.actorProvider = actorProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать задачу", description = "Доступно USER и ADMIN")
    public TaskView create(@Valid @RequestBody TaskCommand command) {
        return taskService.create(command, actorProvider.resolveId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить задачу по ID")
    public TaskView getById(@PathVariable Long id) {
        return taskService.fetchById(id);
    }

    @GetMapping
    @Operation(summary = "Получить список задач", description = "Фильтры: status, assigneeId, authorId")
    public List<TaskView> getAll(@ModelAttribute TaskSearchQuery query) {
        return taskService.search(query);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить задачу", description = "Только автор задачи или админ")
    public TaskView update(@PathVariable Long id, @Valid @RequestBody TaskCommand command) {
        return taskService.modify(id, command, actorProvider.resolveId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить задачу", description = "Только автор задачи или админ")
    public void delete(@PathVariable Long id) {
        taskService.remove(id, actorProvider.resolveId());
    }
}