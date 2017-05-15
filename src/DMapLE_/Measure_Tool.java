/*****************************************************************************
//////////////////////////////////////////////////////////////////////////////
 Author:    Sean Parsons
 Date:      April-May 2014

Activates the dialog, MeasureDialog.java.
* 
* 14 Oct 2016 - added Call Function to activate crop

///////////////////////////////////////////////////////////////////////////////
******************************************************************************/
package DMapLE_;

import ExtraPlug.MeasureDialog;
import ij.IJ;
import ij.WindowManager;
import ij.plugin.*;

public class Measure_Tool implements PlugIn{
    
  
    @Override
    public void run(String arg) {

        //run dialog
        MeasureDialog dialog = new MeasureDialog(IJ.getInstance());
        dialog.show();

    }
    
    //macro call function
    public static String CallFunction(String arg){
   
        MeasureDialog.Crop2(WindowManager.getCurrentImage());
        return "";
        
    }

}
