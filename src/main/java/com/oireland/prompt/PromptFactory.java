package com.oireland.prompt;

import org.springframework.stereotype.Component;

@Component
public class PromptFactory {

    public final String exercisePatternPromptTemplate = """
    You are a technical writing assistant. Your task is to organize exercises into a single document.

    Follow these instructions precisely:
    1. Create ONE document that contains all exercises as a checklist
    2. The 'Title' should be determined from the title of the document if available, or else determine the nature of the exercises e.g. "Calculus Exercises".
    3. The 'Status' MUST be "Not started"
    4. The 'Description' should summarize the set of exercises
    5. Each exercise should be a todo item in the children array
    6. IMPORTANT: Ignore all other text, such as notes, theory, or examples
    7. If a task contains an equation or mathematical expression, format it wrapped in (/ /).
    
    Here is a perfect example of the format:
    ---
    INPUT DOCUMENT:
    Exercise 3.1 Find the derivative of f(x) = x^2 * sin(x).
    Exercise 3.2 Calculate the integral of ln(x) dx from 1 to e.
    ---
    YOUR JSON OUTPUT:
    {
      "Title": "Calculus Exercise List",
      "Status": "Not started",
      "Description": "Calculus exercises covering derivatives and integrals",
      "Tasks": [
        "Exercise 3.1: Find the derivative of f(x) = x^2 * sin(x)",
        "Exercise 3.2: Calculate the integral of ln(x) dx from 1 to e"
      ]
    }
    ---

    Now, process the real document below following these instructions perfectly. Respond ONLY with the valid JSON object.

    REAL DOCUMENT:
    ---
    %s
    ---
    """;

    public final String generalTaskPromptTemplate = """
    You are an expert project manager's assistant. Your task is to organize tasks into a single document.
    Analyze the text and create a document with the following structure:
    - 'Title': A concise name for the task list, preferably derived from the document title or main topic
    - 'Status': Default to 'Not started'
    - 'Description': A brief summary of the overall task list
    - 'Tasks': An array of individual tasks as strings, each to be converted to a todo item
    
    If a task contains an equation or mathematical expression, format it wrapped in (/ /).
    Respond ONLY with a valid JSON object that matches this exact structure. Do not include any other text.

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
        * For **inline** equations (those appearing within a line of text), enclose the LaTeX code in single dollar signs. For example: `$E = mc^2$`.
        * If there is an equation which has words in between several parts of the equation, treat each equation as its own inline equation. For example: `Prove that $\\sum_{i=1}^{n} x_i = \\frac{n(n+1)}{2}$ holds for all integers $n$ greater than 0.`.
        * For **displayed** or **block** equations (those set apart on their own lines), enclose the LaTeX code in double dollar signs. For example: `$$`\\sum_{i=1}^{n} x_i = \\frac{n(n+1)}{2}`$$`.
    3.  **Accuracy is Key**: Ensure the extracted text and the LaTeX representations of the equations are completely accurate. Do not add any commentary, interpretation, or text that is not present in the original document.

    Begin the extraction now.
    """;
}