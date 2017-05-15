/*****************************************************************************
//////////////////////////////////////////////////////////////////////////////
 Author:    Sean Parsons
 Date:      April 2014

File filter ("wildcard filter") for files with a specified ending (usually an extention).

///////////////////////////////////////////////////////////////////////////////
******************************************************************************/

package Misc;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 *
 * @author ICCROCK1
 */
public class MyFileFilterEnds extends FileFilter{

    String ending;

    public MyFileFilterEnds(String e){
        ending = e.toLowerCase();
    }

    @Override
    public String getDescription(){
        return "Files ending with " + ending;
    }

    @Override
    public boolean accept(File f){

       if(f.isDirectory()) return true;

       if(f.getName().toLowerCase().endsWith(ending)) return true;

       return false;

    }

}
