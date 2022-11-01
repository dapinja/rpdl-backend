package dev.reeve.rpdl.backend

import com.google.gson.FieldNamingPolicy
import dev.reeve.rpdl.backend.api.CheckGame
import dev.reeve.rpdl.backend.api.SearchQuery
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
	val updateFrequency = 60 * 15

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
					
					try {
						val response = checkGames.mapNotNull {
							val res = CheckGame(it.id, Settings.databaseManager.getGameInstance(it.id)?.torrentId?.toLong() ?: throw GameNotFoundException(it.id))
							
							return@mapNotNull if (res.torrentId != it.torrentId) {
								res
							} else {
								null
							}
						}
						
						call.respond(response)
					} catch (e: GameNotFoundException) {
						call.respondText("Game not found: ${e.id}", status = HttpStatusCode.NotFound)
					}
				}
				
				get("/searchGames") {
					val searchQuery = call.receive<SearchQuery>()
					
					call.respond(Settings.databaseManager.search(searchQuery))
				}
				
				get("/getGameInfo") {
					val gameId = call.request.queryParameters["game"]?.toIntOrNull() ?: return@get call.respondText("Game arg not found", status = HttpStatusCode.NotFound)
					val info = Settings.databaseManager.getGameInstance(gameId) ?: return@get call.respondText("Game not found: $gameId", status = HttpStatusCode.NotFound)
					call.respond(info)
				}
				
				get("/getF95Info") {
					val threadId = call.request.queryParameters["thread"]?.toIntOrNull() ?: return@get call.respondText("Thread arg not found", status = HttpStatusCode.NotFound)
					val info = Settings.databaseManager.getF95Info(threadId) ?: return@get call.respondText("Info not found", status = HttpStatusCode.NotFound)
					
					call.respond(info)
				}
				
				get("/getFullGameInfo") {
					val gameId = call.request.queryParameters["game"]?.toIntOrNull() ?: return@get call.respondText("Game arg not found", status = HttpStatusCode.NotFound)
					val info = Settings.databaseManager.getExtendedGameInstance(gameId) ?: return@get call.respondText("Game not found: $gameId", status = HttpStatusCode.NotFound)
					call.respond(info)
				}
				
				get("/updateCheckFrequency") {
					call.respond(updateFrequency)
				}
			}
		}.start(false)
		
		while (true) {
			Settings.rpdl.checkForUpdates()
			delay(1000L * updateFrequency) // 15 minutes
		}
	}
}