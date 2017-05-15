/********************************************************************************
/////////////////////////////////////////////////////////////////////////////////
April 2017, Sean Parsons

activates Mapper dialog.

/////////////////////////////////////////////////////////////////////////////////
********************************************************************************/

package DMapLE_;

import ij.IJ;
import ij.plugin.*;
import Mapping.MapperDialog;

public class Mapper_ implements PlugIn {

    @Override
    public void run(String arg) {

        //run dialog
        MapperDialog dialog = new MapperDialog(IJ.getInstance());
        dialog.show();

    }
    
    
    //call function
    //macro call function
    public static String CallFunction(String arg){
        
        //intiaite mapping worker
        
        //load string argument
        
        //run
        
        return "";
    }
    
    
}
