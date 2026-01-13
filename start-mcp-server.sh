#!/bin/bash

echo "================================"
echo "Starting MCP Server for ChatAgent"
echo "================================"
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js is not installed!"
    echo "Please install Node.js from https://nodejs.org/"
    exit 1
fi

echo "Node.js version:"
node --version
echo ""

# Check if npx is available
if ! command -v npx &> /dev/null; then
    echo "ERROR: npx is not available!"
    exit 1
fi

echo "Installing MCP server (if not installed)..."
npm install -g @modelcontextprotocol/server-filesystem

echo ""
echo "================================"
echo "Starting MCP Server..."
echo "================================"
echo ""
echo "Server will be available at: http://localhost:3000"
echo "Project directory: $(pwd)"
echo ""
echo "IMPORTANT: Keep this terminal open!"
echo ""
echo "To connect from Android:"
echo "1. Find your PC IP address:"
echo "   - Linux: ip addr show | grep inet"
echo "   - Mac: ifconfig | grep inet"
echo "2. In Android app, go to MCP screen"
echo "3. Enter: http://YOUR_PC_IP:3000"
echo "   Example: http://192.168.1.100:3000"
echo ""
echo "Press Ctrl+C to stop the server"
echo "================================"
echo ""

# Start the server with git commands allowed
npx @modelcontextprotocol/server-filesystem "$(pwd)" --allow-commands git --port 3000
