package parking.database;

import parking.security.User.Permission;
import parking.util.Logger;
import parking.util.LoggingTag;

import java.io.File;

public class DatabaseSetup {

	private final static String email = "mike.huculak@gmail.com";
	private final static String defaultDB = "demo";
	private final static String defaultPassword = "";

	public static void main(String[] args) {
		File tagMapsFile = new File("tagMaps.txt");
		Logger logger = new Logger(tagMapsFile, LoggingTag.Database, "main");
		String db = null;
		String user = null;
		String password = null;
		if (args.length > 0) {
			user = args[0];
		}
		if (args.length > 1) {
			db = args[1];
		}
		if (args.length > 2) {
			password = args[1];
		}
		if (user == null) {
			System.out.println("db-setup user <db> <password>");
			System.exit(1);
		}
		password = password == null ? defaultPassword : password;
		db = db == null ? defaultDB : db;

		System.out.println("init DB for user: "+user+" db = "+db);
		MongoInterface mongo = MongoInterface.getInstance(db, logger);
		mongo.getUserDB().addUser(user);	
		mongo.getUserDB().setPassword(user, password);

	}
	
}