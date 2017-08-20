package parking.database;

import parking.security.User;
import parking.security.User.Permission;
import parking.security.Password;

import parking.util.Logger;
import parking.util.LoggingTag;

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
// import android.util.Base64;

public class UserDB {

	private MongoInterface m_mongo;
	private DBCollection m_users;
	private Logger m_logger;
	private final String defaultPassword = "password";

	public UserDB(MongoInterface mongo) {
		m_mongo = mongo;		
		m_users = m_mongo.getDB().getCollection("users");
		m_logger = new Logger(m_mongo.getLogger(), this, LoggingTag.UserDB);
	}

	public void addUser(String name) {
		Logger logger = new Logger(m_logger, "addUser");
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
			logger.log("User " + name + " already exists");
		}		
	}

	public void addUser(User user) {
		Logger logger = new Logger(m_logger, "addUser");
		BasicDBObject searchQuery = new BasicDBObject();
		if (user == null || user.getUserName() == null) {
			logger.error("cannot add null user");
			return;
		}
		searchQuery.put("name", user.getUserName());		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 0) {
			BasicDBObject document = new BasicDBObject();
			Object id = m_mongo.getNextID();
			document.append("_id", id);
			document.append("name", user.getUserName());
			if (user.getPassword() != null) {
				setPassword(document, user.getPassword());
			}
			if (user.getPhone() != null) {
				document.append("phone", user.getPhone());
			}
			if (user.getEmail() != null) {
				document.append("email", user.getEmail());
			}
			if (user.getAddress() != null) {
				document.append("address", user.getAddress());
			}
			document.append("permission", Permission.user.toString()); // default permission
			m_users.insert(document);
		}
		else {
			logger.log("User " + user.getUserName() + " already exists");
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
	    	BasicDBObject updateQuery =  new BasicDBObject();
	    	BasicDBObject newFields = new BasicDBObject();
	    	setPassword(newFields, password);	    	
	    	updateQuery.append( "$set", newFields);
//	    	System.out.println("Storing password in DB as " + encryptedPassword + " with salt " + encodedSalt);
	    	m_users.update(searchQuery, updateQuery);
		}
	}

	private void setPassword(BasicDBObject document, String password) {
//		Logger logger = new Logger(m_logger, "setPassword");
//		logger.log("Adding password "+password+" to the DB");
		byte[] salt = Password.getRandomSalt();
		byte[] encrypted = Password.createKey(password, salt);
		String encryptedPassword = Base64.getEncoder().encodeToString(encrypted);
		String encodedSalt = Base64.getEncoder().encodeToString(salt);
		document.append("password",  encryptedPassword);
		document.append("salt", encodedSalt);
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
//	    	System.out.println("Comparing encrypted input password " + inputKey.toString() + " with correct key " + correctKey.toString() + " using salt " + salt.toString());
	    	for ( int i=0 ; i<correctKey.length ; i++) {	    		
	    		if (correctKey[i] != inputKey[i]) {
	    			return false;
	    		}
	    	}
	    	return true;
		}
		return false;
	}

	public boolean checkPhone(String name, String phone) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", name);		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 1) {
			DBObject document = cursor.next();
			if (document.get("phone") != null) {
				String correctPhone = (String)document.get("phone");
				if (correctPhone.equals(phone)) {
					return true;
				}
			}
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
//			System.out.println("Found user " + name);
			return true;
		}
		else {
//			System.out.println("Found " + cursor.count() + " users");
			return false;
		}
	}

	public void changeUserName(String oldName, String newName) {
		Logger logger = new Logger(m_logger, "changeUserName");
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
			logger.error("found " + cursor.count() + " users");
		}
	}

	public void uploadAvatar(String name, String avatarFile) {
		Logger logger = new Logger(m_logger, "uploadAvatar");
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
	    	logger.log("Adding avatar name " + avatarFileName + " file " + avatarFile);
	    	m_mongo.getPictureDB().addPicture(avatarFileName, avatarFile);
		}
		else {
			logger.error("found " + cursor.count() + " users");
		}				
	}

	public String getAvatarName(String name) {
		Logger logger = new Logger(m_logger, "getAvatarName");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", name);		
		DBCursor cursor = m_users.find(searchQuery);
		if (cursor.count() == 1) {
			DBObject document = cursor.next();
			Object avatarObj = document.get("avatar");
			if (avatarObj != null) {				
				String avatarName = avatarObj.toString();
				logger.log("Found avatar " + avatarName);
				return avatarName;
			}
			else {
				logger.error("Avatar not found for " + name);
			}
		}
		else {
			logger.error("No record found for " + name);
		}
		return null;
	}

}
    