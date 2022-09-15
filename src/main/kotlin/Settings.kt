import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import f95.F95ZoneGrabber
import rpdl.RpdlGrabber
import java.io.File

object Settings {
	const val databasePath = "./data/info.db"
	val databaseManager = DatabaseManager()
	val rpdl = RpdlGrabber()
	val f95 = F95ZoneGrabber()
	
	object RegExp {
		val tags = Regex("<a href=\"/tags/(?<tagURL>[^/]+)/\" class=\"tagItem\" dir=\"auto\">(?<tagDisplay>[^<]+)</a>")
		val description = Regex("""(?<=<b>)(?: ?(?:Overview)|(?:The story):?)([\s\S]*?)(?=(?:&#8203;)|(?:<div )|(?:<span ))""")
		val rating = Regex("""ratingValue": "(?<data>[\d.]+)"""")
		
		val markdown = Regex("""(?<=\[(?<linkName>.{1,30})\] ?\()(?<link>[^\n\r)]+)""")
		val threadID = Regex("""\d+/?${'$'}""")
		val searchID = Regex("""(?<=/)\d+(?=/)""")
		
		val link = Regex("""<a href="([^"]*)">""")
	}
	
	object Url {
		const val rpdlURL = "https://dl.rpdl.net/"
		const val f95URL = "https://f95zone.to/"
	}
}