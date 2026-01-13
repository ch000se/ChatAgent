#!/usr/bin/env python3
"""
RAG Engine - TF-IDF based document indexing and search
Port from Kotlin: app/src/main/java/com/example/chatagent/data/util/TfidfVectorizer.kt
"""

import math
import re
import os
from typing import List, Dict, Tuple, Optional
from dataclasses import dataclass


@dataclass
class SearchResult:
    """Search result with document chunk"""
    text: str
    filename: str
    chunk_index: int
    similarity: float
    rank: int


class TfidfVectorizer:
    """
    TF-IDF Vectorizer - exact port from Kotlin implementation
    Generates 384-dimensional embeddings for text similarity search
    """

    # Constants matching Kotlin implementation
    MAX_FEATURES = 384
    MIN_WORD_LENGTH = 2

    # Stop words - exact match from Kotlin (43 words)
    STOP_WORDS = {
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
        "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
        "to", "was", "will", "with", "the", "this", "but", "they", "have",
        "had", "what", "when", "where", "who", "which", "why", "how",
        "been", "being", "do", "does", "did", "doing"
    }

    def __init__(self):
        self.vocabulary: Dict[str, int] = {}
        self.idf_scores: Dict[str, float] = {}
        self.num_documents = 0

    def tokenize(self, text: str) -> List[str]:
        """
        Tokenizes and cleans text
        Port from Kotlin tokenize() method
        """
        # Lowercase
        text = text.lower()

        # Replace non-alphanumeric with spaces (keep only a-z, 0-9, spaces)
        text = re.sub(r'[^a-z0-9\s]', ' ', text)

        # Split on whitespace
        tokens = text.split()

        # Filter by length and stop words
        tokens = [
            token for token in tokens
            if len(token) >= self.MIN_WORD_LENGTH and token not in self.STOP_WORDS
        ]

        return tokens

    def fit(self, documents: List[str]) -> None:
        """
        Builds vocabulary and IDF scores from document collection
        Port from Kotlin fit() method
        """
        self.vocabulary.clear()
        self.idf_scores.clear()
        self.num_documents = len(documents)

        if not documents:
            return

        # Count document frequency AND total frequency for each term
        document_frequency: Dict[str, int] = {}
        total_frequency: Dict[str, int] = {}

        for doc in documents:
            tokens = self.tokenize(doc)

            # Count total occurrences across all documents
            for token in tokens:
                total_frequency[token] = total_frequency.get(token, 0) + 1

            # Count in how many documents each term appears
            unique_tokens = set(tokens)
            for token in unique_tokens:
                document_frequency[token] = document_frequency.get(token, 0) + 1

        # Sort by TOTAL frequency (not document frequency) and take top MAX_FEATURES
        sorted_terms = sorted(
            total_frequency.items(),
            key=lambda x: x[1],
            reverse=True
        )[:self.MAX_FEATURES]

        # Build vocabulary and calculate IDF
        for index, (term, _) in enumerate(sorted_terms):
            self.vocabulary[term] = index
            df = document_frequency.get(term, 1)
            # IDF = log((N + 1) / (df + 1)) + 1 to avoid zero and negative values
            self.idf_scores[term] = math.log10((self.num_documents + 1) / (df + 1)) + 1.0

        print(f"[TfidfVectorizer] Fitted on {len(documents)} documents, vocabulary size: {len(self.vocabulary)}")

    def transform(self, text: str) -> List[float]:
        """
        Transforms a single document into a TF-IDF vector
        Port from Kotlin transform() method
        """
        if not self.vocabulary:
            # If not fitted, return zero vector
            print("[WARNING] Vectorizer not fitted! Returning zero vector")
            return [0.0] * self.MAX_FEATURES

        tokens = self.tokenize(text)
        term_frequency: Dict[str, int] = {}

        # Count term frequency
        found_terms = 0
        for token in tokens:
            if token in self.vocabulary:
                term_frequency[token] = term_frequency.get(token, 0) + 1
                found_terms += 1

        # Calculate TF-IDF vector
        vector = [0.0] * self.MAX_FEATURES

        for term, tf in term_frequency.items():
            if term not in self.vocabulary:
                continue

            index = self.vocabulary[term]
            idf = self.idf_scores.get(term, 0.0)
            # TF-IDF = (tf / total_tokens) * idf
            tfidf = (tf / len(tokens)) * idf if len(tokens) > 0 else 0.0
            vector[index] = tfidf

        # Normalize vector to unit length
        normalized = self._normalize_vector(vector)

        return normalized

    def _normalize_vector(self, vector: List[float]) -> List[float]:
        """
        Normalizes a vector to unit length (L2 normalization)
        Port from Kotlin normalizeVector() method
        """
        magnitude = math.sqrt(sum(v * v for v in vector))

        if magnitude > 0:
            return [v / magnitude for v in vector]
        else:
            return vector

    def fit_transform(self, documents: List[str]) -> List[List[float]]:
        """
        Fits and transforms documents in one step
        Port from Kotlin fitTransform() method
        """
        self.fit(documents)
        return [self.transform(doc) for doc in documents]

    def cosine_similarity(self, vec1: List[float], vec2: List[float]) -> float:
        """
        Calculate cosine similarity between two vectors
        Cosine similarity = dot_product / (norm1 * norm2)
        Since vectors are already L2 normalized, this simplifies to just dot product
        """
        if len(vec1) != len(vec2):
            return 0.0

        dot_product = sum(v1 * v2 for v1, v2 in zip(vec1, vec2))
        return dot_product

    def get_vocabulary_size(self) -> int:
        """Returns the vocabulary size"""
        return len(self.vocabulary)


