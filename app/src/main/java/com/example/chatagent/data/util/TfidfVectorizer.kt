package com.example.chatagent.data.util

import android.util.Log
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * TF-IDF (Term Frequency-Inverse Document Frequency) Vectorizer
 * Generates embeddings locally without external API dependencies
 */
class TfidfVectorizer {

    private val vocabulary = mutableMapOf<String, Int>()
    private val idfScores = mutableMapOf<String, Double>()
    private var numDocuments = 0

    companion object {
        private const val TAG = "TfidfVectorizer"
        private const val MAX_FEATURES = 384 // Same dimension as MiniLM model
        private const val MIN_WORD_LENGTH = 2

        // Common stop words to ignore
        private val STOP_WORDS = setOf(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
            "to", "was", "will", "with", "the", "this", "but", "they", "have",
            "had", "what", "when", "where", "who", "which", "why", "how"
        )
    }

    /**
     * Tokenizes and cleans text
     */
    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= MIN_WORD_LENGTH && it !in STOP_WORDS }
    }

    /**
     * Builds vocabulary and IDF scores from a collection of documents
     */
    fun fit(documents: List<String>) {
        vocabulary.clear()
        idfScores.clear()
        numDocuments = documents.size

        if (documents.isEmpty()) return

        // Count document frequency AND total frequency for each term
        val documentFrequency = mutableMapOf<String, Int>()
        val totalFrequency = mutableMapOf<String, Int>()

        documents.forEach { doc ->
            val tokens = tokenize(doc)

            // Count total occurrences across all documents
            tokens.forEach { token ->
                totalFrequency[token] = totalFrequency.getOrDefault(token, 0) + 1
            }

            // Count in how many documents each term appears
            val uniqueTokens = tokens.toSet()
            uniqueTokens.forEach { token ->
                documentFrequency[token] = documentFrequency.getOrDefault(token, 0) + 1
            }
        }

        // Sort by TOTAL frequency (not document frequency) and take top MAX_FEATURES
        val sortedTerms = totalFrequency.entries
            .sortedByDescending { it.value }
            .take(MAX_FEATURES)

        // Build vocabulary and calculate IDF
        sortedTerms.forEachIndexed { index, (term, _) ->
            vocabulary[term] = index
            val df = documentFrequency[term] ?: 1
            // IDF = log((N + 1) / (df + 1)) + 1 to avoid zero and negative values
            idfScores[term] = log10((numDocuments + 1).toDouble() / (df + 1)) + 1.0
        }

        // Log top terms by total frequency
        val topByFreq = totalFrequency.entries
            .sortedByDescending { it.value }
            .take(15)
        Log.d(TAG, "Top 15 most frequent terms: ${topByFreq.map { "${it.key}(${it.value})" }}")

        // Log top terms with highest IDF (most discriminative)
        val topByIdf = idfScores.entries
            .sortedByDescending { it.value }
            .take(10)
        Log.d(TAG, "Top 10 discriminative terms: ${topByIdf.map { "${it.key}(${String.format("%.2f", it.value)})" }}")
    }

    /**
     * Transforms a single document into a TF-IDF vector
     */
    fun transform(text: String): List<Float> {
        if (vocabulary.isEmpty()) {
            // If not fitted, return zero vector
            Log.w(TAG, "Vectorizer not fitted! Returning zero vector")
            return List(MAX_FEATURES) { 0f }
        }

        val tokens = tokenize(text)
        val termFrequency = mutableMapOf<String, Int>()

        // Count term frequency
        var foundTerms = 0
        tokens.forEach { token ->
            if (token in vocabulary) {
                termFrequency[token] = termFrequency.getOrDefault(token, 0) + 1
                foundTerms++
            }
        }

        Log.d(TAG, "Transform text (${text.take(50)}...): ${tokens.size} tokens, $foundTerms found in vocab")

        // Calculate TF-IDF vector
        val vector = FloatArray(MAX_FEATURES) { 0f }

        val topTfidfTerms = mutableListOf<Pair<String, Float>>()
        termFrequency.forEach { (term, tf) ->
            val index = vocabulary[term] ?: return@forEach
            val idf = idfScores[term] ?: 0.0
            // TF-IDF = (tf / total_tokens) * idf
            val tfidf = (tf.toDouble() / tokens.size) * idf
            vector[index] = tfidf.toFloat()
            topTfidfTerms.add(term to tfidf.toFloat())
        }

        // Log top contributing terms
        val top5 = topTfidfTerms.sortedByDescending { it.second }.take(5)
        Log.d(TAG, "Top 5 TF-IDF terms: ${top5.map { "${it.first}(${String.format("%.3f", it.second)})" }}")

        // Log vector stats before normalization
        val nonZeroCount = vector.count { it > 0 }
        val magnitude = kotlin.math.sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        Log.d(TAG, "Vector before norm: $nonZeroCount non-zero values, magnitude=${"%.3f".format(magnitude)}")

        // Normalize vector to unit length
        val normalized = normalizeVector(vector.toList())
        Log.d(TAG, "Vector after norm: magnitude=${"%.3f".format(kotlin.math.sqrt(normalized.sumOf { (it * it).toDouble() }))}")
        return normalized
    }

    /**
     * Normalizes a vector to unit length (L2 normalization)
     */
    private fun normalizeVector(vector: List<Float>): List<Float> {
        val magnitude = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()

        return if (magnitude > 0) {
            vector.map { it / magnitude }
        } else {
            vector
        }
    }

    /**
     * Fits and transforms documents in one step (for convenience)
     */
    fun fitTransform(documents: List<String>): List<List<Float>> {
        fit(documents)
        return documents.map { transform(it) }
    }

    /**
     * Returns the vocabulary size
     */
    fun getVocabularySize(): Int = vocabulary.size
}
