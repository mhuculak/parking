package parking.parkingapp;

import parking.map.Position;
import parking.security.User;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;

import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class UserCredentials {

    private User user;
//    private File filesDir;
    private static String credentialsFile = "credentials";
    private static String key = "parkingallowed16";
    private static String TAG = "parking";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    public UserCredentials(User user) {
        this.user = user;
    }

    public static UserCredentials getCredentials(Context context) {
        File filesDir = context.getFilesDir();
        Log.i(TAG, "get credentials from dir = "+filesDir.getAbsolutePath()+" file = "+credentialsFile);
        User user = readCredentials(filesDir, credentialsFile);
        if (user != null) {
            return new UserCredentials(user);
        }
        return null;
    }

    public static void setCredentials(Context context, User user) {
        File filesDir = context.getFilesDir();
        writeCredentials(filesDir, credentialsFile, user);
    }

    public User getUser() {
        return user;
    }

    private static User readCredentials(File filesDir, String fileName) {
        File file = new File(filesDir+File.separator+fileName);
        try {
            if (file.exists()) {
                Log.i(TAG, "reading credentials from "+file.getAbsolutePath());
                String userName = null;
                char[] password = null;
                Key secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                FileInputStream inputStream = new FileInputStream(file);
                byte[] inputBytes = new byte[(int) file.length()];
                inputStream.read(inputBytes);
                byte[] decryptedBytes = cipher.doFinal(inputBytes);
                BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(decryptedBytes)));
                String line = reader.readLine();
                while (line != null) {
                    String[] comp = line.split(":");
                    if (comp.length == 2) {
                        String key = comp[0];
                        String value = comp[1];
                        if (key.equals("userName")) {
                            userName = value;
                        }
                        else if (key.equals("password")) {
                            password = value.toCharArray();
                        }
                        else {
                            Log.e(TAG, "unknown key "+key+" found in "+fileName);
                        }
                    }
                    line = reader.readLine();
                }
                inputStream.close();
                if (userName != null && password != null) {
                    Log.i(TAG, "returning new user credentials");
                    return new User(userName, password);
                }
                Log.i(TAG, "no credentials found userName = "+userName+" password = "+password);
            }
            else {
                Log.i(TAG, "credentials file "+file.getAbsolutePath()+" does not exist");
            }
        }
        catch (Exception ex) {
            Log.e(TAG, "exception", ex);
        }
        return null;
    }

    private static void writeCredentials(File filesDir, String fileName, User user) {
        try {
            Log.i(TAG, "open file " + fileName);
            File file = new File(filesDir+File.separator+fileName);
            if (user == null) {
                if (file.delete()) {
                    Log.i(TAG, "deleted file " + fileName);
                }
                else {
                    Log.i(TAG, "failed to deleted file " + fileName);
                }
                return;
            }
            Log.i(TAG, "writing credentials to "+file.getAbsolutePath());
            Key secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            StringBuilder sb = new StringBuilder(10);
            sb.append("userName:"+ user.getUserName()+"\n");
            sb.append("password:"+user.getPassword()+"\n");
            byte[] unencryptedBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = cipher.doFinal(unencryptedBytes);
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(encryptedBytes);
            outputStream.close();
        }
        catch (Exception ex) {
            Log.e(TAG, "exception", ex);
        }
    }

}
