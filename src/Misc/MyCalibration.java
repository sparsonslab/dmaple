/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Misc;

import ij.measure.*;
import ij.ImagePlus;
/**
 *
 * @author ICCROCK1
 */
public class MyCalibration {

    public static Calibration GetCalibration(ImagePlus img){

        Calibration cal = img.getCalibration();

        if((cal != null) && (cal.info != null) && (cal.info.length() > 0)){
            String str = cal.info;
            int i = str.indexOf("YUNIT");
            int j = str.lastIndexOf("YUNIT");
            if((i >= 0) && (j >= 0) && (i < j - 5))
                cal.setYUnit(str.substring(i + 5, j));
        }

        return cal;

    }

    public static void SetCalibration(ImagePlus img, Calibration cal){
        cal.info = "YUNIT" + cal.getYUnit() + "YUNIT";
        img.setCalibration(cal);
    }

}
