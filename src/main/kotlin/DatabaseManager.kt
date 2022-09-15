import com.google.gson.Gson
import f95.F95Info
import rpdl.Category
import rpdl.GameInstance
import rpdl.Uploader
import java.io.Closeable
import java.sql.DriverManager
import java.sql.Statement
import java.util.Date

class DatabaseManager : Closeable {
	private val connection = DriverManager.getConnection("jdbc:sqlite:${Settings.databasePath}")
	
	fun reindex() {
		val statement = connection.createStatement()
		statement.execute("REINDEX rpdlInstances_threadID;")
		statement.execute("REINDEX rpdlInstances_categoryID;")
		statement.execute("REINDEX rpdlInstances_uploaderID;")
		statement.close()
	}
	
	fun getF95Info(thread: Int): F95Info? {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM f95zone WHERE id = $thread")
		if (result.next()) {
			return F95Info(
				result.getInt("id"),
				Gson().fromJson(result.getString("tags"), Array<String>::class.java).toList(),
				result.getString("rating").toDoubleOrNull(),
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
	
	fun getGameInstance(torrentID: Long): GameInstance? {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM rpdlInstances WHERE torrentID = $torrentID")
		if (result.next()) {
			return GameInstance(result)
		}
		return null
	}
	
	fun getGameInstances(threadID: Int): List<GameInstance> {
		val statement = connection.createStatement()
		val result = statement.executeQuery("SELECT * FROM rpdlInstances WHERE threadID = $threadID")
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
				result.getInt("id"), result.getString("name"), result.getInt("numTorrents")
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
					result.getInt("id"), result.getString("name"), result.getInt("numTorrents")
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
		val str = "INSERT INTO f95zone " + "VALUES (${info.threadID}, '${Gson().toJson(info.tags)}', ${if (info.rating == null) null else "${info.rating}"}, '${info.description}') " + "ON CONFLICT(id) " + "DO UPDATE SET tags = '${
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
		statement.execute("INSERT INTO categories VALUES (${category.id}, '${category.name}', ${category.numTorrents})")
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
		if (instance.id == null && getGameInstance(instance.torrentID) == null) {
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