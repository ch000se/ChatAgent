#!/usr/bin/env python3
"""
Main PR Review Script
Orchestrates the entire PR review process
"""

import os
import sys
import json
from typing import Optional

from mcp_client import McpClient
from rag_engine import DocumentIndexer
from claude_reviewer import ClaudeReviewer, ReviewContext
from github_api import GitHubAPI


class PRReviewSystem:
    """Main orchestrator for PR review"""

    def __init__(
        self,
        github_token: str,
        anthropic_key: str,
        repo: str,
        mcp_url: str = "http://localhost:3002",
        docs_path: str = "../../app/src/main/assets/docs"
    ):
        self.mcp_client = McpClient(mcp_url)
        self.rag_indexer = DocumentIndexer(docs_path)
        self.claude_reviewer = ClaudeReviewer(anthropic_key)
        self.github_api = GitHubAPI(github_token, repo)

    def review_pr(
        self,
        pr_number: int,
        base_ref: str,
        head_ref: str
    ) -> bool:
        """
        Perform complete PR review

        Args:
            pr_number: Pull request number
            base_ref: Base branch reference (e.g., 'origin/master')
            head_ref: Head branch reference (e.g., 'HEAD')

        Returns:
            True if review was successful
        """
        print("=" * 60)
        print(f"Starting PR Review for PR #{pr_number}")
        print(f"Base: {base_ref} -> Head: {head_ref}")
        print("=" * 60)

        # Step 1: Check MCP server
        print("\n[1/6] Checking MCP Git Server...")
        if not self.mcp_client.health_check():
            print("[ERROR] MCP server is not available!")
            print("Please start: cd mcp_servers && python git_server.py")
            return False
        print("[OK] MCP server is healthy")

        # Step 2: Get PR diff
        print("\n[2/6] Fetching PR diff...")
        pr_diff = self.mcp_client.get_pr_diff(base_ref, head_ref, context_lines=3)
        if pr_diff is None:
            print("[ERROR] Failed to get PR diff")
            return False

        if not pr_diff:
            print("[WARNING] PR has no changes")
            return True

        print(f"[OK] Fetched diff: {len(pr_diff)} characters")

        # Step 3: Get changed files
        print("\n[3/6] Getting changed files...")
        changed_files = self.mcp_client.get_changed_files(base_ref, head_ref)
        file_paths = [f.filepath for f in changed_files]
        print(f"[OK] Found {len(file_paths)} changed files")

        # Step 4: Index documentation and search
        print("\n[4/6] Indexing project documentation...")
        chunk_count = self.rag_indexer.index_documents()
        print(f"[OK] Indexed {chunk_count} documentation chunks")

        print("\n[4/6] Searching relevant documentation...")
        # Create search query from changed files and diff
        search_query = f"code review best practices {' '.join(file_paths[:10])}"
        search_results = self.rag_indexer.search(search_query, top_k=5)

        relevant_docs = []
        for result in search_results:
            doc_text = f"[{result.filename}] (similarity: {result.similarity:.2f})\n{result.text}"
            relevant_docs.append(doc_text)

        print(f"[OK] Found {len(relevant_docs)} relevant documentation chunks")

        # Step 5: Perform AI review
        print("\n[5/6] Performing AI code review with Claude...")
        context = ReviewContext(
            pr_diff=pr_diff,
            changed_files=file_paths,
            relevant_docs=relevant_docs,
            base_ref=base_ref,
            head_ref=head_ref
        )

        review = self.claude_reviewer.review_code(context)
        if not review:
            print("[ERROR] Failed to perform code review")
            return False

        print("[OK] Review completed")

        # Format review as markdown
        markdown = self.claude_reviewer.format_review_markdown(review)

        # Step 6: Post to GitHub
        print("\n[6/6] Posting review to GitHub...")

        # Check for existing bot comment
        existing_comment_id = self.github_api.find_bot_comment(pr_number)

        if existing_comment_id:
            print(f"[INFO] Updating existing comment {existing_comment_id}")
            success = self.github_api.update_pr_comment(existing_comment_id, markdown)
        else:
            print("[INFO] Creating new comment")
            success = self.github_api.post_pr_comment(pr_number, markdown)

        if success:
            print("[OK] Review posted to GitHub")
            print("=" * 60)
            print("PR Review Complete!")
            print("=" * 60)
            return True
        else:
            print("[ERROR] Failed to post review")
            return False


def main():
    """Main entry point"""
    # Get environment variables
    github_token = os.getenv("GITHUB_TOKEN")
    anthropic_key = os.getenv("ANTHROPIC_API_KEY")
    pr_number = os.getenv("PR_NUMBER")
    repo = os.getenv("REPO_NAME")
    base_ref = os.getenv("BASE_REF", "origin/master")
    head_ref = os.getenv("HEAD_REF", "HEAD")
    mcp_url = os.getenv("MCP_URL", "http://localhost:3002")

    # Validate required variables
    if not github_token:
        print("[ERROR] GITHUB_TOKEN environment variable not set")
        sys.exit(1)

    if not anthropic_key:
        print("[ERROR] ANTHROPIC_API_KEY environment variable not set")
        sys.exit(1)

    if not pr_number:
        print("[ERROR] PR_NUMBER environment variable not set")
        sys.exit(1)

    if not repo:
        print("[ERROR] REPO_NAME environment variable not set")
        sys.exit(1)

    # Convert PR number to int
    try:
        pr_number = int(pr_number)
    except ValueError:
        print(f"[ERROR] Invalid PR_NUMBER: {pr_number}")
        sys.exit(1)

    # Create review system
    system = PRReviewSystem(
        github_token=github_token,
        anthropic_key=anthropic_key,
        repo=repo,
        mcp_url=mcp_url
    )

    # Perform review
    success = system.review_pr(pr_number, base_ref, head_ref)

    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
