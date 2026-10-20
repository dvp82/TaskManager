package com.dvp.taskmanager.service;

import com.dvp.taskmanager.dto.TaskCommand;
import com.dvp.taskmanager.dto.TaskSearchQuery;
import com.dvp.taskmanager.dto.TaskView;
import com.dvp.taskmanager.dto.UserView;
import com.dvp.taskmanager.exception.ForbiddenException;
import com.dvp.taskmanager.exception.NotFoundException;
import com.dvp.taskmanager.model.Role;
import com.dvp.taskmanager.model.Task;
import com.dvp.taskmanager.model.TaskPriority;
import com.dvp.taskmanager.model.TaskStatus;
import com.dvp.taskmanager.model.User;
import com.dvp.taskmanager.repository.TaskRepository;
import com.dvp.taskmanager.repository.UserRepository;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskView create(TaskCommand command, Long actorId) {
        Task draft = Task.builder()
                .title(command.getTitle())
                .description(command.getDescription())
                .status(firstOr(command.getStatus(), TaskStatus.TODO))
                .priority(firstOr(command.getPriority(), TaskPriority.MEDIUM))
                .author(requireUser(actorId))
                .assignee(assigneeById(command.getAssigneeId()))
                .build();
        return toView(taskRepository.save(draft));
    }

    @Transactional(readOnly = true)
    public TaskView fetchById(Long id) {
        return toView(requireTask(id));
    }

    @Transactional(readOnly = true)
    public List<TaskView> search(TaskSearchQuery query) {
        return taskRepository.findAll(querySpecification(query)).stream()
                .map(this::toView)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public TaskView modify(Long id, TaskCommand command, Long actorId) {
        Task current = requireTask(id);
        User actor = requireUser(actorId);
        checkAccess(current, actor);

        current.setTitle(command.getTitle());
        current.setDescription(command.getDescription());
        if (command.getStatus() != null) {
            current.setStatus(command.getStatus());
        }
        if (command.getPriority() != null) {
            current.setPriority(command.getPriority());
        }
        current.setAssignee(assigneeById(command.getAssigneeId()));

        return toView(taskRepository.save(current));
    }

    @Transactional
    public void remove(Long id, Long actorId) {
        Task task = requireTask(id);
        checkAccess(task, requireUser(actorId));
        taskRepository.deleteById(id);
    }

    private Task requireTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found (id=" + id + ")"));
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found (id=" + id + ")"));
    }

    private User assigneeById(Long candidateId) {
        if (candidateId == null) {
            return null;
        }
        return userRepository.findById(candidateId)
                .orElseThrow(() -> new NotFoundException("Assignee not found (id=" + candidateId + ")"));
    }

    private void checkAccess(Task task, User actor) {
        boolean isOwner = task.getAuthor().getId().equals(actor.getId());
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You can modify only tasks you authored");
        }
    }

    private Specification<Task> querySpecification(TaskSearchQuery query) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> constraints = new ArrayList<>();
            if (query.getStatus() != null) {
                constraints.add(builder.equal(root.get("status"), query.getStatus()));
            }
            if (query.getAssigneeId() != null) {
                constraints.add(builder.equal(root.get("assignee").get("id"), query.getAssigneeId()));
            }
            if (query.getAuthorId() != null) {
                constraints.add(builder.equal(root.get("author").get("id"), query.getAuthorId()));
            }
            return builder.and(constraints.toArray(new Predicate[0]));
        };
    }

    private TaskView toView(Task task) {
        User author = task.getAuthor();
        User assignee = task.getAssignee();
        return new TaskView(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                new UserView(author.getId(), author.getUsername(), author.getEmail(), author.getRole()),
                assignee != null ? new UserView(assignee.getId(), assignee.getUsername(), assignee.getEmail(), assignee.getRole()) : null,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private static <T> T firstOr(T primary, T fallback) {
        return primary != null ? primary : fallback;
    }
}