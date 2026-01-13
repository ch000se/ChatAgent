#!/usr/bin/env python3
"""Test script for RAG engine"""

from rag_engine import TfidfVectorizer, DocumentIndexer


def test_vectorizer():
    """Test TfidfVectorizer"""
    print("Testing TfidfVectorizer...")

    vectorizer = TfidfVectorizer()

    # Test documents
    docs = [
        "Android application with Clean Architecture and MVVM pattern",
        "Kotlin coroutines for asynchronous operations",
        "Jetpack Compose for modern UI development"
    ]

    # Fit and transform
    vectors = vectorizer.fit_transform(docs)

    assert len(vectors) == 3
    assert len(vectors[0]) == 384
    assert vectorizer.get_vocabulary_size() > 0

    # Test similarity
    query = "Android MVVM architecture"
    query_vec = vectorizer.transform(query)

    similarities = [vectorizer.cosine_similarity(query_vec, doc_vec) for doc_vec in vectors]

    # First doc should have highest similarity
    assert similarities[0] > similarities[1]
    assert similarities[0] > similarities[2]

    print("[OK] TfidfVectorizer tests passed")


def test_document_indexer():
    """Test DocumentIndexer"""
    print("\nTesting DocumentIndexer...")

    indexer = DocumentIndexer('../../app/src/main/assets/docs')
    count = indexer.index_documents()

    assert count > 0
    print(f"[OK] Indexed {count} chunks")

    # Test search
    results = indexer.search('Clean Architecture', top_k=5)

    assert len(results) > 0
    assert results[0].similarity > 0

    print(f"[OK] Search returned {len(results)} results")
    print(f"[OK] Top result similarity: {results[0].similarity:.4f}")


def test_chunking():
    """Test text chunking"""
    print("\nTesting text chunking...")

    indexer = DocumentIndexer('.')

    # Test small text (no chunking needed)
    small_text = "A" * 400
    chunks = indexer._chunk_text(small_text)
    assert len(chunks) == 1

    # Test large text (needs chunking)
    large_text = "A" * 1500
    chunks = indexer._chunk_text(large_text)
    assert len(chunks) > 1

    # Check overlap
    chunk_size = 500
    overlap = 50
    expected_chunks = 1 + ((1500 - chunk_size) // (chunk_size - overlap))
    assert len(chunks) >= expected_chunks

    print(f"[OK] Chunking works correctly ({len(chunks)} chunks for 1500 chars)")


if __name__ == '__main__':
    try:
        test_vectorizer()
        test_document_indexer()
        test_chunking()
        print("\n[PASS] All tests passed!")
    except AssertionError as e:
        print(f"\n[FAIL] Test failed: {e}")
        raise
    except Exception as e:
        print(f"\n[FAIL] Error: {e}")
        raise
