package com.hdreader.app.readium

import org.readium.r2.shared.publication.Publication

/**
 * Process-local holder so ReaderActivity can receive an opened Publication without serializing it.
 * Stage B+ will replace this with path-based reopen + progress store.
 */
object PublicationHolder {
    @Volatile
    var current: Publication? = null
        private set

    @Volatile
    var displayTitle: String = ""
        private set

    @Volatile
    var displayAuthor: String = ""
        private set

    @Volatile
    var sourceLabel: String = ""
        private set

    fun set(publication: Publication, sourceLabel: String) {
        current?.close()
        current = publication
        this.sourceLabel = sourceLabel
        displayTitle = publication.metadata.title?.takeIf { it.isNotBlank() } ?: "未命名"
        displayAuthor = publication.metadata.authors
            .mapNotNull { it.name }
            .filter { it.isNotBlank() }
            .joinToString("、")
            .ifBlank { "未知作者" }
    }

    fun clear() {
        current?.close()
        current = null
        displayTitle = ""
        displayAuthor = ""
        sourceLabel = ""
    }
}
