# ChatAgent Support FAQ

## Frequently Asked Questions

### 1. Authentication and Authorization

#### Q: Why doesn't authentication work?
**A:** There are several common reasons for authentication issues:

1. **Invalid API Key**: Check that your Claude API key is correctly set in `local.properties`:
   ```properties
   claude_api_key=your_api_key_here
   ```

2. **Network Connection**: Ensure you have an active internet connection. ChatAgent requires network access to communicate with Claude API.

3. **API Key Permissions**: Verify that your API key has the necessary permissions. You can check this in your Anthropic console at https://console.anthropic.com/

4. **Rate Limiting**: If you've exceeded your API quota, authentication may fail. Check your usage limits in the Anthropic dashboard.

5. **Firewall/Proxy**: Some corporate networks block API requests. Try using a different network.

**Resolution Steps:**
- Navigate to Settings → API Configuration
- Re-enter your API key
- Test the connection using the "Test API" button
- Check the logs for specific error messages

---

#### Q: How do I change my API key?
**A:** To change your Claude API key:

1. Open `local.properties` file in the project root
2. Update the line: `claude_api_key=your_new_api_key`
3. Rebuild the app or restart the application
4. The new key will be automatically loaded

---

### 2. RAG (Retrieval-Augmented Generation)

#### Q: Why isn't RAG finding my documents?
**A:** RAG search issues usually occur due to:

1. **Documents Not Indexed**: Documents must be indexed before they can be searched.
   - Use `/docs` command to check indexed documents
   - Re-index with "Index All Documents" button in Documents screen

2. **Search Query Too Specific**: TF-IDF works better with broader queries.
   - Instead of: "How to configure MCP server port 3000"
   - Try: "MCP server configuration"

3. **Document Format**: Only text-based formats are supported (.md, .txt, .kt, .java, etc.)
   - PDF and binary files are not currently supported

4. **Embedding Dimension Mismatch**: The system uses 384-dimensional TF-IDF vectors.
   - If you manually added embeddings, ensure they match this dimension

**Resolution Steps:**
- Go to Documents screen
- Click "Re-index All" button
- Wait for indexing to complete
- Try your search again with a simpler query

---

#### Q: How does the TF-IDF vectorization work?
**A:** ChatAgent uses TF-IDF (Term Frequency-Inverse Document Frequency) for document retrieval:

1. **Indexing Phase**:
   - Documents are split into chunks
   - Each chunk is vectorized into 384 dimensions
   - Vectors are stored in the local database

2. **Search Phase**:
   - Your query is vectorized using the same TF-IDF model
   - Cosine similarity is calculated between query and document vectors
   - Top K most similar documents are returned

3. **Benefits**:
   - Fast and efficient (no external API calls)
   - Works offline
   - Low resource usage

**Performance Tips:**
- Use 3-5 word queries for best results
- Include technical keywords from your domain
- Re-index after adding new documents

---

### 3. MCP (Model Context Protocol)

#### Q: MCP servers are not responding
**A:** MCP connectivity issues can be caused by:

1. **Server Not Running**: MCP servers must be started manually.
   ```bash
   cd mcp_servers
   python search_real.py  # Port 3000
   python filesystem_demo.py  # Port 3001
   # Git server on port 3002
   ```

2. **Port Conflicts**: Another application may be using the required ports.
   - Check with: `netstat -ano | findstr "3000 3001 3002"`
   - Kill conflicting processes or change MCP ports

3. **Python Dependencies**: Missing Python packages.
   ```bash
   pip install -r mcp_servers/requirements.txt
   ```

4. **Network Configuration**: Check that the app can connect to localhost.
   - MCP servers run on: `http://localhost:3000`, `http://localhost:3001`, `http://localhost:3002`

**Resolution Steps:**
- Verify servers are running: `ps aux | grep python` (Linux/Mac) or Task Manager (Windows)
- Check server logs in `mcp_servers/output/`
- Test connectivity: `curl http://localhost:3000/health`
- Restart servers if needed

---

#### Q: How do I add a custom MCP server?
**A:** To add a new MCP server:

