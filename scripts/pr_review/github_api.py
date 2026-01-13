#!/usr/bin/env python3
"""
GitHub API wrapper for posting PR review comments
"""

import requests
from typing import Optional


class GitHubAPI:
    """Simple GitHub API client for PR comments"""

    def __init__(self, token: str, repo: str):
        """
        Args:
            token: GitHub token
            repo: Repository in format 'owner/repo'
        """
        self.token = token
        self.repo = repo
        self.api_base = "https://api.github.com"
        self.headers = {
            "Authorization": f"token {token}",
            "Accept": "application/vnd.github.v3+json"
        }

    def post_pr_comment(self, pr_number: int, body: str) -> bool:
        """
        Post a comment to PR

        Args:
            pr_number: Pull request number
            body: Comment body (markdown)

        Returns:
            True if successful, False otherwise
        """
        url = f"{self.api_base}/repos/{self.repo}/issues/{pr_number}/comments"

        try:
            response = requests.post(
                url,
                headers=self.headers,
                json={"body": body},
                timeout=30
            )
            response.raise_for_status()
            print(f"[GitHubAPI] Successfully posted comment to PR #{pr_number}")
            return True

        except Exception as e:
            print(f"[ERROR] Failed to post comment: {e}")
            return False

    def update_pr_comment(self, comment_id: int, body: str) -> bool:
        """
        Update existing PR comment

        Args:
            comment_id: Comment ID
            body: New comment body

        Returns:
            True if successful
        """
        url = f"{self.api_base}/repos/{self.repo}/issues/comments/{comment_id}"

        try:
            response = requests.patch(
                url,
                headers=self.headers,
                json={"body": body},
                timeout=30
            )
            response.raise_for_status()
            print(f"[GitHubAPI] Updated comment {comment_id}")
            return True

        except Exception as e:
            print(f"[ERROR] Failed to update comment: {e}")
            return False

    def find_bot_comment(self, pr_number: int, marker: str = "🤖 AI Code Review") -> Optional[int]:
        """
        Find existing bot comment in PR

        Args:
            pr_number: Pull request number
            marker: Text marker to identify bot comment

        Returns:
            Comment ID if found, None otherwise
        """
        url = f"{self.api_base}/repos/{self.repo}/issues/{pr_number}/comments"

        try:
            response = requests.get(url, headers=self.headers, timeout=30)
            response.raise_for_status()

            comments = response.json()
            for comment in comments:
                if marker in comment.get("body", ""):
                    return comment["id"]

            return None

        except Exception as e:
            print(f"[ERROR] Failed to find bot comment: {e}")
            return None
