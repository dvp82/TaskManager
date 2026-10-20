package com.dvp.taskmanager.service;

import com.dvp.taskmanager.dto.TaskCommand;
import com.dvp.taskmanager.dto.TaskSearchQuery;
import com.dvp.taskmanager.dto.TaskView;
import com.dvp.taskmanager.exception.ForbiddenException;
import com.dvp.taskmanager.exception.NotFoundException;
import com.dvp.taskmanager.model.Role;
import com.dvp.taskmanager.model.Task;
import com.dvp.taskmanager.model.TaskPriority;
import com.dvp.taskmanager.model.TaskStatus;
import com.dvp.taskmanager.model.User;
import com.dvp.taskmanager.repository.TaskRepository;
import com.dvp.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;

    private TaskService underTest;

    private User author;
    private User stranger;
    private User boss;

    @BeforeEach
    void setUp() {
        underTest = new TaskService(taskRepository, userRepository);
        author = User.builder().id(1L).username("author").email("author@example.com").role(Role.USER).build();
        stranger = User.builder().id(2L).username("stranger").email("stranger@example.com").role(Role.USER).build();
        boss = User.builder().id(3L).username("boss").email("boss@example.com").role(Role.ADMIN).build();
    }

    @Test
    void create_shouldDefaultStateWhenOmitted() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(userRepository.findById(2L)).thenReturn(Optional.of(stranger));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskView view = underTest.create(new TaskCommand("New task", "desc", null, null, 2L), 1L);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(saved.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(saved.getAuthor().getId()).isEqualTo(1L);
        assertThat(saved.getAssignee().getId()).isEqualTo(2L);
        assertThat(view.getTitle()).isEqualTo("New task");
    }

    @Test
    void modify_shouldDenyStrangerButAllowAdmin() {
        Task owned = Task.builder()
                .id(10L)
                .title("Old")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .author(author)
                .build();
        when(taskRepository.findById(10L)).thenReturn(Optional.of(owned));
        when(userRepository.findById(2L)).thenReturn(Optional.of(stranger));

        TaskCommand command = new TaskCommand("Updated", null, TaskStatus.DONE, TaskPriority.HIGH, null);

        assertThatThrownBy(() -> underTest.modify(10L, command, stranger.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You can modify only tasks you authored");
        verify(taskRepository, never()).save(any(Task.class));

        when(userRepository.findById(3L)).thenReturn(Optional.of(boss));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TaskView updated = underTest.modify(10L, command, boss.getId());

        assertThat(updated.getTitle()).isEqualTo("Updated");
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void remove_shouldFailOnMissingTask() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.remove(99L, author.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task not found (id=99)");

        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    void search_shouldCombineGivenFilters() {
        Task found = Task.builder().id(1L).title("T").status(TaskStatus.IN_PROGRESS).priority(TaskPriority.HIGH)
                .author(author).assignee(stranger)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        when(taskRepository.findAll(any(Specification.class))).thenReturn(List.of(found));

        List<TaskView> views = underTest.search(new TaskSearchQuery(TaskStatus.IN_PROGRESS, 2L, 1L));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskRepository).findAll(any(Specification.class));
    }
}