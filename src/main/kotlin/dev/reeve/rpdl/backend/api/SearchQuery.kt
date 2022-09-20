package dev.reeve.rpdl.backend.api

data class SearchQuery(
	val query: String,
	val engine: String,
	val andTags: List<String>,
	val orTags: List<String>,
	val notTags: List<String>,
)