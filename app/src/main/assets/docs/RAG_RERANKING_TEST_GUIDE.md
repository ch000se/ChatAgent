# RAG Reranking & Filtering - Testing Guide

## Quick Test Scenarios

This guide helps you test and compare the impact of filtering and reranking on RAG answer quality.

## Test Setup

### Prerequisites
1. Have documents indexed in the RAG system
2. Navigate to "RAG Comparison Demo" screen
3. Have a test query ready

### Test Queries to Try

**Good test queries** (should have relevant documents):
- "What are the vacation policies?"
- "How many sick days do employees get?"
- "What is the remote work policy?"
- "Company benefits overview"

**Challenging queries** (may have marginal relevance):
- "What is company culture like?"
- "Tell me about the team"
- "Office location details"

**No-match queries** (should have low/no relevant docs):
- "What is the weather today?"
- "Latest stock prices"
- "Celebrity news"

## Testing Scenarios

### Scenario 1: Baseline (No Filtering, No Reranking)

**Purpose**: Establish baseline RAG performance

**Settings**:
```
Top-K: 3
Similarity Threshold: 0.00
Reranking: OFF
```

**Expected Behavior**:
- Uses top 3 chunks by similarity
- May include some less-relevant chunks
- Baseline answer quality

**What to observe**:
- Look at chunk similarity scores
- Note which documents were used
- Assess answer quality and relevance

---

### Scenario 2: Filtering Only (Moderate Threshold)

**Purpose**: Test impact of similarity filtering

**Settings**:
```
Top-K: 3
Similarity Threshold: 0.50
Reranking: OFF
```

**Expected Behavior**:
- Filters out chunks with similarity < 0.50
- May reduce number of chunks used
- Should improve answer quality if baseline had low-relevance chunks

**What to observe**:
- "Chunks found" vs "After filtering" numbers
- Compare chunk list to Scenario 1
- Is answer more focused and accurate?

**Compare to Scenario 1**:
- ✅ Better: More focused, fewer irrelevant details
- ❌ Worse: May lose context if too few chunks pass

---

### Scenario 3: Filtering Only (Strict Threshold)

**Purpose**: Test strict filtering

**Settings**:
```
Top-K: 3
Similarity Threshold: 0.70
Reranking: OFF
```

**Expected Behavior**:
- Only very relevant chunks included
- May get "no chunks found" error for marginal queries
- Very precise answers when chunks are found

**What to observe**:
- How many queries return errors?
- Quality of answers when chunks are found
- Are strict thresholds too limiting?

**Compare to Scenario 2**:
- ✅ Better: More precise, higher confidence answers
- ❌ Worse: More queries fail to find relevant chunks

---

### Scenario 4: Reranking Only (No Filtering)

**Purpose**: Test impact of reranking alone

**Settings**:
```
Top-K: 3
Similarity Threshold: 0.00
Reranking: ON
```

**Expected Behavior**:
- All chunks considered (no filtering)
- Chunks reordered by enhanced scoring
- Better diversity and length optimization

**What to observe**:
- Check logs for reranking scores (original vs reranked)
- Are chunks from different documents?
- Chunk length distribution

**Compare to Scenario 1**:
- ✅ Better: More diverse sources, better chunk quality
- ❌ Worse: Still may include low-relevance chunks

---

### Scenario 5: Filtering + Reranking (Moderate)

**Purpose**: Test combined optimization

**Settings**:
```
Top-K: 3
Similarity Threshold: 0.40
Reranking: ON
```

**Expected Behavior**:
- Filter removes low-quality chunks
- Reranking optimizes remaining chunks
- Best overall quality

**What to observe**:
- Pipeline: Found → Filtered → Reranked → Used
- Answer quality vs all previous scenarios
- Chunk diversity and relevance

**Compare to All Previous**:
- Should outperform most scenarios
- Good balance of quality and coverage

---

### Scenario 6: Filtering + Reranking (Strict)

**Purpose**: Maximum quality optimization

**Settings**:
```
Top-K: 3
Similarity Threshold: 0.60
Reranking: ON
```

**Expected Behavior**:
- Very high bar for chunk inclusion
- Optimal reranking of high-quality chunks
- Best answers when chunks are found

**What to observe**:
- Success rate (how many queries work?)
- Answer quality for successful queries
- Is this too strict for your use case?

**Compare to Scenario 5**:
- ✅ Better: Maximum precision
- ❌ Worse: Lower recall (more failures)

---

## Comparison Matrix

| Scenario | Threshold | Reranking | Best For | Pros | Cons |
|----------|-----------|-----------|----------|------|------|
| 1. Baseline | 0.00 | OFF | Benchmark | Simple, fast | May include noise |
| 2. Moderate Filter | 0.50 | OFF | General use | Good balance | May still have noise |
| 3. Strict Filter | 0.70 | OFF | Precision critical | Very accurate | Low recall |
| 4. Rerank Only | 0.00 | ON | Diverse sources | Better ordering | No filtering |
| 5. Moderate Both | 0.40 | ON | **Recommended** | Quality + coverage | Slight overhead |
| 6. Strict Both | 0.60 | ON | High stakes | Best quality | Strictest |

---

## Testing Workflow

### Step-by-Step Testing

1. **Pick a test query** (e.g., "What is the vacation policy?")

2. **Run Scenario 1** (Baseline)
   - Set: Threshold=0.00, Reranking=OFF
   - Click "Compare"
   - Screenshot or note results

3. **Run Scenario 5** (Recommended)
   - Set: Threshold=0.40, Reranking=ON
   - Click "Compare"
   - Compare to Scenario 1

