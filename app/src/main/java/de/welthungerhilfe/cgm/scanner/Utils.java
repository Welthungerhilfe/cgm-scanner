package de.welthungerhilfe.cgm.scanner;

import android.util.Base64;
import android.widget.Switch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;

public class Utils {

    public static String getLandmarkType(int i){
        switch (i){
            case 0:
                return "nose";
            case 1:
                return "left_eye_inner";
            case 2:
                return "left_eye";
            case 3:
                return "left_eye_outer";
            case 4:
                return "right_eye_inner";
            case 5:
                return "right_eye";
            case 6:
                return "right_eye_outer";
            case 7:
                return "left_ear";
            case 8:
                return "right_ear";
            case 9:
                return "left_mouth";
            case 10:
                return "right_mouth";
            case 11:
                return "left_shoulder";
            case 12:
                return "right_shoulder";
            case 13:
                return "left_elbow";
            case 14:
                return "right_elbow";
            case 15:
                return "left_wrist";
            case 16:
                return "right_wrist";
            case 17:
                return "left_pinky";
            case 18:
                return "right_pinky";
            case 19:
                return "left_index";
            case 20:
                return "right_index";
            case 21:
                return "left_thumb";
            case 22:
                return "right_thumb";
            case 23:
                return "left_hip";
            case 24:
                return "right_hip";
            case 25:
                return "left_knee";
            case 26:
                return "right_knee";
            case 27:
                return "left_ankle";
            case 28:
                return "right_ankle";
            case 29:
                return "left_heel";
            case 30:
                return "right_heel";
            case 31:
                return "left_foot_index";
            case 32:
                return "right_foot_index";
            default:
                return "not_detected";

        }
    }

    public static String printByteArrayUsingArraysToString(byte[] byteArray) {
        return Arrays.toString(byteArray).substring(0, 25);
    }

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // GCM tag length
    private static final int IV_LENGTH_BYTE = 12; // GCM IV length
    private static final int KEY_LENGTH_BIT = 256;

    public static void encryptFile(String filePath, String passphrase) {
        // Step 1: Generate AES key from passphrase
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(passphrase.getBytes("UTF-8"));
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            // Step 2: Read the file
            File file = new File(filePath);
            byte[] fileBytes = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(fileBytes);
            fis.close();

            // Step 3: Generate a random IV
            byte[] iv = new byte[IV_LENGTH_BYTE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

            // Step 4: Encrypt the file content
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encrypted = cipher.doFinal(fileBytes);

            // Step 5: Combine IV and encrypted data
            byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);

            // Step 6: Base64 encode for easier storage/transmission
            String encryptedBase64 = Base64.encodeToString(encryptedWithIv, Base64.DEFAULT);

            // Step 7: Write the encrypted data back to the file
            FileWriter writer = new FileWriter(file);
            writer.write(encryptedBase64);
            writer.close();
        }catch (Exception e){
            LogFileUtils.logError("ScanmodeActivity1","this is encryption error "+e.getMessage());
        }
    }
}
