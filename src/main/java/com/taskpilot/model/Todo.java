package com.taskpilot.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "todos")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean checked = false;

    @Column
    private LocalDateTime deadline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_list_id", nullable = false)
    private TaskList taskList;

    public Todo() {}

    public Todo(String content, boolean checked, LocalDateTime deadline, TaskList taskList) {
        this.content = content;
        this.checked = checked;
        this.deadline = deadline;
        this.taskList = taskList;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public TaskList getTaskList() { return taskList; }
    public void setTaskList(TaskList taskList) { this.taskList = taskList; }
}