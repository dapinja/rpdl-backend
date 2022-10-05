package dev.reeve.rpdl.backend

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dev.reeve.rpdl.backend.api.ExtendedInstance
import dev.reeve.rpdl.backend.api.SearchQuery
import dev.reeve.rpdl.backend.f95.F95Info
import dev.reeve.rpdl.backend.rpdl.Category
import dev.reeve.rpdl.backend.rpdl.GameInstance
import dev.reeve.rpdl.backend.rpdl.Uploader
import org.postgresql.Driver
import java.io.Closeable
import java.io.File
import java.sql.DriverManager
import java.sql.Statement
import java.util.*

class DatabaseManager(private val config: Config) : Closeable {
	init {
		DriverManager.registerDriver(Driver())
	}
	
	private val connection = when (Settings.databaseType) {
		Settings.DatabaseType.SQLITE -> DriverManager.getConnection("jdbc:sqlite:${config.databasePath}")
		Settings.DatabaseType.POSTGRESQL -> DriverManager.getConnection("jdbc:postgresql://${config.postgresAddress}:${config.port}/", "postgres", "postgres")
	}
	
	fun reindex() {
		val statement = connection.createStatement()
		statement.execute("REINDEX INDEX rpdlInstances_threadID;")
		statement.execute("REINDEX INDEX rpdlInstances_categoryID;")
		statement.execute("REINDEX INDEX rpdlInstances_uploaderID;")
		statement.close()
	}
	
	fun search(searchQuery: SearchQuery): MutableList<ExtendedInstance> {
		searchQuery.serializeInputs()
		
		val andTagsString = buildString {
			if (searchQuery.andTags.isNotEmpty()) {
				append(" (")
				for (tag in searchQuery.andTags) {
					append("tags LIKE '%${tag}%' AND ")
				}
				delete(length - 4, length)
				append(")")
			}
		}
		val orTagsString = buildString {
			if (searchQuery.orTags.isNotEmpty()) {
				if (andTagsString.isNotEmpty()) {
					append(" AND")
				}
				append(" (")
				for (tag in searchQuery.orTags) {
					append("tags LIKE '%${tag}%' OR ")
				}
				delete(length - 3, length)
				append(")")
			}
		}
		val notTagsString = buildString {
			if (searchQuery.notTags.isNotEmpty()) {
				if (orTagsString.isNotEmpty() || andTagsString.isNotEmpty()) {
					append(" AND")
				}
				append(" (")
				for (tag in searchQuery.notTags) {
					append("tags NOT LIKE '%${tag}%' AND ")
				}
				delete(length - 4, length)
				append(")")
			}
		}
		val searchTermString = buildString {
			if (searchQuery.query.isNotEmpty()) {
				append(" (")
				append("title LIKE '%${searchQuery.query}%' OR ")
				append("description LIKE '%${searchQuery.query}%'")
				append(")")
			}
		}
		val engineString = buildString {
			if (searchQuery.engine.isNotEmpty()) {
				if (searchTermString.isNotEmpty()) {
					append(" AND")
				}
				append(" (")
				append("categoryID = ${getCategories().find { it.name.lowercase() == searchQuery.engine }!!.id}")
				append(")")
			}
		}
		
		val query = buildString {
			append("SELECT ")
			append("rpdl.id as id, threadID, title, version, fileSize, categoryID, torrentID, uploadDate, uploaderID, links, tags, rating, description")
			append(" FROM rpdlInstances rpdl JOIN (")
			append("SELECT * FROM f95zone")
			if (searchQuery.andTags.isNotEmpty() || searchQuery.orTags.isNotEmpty() || searchQuery.notTags.isNotEmpty()) {
				append(" WHERE")
				append(andTagsString)
				append(orTagsString)
				append(notTagsString)
			}
			append(") f95 ON rpdl.threadID = f95.id")
			if (searchQuery.query.isNotEmpty() || searchQuery.engine.isNotEmpty()) {
				append(" WHERE")
				append(searchTermString)
				append(engineString)
			}
			append(" ORDER BY uploadDate DESC")
		}
		
		val statement = connection.createStatement()
		val result = statement.executeQuery(query)
		
		Caches.uploaderCache.getAndSet()
		Caches.categoryCache.getAndSet()
		
		val list = mutableListOf<ExtendedInstance>()
		while (result.next()) {
			list.add(ExtendedInstance(result))
		}
		
		Caches.uploaderCache.clear()
		Caches.categoryCache.clear()
		
		return list
	}
	
	fun getF95Info(thread: Int): F95Info? {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM f95zone WHERE id = $thread")
		if (result.next()) {
			return F95Info(
				result.getInt("id"),
				Gson().fromJson(result.getString("tags"), Array<String>::class.java).toList(),
				result.getString("rating").toDouble(),
				result.getString("description")
			)
		}
		
		return null
	}
	
