#!/usr/bin/env python3
"""
Claude Reviewer - AI code review using Anthropic API
"""

import json
import os
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, asdict
from anthropic import Anthropic


@dataclass
class Issue:
    """Code review issue"""
    severity: str  # "critical", "major", "minor", "info"
    category: str  # "architecture", "style", "bug", "security"
    title: str
    description: str
    file: Optional[str] = None
    line: Optional[int] = None
    suggestion: Optional[str] = None


@dataclass
class ReviewOutput:
    """Structured review output"""
    architecture_issues: List[Issue]
    style_issues: List[Issue]
    bug_risks: List[Issue]
    security_issues: List[Issue]
    positive_notes: List[str]
    summary: str


@dataclass
class ReviewContext:
    """Context for code review"""
    pr_diff: str
    changed_files: List[str]
    relevant_docs: List[str]
    base_ref: str
    head_ref: str


class ClaudeReviewer:
    """
    Claude API integration for automated code review
    """

    def __init__(self, api_key: str, model: str = "claude-sonnet-4-5-20250929"):
        self.client = Anthropic(api_key=api_key)
        self.model = model

    def _build_system_prompt(self) -> str:
        """Build system prompt for code review"""
        return """You are an expert Android/Kotlin code reviewer specializing in:
- Clean Architecture (domain/data/presentation layers)
- MVVM pattern
- Kotlin conventions and best practices
- Jetpack Compose UI
- Coroutine error handling and lifecycle management
- Hilt dependency injection
- Room database patterns
- Security best practices

Review code changes for:
1. ARCHITECTURE: Clean Architecture violations, layer dependencies, SOLID principles
2. CODE STYLE: Kotlin naming conventions, documentation, code organization
3. BEST PRACTICES: Android patterns, Compose guidelines, proper use of APIs
4. BUG RISKS: Null safety, coroutine cancellation, lifecycle issues, memory leaks
5. SECURITY: API key exposure, SQL injection, insecure data storage, XSS

Use the provided project documentation to ensure consistency with existing patterns.

Output ONLY valid JSON matching this exact schema:
{
  "architecture_issues": [{"severity": "major|minor|info", "category": "architecture", "title": "string", "description": "string", "file": "string", "line": 0, "suggestion": "string"}],
  "style_issues": [{"severity": "minor|info", "category": "style", "title": "string", "description": "string", "file": "string", "line": 0}],
  "bug_risks": [{"severity": "critical|major|minor", "category": "bug", "title": "string", "description": "string", "file": "string", "line": 0, "suggestion": "string"}],
  "security_issues": [{"severity": "critical|major", "category": "security", "title": "string", "description": "string", "file": "string", "line": 0, "suggestion": "string"}],
  "positive_notes": ["string"],
  "summary": "string"
}

Be specific: include file paths and line numbers where possible."""

    def _build_user_prompt(self, context: ReviewContext) -> str:
        """Build user prompt with review context"""

        # Format changed files list
        files_list = "\n".join(f"- {f}" for f in context.changed_files[:50])  # Limit to 50 files

        # Format relevant documentation
        docs_section = ""
        if context.relevant_docs:
            docs_section = "## Project Documentation (RAG Context)\n\n"
            for idx, doc in enumerate(context.relevant_docs[:5], 1):  # Top 5
                docs_section += f"### Document {idx}\n{doc}\n\n"

        # Build full prompt
        prompt = f"""Review this Pull Request against ChatAgent project standards:

## PR Information
Base: {context.base_ref}
Head: {context.head_ref}
Files Changed: {len(context.changed_files)}

## Changed Files
{files_list}

{docs_section}
## Code Diff
```diff
{context.pr_diff[:50000]}  # Limit diff to 50k chars
```

Analyze for:
- Clean Architecture compliance (domain must not depend on data/presentation)
- Proper use of Repository pattern
- Hilt DI best practices
- Compose state management (remember, rememberSaveable, derivedStateOf)
- Coroutine error handling (try/catch, SupervisorJob, proper scopes)
- Kotlin naming conventions (camelCase for functions/variables, PascalCase for classes)
- Security issues (API keys in code, SQL injection, insecure storage)

Provide specific file/line references. Focus on issues that impact correctness, maintainability, or security."""

        return prompt

    def review_code(self, context: ReviewContext) -> Optional[ReviewOutput]:
        """
        Perform code review using Claude API

        Args:
            context: ReviewContext with PR diff and metadata

        Returns:
            ReviewOutput with structured review, or None if failed
        """
        try:
            system_prompt = self._build_system_prompt()
            user_prompt = self._build_user_prompt(context)

            print(f"[ClaudeReviewer] Calling Claude API (model: {self.model})...")
            print(f"[ClaudeReviewer] Prompt size: {len(user_prompt)} chars")

            # Call Claude API
            response = self.client.messages.create(
                model=self.model,
                max_tokens=4096,
                system=system_prompt,
                messages=[{
                    "role": "user",
                    "content": user_prompt
                }]
            )

            # Extract response text
            response_text = response.content[0].text

            print(f"[ClaudeReviewer] Received response: {len(response_text)} chars")

            # Parse JSON response
            review_data = json.loads(response_text)

            # Convert to ReviewOutput
            review = ReviewOutput(
                architecture_issues=[Issue(**issue) for issue in review_data.get("architecture_issues", [])],
                style_issues=[Issue(**issue) for issue in review_data.get("style_issues", [])],
                bug_risks=[Issue(**issue) for issue in review_data.get("bug_risks", [])],
                security_issues=[Issue(**issue) for issue in review_data.get("security_issues", [])],
                positive_notes=review_data.get("positive_notes", []),
                summary=review_data.get("summary", "")
            )

            return review

        except json.JSONDecodeError as e:
            print(f"[ERROR] Failed to parse Claude response as JSON: {e}")
            print(f"Response was: {response_text[:500]}")
            return None

        except Exception as e:
            print(f"[ERROR] Failed to perform review: {e}")
            return None

    def format_review_markdown(self, review: ReviewOutput) -> str:
        """
        Format review output as markdown for PR comment

        Args:
            review: ReviewOutput with issues and summary

        Returns:
            Formatted markdown string
        """
        lines = []

        lines.append("## AI Code Review")
        lines.append("")
        lines.append("### Summary")
        lines.append(review.summary)
        lines.append("")

        # Statistics
        total_issues = (
            len(review.architecture_issues) +
            len(review.style_issues) +
            len(review.bug_risks) +
            len(review.security_issues)
        )

        critical_count = sum(1 for issue in review.bug_risks + review.security_issues if issue.severity == "critical")
        major_count = sum(1 for issues in [review.architecture_issues, review.bug_risks, review.security_issues]
                         for issue in issues if issue.severity == "major")
        minor_count = sum(1 for issues in [review.architecture_issues, review.style_issues, review.bug_risks]
                         for issue in issues if issue.severity == "minor")

        lines.append("### Statistics")
        lines.append(f"- **Total Issues:** {total_issues}")
        lines.append(f"- **Critical:** {critical_count}")
        lines.append(f"- **Major:** {major_count}")
        lines.append(f"- **Minor:** {minor_count}")
        lines.append("")

        # Architecture issues
        if review.architecture_issues:
            lines.append("### Architecture Issues")
            lines.append("")
            for issue in review.architecture_issues:
                self._format_issue(lines, issue)

        # Security issues
        if review.security_issues:
            lines.append("### Security Issues")
            lines.append("")
            for issue in review.security_issues:
                self._format_issue(lines, issue)

        # Bug risks
        if review.bug_risks:
            lines.append("### Potential Bugs")
            lines.append("")
            for issue in review.bug_risks:
                self._format_issue(lines, issue)

        # Style issues
        if review.style_issues:
            lines.append("### Code Style")
            lines.append("")
            for issue in review.style_issues:
                self._format_issue(lines, issue)

        # Positive notes
        if review.positive_notes:
            lines.append("### Positive Notes")
            lines.append("")
            for note in review.positive_notes:
                lines.append(f"- {note}")
            lines.append("")

        lines.append("---")
        lines.append("*Generated by Claude Sonnet 4.5*")

        return "\n".join(lines)

    def _format_issue(self, lines: List[str], issue: Issue) -> None:
        """Format a single issue in markdown"""
        severity_emoji = {
            "critical": "",
            "major": "",
            "minor": "",
            "info": ""
        }

        lines.append(f"#### {severity_emoji.get(issue.severity, '')} {issue.severity.upper()}: {issue.title}")

        if issue.file:
            location = f"**File:** `{issue.file}`"
            if issue.line:
                location += f" (line {issue.line})"
            lines.append(location)
            lines.append("")

        lines.append(issue.description)
        lines.append("")

        if issue.suggestion:
            lines.append("**Suggestion:**")
            lines.append(f"```kotlin\n{issue.suggestion}\n```")
            lines.append("")


if __name__ == '__main__':
    # Test the Claude reviewer
    print("Testing ClaudeReviewer...")

    api_key = os.getenv("ANTHROPIC_API_KEY")
    if not api_key:
        print("[ERROR] ANTHROPIC_API_KEY environment variable not set")
        exit(1)

    reviewer = ClaudeReviewer(api_key)

    # Create test context
    context = ReviewContext(
        pr_diff="""diff --git a/Test.kt b/Test.kt
index 1234567..abcdefg 100644
--- a/Test.kt
+++ b/Test.kt
@@ -1,3 +1,5 @@
 class Test {
-    fun process() {}
+    fun process() {
+        val data = getData()  // Potential null pointer
+    }
 }""",
        changed_files=["Test.kt"],
        relevant_docs=["Clean Architecture: domain layer should not depend on data layer"],
        base_ref="master",
        head_ref="feature"
    )

    # Perform review
    review = reviewer.review_code(context)

    if review:
        print("\n[OK] Review completed")
        markdown = reviewer.format_review_markdown(review)
        print("\n" + markdown)
    else:
        print("[FAIL] Review failed")
