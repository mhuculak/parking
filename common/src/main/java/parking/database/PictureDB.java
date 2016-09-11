package parking.database;

import parking.map.Sign;

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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
//			System.out.println("addPicture FILE is " + imageFile.getAbsolutePath());
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

	public static String createPictureName(String signID, String path) {
		String name = signID;
		if (path != null) {
			int i = path.lastIndexOf('.');
			String extension = null;
			if (i > 0) {
    			extension = path.substring(i+1);
    			name = name + "." + extension;
			}
			return name;
		}
		return name + ".jpg"; // FIXME: should be a utility that can do a better job of guessing the extension
	}
	// FIXME: adding the same name creates multiple entries
	public String addPicture(String signID, File imageFile) {
		String path = imageFile.getAbsolutePath();
		String name = createPictureName(signID, path);	
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

	public boolean renamePicture(String oldName, String newName) {
		GridFSDBFile pic = getPicture(oldName);
		if (pic != null) {
			GridFSInputFile inPic = m_pictures.createFile(pic.getInputStream());
			inPic.setFilename(newName);
			inPic.save();
			removePicture(oldName);
            return true;
		}
		m_logger.error("Rename failed because "+oldName+" not found");
		return false;
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

	public List<String> getPictureNames() {
		DBCursor cursor = m_pictures.getFileList();
		List<String> nameList = new ArrayList<String>();
		while (cursor.hasNext()) {
	    	GridFSDBFile gfsFile = (GridFSDBFile)cursor.next();
//	    	System.out.println("Found picture "+gfsFile.toString()+" id = "+gfsFile.getId());  

	    	nameList.add(gfsFile.getFilename());	
		}
		return nameList;
	}

	public boolean removePicture(GridFSDBFile gfsFile) {
		BasicDBObject searchQuery = new BasicDBObject("_id", gfsFile.getId());
		List<GridFSDBFile> removeList = m_pictures.find(searchQuery);
		if (removeList.size() == 1) {
//			System.out.println("Remove "+gfsFile.toString());
			m_pictures.remove(searchQuery);
			return true;
		}		
		m_logger.error("Cannot remove sign "+gfsFile.toString()+" because number found = "+removeList.size());
		return false;
	}

	public void removeDuplicates(Map<String, Sign> inUse) {
		DBCursor cursor = m_pictures.getFileList();
		Map<String, Integer> md5Count = new HashMap<String, Integer>();
		int keep = 0;
		int remove = 0;
		while (cursor.hasNext()) {
	    	GridFSDBFile gfsFile = (GridFSDBFile)cursor.next();
	    	String name = gfsFile.getFilename();
	    	String md5 = gfsFile.getMD5();
	    	if (inUse.get(name) == null) {    		
	    		
	    			Integer md5c = md5Count.get(md5);
	    			if (md5c == null) {
	    				md5Count.put(md5, 1);
	    				System.out.println("Keep "+gfsFile.toString());
	    				keep++;
	    			}
	    			else {
	    				remove++;
	    				System.out.println("Remove "+gfsFile.toString());
	    				removePicture(gfsFile);
	    			}
	    		
	    	}
	    	else {
	    		System.out.println("Keep in use"+gfsFile.toString());
	    		md5Count.put(md5, 1);
	    		keep++;
	    	}
	    }
	    System.out.println("Keep "+keep+" remove "+remove);
	}
	public void removeDuplicates2(Map<String, Sign> inUse) {
		DBCursor cursor = m_pictures.getFileList();
		Map<String, Integer> md5Count = new HashMap<String, Integer>();
		int keep = 0;
		int remove = 0;
		Map<String, String> unique = getUniquePictures();
		while (cursor.hasNext()) {
	    	GridFSDBFile gfsFile = (GridFSDBFile)cursor.next();
	    	String name = gfsFile.getFilename();
	    	String md5 = gfsFile.getMD5();
	    	if (inUse.get(name) == null) {    		
	    		String picName = unique.get(md5);
	    		if (picName == null) {
	    			Integer md5c = md5Count.get(md5);
	    			if (md5c == null) {
	    				md5Count.put(md5, 1);
	    				System.out.println("Keep "+gfsFile.toString());
	    				keep++;
	    			}
	    			else {
	    				remove++;
	    				System.out.println("Remove "+gfsFile.toString());
	    				removePicture(gfsFile);
	    			}
	    		}
	    		else {
	    			System.out.println("Keep unique "+gfsFile.toString());
	    			md5Count.put(md5, 1);
	    			keep++;
	    		}
	    	}
	    	else {
	    		System.out.println("Keep in use"+gfsFile.toString());
	    		md5Count.put(md5, 1);
	    		keep++;
	    	}
	    }
	    System.out.println("Keep "+keep+" remove "+remove);
	}

	public void removeDuplicates() {
		DBCursor cursor = m_pictures.getFileList();
		Map<String, Integer> md5Count = new HashMap<String, Integer>();
		while (cursor.hasNext()) {
	    	GridFSDBFile gfsFile = (GridFSDBFile)cursor.next();
	    	String md5 = gfsFile.getMD5();
	    	Integer md5c = md5Count.get(md5);
	    	if (md5c == null) {
	    		md5Count.put(md5, 1);
//	    		System.out.println("Keep "+gfsFile.toString());
	    	}
	    	else {
//	    		removePicture(gfsFile);
	    	}
	    }
	}

	public Map<String, String> getUniquePictures() {
		DBCursor cursor = m_pictures.getFileList();
		Map<String, String> unique = new HashMap<String, String>();
		while (cursor.hasNext()) {
	    	GridFSDBFile gfsFile = (GridFSDBFile)cursor.next();
	    	String name = gfsFile.getFilename();
	    	String md5 = gfsFile.getMD5();
	    	String found = unique.get(md5);
	    	if (found == null) {
	    		unique.put(md5, name);
	    	}
	    }
	    return unique;
	}

	public String printDuplicates() {
		DBCursor cursor = m_pictures.getFileList();
		Map<String, Integer> nameCount = new HashMap<String, Integer>();
		Map<String, Integer> md5Count = new HashMap<String, Integer>();
		
		while (cursor.hasNext()) {
	    	GridFSDBFile gfsFile = (GridFSDBFile)cursor.next();
	    	String name = gfsFile.getFilename();
	    	Integer count = nameCount.get(name);
	    	String md5 = gfsFile.getMD5();
	    	Integer md5c = md5Count.get(md5);
	    	if (md5c == null) {
	    		md5Count.put(md5, 1);
	    	}
	    	else {
	    		md5Count.put(md5, ++md5c);
	    	}
	    	if (count == null) {
	    		nameCount.put(name, 1);
	    	}
	    	else {
	    		nameCount.put(name, ++count);
	    	}
	    }
	    int uniqueName = 0;
	    for (String name: nameCount.keySet()) {
	    	if (nameCount.get(name) > 1) {
//	    		System.out.println("Got count of "+(nameCount.get(name))+" for name "+name);
	    	}
	    	else {
	    		uniqueName++;
	    	}
	    }
	    int uniqueMd5 = 0;
	    for (String md5 : md5Count.keySet()) {
	    	if (md5Count.get(md5) > 1 ) {
//	    		System.out.println("Got count of "+(md5Count.get(md5))+" for md5 "+md5);
	    	}
	    	else {
	    		uniqueMd5++;
	    	}
	    }		
	    return "unique md5 = "+uniqueMd5+" unique names = "+uniqueName;
	}
}
