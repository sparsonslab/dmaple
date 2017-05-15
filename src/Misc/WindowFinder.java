/*****************************************************************************
//////////////////////////////////////////////////////////////////////////////
 Author:    Sean Parsons
 Date:      April-May 2014

 Iteratively searchs up the component heirarchy to find the containing window.

 Used by MeasureDialog.java

///////////////////////////////////////////////////////////////////////////////
******************************************************************************/

package Misc;

import java.awt.*;


public class WindowFinder {

    public static Window findWindow(Component c) {
        if (c == null) return null;
        else if (c instanceof Window) return (Window) c;
        else return findWindow(c.getParent());
    }

}
