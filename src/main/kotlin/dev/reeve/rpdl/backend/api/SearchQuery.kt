package dev.reeve.rpdl.backend.api

data class SearchQuery(
	var query: String,
	var engine: String,
	var andTags: List<String>,
	var orTags: List<String>,
	var notTags: List<String>,
) {
	fun serializeInputs() {
		val clean = Regex("[^a-zA-Z0-9-. ]");
		
		query = query.replace(clean, "")
		engine = engine.replace(clean, "")
		andTags = andTags.map { it.replace(clean, "") }
		orTags = orTags.map { it.replace(clean, "") }
		notTags = notTags.map { it.replace(clean, "") }
	}
}