# AI-Powered PR Code Review System

Автоматична система ревью коду для Pull Requests з використанням:
- **Claude Sonnet 4.5** для інтелектуального аналізу коду
- **RAG (TF-IDF)** для контексту з документації проекту
- **MCP Git Server** для отримання PR diff та метаданих
- **GitHub Actions** для автоматизації

## Архітектура

```
GitHub PR Event
    ↓
GitHub Actions Workflow
    ↓
┌─────────────────────────────────────────┐
│   review_pr.py (Main Script)            │
│   ├─ MCP Client → Git Server            │
│   ├─ RAG Engine → Search Docs           │
│   ├─ Claude Reviewer → AI Analysis      │
│   └─ GitHub API → Post Comment          │
└─────────────────────────────────────────┘
    ↓
PR Comment з детальним ревью
```

## Швидкий старт

### 1. Налаштування GitHub Secrets

Додайте у ваш репозиторій GitHub secret:

1. Перейдіть: `Settings` → `Secrets and variables` → `Actions`
2. Натисніть `New repository secret`
3. Додайте:
   - **Name:** `ANTHROPIC_API_KEY`
   - **Value:** ваш Anthropic API ключ (з `local.properties`)

`GITHUB_TOKEN` надається автоматично GitHub Actions.

### 2. Встановлення залежностей (локально)

```bash
cd scripts/pr_review
pip install -r requirements.txt
```

### 3. Тестування локально

#### Запустіть MCP Git Server:
```bash
cd mcp_servers
python git_server.py
```

#### У новому терміналі:
```bash
cd scripts/pr_review

# Встановіть змінні оточення
export GITHUB_TOKEN="your-github-token"
export ANTHROPIC_API_KEY="your-anthropic-key"
export PR_NUMBER="14"
export REPO_NAME="ch000se/ChatAgent"
export BASE_REF="origin/master"
export HEAD_REF="HEAD"

# Запустіть ревью
python review_pr.py
```

## Компоненти

### 1. MCP Git Server Extensions (`../../mcp_servers/git_server.py`)

Розширений Git MCP сервер з новими tools:
- `git_diff_unified` - детальний unified diff
- `git_diff_files` - список змінених файлів
- `git_show_file` - вміст файлу на коміті
- `git_pr_context` - метадані PR

### 2. RAG Engine (`rag_engine.py`)

TF-IDF векторизатор (порт з Kotlin):
- 384-вимірні embeddings
- Chunking: 500 chars з 50 chars overlap
- Cosine similarity search

**Використання:**
```python
from rag_engine import DocumentIndexer

indexer = DocumentIndexer('../../app/src/main/assets/docs')
indexer.index_documents()
results = indexer.search('Clean Architecture patterns', top_k=5)
```

### 3. MCP Client (`mcp_client.py`)

HTTP клієнт для MCP Git Server:
```python
from mcp_client import McpClient

client = McpClient('http://localhost:3002')
diff = client.get_pr_diff('origin/master', 'HEAD')
files = client.get_changed_files('origin/master', 'HEAD')
```

### 4. Claude Reviewer (`claude_reviewer.py`)

AI код ревьювер:
```python
from claude_reviewer import ClaudeReviewer, ReviewContext

reviewer = ClaudeReviewer(api_key)
context = ReviewContext(pr_diff, changed_files, relevant_docs, base, head)
review = reviewer.review_code(context)
```

### 5. GitHub API (`github_api.py`)

Публікація коментарів у PR:
```python
from github_api import GitHubAPI

api = GitHubAPI(token, 'owner/repo')
api.post_pr_comment(pr_number, markdown_text)
```

## Категорії ревью

Система аналізує код за категоріями:

1. **Architecture** - Clean Architecture, layer boundaries, SOLID
2. **Code Style** - Kotlin conventions, naming, documentation
3. **Best Practices** - Android patterns, Compose, Room
4. **Bug Risks** - Null safety, coroutines, lifecycle, memory leaks
5. **Security** - API keys, SQL injection, insecure storage

## Severity Levels

- **Critical** - критичні баги або security issues
- **Major** - серйозні архітектурні проблеми
- **Minor** - стиль коду, покращення
- **Info** - інформаційні нотатки

## Приклад Output

```markdown
## 🤖 AI Code Review

### Summary
Code quality is good overall. Found 1 major architecture issue.

### Statistics
- **Total Issues:** 4
- **Critical:** 0
- **Major:** 1
- **Minor:** 3

### Architecture Issues

#### MAJOR: Layer Boundary Violation
**File:** `domain/ReviewUseCase.kt` (line 45)

Domain layer imports `retrofit2.Response` - data layer dependency.

**Suggestion:**
Use domain Result sealed class instead.

### Positive Notes
- Excellent coroutine error handling
- Well-structured Composables
```

## GitHub Actions Workflow

Workflow автоматично запускається при:
- Відкритті нового PR
- Оновленні PR (push)
- Reopening PR

Файл: `.github/workflows/pr-review.yml`

## Тестування

```bash
# Тест RAG Engine
python test_rag.py

# Тест MCP Client (потрібен запущений MCP server)
python mcp_client.py

# Тест Claude Reviewer (потрібен API key)
export ANTHROPIC_API_KEY="your-key"
python claude_reviewer.py
```

## Troubleshooting

### MCP Server не стартує
```bash
# Перевірте порт
netstat -ano | grep 3002

# Перевірте логи
cd mcp_servers
python git_server.py
```

### Review не публікується
- Перевірте `GITHUB_TOKEN` має write permissions для PR
- Перевірте `ANTHROPIC_API_KEY` валідний

### RAG не знаходить документи
- Перевірте шлях: `app/src/main/assets/docs/`
- Документи повинні бути `.md` або `.txt`

## Вартість

**Per PR review:**
- ~8,000 input tokens
- ~1,500 output tokens
- Вартість: ~$0.12 (Claude Sonnet 4.5)

**Місячно (50 PRs):**
- ~$6/month
- GitHub Actions: безкоштовно (в рамках free tier)

## Розширення

### Додати нові правила ревью

Редагуйте `claude_reviewer.py` → `_build_system_prompt()`:
```python
- Add new rule category
- Update JSON schema
```

### Додати inline коментарі

Використовуйте GitHub Review API:
```python
# In github_api.py
def create_review_comment(self, pr_number, commit_id, path, line, body):
    # POST /repos/{owner}/{repo}/pulls/{pr_number}/comments
```

### Інтеграція з іншими CI

Workflow можна адаптувати для:
- GitLab CI (`.gitlab-ci.yml`)
- Jenkins (Jenkinsfile)
- CircleCI (`.circleci/config.yml`)

## Ліцензія

MIT
