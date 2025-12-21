#!/usr/bin/env python3
"""
MCP Reminder Server - Local server with reminder tools
Supports: add_reminder, list_reminders, delete_reminder, get_summary
Storage: SQLite database
"""

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
import sqlite3
import json
from datetime import datetime, timedelta
from typing import Optional
import uvicorn

app = FastAPI()

# Enable CORS for Android app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Database setup
DB_FILE = "reminders.db"

def init_db():
    """Initialize SQLite database"""
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS reminders (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            description TEXT,
            due_date TEXT,
            priority TEXT DEFAULT 'medium',
            completed INTEGER DEFAULT 0,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP
        )
    """)
    conn.commit()
    conn.close()

init_db()

def get_db():
    """Get database connection"""
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn

@app.post("/mcp")
async def mcp_handler(request: Request):
    """Main MCP endpoint handler"""
    body = await request.json()
    method = body.get("method")
    req_id = body.get("id")
    params = body.get("params", {})

    # Initialize
    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {"tools": {}},
                "serverInfo": {
                    "name": "reminder-server",
                    "version": "1.0.0"
                }
            }
        }

    # List tools
    if method == "tools/list":
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "tools": [
                    {
                        "name": "add_reminder",
                        "description": "Add a new reminder/task",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "title": {
                                    "type": "string",
                                    "description": "Title of the reminder"
                                },
                                "description": {
                                    "type": "string",
                                    "description": "Detailed description (optional)"
                                },
                                "due_date": {
                                    "type": "string",
                                    "description": "Due date in YYYY-MM-DD format (optional)"
                                },
                                "priority": {
                                    "type": "string",
                                    "enum": ["low", "medium", "high"],
                                    "description": "Priority level (optional, default: medium)"
                                }
                            },
                            "required": ["title"]
                        }
                    },
                    {
                        "name": "list_reminders",
                        "description": "Get all reminders, optionally filter by status",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "show_completed": {
                                    "type": "boolean",
                                    "description": "Include completed reminders (default: false)"
                                }
                            }
                        }
                    },
                    {
                        "name": "complete_reminder",
                        "description": "Mark a reminder as completed",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "id": {
                                    "type": "integer",
                                    "description": "ID of the reminder to complete"
                                }
                            },
                            "required": ["id"]
                        }
                    },
                    {
                        "name": "delete_reminder",
                        "description": "Delete a reminder",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "id": {
                                    "type": "integer",
                                    "description": "ID of the reminder to delete"
                                }
                            },
                            "required": ["id"]
                        }
                    },
                    {
                        "name": "get_summary",
                        "description": "Get summary of all reminders (total, pending, overdue, high priority)",
                        "inputSchema": {
                            "type": "object",
                            "properties": {}
                        }
                    }
                ]
            }
        }

    # Call tool
    if method == "tools/call":
        tool_name = params.get("name")
        arguments = params.get("arguments", {})

        try:
            result = execute_tool(tool_name, arguments)
            return {
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {
                    "content": [
                        {
                            "type": "text",
                            "text": result
                        }
                    ]
                }
            }
        except Exception as e:
            return {
                "jsonrpc": "2.0",
                "id": req_id,
                "error": {
                    "code": -32000,
                    "message": str(e)
                }
            }

    return {
        "jsonrpc": "2.0",
        "id": req_id,
        "error": {
            "code": -32601,
            "message": f"Method not found: {method}"
        }
    }

def execute_tool(tool_name: str, arguments: dict) -> str:
    """Execute tool and return result"""

    if tool_name == "add_reminder":
        return add_reminder(
            title=arguments["title"],
            description=arguments.get("description", ""),
            due_date=arguments.get("due_date"),
            priority=arguments.get("priority", "medium")
        )

    elif tool_name == "list_reminders":
        show_completed = arguments.get("show_completed", False)
        return list_reminders(show_completed)

    elif tool_name == "complete_reminder":
        return complete_reminder(arguments["id"])

    elif tool_name == "delete_reminder":
        return delete_reminder(arguments["id"])

    elif tool_name == "get_summary":
        return get_summary()

    else:
        raise ValueError(f"Unknown tool: {tool_name}")

def add_reminder(title: str, description: str, due_date: Optional[str], priority: str) -> str:
    """Add a new reminder"""
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute(
        """
        INSERT INTO reminders (title, description, due_date, priority)
        VALUES (?, ?, ?, ?)
        """,
        (title, description, due_date, priority)
    )
    conn.commit()
    reminder_id = cursor.lastrowid
    conn.close()

    return f"✅ Reminder added successfully! ID: {reminder_id}\nTitle: {title}\nPriority: {priority.upper()}" + \
           (f"\nDue: {due_date}" if due_date else "")

def list_reminders(show_completed: bool = False) -> str:
    """List all reminders"""
    conn = get_db()
    cursor = conn.cursor()

    if show_completed:
        cursor.execute("SELECT * FROM reminders ORDER BY created_at DESC")
    else:
        cursor.execute("SELECT * FROM reminders WHERE completed = 0 ORDER BY created_at DESC")

    reminders = cursor.fetchall()
    conn.close()

    if not reminders:
        return "📝 No reminders found."

    result = f"📋 **Your Reminders** ({len(reminders)} total)\n\n"

    for r in reminders:
        status = "✅" if r["completed"] else "⏳"
        priority_emoji = {"high": "🔴", "medium": "🟡", "low": "🟢"}.get(r["priority"], "⚪")

        result += f"{status} [{r['id']}] {priority_emoji} **{r['title']}**\n"

        if r["description"]:
            result += f"   📝 {r['description']}\n"

        if r["due_date"]:
            due = datetime.strptime(r["due_date"], "%Y-%m-%d")
            today = datetime.now()
            days_left = (due - today).days

            if days_left < 0:
                result += f"   ⚠️ OVERDUE by {abs(days_left)} days!\n"
            elif days_left == 0:
                result += f"   ⏰ Due TODAY!\n"
            elif days_left <= 3:
                result += f"   ⚡ Due in {days_left} days\n"
            else:
                result += f"   📅 Due: {r['due_date']}\n"

        result += "\n"

    return result

def complete_reminder(reminder_id: int) -> str:
    """Mark reminder as completed"""
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("UPDATE reminders SET completed = 1 WHERE id = ?", (reminder_id,))
    conn.commit()

    if cursor.rowcount == 0:
        conn.close()
        return f"❌ Reminder ID {reminder_id} not found."

    conn.close()
    return f"✅ Reminder ID {reminder_id} marked as completed!"

def delete_reminder(reminder_id: int) -> str:
    """Delete a reminder"""
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("DELETE FROM reminders WHERE id = ?", (reminder_id,))
    conn.commit()

    if cursor.rowcount == 0:
        conn.close()
        return f"❌ Reminder ID {reminder_id} not found."

    conn.close()
    return f"🗑️ Reminder ID {reminder_id} deleted successfully!"

def get_summary() -> str:
    """Get summary of all reminders"""
    conn = get_db()
    cursor = conn.cursor()

    # Total reminders (all)
    cursor.execute("SELECT COUNT(*) as count FROM reminders")
    total_all = cursor.fetchone()["count"]

    # Completed reminders
    cursor.execute("SELECT COUNT(*) as count FROM reminders WHERE completed = 1")
    total_completed = cursor.fetchone()["count"]

    # Pending reminders
    cursor.execute("SELECT COUNT(*) as count FROM reminders WHERE completed = 0")
    total_pending = cursor.fetchone()["count"]

    # High priority (pending only)
    cursor.execute("SELECT COUNT(*) as count FROM reminders WHERE completed = 0 AND priority = 'high'")
    high_priority = cursor.fetchone()["count"]

    # Overdue
    today = datetime.now().strftime("%Y-%m-%d")
    cursor.execute(
        "SELECT COUNT(*) as count FROM reminders WHERE completed = 0 AND due_date < ?",
        (today,)
    )
    overdue = cursor.fetchone()["count"]

    # Due today
    cursor.execute(
        "SELECT COUNT(*) as count FROM reminders WHERE completed = 0 AND due_date = ?",
        (today,)
    )
    due_today = cursor.fetchone()["count"]

    # Due this week
    week_end = (datetime.now() + timedelta(days=7)).strftime("%Y-%m-%d")
    cursor.execute(
        """
        SELECT COUNT(*) as count FROM reminders
        WHERE completed = 0 AND due_date > ? AND due_date <= ?
        """,
        (today, week_end)
    )
    due_this_week = cursor.fetchone()["count"]

    # Completed today
    cursor.execute(
        """
        SELECT COUNT(*) as count FROM reminders
        WHERE completed = 1 AND DATE(created_at) = ?
        """,
        (today,)
    )
    completed_today = cursor.fetchone()["count"]

    conn.close()

    # Build summary
    summary = "📊 **Reminders Summary**\n\n"
    summary += f"📋 Total tasks: {total_all}\n"
    summary += f"✅ Completed: {total_completed}\n"
    summary += f"📝 Pending: {total_pending}\n"

    if completed_today > 0:
        summary += f"🎉 Completed today: {completed_today}\n"

    summary += "\n"

    if overdue > 0:
        summary += f"⚠️ OVERDUE: {overdue}\n"

    if due_today > 0:
        summary += f"⏰ Due TODAY: {due_today}\n"

    if due_this_week > 0:
        summary += f"📅 Due this week: {due_this_week}\n"

    if high_priority > 0:
        summary += f"🔴 High priority: {high_priority}\n"

    if total_pending == 0:
        summary += "\n✨ You're all caught up! No pending reminders."
    elif overdue > 0:
        summary += "\n⚡ Action needed: You have overdue reminders!"

    return summary

if __name__ == "__main__":
    print("🚀 Starting MCP Reminder Server...")
    print("📍 Server will run on http://localhost:3000")
    print("📱 Android emulator: http://10.0.2.2:3000/mcp")
    print("\n💡 Available tools:")
    print("   - add_reminder: Add new reminder")
    print("   - list_reminders: View all reminders")
    print("   - complete_reminder: Mark as done")
    print("   - delete_reminder: Remove reminder")
    print("   - get_summary: Get overview\n")

    uvicorn.run(app, host="0.0.0.0", port=3000)
