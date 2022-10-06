package dev.reeve.rpdl.backend

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dev.reeve.rpdl.backend.f95.F95ZoneGrabber
import dev.reeve.rpdl.backend.rpdl.RpdlGrabber
import java.io.File

object Settings {
	private val configDir = File("./config")
	private val configFile = File(configDir,"config.json")
	private val config = if (configDir.exists() && configFile.exists()) {
		Gson().fromJson(configFile.readText(), Config::class.java)
	} else {
		configDir.mkdirs()
		Config().also {
			configFile.writeText(GsonBuilder().setPrettyPrinting().create().toJson(it))
		}
	}
	val databaseType = DatabaseType.POSTGRESQL
	val databaseManager by lazy {
		DatabaseManager(config)
	}
	val rpdl by lazy {
		RpdlGrabber()
	}
	val f95 by lazy {
		F95ZoneGrabber()
	}
	
	enum class DatabaseType {
		POSTGRESQL,
		SQLITE
	}
	
	object RegExp {
		val tags = Regex("<a href=\"/tags/(?<tagURL>[^/]+)/\" class=\"tagItem\" dir=\"auto\">(?<tagDisplay>[^<]+)</a>")
		val description = Regex("""(?<=<b>)(?: ?(?:Overview)|(?:The story):?)([\s\S]*?)(?=(?:&#8203;)|(?:<div )|(?:<span ))""")
		val rating = Regex("""ratingValue": "(?<data>[\d.]+)"""")
		
		val markdown = Regex("""(?<=\[(?<linkName>.{1,30})\] ?\()(?<link>[^\n\r)]+)""")
		val threadID = Regex("""(?<=[./])\d+(?=/|${'$'})""")
		val searchID = Regex("""(?<=/)\d+(?=/)""")
		
		val link = Regex("""<a href="([^"]*)">""")
	}
	
	object Url {
		const val rpdlURL = "https://dl.rpdl.net/"
		const val f95URL = "https://f95zone.to/"
	}
}