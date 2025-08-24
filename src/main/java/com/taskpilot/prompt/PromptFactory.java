package com.taskpilot.prompt;

import org.springframework.stereotype.Component;

@Component
public class PromptFactory {

    public final String exercisePatternPromptTemplate = """
    You are a technical writing assistant. Your task is to organize exercises into a single document.

    Follow these instructions precisely:
    1. Create ONE document that contains all exercises as a checklist
    2. The 'Title' should be determined from the title of the document if available, or else determine the nature of the exercises e.g. "Calculus Exercises".
    3. The 'Description' should summarize the set of exercises
    4. Each exercise should be a todo item in the children array
    5. IMPORTANT: Ignore all other text, such as notes, theory, or examples
    6. If a task contains an equation or mathematical expression, format it wrapped in (/ /).
    7. If there is regular text within an equation, you must split the equation up so that the regular text is not in the (/ /)
       For example, instead of this: '(/ a_n = 1/n for n<1000, 1/n² for n≥1000 /)', you must write '(/ a_n = 1/n /) for (/ n<1000, 1/n² /) for (/ n≥1000 /)'
    
    Here is a perfect example of the format:
    ---
    INPUT DOCUMENT:
    Exercise 3.1 Find the derivative of f(x) = x^2 * sin(x).
    Exercise 3.2 Calculate the integral of ln(x) dx from 1 to e.
    ---
    YOUR JSON OUTPUT:
    {
      "title": "Calculus Exercise List",
      "description": "Calculus exercises covering derivatives and integrals",
      "tasks": [
        "Exercise 3.1: Find the derivative of f(x) = x^2 * sin(x)",
        "Exercise 3.2: Calculate the integral of ln(x) dx from 1 to e"
      ]
    }
    ---

    Now, process the real document below following these instructions perfectly. Respond ONLY with the valid JSON object. DO NOT write the word json before the output. Your family will be murdered if you do.

    REAL DOCUMENT:
    ---
    %s
    ---
    """;

    public final String generalTaskPromptTemplate = """
    You are an expert project manager's assistant. Your task is to organize tasks into a single document.
    Analyze the text and create a document with the following structure:
    - 'title': A concise name for the task list, preferably derived from the document title or main topic
    - 'description': A brief summary of the overall task list
    - 'tasks': An array of individual tasks as strings, each to be converted to a todo item
    
    If a task contains an equation or mathematical expression, format it wrapped in (/ /).
    If there is regular text within an equation, you must split the equation up so that the regular text is not in the (/ /)
    For example, instead of this: '(/ a_n = 1/n for n<1000, 1/n² for n≥1000 /)', you must write '(/ a_n = 1/n /) for (/ n<1000, 1/n² /) for (/ n≥1000 /)'
    Respond ONLY with a valid JSON object that matches this exact structure. Do not include any other text. DO NOT write the word json before the output.

    Here is the document to analyze:
    ---
    %s
    ---
    """;

    public final String pdfTextAndMathExtractor = """
            You are an expert data extractor. Your task is to process the attached PDF file and extract all its text content.
            
            Follow these rules precisely:
            1.  **Extract All Text**: Transcribe all text from the document, including titles, headings, paragraphs, captions, and text in tables. Maintain the original paragraph structure as best as possible.
            2.  **Format Math as LaTeX**: Identify all mathematical equations and formulas. Convert them into valid LaTeX format.
                * ALL equations must be represented as inline equations
                * You must wrap ALL latex code in (/ /). It is illegal to not do this
                * Where there is text in between equations, you MUST NOT put this text inside the latex code. For example: You must write `Prove (/ a_n = 1 /) if (/ n < 1000 /)` instead of `Prove (/ a_n = 1 if n < 1000 /)`
                * Where there is a limit, e.g. lim n->0, you MUST use the correct latex \\lim_{n \to 0}
                * For functions like cos, sin etc. that seem to be just plain english words, you MUST use the correct latex so that it is formatted correctly. E.g. You MUST write `\\sin(3n)` instead of `sin(3n)`
            3.  **Accuracy is Key**: Ensure the extracted text and the LaTeX representations of the equations are completely accurate. Do not add any commentary, interpretation, or text that is not present in the original document.
            Begin the extraction now.
            """;
}