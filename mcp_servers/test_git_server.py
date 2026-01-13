#!/usr/bin/env python3
"""
Test Git MCP Server
"""

import requests
import json

BASE_URL = "http://localhost:3002"

def test_health():
    """Test health endpoint"""
    print("=" * 60)
    print("Testing Health Endpoint")
    print("=" * 60)

    response = requests.get(f"{BASE_URL}/health")
    data = response.json()

    print(f"Status: {data['status']}")
    print(f"Git Available: {data['git']}")
    print(f"Repository: {data['repository']}")
    print()

    return data['git']

def test_list_tools():
    """Test tools list"""
    print("=" * 60)
    print("Testing Tools List")
    print("=" * 60)

    response = requests.post(f"{BASE_URL}/mcp/v1/tools/list")
    data = response.json()

    tools = data['tools']
    print(f"Available tools: {len(tools)}")
    for tool in tools:
        print(f"  - {tool['name']}: {tool['description']}")
    print()

def test_git_status():
    """Test git status command"""
    print("=" * 60)
    print("Testing Git Status")
    print("=" * 60)

    response = requests.post(
        f"{BASE_URL}/mcp/v1/tools/call",
        json={
            'name': 'git_status',
            'arguments': {}
        }
    )
    data = response.json()

    if 'isError' in data and data['isError']:
        print("❌ Error:")
        print(data['content'][0]['text'])
    else:
        print("✓ Status:")
        print(data['content'][0]['text'])
    print()

def test_git_current_branch():
    """Test current branch"""
    print("=" * 60)
    print("Testing Current Branch")
    print("=" * 60)

    response = requests.post(
        f"{BASE_URL}/mcp/v1/tools/call",
        json={
            'name': 'git_current_branch',
            'arguments': {}
        }
    )
    data = response.json()

    if 'isError' in data and data['isError']:
        print("❌ Error:")
        print(data['content'][0]['text'])
    else:
        branch = data['content'][0]['text']
        print(f"✓ Current branch: {branch}")
    print()

def test_git_log():
    """Test git log"""
    print("=" * 60)
    print("Testing Git Log (last 5 commits)")
    print("=" * 60)

    response = requests.post(
        f"{BASE_URL}/mcp/v1/tools/call",
        json={
            'name': 'git_log',
            'arguments': {'count': 5}
        }
    )
    data = response.json()

    if 'isError' in data and data['isError']:
        print("❌ Error:")
        print(data['content'][0]['text'])
    else:
        print("✓ Recent commits:")
        print(data['content'][0]['text'])
    print()

def test_execute_command():
    """Test execute_command"""
    print("=" * 60)
    print("Testing Execute Command (git remote -v)")
    print("=" * 60)

    response = requests.post(
        f"{BASE_URL}/mcp/v1/tools/call",
        json={
            'name': 'execute_command',
            'arguments': {
                'command': 'git',
                'args': ['remote', '-v']
            }
        }
    )
    data = response.json()

    if 'isError' in data and data['isError']:
        print("❌ Error:")
        print(data['content'][0]['text'])
    else:
        print("✓ Remote info:")
        print(data['content'][0]['text'])
    print()

if __name__ == '__main__':
    print()
    print("=" * 60)
    print("Git MCP Server Test Suite")
    print("=" * 60)
    print()

    try:
        # Test health first
        git_available = test_health()

        if not git_available:
            print("⚠ WARNING: Git is not available!")
            print("Some tests may fail.")
            print()

        # Test all endpoints
        test_list_tools()
        test_git_status()
        test_git_current_branch()
        test_git_log()
        test_execute_command()

        print("=" * 60)
        print("🎉 All tests completed!")
        print("=" * 60)
        print()
        print("✓ Git MCP Server is working correctly")
        print()
        print("You can now use it in ChatAgent:")
        print("1. Go to MCP screen")
        print("2. Connect to: http://10.0.2.2:3002 (emulator)")
        print("   or http://YOUR_IP:3002 (physical device)")
        print("3. Try /git status command in chat")
        print()

    except requests.exceptions.ConnectionError:
        print()
        print("=" * 60)
        print("❌ ERROR: Cannot connect to server")
        print("=" * 60)
        print()
        print("Please make sure the Git MCP Server is running:")
        print("  python git_server.py")
        print("or")
        print("  start_git.bat")
        print()
    except Exception as e:
        print()
        print("=" * 60)
        print(f"❌ ERROR: {e}")
        print("=" * 60)
        print()
