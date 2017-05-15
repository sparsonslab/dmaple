/*****************************************************************************
//////////////////////////////////////////////////////////////////////////////
 Author:    Sean Parsons
 Date:      April 2017

Comparator for sorting parameters.

///////////////////////////////////////////////////////////////////////////////
******************************************************************************/
package Param;

import java.util.Comparator;


public class ParamComparator implements Comparator<Param>{
    
    @Override
    public int compare(Param p1, Param p2){
  
        if(p1.GetID().compareTo(p2.GetID()) > 0) return 1;
 
        return -1;
    }
    
}
