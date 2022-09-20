package dev.reeve.rpdl.backend.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.reeve.rpdl.backend.Caches
import dev.reeve.rpdl.backend.rpdl.Category
import dev.reeve.rpdl.backend.rpdl.GameInstance
import dev.reeve.rpdl.backend.rpdl.Uploader
import java.sql.ResultSet

class ExtendedInstance(
	val id: Int?,
	val threadID: Int?,
	val title: String,
	val version: String?,
	val fileSize: Long,
	val category: Category,
	val torrentID: Long,
	val uploadedDate: Long,
	val uploader: Uploader,
	val links: HashMap<String, String>?,
	val tags: List<String>,
	val rating: Double,
	val description: String,
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
		},
		result.getString("tags").let {
			return@let Gson().fromJson(it, TypeToken.getParameterized(ArrayList::class.java, String::class.java).type)
		},
		result.getString("rating").toDouble(),
		result.getString("description")
	)
}