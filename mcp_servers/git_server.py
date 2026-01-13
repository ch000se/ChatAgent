#!/usr/bin/env python3
"""
Git MCP Server for ChatAgent
Provides git operations through MCP protocol
"""

import json
import subprocess
import os
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# Git repository path (current project)
REPO_PATH = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def execute_git_command(*args):
    """Execute git command and return output"""
    try:
        cmd = ['git'] + list(args)
        result = subprocess.run(
            cmd,
            cwd=REPO_PATH,
            capture_output=True,
            text=True,
            timeout=10
        )

        if result.returncode == 0:
            return {
                'success': True,
                'output': result.stdout.strip(),
                'error': None
            }
        else:
            return {
                'success': False,
                'output': result.stdout.strip(),
                'error': result.stderr.strip()
            }
    except subprocess.TimeoutExpired:
        return {
            'success': False,
            'output': '',
            'error': 'Command timeout'
        }
    except Exception as e:
        return {
            'success': False,
            'output': '',
            'error': str(e)
        }

@app.route('/', methods=['POST'])
def handle_mcp_request():
    """Handle MCP JSON-RPC requests"""
    data = request.json
    method = data.get('method')
    request_id = data.get('id')

    print(f"[MCP] Received: {method}")

    if method == 'initialize':
        return jsonify({
            "jsonrpc": "2.0",
            "id": request_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "serverInfo": {
                    "name": "Git Operations",
                    "version": "1.0.0"
                },
                "capabilities": {
                    "tools": {}
                }
            }
        })

    elif method == 'tools/list':
        return jsonify({
            "jsonrpc": "2.0",
            "id": request_id,
            "result": {
                "tools": [
                    {
                        "name": "git_status",
                        "description": "Get git status of the repository",
                        "inputSchema": {
                            "type": "object",
                            "properties": {},
                            "required": []
                        }
                    },
                    {
                        "name": "git_log",
                        "description": "Get git commit history",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "count": {
                                    "type": "number",
                                    "description": "Number of commits to show",
                                    "default": 10
                                }
                            },
                            "required": []
                        }
                    },
                    {
                        "name": "git_diff",
                        "description": "Get git diff statistics",
                        "inputSchema": {
                            "type": "object",
                            "properties": {},
                            "required": []
                        }
                    },
                    {
                        "name": "git_branch",
                        "description": "List all branches",
                        "inputSchema": {
                            "type": "object",
                            "properties": {},
                            "required": []
                        }
                    },
                    {
                        "name": "git_current_branch",
                        "description": "Get current branch name",
                        "inputSchema": {
                            "type": "object",
                            "properties": {},
                            "required": []
                        }
                    },
                    {
                        "name": "git_remote",
                        "description": "Get remote repository information",
                        "inputSchema": {
                            "type": "object",
                            "properties": {},
                            "required": []
                        }
                    },
                    {
                        "name": "execute_command",
                        "description": "Execute git command with arguments",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "command": {
                                    "type": "string",
                                    "description": "Command to execute (must be git)"
                                },
                                "args": {
                                    "type": "array",
                                    "description": "Command arguments",
                                    "items": {"type": "string"}
                                }
                            },
                            "required": ["command", "args"]
                        }
                    }
                ]
            }
        })

    elif method == 'tools/call':
        params = data.get('params', {})
        tool_name = params.get('name')
        arguments = params.get('arguments', {})

        print(f"[Tool] {tool_name}: {arguments}")

        if tool_name == 'git_status':
            result = execute_git_command('status', '--short', '--branch')

        elif tool_name == 'git_log':
            count = arguments.get('count', 10)
            result = execute_git_command('log', '--oneline', f'-{count}')

        elif tool_name == 'git_diff':
            result = execute_git_command('diff', '--stat')

        elif tool_name == 'git_branch':
            result = execute_git_command('branch', '-a')

        elif tool_name == 'git_current_branch':
            result = execute_git_command('rev-parse', '--abbrev-ref', 'HEAD')

        elif tool_name == 'git_remote':
            result = execute_git_command('remote', '-v')

        elif tool_name == 'execute_command':
            command = arguments.get('command', '')
            args = arguments.get('args', [])

            # Security: only allow git commands
            if command != 'git':
                return jsonify({
                    "jsonrpc": "2.0",
                    "id": request_id,
                    "error": {
                        "code": -32000,
                        "message": f'Only git commands are allowed. Got: {command}'
                    }
                })

            result = execute_git_command(*args)
        else:
            return jsonify({
                "jsonrpc": "2.0",
                "id": request_id,
                "error": {
                    "code": -32000,
                    "message": f'Unknown tool: {tool_name}'
                }
            })

        # Format response
        if result['success']:
            output = result['output'] if result['output'] else '(empty output)'
            response_text = output
        else:
            response_text = f"Error: {result['error']}"

        print(f"[Tool] Result: {response_text[:200]}...")

        return jsonify({
            "jsonrpc": "2.0",
            "id": request_id,
            "result": {
                "content": [{
                    "type": "text",
                    "text": response_text
                }]
            }
        })

    return jsonify({
        "jsonrpc": "2.0",
        "id": request_id,
        "error": {
            "code": -32601,
            "message": f"Method not found: {method}"
        }
    }), 404

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    # Check if git is available
    git_check = execute_git_command('--version')

    return jsonify({
        'status': 'healthy' if git_check['success'] else 'degraded',
        'git': git_check['success'],
        'repository': REPO_PATH,
        'version': '1.0.0'
    })

if __name__ == '__main__':
    print("=" * 60)
    print("Git MCP Server for ChatAgent")
    print("=" * 60)
    print()
    print(f"Repository: {REPO_PATH}")
    print()

    # Check if git is available
    git_check = execute_git_command('--version')
    if git_check['success']:
        print(f"[OK] Git version: {git_check['output']}")
    else:
        print("[ERROR] WARNING: Git is not available!")
        print(f"  Error: {git_check['error']}")

    # Get current branch
    branch_check = execute_git_command('rev-parse', '--abbrev-ref', 'HEAD')
    if branch_check['success']:
        print(f"[OK] Current branch: {branch_check['output']}")

    print()
    print("Server starting on port 3002...")
    print()
    print("For Android Emulator use: http://10.0.2.2:3002")
    print("For physical device use: http://YOUR_COMPUTER_IP:3002")
    print()
    print("Press Ctrl+C to stop")
    print("=" * 60)
    print()

    app.run(host='0.0.0.0', port=3002, debug=False)
