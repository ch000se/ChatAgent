# RAG Reranking and Filtering Implementation

## Overview

This document describes the implementation of reranking and filtering functionality for the RAG (Retrieval-Augmented Generation) system in the ChatAgent application.

## Features Implemented

### 1. Similarity Threshold Filtering

**Purpose**: Filter out irrelevant chunks that don't meet a minimum similarity threshold.

**Implementation** (`RagComparisonViewModel.kt:257-270`):
```kotlin
private fun filterChunksBySimilarity(
    chunks: List<DocumentSearchResult>,
    threshold: Float
): List<DocumentSearchResult>
```

**How it works**:
- Takes all retrieved chunks and a similarity threshold (0.0 - 1.0)
- Filters out chunks with similarity score below the threshold
- Logs which chunks passed/failed filtering for debugging
- Returns only chunks that meet the threshold

**Benefits**:
- Removes low-quality, irrelevant context
- Reduces noise in the prompt sent to the LLM
- Improves answer quality by focusing on highly relevant content
- Prevents hallucination from marginally related content

### 2. Chunk Reranking

**Purpose**: Re-score and reorder chunks using multiple relevance signals beyond just semantic similarity.

**Implementation** (`RagComparisonViewModel.kt:272-315`):
```kotlin
private fun rerankChunks(
    chunks: List<DocumentSearchResult>,
    query: String
): List<DocumentSearchResult>
```

**Reranking Strategy**:

The reranking algorithm combines multiple factors:

1. **Document Diversity Bonus (+0.05)**
   - First chunk from each unique document gets a bonus
   - Encourages diverse information sources
   - Prevents over-reliance on a single document

2. **Length Normalization**
   - Optimal chunk length: 100-500 characters
   - Too short (< 50 chars): -0.05 penalty
   - Too long (> 1000 chars): -0.03 penalty
   - Good length: +0.02 bonus
   - Rationale: Very short chunks lack context, very long chunks add noise

3. **Position Bonus (up to +0.02)**
   - Earlier chunks in a document get a small bonus
   - Formula: `max(0, (10 - chunkIndex) * 0.002)`
   - Rationale: Important information often appears early

**Final Score**: `original_similarity + diversity_bonus + length_score + position_bonus`

All scores are clamped to [0.0, 1.0] range.

### 3. Enhanced Pipeline

**New RAG Pipeline** (`RagComparisonViewModel.kt:115-250`):

```
1. Search for relevant chunks (retrieve topK * 2 for filtering margin)
   ↓
2. Filter by similarity threshold
   ↓
3. Rerank filtered chunks (if enabled)
   ↓
4. Take top-K chunks
   ↓
5. Build context from final chunks
   ↓
6. Send to LLM with enhanced prompt
```

**Parameters**:
- `topK`: Number of final chunks to use (1-10)
- `similarityThreshold`: Minimum similarity score (0.0-0.9)
- `useReranking`: Enable/disable reranking (boolean)

### 4. UI Controls

**Added Controls** (`RagComparisonScreen.kt:130-178`):

1. **Similarity Threshold Slider**
   - Range: 0.00 - 0.90
   - Step: 0.05
   - Default: 0.00 (no filtering)

2. **Reranking Toggle**
   - Switch to enable/disable reranking
   - Shows helpful description
   - Default: OFF

3. **Statistics Display**
   - Total chunks found before filtering
   - Chunks remaining after filtering
   - Final chunks used in context
   - Active threshold value
   - Reranking status

## Usage Guide

### Basic Usage

1. Navigate to RAG Comparison screen
2. Enter your question
3. Configure settings:
   - Set Top-K (how many chunks to use)
   - Set Similarity Threshold (0.0 = no filter, 0.5 = moderate, 0.7 = strict)
   - Toggle Reranking (recommended: ON for better results)
4. Click "Compare"
5. Review results and statistics

### Recommended Settings

**For General Queries**:
- Top-K: 3-5
- Similarity Threshold: 0.3-0.5
- Reranking: ON

**For Specific Fact Finding**:
- Top-K: 3
- Similarity Threshold: 0.6-0.7
- Reranking: ON

**For Exploratory Questions**:
- Top-K: 5-7
- Similarity Threshold: 0.2-0.3
- Reranking: ON

### Comparison Testing

**Test 1: Without Filtering**
```
Threshold: 0.00
Reranking: OFF
→ See baseline RAG performance
```

**Test 2: With Filtering Only**
```
Threshold: 0.50
Reranking: OFF
→ Measure impact of threshold filtering
```

**Test 3: With Filtering + Reranking**
```
Threshold: 0.50
Reranking: ON
→ See full optimization impact
```

## Performance Metrics

The system now tracks and displays:

1. **Chunks Found**: Total chunks retrieved initially
2. **After Filtering**: Chunks passing similarity threshold
3. **Used**: Final chunks included in prompt
4. **Execution Time**: Total processing time in milliseconds

Example output:
```
Chunks found: 6 → After filtering: 4 → Used: 3
Similarity threshold: 0.50
Reranking: ENABLED
Execution time: 1250 ms
```

## Technical Details

### Data Flow

**Modified ResponseData** (`RagComparisonViewModel.kt:399-409`):
```kotlin
data class ResponseData(
    val answer: String,
    val prompt: String,
    val context: String?,
    val relevantChunks: List<DocumentSearchResult>,
    val executionTimeMs: Long,
    val totalChunksFound: Int = 0,           // NEW
    val chunksAfterFiltering: Int = 0,       // NEW
    val similarityThreshold: Float = 0.0f,   // NEW
    val usedReranking: Boolean = false       // NEW
)
```

