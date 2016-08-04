package parking.security;

import java.io.UnsupportedEncodingException; 
import java.security.InvalidKeyException; 
import java.security.NoSuchAlgorithmException; 
import java.security.SecureRandom; 
import java.security.spec.InvalidKeySpecException; 
 
import javax.crypto.SecretKeyFactory; 
import javax.crypto.interfaces.PBEKey; 
import javax.crypto.spec.PBEKeySpec; 

public class Password {

    public static final int SALT_LEN = 8; 
    private static final int ROUNDS = 1024; 
    private static final int KEY_BITS = 128;
    private static final String publicPassword = "password"; 

    public static byte[] createKey(String password, byte[] saltBytes) {
         try {
            PBEKeySpec pwKey = new PBEKeySpec(publicPassword.toCharArray(), saltBytes, ROUNDS, KEY_BITS);  
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");         
            PBEKey pbeKey = (PBEKey) factory.generateSecret(pwKey);
            byte[] pbkdfKey = pbeKey.getEncoded();
            return pbkdfKey;
            
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }                     
        return null;
    }

    public static byte[] getRandomSalt() { 
        SecureRandom random = new SecureRandom(); 
        byte[] saltBytes = new byte[SALT_LEN]; 
        random.nextBytes(saltBytes); 
        return saltBytes;   
    }

    public static String makeRandomPassword(int len) {
        SecureRandom random = new SecureRandom();
        String alphabet = "0123456789abcdefghijhlmnopqrstuvwxyzxyz";
        StringBuilder sb = new StringBuilder(8);
        char[] password = new char[len];
        for ( int i=0 ; i<len ; i++) {
            password[i] = alphabet.charAt(random.nextInt(alphabet.length()));           
        }
        return new String(password);    
    }
        
}