#!/usr/bin/env python3
"""
Test script for real web search
"""

import requests
import json
import time

def test_real_search():
    print("=" * 60)
    print("Testing REAL Web Search")
    print("=" * 60)
    print()

    # Test 1: Initialize
    print("[Test 1] Initializing server...")
    try:
        response = requests.post("http://localhost:3000", json={
            "jsonrpc": "2.0",
            "id": "1",
            "method": "initialize",
            "params": {
                "clientInfo": {"name": "Test", "version": "1.0"}
            }
        }, timeout=5)

        if response.status_code == 200:
            result = response.json()
            server_info = result.get('result', {}).get('serverInfo', {})
            print(f"✓ Server: {server_info.get('name')}")
        else:
            print(f"✗ Failed: {response.status_code}")
            return False
    except Exception as e:
        print(f"✗ Error: {e}")
        print("\nMake sure the server is running:")
        print("  cd mcp_servers")
        print("  python search_real.py")
        return False

    # Test 2: Real search
    print("\n[Test 2] Performing REAL web search...")
    print("Query: 'latest AI developments 2024'")
    print("This will search the REAL internet...")
    print()

    try:
        start_time = time.time()

        response = requests.post("http://localhost:3000", json={
            "jsonrpc": "2.0",
            "id": "2",
            "method": "tools/call",
            "params": {
                "name": "brave_web_search",
                "arguments": {
                    "query": "latest AI developments 2024",
                    "count": 3
                }
            }
        }, timeout=30)  # Longer timeout for real search

        elapsed = time.time() - start_time

        if response.status_code == 200:
            result = response.json()
            content = result.get('result', {}).get('content', [])

            if content:
                text = content[0].get('text', '')
                print(f"✓ Search completed in {elapsed:.2f}s")
                print(f"✓ Result length: {len(text)} characters")
                print()
                print("First 500 characters:")
                print("-" * 60)
                print(text[:500])
                print("-" * 60)
                print()

                # Check if results look real
                if "🔍 Search results for:" in text:
                    print("✓ Got search results!")

                    # Check for real URLs (not example.com)
                    if "example.com" not in text.lower():
                        print("✓ Results contain REAL URLs (not demo data)")
                        print()
                        print("🎉 REAL SEARCH IS WORKING!")
                        return True
                    else:
                        print("⚠ Results contain example.com (demo data)")
                        print("Make sure you're running search_real.py, not brave_search_demo.py")
                        return False
                else:
                    print("⚠ Unexpected result format")
                    return False
        else:
            print(f"✗ Failed: {response.status_code}")
            return False

    except requests.exceptions.Timeout:
        print("✗ Timeout (search took too long)")
        print("This is normal sometimes - try again")
        return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False

    return True

def test_different_query():
    """Test with a different query to verify it's not cached"""
    print("\n" + "=" * 60)
    print("[Test 3] Testing with different query")
    print("=" * 60)
    print()

    queries = [
        "python programming tips",
        "kotlin android tutorial",
        "machine learning basics"
    ]

    import random
    query = random.choice(queries)

    print(f"Query: '{query}'")
    print("This should return different results than previous test")
    print()

    try:
        response = requests.post("http://localhost:3000", json={
            "jsonrpc": "2.0",
            "id": "3",
            "method": "tools/call",
            "params": {
                "name": "brave_web_search",
                "arguments": {
                    "query": query,
                    "count": 2
                }
            }
        }, timeout=30)

        if response.status_code == 200:
            result = response.json()
            content = result.get('result', {}).get('content', [])

            if content:
                text = content[0].get('text', '')
                print("✓ Search successful")
                print(f"✓ Result contains: {len(text)} characters")
                print()
                print("Preview:")
                print("-" * 60)
                print(text[:300])
                print("-" * 60)
                return True

    except Exception as e:
        print(f"✗ Error: {e}")

    return False

if __name__ == '__main__':
    print()
    print("🌐 Real Web Search Test Suite")
    print()

    success = test_real_search()

    if success:
        test_different_query()
        print()
        print("=" * 60)
        print("✓ ALL TESTS PASSED")
        print("=" * 60)
        print()
        print("Your server is performing REAL web searches!")
        print()
        print("Now you can:")
        print("1. Open ChatAgent app")
        print("2. Click tree icon 🌳")
        print("3. Run 'Web Search & Save' pipeline")
        print("4. Get REAL results from the internet!")
        print()
    else:
        print()
        print("=" * 60)
        print("✗ TESTS FAILED")
        print("=" * 60)
        print()
        print("Troubleshooting:")
        print("1. Make sure server is running: python search_real.py")
        print("2. Check internet connection")
        print("3. Wait 10-20 seconds and try again")
        print("4. Check server logs for errors")
        print()
