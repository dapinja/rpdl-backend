package rpdl

import Caches
import Settings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.sql.ResultSet

data class GameInstance(
	var id: Int?,
	val threadID: Int?,
	val title: String,
	val version: String?,
	val fileSize: Long,
	val category: Category,
	val torrentID: Long,
	val uploadedDate: Long,
	val uploader: Uploader,
	val links: HashMap<String, String>?
) {
	constructor(result: ResultSet) : this(
		result.getInt("id"),
		result.getInt("threadID").let {
			if (it == -1) {
				return@let null
			} else {
				return@let it
			}
		},
		result.getString("title"),
		result.getString("version"),
		result.getString("fileSize").toLong(),
		result.getInt("categoryID").let {
			return@let Caches.categoryCache.get()[it]
		}!!,
		result.getString("torrentID").toLong(),
		result.getString("uploadDate").toLong(),
		result.getInt("uploaderID").let { id ->
			return@let Caches.uploaderCache.get().find { it.id == id }
		}!!,
		result.getString("links").let {
			return@let Gson().fromJson(it, TypeToken.getParameterized(HashMap::class.java, String::class.java, String::class.java).type)
		}
	)
	
	override fun toString(): String {
		return "${if (id != null) "$id, " else ""}${threadID ?: -1}, '$title', '$version', '$fileSize', ${category.id}, '$torrentID', '$uploadedDate', ${uploader.id}, '${
			Gson().toJson(
				links
			)
		}'"
	}
	
	fun update(): String {
		return "threadID = $threadID, title = '$title', version = '$version', fileSize = '$fileSize', categoryID = ${category.id}, torrentID = '$torrentID', uploadDate = '$uploadedDate', uploaderID = ${uploader.id}, links = '${
			Gson().toJson(
				links
			)
		}'"
	}
}