	fun getGameInstance(gameID: Int): GameInstance? {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM rpdlInstances WHERE id = $gameID")
		if (result.next()) {
			return GameInstance(result)
		}
		return null
	}
	
	private fun getGameInstance(torrentID: Long): GameInstance? {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM rpdlInstances WHERE torrentID = '$torrentID'")
		if (result.next()) {
			return GameInstance(result)
		}
		return null
	}
	
	fun getGameInstances(threadID: Int): List<GameInstance> {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM rpdlInstances WHERE threadID = '$threadID'")
		val list = mutableListOf<GameInstance>()
		while (result.next()) {
			list.add(GameInstance(result))
		}
		return list
	}
	
	fun getGameInstances(): List<GameInstance> {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM rpdlInstances")
		val list = mutableListOf<GameInstance>()
		while (result.next()) {
			list.add(GameInstance(result))
		}
		return list
	}
	
	fun getCategory(categoryID: Int): Category? {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM categories WHERE id = $categoryID")
		if (result.next()) {
			return Category(
				result.getInt("id"), result.getString("name")
			)
		}
		return null
	}
	
	fun getCategories(): List<Category> {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM categories")
		val categories = mutableListOf<Category>()
		while (result.next()) {
			categories.add(
				Category(
					result.getInt("id"), result.getString("name")
				)
			)
		}
		return categories
	}
	
	fun getUploaders(): MutableList<Uploader> {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM uploaders")
		val uploaders = mutableListOf<Uploader>()
		while (result.next()) {
			uploaders.add(
				Uploader(
					result.getInt("id"), result.getString("name"), result.getString("lastSeen").toLong()
				)
			)
		}
		
		return uploaders
	}
	
	fun getUploader(uploaderID: Int): Uploader? {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM uploaders WHERE id = $uploaderID")
		if (result.next()) {
			return Uploader(
				result.getInt("id"), result.getString("name"), result.getString("lastSeen").toLong()
			)
		}
		return null
	}
	
	fun getUploader(name: String): Uploader? {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM uploaders WHERE name = '$name'")
		if (result.next()) {
			return Uploader(
				result.getInt("id"), result.getString("name"), result.getString("lastSeen").toLong()
			)
		}
		return null
	}
	
	fun getLastCheck(): Date {
		val statement = connection.createStatement()
		
		val result = statement.executeQuery("SELECT * FROM data WHERE name = 'lastUpdate'")
		
		if (result.next()) {
			return Date(result.getString("value").toLong())
		}
		
		return Date(0)
	}
	
	fun putLastCheck(date: Date) {
		val statement = connection.createStatement()
		statement.execute("UPDATE data SET value = '${date.time}' WHERE name = 'lastUpdate';")
	}
	
	fun putF95Info(info: F95Info) {
		val statement = connection.createStatement()
		val str =
			"INSERT INTO f95zone " + "VALUES (${info.threadID}, '${Gson().toJson(info.tags)}', ${if (info.rating == null) null else "${info.rating}"}, '${info.description}') " + "ON CONFLICT(id) " + "DO UPDATE SET tags = '${
				Gson().toJson(
					info.tags
				)
			}', rating = ${if (info.rating == null) null else "${info.rating}"}, description = '${info.description}'"
		statement.execute(
			str
		)
	}
	
	fun putCategory(category: Category) {
		val statement = connection.createStatement()
		statement.execute("INSERT INTO categories VALUES (${category.id}, '${category.name}')")
	}
	
	fun putUploader(uploader: Uploader): Int? {
		val transaction = connection.prepareStatement(
			"INSERT INTO uploaders (name, lastSeen) VALUES ('${uploader.name}', '${uploader.lastSeen}') ON CONFLICT(name) DO UPDATE SET lastSeen = '${uploader.lastSeen}'",
			Statement.RETURN_GENERATED_KEYS
		)
		transaction.executeUpdate()
		
		val set = transaction.generatedKeys
		if (set.next()) {
			uploader.id = set.getInt(1)
			return set.getInt(1)
		}
		
		return null
	}
	
	fun putGameInstance(instance: GameInstance): Int? {
		if (instance.id == null && getGameInstance(instance.torrentId) == null) {
			val transaction = connection.prepareStatement(
				"INSERT INTO rpdlInstances (threadID, title, version, fileSize, categoryID, torrentID, uploadDate, uploaderID, links) VALUES ($instance)",
				Statement.RETURN_GENERATED_KEYS
			)
			
			transaction.executeUpdate()
			
			val set = transaction.generatedKeys
			if (set.next()) {
				instance.id = set.getInt(1)
				return set.getInt(1)
			}
			
			return null
		} else {
			val statement = connection.createStatement()
			statement.execute(
				"INSERT INTO rpdlInstances VALUES ($instance) ON CONFLICT (id) DO UPDATE SET ${instance.update()}"
			)
			
			return instance.id
		}
	}
	
	override fun close() {
		connection.close()
	}
}