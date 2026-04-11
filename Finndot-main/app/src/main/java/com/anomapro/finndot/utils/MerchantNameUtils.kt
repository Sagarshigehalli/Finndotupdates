package com.anomapro.finndot.utils

/**
 * Utility functions for merchant name normalization and similarity matching.
 */
object MerchantNameUtils {
    
    /**
     * Calculates Levenshtein distance between two strings.
     * Lower distance means more similar strings.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val s1Lower = s1.lowercase()
        val s2Lower = s2.lowercase()
        
        if (s1Lower == s2Lower) return 0
        if (s1Lower.isEmpty()) return s2Lower.length
        if (s2Lower.isEmpty()) return s1Lower.length
        
        val dp = Array(s1Lower.length + 1) { IntArray(s2Lower.length + 1) }
        
        for (i in 0..s1Lower.length) dp[i][0] = i
        for (j in 0..s2Lower.length) dp[0][j] = j
        
        for (i in 1..s1Lower.length) {
            for (j in 1..s2Lower.length) {
                val cost = if (s1Lower[i - 1] == s2Lower[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[s1Lower.length][s2Lower.length]
    }
    
    /**
     * Calculates similarity ratio between two strings (0.0 to 1.0).
     * 1.0 means identical, 0.0 means completely different.
     */
    fun similarityRatio(s1: String, s2: String): Double {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        
        val distance = levenshteinDistance(s1, s2)
        return 1.0 - (distance.toDouble() / maxLen)
    }
    
    /**
     * Checks if two merchant names are likely the same using multiple heuristics.
     */
    fun areLikelySame(merchant1: String, merchant2: String, threshold: Double = 0.75): Boolean {
        val m1 = merchant1.lowercase().trim()
        val m2 = merchant2.lowercase().trim()
        
        if (m1 == m2) return true
        
        // Check similarity ratio
        if (similarityRatio(m1, m2) >= threshold) return true
        
        // Check if one contains the other (for cases like "AMAZON PAY" vs "AMAZONPAY")
        val normalized1 = normalizeForComparison(m1)
        val normalized2 = normalizeForComparison(m2)
        
        if (normalized1.contains(normalized2) || normalized2.contains(normalized1)) {
            // But only if they share significant words
            return shareSignificantWords(m1, m2)
        }
        
        // Check if they share significant words and have high similarity
        if (shareSignificantWords(m1, m2)) {
            return similarityRatio(m1, m2) >= 0.6
        }
        
        return false
    }
    
    /**
     * Normalizes a string for comparison by removing common separators and extra spaces.
     */
    private fun normalizeForComparison(name: String): String {
        return name
            .replace(Regex("[\\s_-]+"), "")
            .replace(Regex("[^a-z0-9]"), "")
    }
    
    /**
     * Checks if two merchant names share significant words.
     */
    fun shareSignificantWords(name1: String, name2: String, minWordLength: Int = 3): Boolean {
        val words1 = extractSignificantWords(name1, minWordLength)
        val words2 = extractSignificantWords(name2, minWordLength)
        
        if (words1.isEmpty() || words2.isEmpty()) return false
        
        // Check if they share at least one significant word
        val commonWords = words1.intersect(words2)
        if (commonWords.isNotEmpty()) return true
        
        // Check for partial word matches (e.g., "amazon" and "amazonpay")
        return words1.any { word1 ->
            words2.any { word2 ->
                word1.contains(word2) || word2.contains(word1)
            }
        }
    }
    
    /**
     * Extracts significant words from a merchant name.
     */
    fun extractSignificantWords(name: String, minLength: Int = 3): Set<String> {
        return name.lowercase()
            .split(Regex("[\\s_-]+"))
            .filter { it.length >= minLength }
            .filter { !isCommonWord(it) }
            .toSet()
    }
    
    /**
     * Common words that should be ignored when comparing merchant names.
     */
    private val commonWords = setOf(
        "pay", "payment", "payments", "ltd", "limited", "inc", "corp", "corporation",
        "pvt", "private", "llp", "llc", "co", "company", "store", "shop", "mart",
        "services", "service", "india", "indian", "bank", "banking"
    )
    
    private fun isCommonWord(word: String): Boolean {
        return commonWords.contains(word.lowercase())
    }
    
    /**
     * Suggests a normalized name based on common patterns.
     * Converts "AMAZON PAY" -> "Amazon Pay", "AMAZONPAY" -> "Amazon Pay"
     */
    fun suggestNormalizedName(originalName: String): String {
        val trimmed = originalName.trim()
        if (trimmed.isEmpty()) return trimmed
        
        // If already in proper case, return as is
        if (trimmed != trimmed.uppercase() && trimmed != trimmed.lowercase()) {
            return trimmed
        }
        
        // Convert all caps to proper case
        val words = trimmed.lowercase().split(Regex("[\\s_-]+"))
        return words.joinToString(" ") { word ->
            when {
                word.length <= 1 -> word.uppercase()
                word == "pay" || word == "payments" -> word.capitalize()
                word.all { it.isDigit() } -> word // Keep numbers as is
                else -> word.replaceFirstChar { it.uppercase() }
            }
        }
    }
    
    /**
     * Finds similar merchants using multiple algorithms.
     * Returns a list of similar merchant names sorted by similarity score.
     */
    fun findSimilarMerchants(
        targetMerchant: String,
        candidateMerchants: List<String>,
        maxResults: Int = 10,
        minSimilarity: Double = 0.6
    ): List<Pair<String, Double>> {
        val target = targetMerchant.lowercase().trim()
        
        return candidateMerchants
            .filter { it.lowercase().trim() != target }
            .map { candidate ->
                val similarity = calculateSimilarityScore(target, candidate.lowercase().trim())
                candidate to similarity
            }
            .filter { (_, score) -> score >= minSimilarity }
            .sortedByDescending { (_, score) -> score }
            .take(maxResults)
    }
    
    /**
     * Calculates a comprehensive similarity score between two merchant names.
     * Combines multiple factors: Levenshtein distance, word overlap, substring matching.
     */
    private fun calculateSimilarityScore(name1: String, name2: String): Double {
        // Exact match
        if (name1 == name2) return 1.0
        
        // Levenshtein-based similarity
        val levenshteinSimilarity = similarityRatio(name1, name2)
        
        // Word overlap similarity
        val words1 = extractSignificantWords(name1)
        val words2 = extractSignificantWords(name2)
        val wordOverlap = if (words1.isEmpty() || words2.isEmpty()) {
            0.0
        } else {
            words1.intersect(words2).size.toDouble() / maxOf(words1.size, words2.size)
        }
        
        // Substring matching (one contains the other)
        val normalized1 = normalizeForComparison(name1)
        val normalized2 = normalizeForComparison(name2)
        val substringMatch = when {
            normalized1.contains(normalized2) || normalized2.contains(normalized1) -> 0.8
            else -> 0.0
        }
        
        // Weighted combination
        return (levenshteinSimilarity * 0.4 + wordOverlap * 0.4 + substringMatch * 0.2)
            .coerceIn(0.0, 1.0)
    }
}
