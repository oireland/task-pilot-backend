package com.taskpilot.dto.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchUpdateTaskDTO(@NotEmpty @Valid List<UpdateTaskWithIdDTO> tasks) {}