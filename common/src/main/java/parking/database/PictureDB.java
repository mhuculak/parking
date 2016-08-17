package parking.database;


import parking.util.Logger;
import parking.util.LoggingTag;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.BasicDBList;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import java.io.File;
import java.io.IOException;

public class PictureDB {

	private MongoInterface m_mongo;
	private GridFS m_pictures;
	private Logger m_logger;

	public PictureDB(MongoInterface mongo) {
		m_mongo = mongo;
		m_pictures = new GridFS(m_mongo.getDB(), "pictures");
		m_logger = new Logger(m_mongo.getLogger(), this, LoggingTag.PictureDB);
	}


	public void addPicture(String name, String imageFileName) {
		m_logger.log("addPicture " + name + " file " + imageFileName);
		try {
			File imageFile = new File(imageFileName);
			System.out.println("addPicture FILE is |" + imageFile.getAbsolutePath());
			GridFSInputFile gfsFile = m_pictures.createFile(imageFile);
			m_logger.log("set name to " + name);
			gfsFile.setFilename(name);
			m_logger.log("saving...");
			gfsFile.save();
			m_logger.log("Saved Image " + gfsFile.getFilename());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// FIXME: adding the same name creates multiple entries
	public String addPicture(String signID, File imageFile) {
		String path = imageFile.getAbsolutePath();
		String name = signID;
		int i = path.lastIndexOf('.');
		String extension = null;
		if (i > 0) {
    		extension = path.substring(i+1);
    		name = name + "." + extension;
		}
	
		try {
			m_logger.log("addPicture FILE is " + path);
			GridFSInputFile gfsFile = m_pictures.createFile(imageFile);
			m_logger.log("set name to " + name);
			gfsFile.setFilename(name);
			m_logger.log("saving...");
			gfsFile.save();
			m_logger.log("Saved Image " + gfsFile.getFilename());			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return name;
	}

	public GridFSDBFile getPicture(String name) {
		GridFSDBFile image = m_pictures.findOne(name);
		if (image == null) {
			m_logger.log("No image found for name " + name);
		}
		m_logger.log("Found image for name " + name);
		return image;
	}

	public void removePicture(String name) {
		m_pictures.remove(m_pictures.findOne(name));
	}
}
