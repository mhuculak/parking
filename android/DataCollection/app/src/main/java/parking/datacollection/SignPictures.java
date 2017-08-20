package parking.datacollection;

import parking.map.Position;
import parking.util.Utils;
import android.util.Log;
import android.media.MediaScannerConnection;
import android.content.Context;

import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.BufferedWriter;

public class SignPictures {

    private static final String picCountFile = "dcPicCount";
    private static final String locationtDir = "location";
    private static final String locationtFile = "signLocation";
    private static final String TAG = "parking";

    public static int readPictureCount(File filesDir) {
        File file = new File(filesDir+File.separator+picCountFile);
        int count = 0;
        try {
            if (file.exists()) {
                Log.i(TAG,"Read picture count from "+file.getAbsolutePath());
                FileInputStream inputStream = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = reader.readLine();
                count = Utils.parseInt(line);
                while(line != null) {
                    Log.i(TAG,"read: \""+line+"\"");
                    line = reader.readLine();
                }
                inputStream.close();
            }
            else {
                Log.e(TAG,"Picture count file "+file.getAbsolutePath()+" does not exist");
            }
            return count;
        }
        catch (Exception ex) {
            Log.e(TAG, "exception", ex);
        }
        return 0;
    }

    public static void writePictureCount(File filesDir, Integer count) {
        try {
            File file = new File(filesDir+File.separator+picCountFile);

            Log.i(TAG,"Write count "+count+" to "+file.getAbsolutePath());
            FileWriter fstream = new FileWriter(file, false);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(count.toString());
            out.newLine();
            out.close();
        }
        catch (Exception ex) {
            Log.e(TAG,"exception", ex);
        }
    }

    public static void saveLocationData(Context context, File filesDir, String appDir, int num, List<Position> points) {
        if (points == null || points.size()==0) {
            return;
        }
        Log.i(TAG,"saving "+points.size()+" positions");
        try {
            File locDir = new File(filesDir+File.separator+appDir+File.separator+locationtDir);
            if (!locDir.exists()) {
                if (locDir.mkdirs()) {
                    Log.i(TAG, locDir.getAbsolutePath() + " was created");
                }
                else {
                    Log.e(TAG, "failed to create "+locDir.getAbsolutePath());
                    return;
                }
            }
            File file = new File(locDir+File.separator+locationtFile+num);
            Log.i(TAG,"Write position data to "+file.getAbsolutePath());
            FileWriter fstream = new FileWriter(file, false);
            BufferedWriter out = new BufferedWriter(fstream);
            for (Position p: points) {
                out.write(p.toString());
                out.newLine();
            }
            out.close();
            Log.i(TAG,"Scan file "+locDir.toString());
            MediaScannerConnection.scanFile(context, new String[] {file.toString()}, null, null);
        }
        catch (Exception ex) {
            Log.e(TAG,"exception", ex);
        }
    }
}
