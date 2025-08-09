package com.taskpilot.dto.notion;

import com.taskpilot.dto.task.ExtractedDocDataDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotionApiV1Test {

    @Test
    void buildCreateTaskRequest_shouldParseEquationsInTasksCorrectly() {
        // --- ARRANGE ---
        var docData = new ExtractedDocDataDTO(
                "Math Problems",
                "Not Started",
                "A list of calculus exercises.",
                List.of(
                        "1. Solve the equation (/ E = mc^2 /) for m.",
                        "2. This task starts with text (/a/) and ends with an equation (/b/).",
                        "(/c/) This task starts with an equation."
                )
        );

        // --- ACT ---
        var request = NotionApiV1.buildCreateTaskRequest(docData, "test_db_id");
        List<NotionApiV1.TodoBlock> children = request.children();

        // --- ASSERT ---
        assertThat(children).hasSize(3);

        // Test Case 1: Equation in the middle
        var task1RichText = children.getFirst().todo().richText();
        assertThat(task1RichText).hasSize(3);
        assertThat(task1RichText.get(0).type()).isEqualTo("text");
        assertThat(task1RichText.get(0).text().content()).isEqualTo("1. Solve the equation ");
        assertThat(task1RichText.get(1).type()).isEqualTo("equation");
        assertThat(task1RichText.get(1).equation().expression()).isEqualTo("E = mc^2");
        assertThat(task1RichText.get(2).type()).isEqualTo("text");
        assertThat(task1RichText.get(2).text().content()).isEqualTo(" for m.");

        // Test Case 2: Multiple equations
        var task2RichText = children.get(1).todo().richText();
        assertThat(task2RichText).hasSize(5); // Correctly assert 5 parts
        assertThat(task2RichText.get(0).text().content()).isEqualTo("2. This task starts with text ");
        assertThat(task2RichText.get(1).equation().expression()).isEqualTo("a");
        assertThat(task2RichText.get(2).text().content()).isEqualTo(" and ends with an equation ");
        assertThat(task2RichText.get(3).equation().expression()).isEqualTo("b");
        assertThat(task2RichText.get(4).text().content()).isEqualTo(".");

        // Test Case 3: Starts with an equation
        var task3RichText = children.get(2).todo().richText();
        assertThat(task3RichText).hasSize(2);
        assertThat(task3RichText.get(0).type()).isEqualTo("equation");
        assertThat(task3RichText.get(0).equation().expression()).isEqualTo("c");
        assertThat(task3RichText.get(1).type()).isEqualTo("text");
        assertThat(task3RichText.get(1).text().content()).isEqualTo(" This task starts with an equation.");
    }
}