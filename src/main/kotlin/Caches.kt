object Caches {
	val categoryCache = Cache {
		Settings.databaseManager.getCategories().associateBy {
			it.id
		}
	}
	val uploaderCache = Cache {
		Settings.databaseManager.getUploaders().toMutableList()
	}
	val instanceCache = Cache {
		Settings.databaseManager.getGameInstances().associateBy {
			it.id!!
		}.toMutableMap()
	}
}