#!/usr/bin/env python3
"""
Simple File System MCP Server for testing
Port: 3001
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import os
import json
from pathlib import Path

app = Flask(__name__)
CORS(app)

# Allowed directories (adjust for your system)
ALLOWED_PATHS = [
    str(Path.home() / "Downloads"),
    "/tmp",
    "/sdcard/Download",
    str(Path.cwd() / "output")  # Local output folder
]

# Create output folder if it doesn't exist
output_dir = Path.cwd() / "output"
output_dir.mkdir(exist_ok=True)

def is_path_allowed(path):
    """Check if path is in allowed directories"""
    path = os.path.abspath(path)
    return any(path.startswith(allowed) for allowed in ALLOWED_PATHS)

@app.route('/', methods=['POST'])
def handle_mcp_request():
    """Handle MCP JSON-RPC requests"""
    data = request.json
    method = data.get('method')
    request_id = data.get('id')

    print(f"[File System MCP] Received: {method}")

    if method == 'initialize':
        return jsonify({
            "jsonrpc": "2.0",
            "id": request_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "serverInfo": {
                    "name": "File System Demo",
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
                        "name": "write_file",
                        "description": "Write content to a file",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "path": {
                                    "type": "string",
                                    "description": "File path"
                                },
                                "content": {
                                    "type": "string",
                                    "description": "Content to write"
                                }
                            },
                            "required": ["path", "content"]
                        }
                    },
                    {
                        "name": "read_file",
                        "description": "Read content from a file",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "path": {
                                    "type": "string",
                                    "description": "File path"
                                }
                            },
                            "required": ["path"]
                        }
                    },
                    {
                        "name": "list_directory",
                        "description": "List files in a directory",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "path": {
                                    "type": "string",
                                    "description": "Directory path"
                                }
                            },
                            "required": ["path"]
                        }
                    }
                ]
            }
        })

    elif method == 'tools/call':
        params = data.get('params', {})
        tool_name = params.get('name')
        args = params.get('arguments', {})

        if tool_name == 'write_file':
            path = args.get('path', '')
            content = args.get('content', '')

            print(f"[Write] Path: {path}, Content length: {len(content)}")

            # Convert Android path to local path for testing
            if path.startswith('/sdcard/Download'):
                path = str(output_dir / path.split('/')[-1])

            # Check if path is allowed
            if not is_path_allowed(path):
                # If not allowed, write to output folder instead
                path = str(output_dir / Path(path).name)
                print(f"[Write] Redirected to: {path}")

            try:
                # Create directory if needed
                os.makedirs(os.path.dirname(path), exist_ok=True)

                # Write file
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(content)

                file_size = os.path.getsize(path)
                message = f"✓ File written successfully\n📁 Path: {path}\n📊 Size: {file_size} bytes"

                print(f"[Write] Success: {path}")

                return jsonify({
                    "jsonrpc": "2.0",
                    "id": request_id,
                    "result": {
                        "content": [{
                            "type": "text",
                            "text": message
                        }]
                    }
                })

            except Exception as e:
                error_msg = f"Failed to write file: {str(e)}"
                print(f"[Write] Error: {error_msg}")

                return jsonify({
                    "jsonrpc": "2.0",
                    "id": request_id,
                    "error": {
                        "code": -32000,
                        "message": error_msg
                    }
                })

        elif tool_name == 'read_file':
            path = args.get('path', '')

            print(f"[Read] Path: {path}")

            # Convert Android path
            if path.startswith('/sdcard/Download'):
                path = str(output_dir / path.split('/')[-1])

            if not is_path_allowed(path):
                path = str(output_dir / Path(path).name)

            try:
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()

                print(f"[Read] Success: {path}, Length: {len(content)}")

                return jsonify({
                    "jsonrpc": "2.0",
                    "id": request_id,
                    "result": {
                        "content": [{
                            "type": "text",
                            "text": content
                        }]
                    }
                })

            except Exception as e:
                error_msg = f"Failed to read file: {str(e)}"
                print(f"[Read] Error: {error_msg}")

                return jsonify({
                    "jsonrpc": "2.0",
                    "id": request_id,
                    "error": {
                        "code": -32000,
                        "message": error_msg
                    }
                })

        elif tool_name == 'list_directory':
            path = args.get('path', str(output_dir))

            print(f"[List] Path: {path}")

            try:
                files = os.listdir(path)
                file_list = "\n".join([f"📄 {f}" for f in files])

                return jsonify({
                    "jsonrpc": "2.0",
                    "id": request_id,
                    "result": {
                        "content": [{
                            "type": "text",
                            "text": f"Files in {path}:\n\n{file_list}"
                        }]
                    }
                })

            except Exception as e:
                error_msg = f"Failed to list directory: {str(e)}"
                print(f"[List] Error: {error_msg}")

                return jsonify({
                    "jsonrpc": "2.0",
                    "id": request_id,
                    "error": {
                        "code": -32000,
                        "message": error_msg
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

if __name__ == '__main__':
    print("=" * 60)
    print("💾 File System MCP Server (Demo)")
    print("=" * 60)
    print("Port: 3001")
    print("Tools: write_file, read_file, list_directory")
    print(f"\nOutput directory: {output_dir}")
    print("\nAllowed paths:")
    for path in ALLOWED_PATHS:
        print(f"  - {path}")
    print("\nFor Android Emulator use: http://10.0.2.2:3001")
    print("For physical device use: http://YOUR_IP:3001")
    print("=" * 60)
    app.run(host='0.0.0.0', port=3001, debug=True)
