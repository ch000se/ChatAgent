package com.example.chatagent.domain.model

data class OllamaModel(
    val name: String,
    val size: Long,
    val modifiedAt: String,
    val parameterSize: String? = null,
    val quantizationLevel: String? = null,
    val family: String? = null
) {
    val displayName: String
        get() = name.substringBefore(":")

    val tag: String
        get() = name.substringAfter(":", "latest")

    val formattedSize: String
        get() {
            val gb = size / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1) {
                String.format("%.1f GB", gb)
            } else {
                val mb = size / (1024.0 * 1024.0)
                String.format("%.0f MB", mb)
            }
        }
}