1. **Create Server Script**:
   ```python
   # mcp_servers/my_custom_server.py
   from flask import Flask, request, jsonify

   app = Flask(__name__)

   @app.route('/tools', methods=['GET'])
   def get_tools():
       return jsonify({
           "tools": [
               {
                   "name": "my_tool",
                   "description": "Does something useful",
                   "input_schema": {
                       "type": "object",
                       "properties": {
                           "param": {"type": "string"}
                       }
                   }
               }
           ]
       })

   @app.route('/execute', methods=['POST'])
   def execute_tool():
       data = request.json
       # Process tool call
       return jsonify({"result": "success"})

   if __name__ == '__main__':
       app.run(port=3003)
   ```

2. **Register in App**:
   - Edit `data/remote/client/MultiMcpClient.kt`
   - Add your server URL to the client list

3. **Update Tool Definitions**:
   - Modify `data/remote/dto/McpDto.kt` if needed
   - Add new tool types to `McpTool` sealed class

---

### 4. Commands System

#### Q: Commands are not recognized
**A:** Command recognition issues:

1. **Incorrect Syntax**: Commands must start with `/`
   - Correct: `/help authentication`
   - Incorrect: `help authentication` or `\help`

2. **Unknown Command**: Only these commands are supported:
   - `/help [query]` - Search documentation
   - `/code <query>` - Search project code
   - `/docs <query>` - Search markdown files
   - `/git [status|log|diff|branch]` - Git operations
   - `/project` - Project information
   - `/support [ticket_id|query]` - Support assistant

3. **Missing Parameters**: Some commands require parameters.
   - `/code` requires a search query
   - `/docs` requires a search query

**Examples:**
```
/help Why isn't authentication working?
/code ChatRepository
/docs MCP setup
/git status
/project
/support ticket-123
```

---

#### Q: How do I use the /git command?
**A:** The `/git` command supports these operations:

1. **Status**: `git status`
   ```
   /git status
   ```
   Shows current branch, staged/unstaged changes

2. **Log**: `git log`
   ```
   /git log
   ```
   Shows recent commits

3. **Diff**: `git diff`
   ```
   /git diff
   ```
   Shows uncommitted changes

4. **Branch**: `git branch`
   ```
   /git branch
   ```
   Lists all branches

**Note:** Git commands execute in the project root directory.

---

### 5. Performance and Optimization

#### Q: The app is slow or laggy
**A:** Performance optimization tips:

1. **Clear Old Conversations**:
   - Long conversation history can slow down the app
   - Use "Clear Chat" or manually delete old messages
   - Enable auto-summarization in Settings

2. **Reduce Token Usage**:
   - Shorter prompts use fewer tokens
   - Disable unnecessary context inclusion
   - Use commands instead of full sentences

3. **Optimize RAG**:
   - Limit number of retrieved documents (Settings → RAG → Max Results)
   - Increase similarity threshold to filter irrelevant results

4. **Database Maintenance**:
   - Large databases can slow queries
   - Clear old embeddings: Settings → Database → Clear Embeddings
   - Vacuum database periodically

---

#### Q: Token usage is too high
**A:** To reduce token consumption:

1. **Use Commands**: Commands are more efficient than natural language
   - Instead of: "Can you show me the git status?"
   - Use: `/git status`

2. **Disable Context**: Turn off "Include full history" in Settings

3. **Shorter Prompts**: Be concise in your questions

4. **Use RAG**: RAG retrieval is free; only the final Claude call uses tokens

5. **Monitor Usage**: Check Settings → Token Usage to see statistics

---

### 6. Database and Storage

#### Q: Database errors or corruption
**A:** Database issues can be resolved by:

1. **Clear App Data**:
   - Android Settings → Apps → ChatAgent → Storage → Clear Data
   - **Warning:** This deletes all conversations and indexed documents

2. **Manual Database Reset**:
   ```kotlin
   // In code (for developers)
   context.deleteDatabase("chat_database")
   ```

3. **Migration Failures**:
   - If app crashes after update, database schema may have changed
   - Uninstall and reinstall the app
   - Backup conversations first if possible

---

#### Q: How do I backup my conversations?
**A:** Currently, conversations are stored locally:

1. **Manual Backup**:
   - Database location: `/data/data/com.example.chatagent/databases/chat_database`
   - Requires root access to export

2. **Planned Features** (Coming Soon):
   - Export to JSON
   - Cloud sync
   - Automatic backups

**Workaround:**
- Copy important conversations manually
- Take screenshots for critical information

---

### 7. PR Review System

#### Q: PR reviews are not working
**A:** PR review troubleshooting:

1. **GitHub Token**: Ensure `GITHUB_TOKEN` secret is set in repository settings

