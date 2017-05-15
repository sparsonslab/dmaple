/*****************************************************************************
//////////////////////////////////////////////////////////////////////////////
 Author:    Sean Parsons
 Date:      April 2017 

In map creation by MappingWorker, holds the position coordinates of a single
length of gut.

///////////////////////////////////////////////////////////////////////////////
******************************************************************************/

package Mapping;

import ij.process.ImageProcessor;

public class GutSpine {

    //seeding and range
    public Side seedside;          //side
    public int seedposition;        
    public int p0, p1;
    
    //data
    public int[] spine;
    public int[] smooth;
    public int[] lbound;
    public int[] ubound;
    
    //thresholds
    public int threshold;
    public int maxgap;

    //seeding
    public GutSpine(Side s, int p0arg, int p1arg, int seedpositionarg, int targ, int mg){
        
        //seeding
        seedside = s;
        seedposition = seedpositionarg;
        
        //pixel range
        p0 = p0arg;
        p1 = p1arg;
        int length = 1 + p1 - p0;
        
        //data
        spine = new int[length];
        smooth = new int[length];
        lbound = new int[length];
        ubound = new int[length];
        
        //thersholds
        threshold = targ;
        maxgap = mg;
     
  
    }

    //draw spine and boundaries
    void DrawSpine(ImageProcessor frame, int draws, int drawb){

        if((seedside == Side.TOP) || (seedside == Side.BOTTOM)){
            for(int i = 0; i < spine.length; i++) if(spine[i] > 0) {
                frame.set(spine[i], i + p0, draws);
                frame.set(lbound[i], i + p0, drawb);
                frame.set(ubound[i], i + p0, drawb);
            }
        }
        else{
            for(int i = 0; i < spine.length; i++) if(spine[i] > 0) {
                frame.set(i + p0, spine[i], draws);
                frame.set(i + p0, lbound[i], drawb);
                frame.set(i + p0, ubound[i], drawb);
            }
        }

    }

}
