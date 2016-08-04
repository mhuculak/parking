package parking.database;

import parking.security.User;
import parking.security.User.Permission;
import parking.security.Password;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.BasicDBList;

import java.util.Base64;

public class UserDB {

	private MongoInterface m_mongo;
	private DBCollection m_users;
	private final String defaultPassword = "password";

	public UserDB(MongoInterface mongo) {
		m_mongo = mongo;
		m_users = m_mongo.getDB().getCollection("users");
	}

	public void addUser(String name) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", name);		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 0) {
			BasicDBObject document = new BasicDBObject();
			Object id = m_mongo.getNextID();
			document.append("_id", id);
			document.append("name", name);
			document.append("password", defaultPassword);
			document.append("permission", Permission.user.toString()); // default permission
			byte[] salt = Password.getRandomSalt();
			document.append("salt", Base64.getEncoder().encodeToString(salt));
			m_users.insert(document);
		}
		else {
			System.out.println("User " + name + " already exists");
		}		
	}

	public void setPermission(String name, Permission permission) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", name);		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 1) {
	    	BasicDBObject updateQuery =  new BasicDBObject();
	    	BasicDBObject newFields = new BasicDBObject();
	    	newFields.append("permission", permission.toString());
	    	updateQuery.append( "$set", newFields);
	    	m_users.update(searchQuery, updateQuery);
		}

	}

	public void setPassword(String name, String password) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", name);		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 1) {
			byte[] salt = Password.getRandomSalt();
//			System.out.println("Set password = " + password + " with salt = " + salt.toString());
			byte[] encrypted = Password.createKey(password, salt);
	    	BasicDBObject updateQuery =  new BasicDBObject();
	    	BasicDBObject newFields = new BasicDBObject();
	    	String encryptedPassword = Base64.getEncoder().encodeToString(encrypted);
	    	newFields.append("password",  encryptedPassword);
	    	String encodedSalt = Base64.getEncoder().encodeToString(salt);
	    	newFields.append("salt", encodedSalt);
	    	updateQuery.append( "$set", newFields);
	    	System.out.println("Storing password in DB as " + encryptedPassword + " with salt " + encodedSalt);
	    	m_users.update(searchQuery, updateQuery);
		}
	}

	public boolean checkPassword(String name, String password) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", name);		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 1) {
			DBObject document = cursor.next();
			String encodedSalt = (String)document.get("salt");
	    	byte[] salt = Base64.getDecoder().decode(encodedSalt);
	    	byte[] inputKey = Password.createKey(password, salt);
	    	String encryptedPassword = (String)document.get("password");
	    	byte[] correctKey = Base64.getDecoder().decode(encryptedPassword);	    	
	    	System.out.println("Comparing encrypted input password " + inputKey.toString() + " with correct key " + correctKey.toString() + " using salt " + salt.toString());
	    	for ( int i=0 ; i<correctKey.length ; i++) {	    		
	    		if (correctKey[i] != inputKey[i]) {
	    			return false;
	    		}
	    	}
	    	return true;
		}
		return false;
	}

	public Permission getPermission(String name) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", name);		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 1) {
			DBObject document = cursor.next();
			Permission permission = Permission.valueOf(document.get("permission").toString());
			return permission;
		}
		return null;
	}

	public boolean userNameExists(String name) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", name);		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 1) {			
			System.out.println("Found user " + name);
			return true;
		}
		else {
			System.out.println("Found " + cursor.count() + " users");
			return false;
		}
	}

	public void changeUserName(String oldName, String newName) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", oldName);		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 1) {			
			BasicDBObject updateQuery =  new BasicDBObject();
	    	BasicDBObject newFields = new BasicDBObject();
	    	newFields.append("name", newName);
	    	updateQuery.append( "$set", newFields);
	    	m_users.update(searchQuery, updateQuery);
		}
		else {
			System.out.println("ERROR: changeUserName found " + cursor.count() + " users");
		}
	}

	public void uploadAvatar(String name, String avatarFile) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", name);		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 1) {
			String avatarFileName = name + ":avatar";
			System.out.println("Adding avatar name " + avatarFileName);			
			BasicDBObject updateQuery =  new BasicDBObject();
	    	BasicDBObject newFields = new BasicDBObject();
	    	newFields.append("avatar", avatarFileName);
	    	updateQuery.append( "$set", newFields);
	    	m_users.update(searchQuery, updateQuery);
	    	System.out.println("Adding avatar name " + avatarFileName + " file " + avatarFile);
	    	m_mongo.getPictureDB().addPicture(avatarFileName, avatarFile);
		}
		else {
			System.out.println("ERROR: uploadAvatar found " + cursor.count() + " users");
		}				
	}

	public String getAvatarName(String name) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", name);		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 1) {
			DBObject document = cursor.next();
			Object avatarObj = document.get("avatar");
			if (avatarObj != null) {				
				String avatarName = avatarObj.toString();
				System.out.println("Found avatar " + avatarName);
				return avatarName;
			}
			else {
				System.out.println("Avatar not found for " + name);
			}
		}
		else {
			System.out.println("No record found for " + name);
		}
		return null;
	}

}
    