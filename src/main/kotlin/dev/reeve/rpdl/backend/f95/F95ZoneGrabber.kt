package dev.reeve.rpdl.backend.f95

import dev.reeve.rpdl.backend.Settings
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException

class F95ZoneGrabber {
	private val client = OkHttpClient()
	
	fun downloadPage(threadID: Int, update: HashSet<Int>? = null): F95Info? {
		val inf = Settings.databaseManager.getF95Info(threadID)
		
		if (inf == null || (update != null && !update.contains(threadID))) {
			update?.add(threadID)
			
			val url = "${Settings.Url.f95URL}threads/$threadID/"
			
			val request = Request.Builder().url(url).build()
			
			try {
				client.newCall(request).execute().use { response ->
					if (!response.isSuccessful) {
						return null
					}
					
					val result = response.body!!.string().replace("&quot;", "\"")
					
					/*val file = File("./data/", "$threadID.html")
					file.writeText(result)*/
					
					val rating = Settings.RegExp.rating.find(result)?.groupValues?.get(1) ?: "0"
					val desc = Settings.RegExp.description.find(result)?.groupValues
					
					return F95Info(threadID,
						Settings.RegExp.tags.findAll(result).map { it.groupValues[2] }.toList(),
						rating.toDoubleOrNull() ?: error("hmm"),
						(desc?.get(1)?.let {
							it.replaceFirst(":", "").replace("<b>", "").replace("</b>", "").replace("<br>", "").replace("<i>", "")
								.replace("</i>", "").replace("'", "").trimStart().trimEnd()
						}) ?: ""
					).also {
						Settings.databaseManager.putF95Info(it)
					}
				}
			} catch (e: SocketTimeoutException) {
				println("Error downloading $url, time out")
				return null
			}
		} else {
			println("Info already exists for $threadID")
			return inf
		}
		
	}
}