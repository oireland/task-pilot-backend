package com.oireland.prompt;

import org.springframework.stereotype.Component;

@Component
public class PromptFactory {

    public final String exercisePatternPromptTemplate = """
    You are a technical writing assistant. Your task is to meticulously extract all numbered exercises from a technical or instructional document.

    Follow these instructions precisely:
    1. Identify every line that begins with the pattern "Exercise <Number>".
    2. The 'Task Name' MUST be the exercise identifier itself (e.g., "Exercise 1.1", "Exercise 2.3.4").
    3. The 'Description' MUST be the full text of the exercise problem that follows the identifier.
    4. The 'Status' MUST always be "Not started".
    5. IMPORTANT: Ignore all other text, such as notes, theory, or examples. Focus only on the numbered exercises.

    Here is a perfect example of the format:
    ---
    INPUT DOCUMENT:
    ...and thus we prove the Mean Value Theorem. The formula is f'(c) = (f(b) - f(a)) / (b - a).

    Exercise 3.1 Find the derivative of f(x) = x^2 * sin(x).

    This leads us to integration by parts.

    Exercise 3.2 Calculate the integral of ln(x) dx from 1 to e.
    ---
    YOUR JSON OUTPUT:
    {
      "tasks": [
        {
          "Task Name": "Exercise 3.1",
          "Status": "Not started",
          "Description": "Find the derivative of f(x) = x^2 * sin(x)."
        },
        {
          "Task Name": "Exercise 3.2",
          "Status": "Not started",
          "Description": "Calculate the integral of ln(x) dx from 1 to e."
        }
      ]
    }
    ---

    Now, process the real document below following these instructions and the example format perfectly. Respond ONLY with the valid JSON object.

    REAL DOCUMENT:
    ---
    %s
    ---
    """;

    public final String generalTaskPromptTemplate = """
    You are an expert project manager's assistant. Your task is to extract tasks from the following document.
    Analyze the text and identify all actionable tasks. For each task, extract the following information:
    - 'Task Name': A concise name for the task.
    - 'Status': Default to 'Not started'.
    - 'Description': A one or two-sentence summary of the task's context and purpose.

    Respond ONLY with a valid JSON object containing a single key "tasks" which is an array of the tasks you found. Do not include any other text, explanations, or apologies.

    Here is the document:
    ---
    %s
    ---
    """;
}
