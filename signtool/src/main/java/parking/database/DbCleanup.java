package parking.database;

import parking.map.Sign;
import parking.display.DbSelector;
import parking.util.Logger;
import parking.util.Exif;

import com.mongodb.gridfs.GridFSDBFile;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

import java.util.Collections;
import java.util.Comparator;

class MyPictureComparator implements Comparator<MyPicture> {
	@Override
	public int compare(MyPicture p1, MyPicture p2) {
		if (p1.size > p2.size) {
			return -1;
		}
		else if (p1.size < p2.size) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

class MyPicture {
	private GridFSDBFile gfsFile;
	public long size;

	public MyPicture(GridFSDBFile gfsFile, long size) {
		this.gfsFile = gfsFile;
		this.size = size;
	}
}

public class DbCleanup {

	private DbSelector selector;
	private Logger logger;

	public DbCleanup(Logger logger) {
		selector = new DbSelector(logger, "DbCleanup", false);
		this.logger = logger;
	}

	public void findMissingPictures() {
		String[] dbNames = selector.getDbNames();
		for (int i=0 ; i<dbNames.length ; i++) {			
			findMissingPictures(dbNames[i]);
		}
	}

	public void findMissingPictures(String db) {
		selector.selectDB(db);
		List<String> signIDs = selector.dbIf().getSignDB().getSignsIDs();
		List<String> pictureList = selector.dbIf().getPictureDB().getPictureNames();
		Map<String, String> unique = selector.dbIf().getPictureDB().getUniquePictures();
		System.out.println("Found "+signIDs.size()+" signs "+pictureList.size()+" total pictures, "+unique.size()+" unique for "+db);
		int picHasPosition = 0;
		
		for (String md5 : unique.keySet()) {
			String picName = unique.get(md5);

			GridFSDBFile signPic = selector.dbIf().getPictureDB().getPicture(picName);
			if (Exif.getPosition(signPic.getInputStream()) != null) {
				System.out.println("Found picture with position "+picName+" size = "+signPic.getLength());
				picHasPosition++;
			}
			else {
				System.out.println("Found picture without position "+picName+" size = "+signPic.getLength());
			}
		}
		Map<String, List<String>> shared = new HashMap<String, List<String>>();		
		for (String picName : pictureList) {
			GridFSDBFile signPic = selector.dbIf().getPictureDB().getPicture(picName);
			String md5 = signPic.getMD5();
			List<String> md5List = shared.get(md5);
			if (md5List == null) {
				md5List = new ArrayList<String>();
				shared.put(md5, md5List);
			}
			md5List.add(picName);
		}
		int duplicates = 0;
		for (String md5: shared.keySet()) {
			int size = shared.get(md5).size();
			if (size > 1) {
				duplicates++;
				StringBuilder sb = new StringBuilder(10);
				sb.append("Found duplicates: ");
				for ( String picName : shared.get(md5)) {
					sb.append(picName+" ");
				}
				System.out.println(sb.toString());
			}
		}
		Map<String, Sign> found = new HashMap<String, Sign>();
		Map<String, Sign> missing = new HashMap<String, Sign>();
		int havePosition = 0;
		int haveSchedule = 0;
		int haveAuto = 0;
		for (String id : signIDs) {			
			Sign sign = selector.dbIf().getSignDB().getSign(id);
			System.out.println("Found sign "+sign.toString());
			if (sign.getImageName() == null) {
				sign = selector.dbIf().getSignDB().getSignOLD(id);
			}
			if (sign.getPosition() != null) {
				havePosition++;
			}
			if (sign.getParkingSchedule() != null) {
				haveSchedule++;
			}
			if (sign.getAutoSchedule() != null) {
				haveAuto++;
			}
			if (sign.getImageName() != null) {
				GridFSDBFile signPic = selector.dbIf().getPictureDB().getPicture(sign.getImageName());
				if (signPic == null) {
					missing.put(sign.getImageName(), sign);
				}
				else {
					found.put(sign.getImageName(), sign);
				}
			}
		}
		for (String picName : missing.keySet()) {
//			System.out.println(picName+" used by "+missing.get(picName).displayText()+" is missing");
		}
		for (String picName : found.keySet()) {
//			System.out.println("Found "+picName+" used by "+found.get(picName).displayText());
		}
		System.out.println(duplicates+" of "+pictureList.size()+" pictures are duplicates");
		System.out.println(picHasPosition+" of "+unique.size()+" pictures have position");
		System.out.println("Found "+found.size()+" pictures, missing "+missing.size()+" for "+db);
		System.out.println("Found "+signIDs.size()+" signs, "+havePosition+" have position "+haveSchedule+" have schedule "+haveAuto+" have auto schedule");
	}
	

	public void removeDuplicatePictures() {		
		String[] dbNames = selector.getDbNames();
		for (int i=0 ; i<dbNames.length ; i++) {			
			removeDuplicatePictures(dbNames[i]);
		}
	}

	public void removeDuplicatePictures(String db) {
		selector.selectDB(db);
		Map<String, Sign> inUse = getInUse();
		
//		String before = selector.dbIf().getPictureDB().printDuplicates();
//		logger.log("before:"+before);
		selector.dbIf().getPictureDB().removeDuplicates(inUse);
//		String after = selector.dbIf().getPictureDB().printDuplicates();
//		logger.log("after:"+after);
	}

	private Map<String, Sign> getInUse() {
		List<String> signIDs = selector.dbIf().getSignDB().getSignsIDs();
		Map<String, Sign> inUse = new HashMap<String, Sign>();
		for (String id : signIDs) {
			Sign sign = selector.dbIf().getSignDB().getSign(id);
			if (sign.getImageName() == null) {
				sign = selector.dbIf().getSignDB().getSignOLD(id);
			}
			if (sign.getImageName() != null) {
				GridFSDBFile signPic = selector.dbIf().getPictureDB().getPicture(sign.getImageName());
				if (signPic != null) {
					inUse.put(sign.getImageName(), sign);
//					System.out.println(sign.getImageName()+" is in use");
				}
			}
		}
		return inUse;
	}

	public void addMissingSigns() {
		String[] dbNames = selector.getDbNames();
		for (int i=0 ; i<dbNames.length ; i++) {			
			addMissingSigns(dbNames[i]);
		}
	}

	public void addMissingSigns(String db) {
		selector.selectDB(db);
		Map<String, Sign> inUse = getInUse();
		List<String> pictureList = selector.dbIf().getPictureDB().getPictureNames();
		for (String pic :pictureList) {
			Sign sign = inUse.get(pic);
			if ( sign == null) {
				sign = createNewSign(pic);
				if (sign != null) {
					if (pic != null) {
						System.out.println("created new sign "+sign.toString()+" for pic "+pic.toString());
					}
					else {
						System.out.println("created new sign "+sign.toString()+" for pic null");
					}
				}
				else if (pic != null) {
					System.out.println("Failed to add  new sign for pic "+pic.toString());
				}
				else {
					System.out.println("Failed to add  new sign for null pic");;
				}
			}
			else {
				selector.dbIf().getSignDB().updateSign(sign, null);
				System.out.println("updated sign "+sign.toString()+" for pic "+pic.toString());
			}
		}

	}

	private Sign createNewSign(String picName) {
		GridFSDBFile gfsFile = selector.dbIf().getPictureDB().getPicture(picName);
		Sign sign = null;
		try {
			sign = new Sign(gfsFile, logger);
			sign = selector.dbIf().getSignDB().addSign(sign);
			GridFSDBFile sanity = selector.dbIf().getPictureDB().getPicture(sign.getImageName());
			if (sanity == null) {
				System.out.println("Failed to properly create new file for old pic = "+picName+" sign "+sign.toString());
				return null;
			}		
			return sign;
		}
		catch (Exception ex) {
			
			System.out.println("Caught exception "+ex);
			if (gfsFile != null) {
				System.out.println("gfsFile = "+gfsFile.toString());
			}
			if (sign != null) {
				System.out.println("sign = "+sign.displayText());
			}
			ex.printStackTrace();
		}
		if (sign != null) {
//			System.out.println("Remove sign "+sign.toString());
			selector.dbIf().getSignDB().removeSign(sign);
		}
		return null;
	}
}