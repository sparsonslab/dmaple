/********************************************************************************
/////////////////////////////////////////////////////////////////////////////////
May 2017, Sean Parsons

activates Stitcher dialog.

/////////////////////////////////////////////////////////////////////////////////
********************************************************************************/

package DMapLE_;

import Stitching.StitcherDialog;
import ij.IJ;
import ij.plugin.PlugIn;

/**
 *
 * @author sparsonslab
 */
public class Stitcher_ implements PlugIn {
    
    @Override
    public void run(String arg) {

        //run dialog
        StitcherDialog dialog = new StitcherDialog(IJ.getInstance());
        dialog.show();

    }
    
    
}
