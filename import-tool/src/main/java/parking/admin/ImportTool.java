package parking.admin;

import parking.security.User;
import parking.map.Position;
import parking.schedule.SignSchedule;

import org.json.JSONObject;
import org.json.JSONException;
import java.io.Console;
import okhttp3.*;
import java.io.*;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class ImportTool {

	public static void main(String[] args) {
		File tagMapsFile = new File("tagMaps.txt");
		String path = null;
		String userName = null;
		String db = null;
		if (args.length == 3) {
			userName = args[0];
			db = args[1];
			path = args[2];
		}
		
		if (userName == null || path == null) {
			System.out.println("import-tool user db phone path_to_files");
			System.exit(1);
		}
		User user = new User(userName);
		try {
			doImport(user, db, path);
		}
		catch (Exception ex) {
			System.out.println("Caught "+ex.toString());
			ex.printStackTrace();
		}
	}

	private static void doImport(User user, String db, String path) throws IOException, JSONException {
		final String imageDirName = "images";
		final String locationDirName = "location";
		File scanDir = new File(path);
		if (!scanDir.exists()) {
			System.out.println("Directory "+scanDir.toString()+" does not exist");
			return;
		}
		char[] passwordArray = null;
		Console console = System.console();
		if (console != null) {
		 	passwordArray = console.readPassword("Password: ");
		}
		else {
			System.out.println("ERROR: cannot open System.console");
			return;
		}
		user.setPassword(passwordArray);
		Map<Integer, MySign> signs = new HashMap<Integer, MySign>();
		File imageDir = new File(scanDir+File.separator+imageDirName);
		File[] imageDirListing = imageDir.listFiles();
		int numSigns = 0;
		for ( int i=0 ; i<imageDirListing.length ; i++) {
			System.out.println("Found image file "+imageDirListing[i].getName());
			MySign sign = new MySign(imageDirListing[i]);
			if (sign.getSignNum() >= 0) {
				signs.put(sign.getSignNum(), sign);
				numSigns++;
			}
		}
		System.out.println("Found "+imageDirListing.length+" images "+numSigns+" numbered images");
		File locationDir = new File(scanDir+File.separator+locationDirName);
		File[] locationDirListing = locationDir.listFiles();
		for ( int i=0 ; i<locationDirListing.length ; i++) {
			int num = MySign.getSignNum(locationDirListing[i].getName());
			if (num >= 0) {
				MySign sign = signs.get(num);
				sign.addLocation(locationDirListing[i]);
			}
		}
		System.out.println("Found "+signs.size()+" signs to upload");
		for (Integer signNum : signs.keySet() ) {
			MySign sign = signs.get(signNum);
			if ( sign.getSignImage()!= null && sign.getPosition() != null) {
				System.out.println("Uploading sign "+signNum);
				uploadSign(signs.get(signNum), db, user);
			}
			else {
				System.out.println("Skipping sign "+signNum);
			}
		}
	}

	private static void uploadSign(MySign sign, String db, User user) throws IOException, JSONException {
		int port = 8080;
		if (db.equals("test")) {
			port = 8082;
		}
		else if (db.equals("demo")) {
			port = 8081;
		}
		String server = "parking.allowed.org";
		String url = "http://"+server+":"+port+"/parking/upload";
		int timeout = 120;
		File uploadFile = sign.getSignImage();
		MediaType mediaType = MediaType.parse("image/jpeg");
		OkHttpClient httpClient = new OkHttpClient.Builder()
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
        MultipartBody multipart = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("lat", Double.toString(sign.getPosition().getLatitude()))
                    .addFormDataPart("lng", Double.toString(sign.getPosition().getLongitude()))
                    .addFormDataPart("image", uploadFile.getName(), RequestBody.create(mediaType, uploadFile)).build();
        String userPass = user.getUserName()+":"+user.getPassword();        
        String  encodedUserPass = Base64.getEncoder().encodeToString(userPass.getBytes());
        Request httpRequest = new Request.Builder()
                        .url(url)
                        .post(multipart)
                        .addHeader("Authorization", "Basic "+encodedUserPass)
                        .addHeader("User-Agent", "import-tool")
                        .build();
        Response response = httpClient.newCall(httpRequest).execute();
        String json = readResponse(response);
        if (json != null) {
        	System.out.println("got response "+json);
        	JSONObject jObj = new JSONObject(json);
        	SignSchedule autoSchedule = new SignSchedule(jObj);
        	System.out.println("Result: "+autoSchedule.toString());
        }
        else {
        	System.out.println("no result");
        }
	}

	private static String readResponse(Response response) throws IOException {
        int httpResponseCode = response.code();
        if (response.isSuccessful()) {
            System.out.println("succesful upload "+response.message());
            ResponseBody body = response.body();
            long len = body.contentLength();
            System.out.println( "got "+len+" byte response");
            if (len > 0 ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream()));
                StringBuilder sb = new StringBuilder(100);
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                System.out.println("got response " + sb.toString());
                return sb.toString();
            }
            return "";
        }
        return null;
    }
	
}

class MySign {
	private File imageFile;
	private Position position;
	private int signNum;

	public MySign(File imageFile) {
		this.imageFile = imageFile;
		signNum = getSignNum(imageFile.getName());
	}

	public void addLocation(File locationFile) throws IOException {
		List<Position> pList = new ArrayList<Position>();
		FileInputStream inputStream = new FileInputStream(locationFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = reader.readLine();
        while(line != null) {
        	pList.add(new Position(line));
            line = reader.readLine();
        }
        inputStream.close();
        double avgLat = 0.0;
        double avgLng = 0.0;
        for (Position p : pList) {
        	avgLat += p.getLatitude();
        	avgLng += p.getLongitude();
        }
        if (pList.size() > 1) {
        	avgLat = avgLat / pList.size();
        	avgLng = avgLng / pList.size();
        }
        position = new Position( avgLat, avgLng);
	}

	public Position getPosition() {
		return position;
	}

	public File getSignImage() {
		return imageFile;
	}

	public int getSignNum() {
		return signNum;
	}

	public static int getSignNum(String fileName) {
		 String[] comp = fileName.split("\\.");
         String basename = comp[0];
         String value = basename.replaceAll("[^0-9]","");
         System.out.println("parse value \""+value+"\"");
         if (value.length() > 0) {
         	return Integer.parseInt(value);
         }
         return -1;
	}
}