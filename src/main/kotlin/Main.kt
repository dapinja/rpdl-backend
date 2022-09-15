import api.CheckGame
import com.google.gson.FieldNamingPolicy
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>): Unit = runBlocking {
	coroutineScope {
		embeddedServer(Netty, port = 5671) {
			install(ContentNegotiation) {
				gson(ContentType.Application.Json) {
					setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
					setPrettyPrinting()
				}
			}
			routing {
				post("/checkForUpdates") {
					val checkGames = call.receive<List<CheckGame>>().toMutableList()
					
					println(checkGames)
					
					try {
						val response = checkGames.mapNotNull {
							val res = CheckGame(it.id, Settings.databaseManager.getGameInstance(it.id)!!.torrentID)
							
							return@mapNotNull if (res.torrentId != it.torrentId) {
								res
							} else {
								null
							}
						}
						
						println(response)
						
						call.respond(response)
					} catch (_: NullPointerException) {
						call.respondText("Game not found", status = HttpStatusCode.NotFound)
					}
					
				}
				get("/getF95Info") {
					val game = call.request.queryParameters["game"]?.toIntOrNull() ?: return@get call.respondText("Game arg not found", status = HttpStatusCode.NotFound)
					
					val id = Settings.databaseManager.getGameInstance(game)?.threadID ?: return@get call.respondText("Game not found", status = HttpStatusCode.NotFound)
				
					val info = Settings.databaseManager.getF95Info(id) ?: return@get call.respondText("Info not found", status = HttpStatusCode.NotFound)
					
					call.respond(info)
				}
			}
		}.start(false)
		
		while (true) {
			Settings.rpdl.checkForUpdates()
			delay(1000 * 60 * 15) // 15 minutes
		}
	}
}