2. **Python Environment**: Verify Python dependencies
   ```bash
   cd scripts/pr_review
   pip install -r requirements.txt
   ```

3. **Workflow Permissions**: Check `.github/workflows/pr-review.yml`
   - Needs write permissions for PRs
   - Needs read permissions for repository content

4. **API Rate Limits**: GitHub and Anthropic APIs have rate limits
   - Check GitHub Actions logs for specific errors
   - Verify Claude API key in secrets

---

#### Q: How do I customize PR review criteria?
**A:** Edit the review configuration:

1. **Open**: `scripts/pr_review/claude_reviewer.py`

2. **Modify System Prompt**:
   ```python
   SYSTEM_PROMPT = """
   You are a code reviewer. Focus on:
   - Security vulnerabilities
   - Performance issues
   - Code style consistency
   - Best practices
   - [Add your custom criteria here]
   """
   ```

3. **Adjust Severity Levels**:
   - Modify `severity_threshold` in `review_pr.py`
   - Add custom tags for your team

---

### 8. Troubleshooting

#### Q: App crashes on startup
**A:** Common startup crash causes:

1. **Missing API Key**: Add Claude API key to `local.properties`

2. **Database Migration**: Try clearing app data

3. **Dependency Conflict**: Clean and rebuild:
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

4. **Check Logs**: Use Logcat to see crash details
   ```bash
   adb logcat | grep ChatAgent
   ```

---

#### Q: How do I report a bug?
**A:** To report bugs:

1. **Gather Information**:
   - Android version
   - App version
   - Steps to reproduce
   - Error messages from Logcat

2. **Create Issue**: Submit to GitHub repository with:
   - Clear title describing the issue
   - Detailed description
   - Screenshots if applicable
   - Logs or stack traces

3. **Community Support**: Check existing issues first - your problem may already be reported

---

### 9. Configuration

#### Q: Where are settings stored?
**A:** Configuration locations:

1. **API Keys**: `local.properties` (not committed to git)
   ```properties
   claude_api_key=sk-...
   huggingface_api_key=hf_...
   ```

2. **App Settings**: Stored in Android SharedPreferences
   - Location: `/data/data/com.example.chatagent/shared_prefs/`

3. **MCP Configuration**: Hardcoded in `MultiMcpClient.kt`
   - Can be modified in code and rebuilt

4. **RAG Parameters**: `DocumentRepositoryImpl.kt`
   - Vector dimensions: 384
   - Similarity threshold: configurable
   - Max results: configurable

---

#### Q: Can I use a different AI model?
**A:** Currently, ChatAgent is designed for Claude API:

1. **Claude Models Supported**:
   - claude-3-5-sonnet-20241022 (default)
   - claude-3-opus-latest
   - claude-3-haiku-20240307

2. **Change Model**: Edit `ChatRepositoryImpl.kt`
   ```kotlin
   model = "claude-3-opus-latest"
   ```

3. **Other Providers**: Requires code changes
   - Modify `ChatApiService.kt`
   - Update DTO classes
   - Change authentication headers

---

### 10. Best Practices

#### Q: How do I get the best results from the AI?
**A:** Tips for optimal AI interactions:

1. **Be Specific**: Provide clear, detailed questions
   - Bad: "Fix my code"
   - Good: "Why am I getting a NullPointerException in ChatViewModel line 45?"

2. **Use Context**: Include relevant information
   - Error messages
   - Code snippets
   - Steps already tried

3. **Use Commands**: Leverage the command system
   - `/code` for finding code
   - `/docs` for documentation
   - `/git` for repository info

4. **Iterate**: Refine questions based on responses

5. **Provide Feedback**: Use thumbs up/down to improve results

---

## Additional Resources

- **API Reference**: See `API_REFERENCE.md`
- **Build Instructions**: See `BUILD_INSTRUCTIONS.md`
- **MCP Setup Guide**: See `MCP_SETUP_GUIDE.md`
- **RAG Documentation**: See `RAG_QUICKSTART.md`
- **GitHub Repository**: [Your repo URL]
- **Anthropic Documentation**: https://docs.anthropic.com/

---

## Contact Support

If your issue is not covered in this FAQ:

1. Use `/support` command in the app with your question
2. Check GitHub Issues for similar problems
3. Create a new issue with detailed information
4. Join our community forum (if available)

**Emergency Contact**: For critical security issues, email: security@example.com

---

*Last updated: 2026-01-14*
*Version: 1.0.0*
