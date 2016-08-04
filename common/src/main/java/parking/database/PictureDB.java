package parking.database;

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

	public PictureDB(MongoInterface mongo) {
		m_mongo = mongo;
		m_pictures = new GridFS(m_mongo.getDB(), "pictures");
	}

	public void addPicture(String name, String imageFileName) {
		System.out.println("addPicture " + name + " file " + imageFileName);
		try {
			File imageFile = new File(imageFileName);
			System.out.println("addPicture FILE is |" + imageFile.getAbsolutePath());
			GridFSInputFile gfsFile = m_pictures.createFile(imageFile);
			System.out.println("set name to " + name);
			gfsFile.setFilename(name);
			System.out.println("saving...");
			gfsFile.save();
			System.out.println("Saved Image " + gfsFile.getFilename());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// FIXME: adding the same name creates multiple entries
	public void addPicture(String name, File imageFile) {
		try {
			System.out.println("addPicture FILE is |" + imageFile.getAbsolutePath());
			GridFSInputFile gfsFile = m_pictures.createFile(imageFile);
			System.out.println("set name to " + name);
			gfsFile.setFilename(name);
			System.out.println("saving...");
			gfsFile.save();
			System.out.println("Saved Image " + gfsFile.getFilename());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public GridFSDBFile getPicture(String name) {
		GridFSDBFile image = m_pictures.findOne(name);
		if (image == null) {
			System.out.println("No image found for name " + name);
		}
		System.out.println("Found image for name " + name);
		return image;
	}

	public void removePicture(String name) {
		m_pictures.remove(m_pictures.findOne(name));
	}
}
