#!/usr/bin/env python3
"""
Quick system test - verify all components work
"""

import os
import sys

print("=" * 60)
print("TESTING PR REVIEW SYSTEM COMPONENTS")
print("=" * 60)

# Test 1: RAG Engine
print("\n[1/4] Testing RAG Engine...")
try:
    from rag_engine import DocumentIndexer

    indexer = DocumentIndexer('../../app/src/main/assets/docs')
    count = indexer.index_documents()

    if count > 0:
        print(f"  [OK] Indexed {count} chunks")

        # Test search
        results = indexer.search('Clean Architecture', top_k=3)
        if results:
            print(f"  [OK] Search works ({len(results)} results)")
        else:
            print("  [FAIL] Search returned no results")
    else:
        print("  [FAIL] No documents indexed")

except Exception as e:
    print(f"  [FAIL] RAG Engine error: {e}")
    sys.exit(1)

# Test 2: MCP Client (requires running server)
print("\n[2/4] Testing MCP Client...")
try:
    from mcp_client import McpClient

    client = McpClient()
    if client.health_check():
        print("  [OK] MCP server is healthy")

        # Test get changed files
        changes = client.get_changed_files("HEAD~1", "HEAD")
        print(f"  [OK] Can fetch changed files ({len(changes)} files)")
    else:
        print("  [WARNING] MCP server not running")
        print("  Start with: cd mcp_servers && python git_server.py")

except Exception as e:
    print(f"  [WARNING] MCP Client: {e}")

# Test 3: Claude Reviewer
print("\n[3/4] Testing Claude Reviewer...")
try:
    from claude_reviewer import ClaudeReviewer, ReviewContext, Issue

    api_key = os.getenv("ANTHROPIC_API_KEY")
    if api_key:
        print("  [OK] ANTHROPIC_API_KEY found")
        reviewer = ClaudeReviewer(api_key)
        print("  [OK] Claude Reviewer initialized")
    else:
        print("  [WARNING] ANTHROPIC_API_KEY not set (needed for full test)")

except Exception as e:
    print(f"  [FAIL] Claude Reviewer error: {e}")

# Test 4: GitHub API
print("\n[4/4] Testing GitHub API...")
try:
    from github_api import GitHubAPI

    token = os.getenv("GITHUB_TOKEN", "test-token")
    api = GitHubAPI(token, "owner/repo")
    print("  [OK] GitHub API initialized")

except Exception as e:
    print(f"  [FAIL] GitHub API error: {e}")

# Test 5: Main Script
print("\n[5/5] Testing Main Script...")
try:
    from review_pr import PRReviewSystem
    print("  [OK] Main script can be imported")

except Exception as e:
    print(f"  [FAIL] Main script error: {e}")

print("\n" + "=" * 60)
print("COMPONENT TESTS COMPLETE")
print("=" * 60)
print("\nNext steps:")
print("1. Start MCP server: cd mcp_servers && python git_server.py")
print("2. Set ANTHROPIC_API_KEY environment variable")
print("3. Run full test: python test_full_review.py")
print("=" * 60)
