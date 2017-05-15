/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Misc;

/**
 *
 * @author ICCROCK1
 */
public class DblString {


    public static String LooseTrailingZeros(String x){

        //find last non-zero
        int p = x.lastIndexOf(".");
        int i = x.length() - 1;
        for(; i >= p; i--)
            if(x.charAt(i) != '0') break;

        //if last non-zero is decimal point
        if(i == p) i--;
        
        return x.substring(0, i + 1);
        
    }
    
    /**
     * Remove unwanted decimal points from a {@code String} number.
     * @param x number
     * @param nzeros number of zeros after which the number if truncated.
     * @return the truncated number.
     */
    public static String LooseZeros(String x, int nzeros){

        if(nzeros < 0) return x;

        //decimal point
        int p = x.indexOf(".");
        if(p < 0) return x;

        //zeros directly after decimal
        p++;
        while((p < x.length()) && (x.charAt(p) == '0')) p++;
        
        //if all zeros after decimal point...
        if(p == x.length()) return x.substring(0, x.indexOf(".") + nzeros + 1);

        //...otherwise zeros after this
        int n = 0;
        while((p < x.length()) && (n < nzeros)){
            if(x.charAt(p) == '0') n++; else n = 0;
            p++;
        }

        return x.substring(0, p - n);

    }

    public static String LooseZeros(double x, int ndec, int nzeros){

        return LooseZeros(DblToStr(x, ndec), nzeros);
    }

    /**
     * Get a {@code String} representation of a number.
     * @param x number.
     * @param ndec number of digits after the decimal point.
     * @return the number.
     */
    public static String DblToStr(double x, int ndec){

        x *= Math.pow(10.0, ndec);
        x = Math.rint(x);
        x /= Math.pow(10.0, ndec);
        String x_str = Double.toString(x);
        int q = x_str.indexOf(".") + ndec + 1;
        if(q < x_str.length())
            x_str = x_str.substring(0, x_str.indexOf(".") + ndec + 1);
        else for(int i = 0; i < q - x_str.length(); i++) x_str += "0";
        return x_str;

    }

    /**
     * Get the number of significant decimal places in a number.
     * @param x the number.
     * @return the number of significant decimal places.
     */
    public static int GetNDecimalPlaces(double x){

        int ndec = 1;
        while(x * Math.pow(10.0, ndec) < 1) ndec++;

        return ndec;

    }

}