class DocumentIndexer:
    """
    Document indexer for loading and searching documents
    Based on DocumentRepositoryImpl.kt chunking and search logic
    """

    # Chunking parameters - match Kotlin implementation
    CHUNK_SIZE = 500
    CHUNK_OVERLAP = 50

    def __init__(self, docs_path: str):
        self.docs_path = docs_path
        self.vectorizer = TfidfVectorizer()
        self.chunks: List[Tuple[str, str, int]] = []  # (text, filename, chunk_index)
        self.embeddings: List[List[float]] = []

    def _chunk_text(self, text: str) -> List[str]:
        """
        Chunks text with overlap
        Port from DocumentRepositoryImpl.kt chunkText() method
        """
        if len(text) <= self.CHUNK_SIZE:
            return [text]

        chunks = []
        start = 0

        while start < len(text):
            end = min(start + self.CHUNK_SIZE, len(text))
            chunk = text[start:end]
            chunks.append(chunk)

            # Move start by (CHUNK_SIZE - CHUNK_OVERLAP) for next chunk
            start += (self.CHUNK_SIZE - self.CHUNK_OVERLAP)

            # Break if we've covered the entire text
            if end >= len(text):
                break

        return chunks

    def index_documents(self) -> int:
        """
        Load and index all .md files from docs_path
        Returns number of chunks indexed
        """
        self.chunks.clear()
        self.embeddings.clear()

        if not os.path.exists(self.docs_path):
            print(f"[ERROR] Documentation path does not exist: {self.docs_path}")
            return 0

        # Load all .md and .txt files
        all_texts = []

        for filename in os.listdir(self.docs_path):
            if not (filename.endswith('.md') or filename.endswith('.txt')):
                continue

            filepath = os.path.join(self.docs_path, filename)

            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()

                # Chunk the document
                doc_chunks = self._chunk_text(content)

                # Store chunks with metadata
                for idx, chunk_text in enumerate(doc_chunks):
                    self.chunks.append((chunk_text, filename, idx))
                    all_texts.append(chunk_text)

                print(f"[DocumentIndexer] Indexed {filename}: {len(doc_chunks)} chunks")

            except Exception as e:
                print(f"[ERROR] Failed to read {filename}: {e}")
                continue

        if not all_texts:
            print("[WARNING] No documents found to index")
            return 0

        # Train vectorizer on all chunks
        self.vectorizer.fit(all_texts)

        # Generate embeddings for all chunks
        self.embeddings = [self.vectorizer.transform(text) for text in all_texts]

        print(f"[DocumentIndexer] Indexed {len(self.chunks)} chunks from {len(set(c[1] for c in self.chunks))} documents")

        return len(self.chunks)

    def search(self, query: str, top_k: int = 5) -> List[SearchResult]:
        """
        Search for relevant document chunks
        Port from DocumentRepositoryImpl.kt searchDocuments() method
        """
        if not self.chunks or not self.embeddings:
            print("[WARNING] No documents indexed")
            return []

        # Generate query embedding
        query_embedding = self.vectorizer.transform(query)

        # Calculate similarity for all chunks
        similarities = []
        for idx, chunk_embedding in enumerate(self.embeddings):
            similarity = self.vectorizer.cosine_similarity(query_embedding, chunk_embedding)
            text, filename, chunk_index = self.chunks[idx]
            similarities.append((similarity, text, filename, chunk_index))

        # Sort by similarity descending
        similarities.sort(key=lambda x: x[0], reverse=True)

        # Take top K
        results = []
        for rank, (similarity, text, filename, chunk_index) in enumerate(similarities[:top_k], 1):
            results.append(SearchResult(
                text=text,
                filename=filename,
                chunk_index=chunk_index,
                similarity=similarity,
                rank=rank
            ))

        return results


if __name__ == '__main__':
    # Test the implementation
    print("Testing TfidfVectorizer...")

    vectorizer = TfidfVectorizer()

    # Test documents
    docs = [
        "This is a test document about machine learning",
        "Another document discussing artificial intelligence",
        "A third document about natural language processing"
    ]

    # Fit and transform
    vectors = vectorizer.fit_transform(docs)

    print(f"Vocabulary size: {vectorizer.get_vocabulary_size()}")
    print(f"Generated {len(vectors)} vectors of dimension {len(vectors[0])}")

    # Test similarity
    query = "machine learning and AI"
    query_vec = vectorizer.transform(query)

    for idx, doc_vec in enumerate(vectors):
        sim = vectorizer.cosine_similarity(query_vec, doc_vec)
        print(f"Similarity with doc {idx}: {sim:.4f}")
