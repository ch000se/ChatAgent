#!/usr/bin/env python3
"""
Real Web Search MCP Server using DuckDuckGo
Port: 3000
NO API KEY NEEDED!
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import requests
from bs4 import BeautifulSoup
import json
import time

app = Flask(__name__)
CORS(app)

def search_duckduckgo(query, num_results=3):
    """Search using DuckDuckGo"""
    try:
        print(f"[Search] Searching DuckDuckGo for: '{query}'")

        # DuckDuckGo HTML search
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }

        params = {
            'q': query,
            'kl': 'us-en'
        }

        response = requests.get(
            'https://html.duckduckgo.com/html/',
            headers=headers,
            params=params,
            timeout=10
        )

        if response.status_code != 200:
            print(f"[Search] Error: Status {response.status_code}")
            return None

        soup = BeautifulSoup(response.text, 'html.parser')
        results = []

        # Parse results
        for result_div in soup.find_all('div', class_='result')[:num_results]:
            try:
                # Get title and link
                title_elem = result_div.find('a', class_='result__a')
                if not title_elem:
                    continue

                title = title_elem.get_text(strip=True)
                url = title_elem.get('href', '')

                # Get description
                desc_elem = result_div.find('a', class_='result__snippet')
                description = desc_elem.get_text(strip=True) if desc_elem else ''

                if title and url:
                    results.append({
                        'title': title,
                        'url': url,
                        'description': description,
                        'content': description  # Use description as content
                    })
            except Exception as e:
                print(f"[Search] Error parsing result: {e}")
                continue

        print(f"[Search] Found {len(results)} results")
        return results

    except Exception as e:
        print(f"[Search] Exception: {e}")
        return None

def search_wikipedia(query):
    """Fallback: Search Wikipedia"""
    try:
        print(f"[Search] Trying Wikipedia for: '{query}'")

        response = requests.get(
            'https://en.wikipedia.org/w/api.php',
            params={
                'action': 'opensearch',
                'search': query,
                'limit': 3,
                'format': 'json'
            },
            timeout=10
        )

        if response.status_code == 200:
            data = response.json()
            titles = data[1]
            descriptions = data[2]
            urls = data[3]

            results = []
            for i in range(min(len(titles), 3)):
                results.append({
                    'title': titles[i],
                    'url': urls[i],
                    'description': descriptions[i],
                    'content': descriptions[i]
                })

            print(f"[Search] Wikipedia found {len(results)} results")
            return results

    except Exception as e:
        print(f"[Search] Wikipedia error: {e}")

    return None

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
                    "name": "Real Web Search (DuckDuckGo)",
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
                        "name": "brave_web_search",
                        "description": "Search the web using DuckDuckGo (Real results from internet)",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "query": {
                                    "type": "string",
                                    "description": "Search query"
                                },
                                "count": {
                                    "type": "number",
                                    "description": "Number of results",
                                    "default": 3
                                }
                            },
                            "required": ["query"]
                        }
                    },
                    {
                        "name": "summarize",
                        "description": "Create a summary from text",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "text": {
                                    "type": "string",
                                    "description": "Text to summarize"
                                },
                                "max_length": {
                                    "type": "number",
                                    "description": "Maximum length of summary",
                                    "default": 500
                                }
                            },
                            "required": ["text"]
                        }
                    }
                ]
            }
        })

    elif method == 'tools/call':
        params = data.get('params', {})
        tool_name = params.get('name')
        args = params.get('arguments', {})

        if tool_name == 'brave_web_search':
            query = args.get('query', '')
            count = args.get('count', 3)

            print(f"[Tool] brave_web_search: query='{query}', count={count}")

            # Try DuckDuckGo first
            results = search_duckduckgo(query, count)

            # Fallback to Wikipedia if DuckDuckGo fails
            if not results:
                print("[Tool] DuckDuckGo failed, trying Wikipedia...")
                results = search_wikipedia(query)

            if not results:
                output = f"❌ No results found for: {query}\n\nPlease try a different query."
            else:
                # Format results
                output = f"🔍 Search results for: {query}\n\n"
                output += "\n\n".join([
                    f"📄 {r['title']}\n"
                    f"🔗 {r['url']}\n"
                    f"📝 {r['description']}\n"
                    for r in results
                ])

            print(f"[Tool] Returning {len(results) if results else 0} results")

            return jsonify({
                "jsonrpc": "2.0",
                "id": request_id,
                "result": {
                    "content": [{
                        "type": "text",
                        "text": output
                    }]
                }
            })

        elif tool_name == 'summarize':
            text = args.get('text', '')
            max_length = args.get('max_length', 500)

            print(f"[Tool] summarize: length={len(text)}, max={max_length}")

            # Simple summarization
            sentences = text.split('.')
            summary = ""
            for sentence in sentences:
                if len(summary) + len(sentence) < max_length:
                    summary += sentence + "."
                else:
                    break

            if not summary:
                summary = text[:max_length] + "..."

            summary = f"Summary ({len(summary)} chars):\n\n{summary.strip()}"

            return jsonify({
                "jsonrpc": "2.0",
                "id": request_id,
                "result": {
                    "content": [{
                        "type": "text",
                        "text": summary
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

if __name__ == '__main__':
    print("=" * 60)
    print("🌐 Real Web Search MCP Server")
    print("=" * 60)
    print("Port: 3000")
    print("Search Engine: DuckDuckGo + Wikipedia (fallback)")
    print("API Key: ✓ NOT NEEDED (completely free!)")
    print("\nThis server performs REAL web searches!")
    print("\nFor Android Emulator use: http://10.0.2.2:3000")
    print("For physical device use: http://YOUR_IP:3000")
    print("=" * 60)
    print("\nInstalling dependencies if needed...")

    # Check dependencies
    try:
        import bs4
        print("✓ beautifulsoup4 installed")
    except:
        print("Installing beautifulsoup4...")
        import subprocess
        subprocess.check_call(['pip', 'install', 'beautifulsoup4'])

    print("\nStarting server...\n")
    app.run(host='0.0.0.0', port=3000, debug=True)