4. **Evaluate Improvement**
   - Is answer more accurate?
   - Are chunks more relevant?
   - Check statistics (chunks filtered out)

5. **Try Edge Cases**
   - Query with no relevant docs
   - Query with marginal relevance
   - Very specific queries

6. **Find Optimal Settings**
   - Adjust threshold until you find sweet spot
   - Consider your use case requirements
   - Document your findings

---

## Evaluation Criteria

### Answer Quality Metrics

**Accuracy**:
- Does answer correctly reflect document content?
- No hallucinations or made-up facts?

**Relevance**:
- Is answer on-topic?
- Does it address the actual question?

**Completeness**:
- All important points covered?
- No critical information missing?

**Conciseness**:
- No unnecessary information?
- Clear and to the point?

### Chunk Quality Metrics

**Relevance**:
- Are chunks semantically related to query?
- Similarity scores above threshold?

**Diversity**:
- Chunks from different documents?
- Varied perspectives?

**Length**:
- Chunks neither too short nor too long?
- Optimal context length?

**Position**:
- Important chunks ranked higher?
- Best information used first?

---

## Sample Test Results

### Example Test: "What is the vacation policy?"

**Baseline (Scenario 1)**:
```
Settings: Threshold=0.00, Reranking=OFF
Results:
  Chunks found: 6
  Used: 3
  Chunk similarities: 0.78, 0.65, 0.42
  Answer: "The vacation policy includes... [some irrelevant info about holidays]"
  Quality: Good but includes noise
```

**Optimized (Scenario 5)**:
```
Settings: Threshold=0.40, Reranking=ON
Results:
  Chunks found: 6
  After filtering: 4
  Used: 3
  Chunk similarities: 0.81, 0.78, 0.67 (reranked)
  Answer: "The vacation policy provides..."
  Quality: Excellent, focused, accurate
```

**Improvement**:
- ✅ Filtered out chunk with 0.42 similarity
- ✅ Reranking boosted best chunk from 0.78 to 0.81
- ✅ More focused answer
- ✅ No irrelevant information

---

## Tips for Testing

### 1. Use Logcat for Details

View detailed logs:
```bash
adb logcat -s RagComparisonVM:D
```

Look for:
- `[PASS]` / `[FILTERED]` markers
- Reranking score changes
- Chunk selection details

### 2. Test Multiple Queries

Don't rely on one query! Test:
- 5-10 different queries
- Mix of easy and hard queries
- Some with no relevant docs

### 3. Document Your Findings

Create a test report:
```
Query: "..."
Baseline: [result summary]
Optimized: [result summary]
Improvement: [what got better]
Issues: [any problems]
```

### 4. A/B Testing

For each query:
- Run without optimization
- Run with optimization
- Compare side-by-side
- Note which is better and why

### 5. Find Your Sweet Spot

Every knowledge base is different:
- Start with recommended settings (0.40 threshold, reranking ON)
- Adjust based on your results
- Consider your quality vs coverage needs

---

## Common Observations

### When Filtering Helps

✅ Knowledge base has many documents
✅ Some queries are ambiguous
✅ Embedding quality varies
✅ Want high precision

### When Filtering Hurts

❌ Small knowledge base (few docs)
❌ Very specific queries (need all context)
❌ High-quality embeddings already
❌ Prefer recall over precision

### When Reranking Helps

✅ Multiple documents on same topic
✅ Varied chunk sizes
✅ Want diverse sources
✅ Complex queries

### When Reranking Impact is Small

❌ Single document source
❌ Uniform chunk sizes
❌ Simple queries
❌ Already optimal ranking

---

## Troubleshooting

### Problem: "No relevant documents found"

**Possible Causes**:
- Threshold too high
- Documents not indexed
- Query poorly matched

**Solutions**:
1. Lower threshold (try 0.20)
2. Check documents are indexed
3. Rephrase query
4. Verify embeddings working

### Problem: Answers still have noise

**Possible Causes**:
- Threshold too low
- Reranking disabled
- Poor embedding quality

**Solutions**:
1. Increase threshold (try 0.60)
2. Enable reranking
3. Check document quality
4. Increase Top-K and threshold

### Problem: Answers missing key info

**Possible Causes**:
- Threshold too strict
- Top-K too low
- Important chunks filtered out

**Solutions**:
1. Lower threshold (try 0.30)
2. Increase Top-K to 5-7
3. Check chunk similarity scores
4. May need better embeddings

---

## Recommended Production Settings

Based on testing, recommend starting with:

**General Purpose**:
```
Top-K: 3-5
Similarity Threshold: 0.35-0.45
Reranking: ON
```

**High Precision Needed**:
```
Top-K: 3
Similarity Threshold: 0.60-0.70
Reranking: ON
```

**Exploratory/Broad Search**:
```
Top-K: 5-7
Similarity Threshold: 0.25-0.35
Reranking: ON
```

**Speed Critical**:
```
Top-K: 3
Similarity Threshold: 0.50
Reranking: OFF
```

---

## Conclusion

Through systematic testing, you should find:

1. **Optimal threshold** for your knowledge base
2. **Reranking impact** on answer quality
3. **Best configuration** for your use case
4. **Edge cases** to handle

Document your findings and use them to configure production settings!

## Next Steps

After testing:
1. ✅ Document optimal settings
2. ✅ Configure default values
3. ✅ Consider user-adjustable settings
4. ✅ Monitor quality metrics
5. ✅ Iterate based on user feedback
