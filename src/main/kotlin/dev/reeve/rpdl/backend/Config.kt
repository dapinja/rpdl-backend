package dev.reeve.rpdl.backend

class Config {
	var postgresAddress = "db"
	var port = 5432 // on docker -> 5432, on local -> 5672
	var databasePath = "./data/info.db"
}