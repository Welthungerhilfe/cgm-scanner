package de.welthungerhilfe.cgm.scanner;

import android.widget.Switch;

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
}
