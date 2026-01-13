#!/usr/bin/env python3
"""
MCP Client for CI environment
Simple HTTP client for calling MCP Git Server tools
"""

import json
import requests
from typing import Dict, Any, List, Optional
from dataclasses import dataclass


@dataclass
class FileChange:
    """Represents a changed file in PR"""
    status: str  # A, M, D, R (Added, Modified, Deleted, Renamed)
    filepath: str


class McpClient:
    """
    MCP Client for calling Git MCP Server from CI
    """

    def __init__(self, mcp_url: str = "http://localhost:3002"):
        self.mcp_url = mcp_url
        self.request_id = 0

    def _call_tool(self, tool_name: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
        """
        Call MCP tool and return result
        """
        self.request_id += 1

        payload = {
            "jsonrpc": "2.0",
            "id": self.request_id,
            "method": "tools/call",
            "params": {
                "name": tool_name,
                "arguments": arguments
            }
        }

        try:
            response = requests.post(
                self.mcp_url,
                json=payload,
                headers={"Content-Type": "application/json"},
                timeout=60
            )
            response.raise_for_status()

            result = response.json()

            if "error" in result:
                raise Exception(f"MCP Error: {result['error']['message']}")

            # Extract text from content
            content = result.get("result", {}).get("content", [])
            if content and len(content) > 0:
                return {"success": True, "output": content[0]["text"]}
            else:
                return {"success": False, "output": ""}

        except requests.exceptions.RequestException as e:
            raise Exception(f"Failed to call MCP server: {e}")

    def health_check(self) -> bool:
        """Check if MCP server is healthy"""
        try:
            response = requests.get(f"{self.mcp_url}/health", timeout=5)
            response.raise_for_status()
            data = response.json()
            return data.get("status") == "healthy"
        except:
            return False

    def get_pr_diff(self, base: str, head: str, context_lines: int = 3) -> Optional[str]:
        """
        Get unified diff between two commits/branches

        Args:
            base: Base commit/branch (e.g., 'origin/master')
            head: Head commit/branch (e.g., 'HEAD')
            context_lines: Number of context lines (default: 3)

        Returns:
            Unified diff as string, or None if failed
        """
        try:
            result = self._call_tool("git_diff_unified", {
                "base": base,
                "head": head,
                "context_lines": context_lines
            })

            if result["success"]:
                output = result["output"]
                if output == "(empty output)":
                    return ""
                return output
            return None

        except Exception as e:
            print(f"[ERROR] Failed to get PR diff: {e}")
            return None

    def get_changed_files(self, base: str, head: str) -> List[FileChange]:
        """
        Get list of changed files between two commits

        Args:
            base: Base commit/branch
            head: Head commit/branch

        Returns:
            List of FileChange objects
        """
        try:
            result = self._call_tool("git_diff_files", {
                "base": base,
                "head": head
            })

            if not result["success"]:
                return []

            output = result["output"]
            if output == "(empty output)" or not output:
                return []

            # Parse output: "M\tfile.txt\nA\tfile2.txt"
            changes = []
            for line in output.strip().split('\n'):
                if not line:
                    continue

                parts = line.split('\t', 1)
                if len(parts) == 2:
                    status, filepath = parts
                    changes.append(FileChange(status=status, filepath=filepath))

            return changes

        except Exception as e:
            print(f"[ERROR] Failed to get changed files: {e}")
            return []

    def get_file_content(self, commit: str, filepath: str) -> Optional[str]:
        """
        Get file content at specific commit

        Args:
            commit: Commit reference
            filepath: Path to file

        Returns:
            File content as string, or None if failed
        """
        try:
            result = self._call_tool("git_show_file", {
                "commit": commit,
                "filepath": filepath
            })

            if result["success"]:
                return result["output"]
            return None

        except Exception as e:
            print(f"[ERROR] Failed to get file content: {e}")
            return None

    def get_pr_context(self, base_branch: str, head_branch: str) -> Optional[Dict[str, Any]]:
        """
        Get PR context metadata

        Args:
            base_branch: Base branch name
            head_branch: Head branch name

        Returns:
            Dictionary with merge_base, commits, files_changed
        """
        try:
            result = self._call_tool("git_pr_context", {
                "base_branch": base_branch,
                "head_branch": head_branch
            })

            if not result["success"]:
                return None

            output = result["output"]

            # Parse output
            context = {}
            for line in output.split('\n'):
                if ': ' in line:
                    key, value = line.split(': ', 1)
                    context[key.lower().replace(' ', '_')] = value

            return context

        except Exception as e:
            print(f"[ERROR] Failed to get PR context: {e}")
            return None


if __name__ == '__main__':
    # Test the MCP client
    print("Testing MCP Client...")

    client = McpClient()

    # Check health
    if client.health_check():
        print("[OK] MCP server is healthy")
    else:
        print("[ERROR] MCP server is not available")
        print("Please start the server: cd mcp_servers && python git_server.py")
        exit(1)

    # Test get changed files
    print("\nTesting get_changed_files...")
    changes = client.get_changed_files("HEAD~1", "HEAD")
    print(f"Found {len(changes)} changed files")
    for change in changes[:5]:
        print(f"  {change.status}\t{change.filepath}")

    # Test get PR context
    print("\nTesting get_pr_context...")
    context = client.get_pr_context("master", "feature")
    if context:
        print(f"  Merge Base: {context.get('merge_base', 'N/A')}")
        print(f"  Commits: {context.get('commits', 'N/A')}")
        print(f"  Files Changed: {context.get('files_changed', 'N/A')}")

    print("\n[PASS] MCP Client tests completed!")