### Logging

Detailed logging for debugging:
```
D/RagComparisonVM: === QUERY WITH RAG ===
D/RagComparisonVM: User question: How many vacation days?
D/RagComparisonVM: Top-K: 3
D/RagComparisonVM: Similarity threshold: 0.5
D/RagComparisonVM: Use reranking: true
D/RagComparisonVM: Step 1: Searching relevant chunks...
D/RagComparisonVM: Found 6 raw chunks
D/RagComparisonVM: Step 2: Filtering chunks by threshold...
D/RagComparisonVM:   [PASS] similarity=0.7543 - employee_handbook.txt
D/RagComparisonVM:   [PASS] similarity=0.6821 - benefits_guide.txt
D/RagComparisonVM:   [FILTERED] similarity=0.4532 - company_history.txt
D/RagComparisonVM: After filtering: 4 chunks (removed 2)
D/RagComparisonVM: Step 3: Reranking chunks...
D/RagComparisonVM: Rerank: employee_handbook.txt chunk#2 original=0.7543 reranked=0.8143
D/RagComparisonVM: Final chunks selected: 3
```

## Quality Improvements

### Before Reranking/Filtering

**Issues**:
- Low-relevance chunks included in context
- Redundant information from same document
- Overly long or short chunks reducing quality
- No way to tune relevance strictness

### After Reranking/Filtering

**Benefits**:
- Only high-quality, relevant chunks used
- Diverse information sources
- Optimal chunk lengths
- Configurable relevance threshold
- Improved answer accuracy
- Reduced hallucination

## Examples

### Example 1: Strict Filtering

**Query**: "What is the vacation policy?"

**Settings**:
- Top-K: 3
- Threshold: 0.70
- Reranking: ON

**Results**:
```
Found: 8 chunks
After filtering (>0.70): 3 chunks
Used: 3 chunks
All high-quality, directly relevant chunks
Answer: Precise, accurate policy details
```

### Example 2: Exploratory Search

**Query**: "Tell me about company culture"

**Settings**:
- Top-K: 5
- Threshold: 0.30
- Reranking: ON

**Results**:
```
Found: 10 chunks
After filtering (>0.30): 7 chunks
Used: 5 chunks (reranked for diversity)
Chunks from multiple documents
Answer: Comprehensive overview with varied perspectives
```

### Example 3: No Relevant Content

**Query**: "What is the weather today?"

**Settings**:
- Top-K: 3
- Threshold: 0.60
- Reranking: OFF

**Results**:
```
Found: 5 chunks
After filtering (>0.60): 0 chunks
Error: "No relevant documents found above similarity threshold (0.60)"
System correctly identifies lack of relevant information
```

## Configuration Recommendations

### Threshold Selection Guide

| Threshold | Use Case | Quality | Recall |
|-----------|----------|---------|--------|
| 0.0 - 0.2 | Exploratory, broad topics | Lower | High |
| 0.3 - 0.5 | General queries | Good | Medium |
| 0.6 - 0.7 | Specific facts | High | Lower |
| 0.8 - 0.9 | Exact matches only | Highest | Very Low |

### Reranking Impact

**When to Enable**:
- Multi-document knowledge base
- Varied chunk sizes
- Quality over speed priority
- Complex queries

**When to Disable**:
- Single document source
- Speed-critical applications
- Already high-quality embeddings
- Simple fact lookup

## Future Enhancements

Potential improvements:

1. **Advanced Reranking Models**
   - Use cross-encoder models (e.g., MS MARCO)
   - Implement learned-to-rank algorithms
   - Add query-specific reranking

2. **Dynamic Threshold**
   - Auto-adjust based on query type
   - Learn optimal thresholds from feedback
   - Adaptive filtering strategies

3. **Diversity Algorithms**
   - MMR (Maximal Marginal Relevance)
   - Clustering-based selection
   - Topic diversity enforcement

4. **Performance Monitoring**
   - A/B testing framework
   - Quality metrics dashboard
   - User satisfaction tracking

## Conclusion

The reranking and filtering implementation provides:

- **Configurable relevance control** via similarity threshold
- **Improved chunk quality** through multi-factor reranking
- **Better answers** by reducing noise and improving context
- **Transparency** through detailed statistics
- **Flexibility** to tune for different use cases

This enhancement makes the RAG system more robust, accurate, and suitable for production use.

## File Changes Summary

**Modified Files**:
1. `app/src/main/java/com/example/chatagent/presentation/rag/RagComparisonViewModel.kt`
   - Added `filterChunksBySimilarity()` function
   - Added `rerankChunks()` function
   - Updated `queryWithRAG()` with new parameters
   - Enhanced `ResponseData` class
   - Updated `compareResponses()` signature

2. `app/src/main/java/com/example/chatagent/presentation/rag/RagComparisonScreen.kt`
   - Added similarity threshold slider UI
   - Added reranking toggle switch
   - Enhanced statistics display
   - Updated comparison button handler

**Created Files**:
1. `RAG_RERANKING_FILTERING.md` - This documentation

## References

- Original RAG implementation: `RAG_TASK_COMPLETED.md`
- RAG quickstart guide: `RAG_QUICKSTART.md`
- Comparison demo explained: `RAG_DEMO_EXPLAINED.md`
