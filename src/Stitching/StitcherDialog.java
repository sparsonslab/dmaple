/********************************************************************************
/////////////////////////////////////////////////////////////////////////////////
May 2017, Sean Parsons

Dialog for Stitching maps

/////////////////////////////////////////////////////////////////////////////////
********************************************************************************/
package Stitching;

//mine
import Misc.FileList;
import Param.Param;
import Param.ParamType;
import ij.*;
import ij.gui.*;
import ij.io.OpenDialog;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

//Java
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.*;


public class StitcherDialog  extends JDialog implements  ActionListener {
    
    //parameters
    Param vrflip = new Param("VRFLIP", ParamType.RADIO, " ", "flip%1,mark%1", null);
    Param gapwid = new Param("GAPWID", ParamType.VALUES, "gap width", "10", null);
    Param slcsel = new Param("SLCSEL", ParamType.DROPDOWN, "slice selection", "index%s,filter", null);
    Param slcflt = new Param("SLCFLT", ParamType.VALUES, "slice filter", "0", null);
    
    //buttons
    JButton btRun;
    
    //////////////////////////////////////////////////////////////////////////
    //CNSTR

    public StitcherDialog(Frame parent){
        
        //dialog set up
        super(parent, "Stitcher", Dialog.ModalityType.MODELESS);
        setSize(300,160);
        setResizable(true);
        
        //parameter panes
        int pw = 150;
        int ph = 20;
        JPanel pnPara = Param.GetCentredPanel(pw, ph, vrflip, gapwid, slcsel, slcflt);
        
        //actions
        btRun = new JButton ("STITCH");
        btRun.addActionListener(this);
        
        //add panels
        add(pnPara, BorderLayout.CENTER);
        add(btRun, BorderLayout.SOUTH);
        
        
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //ACTION LISTENER

    public void actionPerformed(ActionEvent ae){
     
        if(ae.getSource() == btRun) Stitch();
   
        
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //STITCHING
  
    private void Stitch(){
        
        //counters
        int i, j, x, y;

        //get selected files
        File[] dir = FileList.Openlist(true, false, ".tiff", "fc.tiff");

        //sort files according to name
        Arrays.sort(dir);
        
        //output prefix (up to first underscore of first file)
        String opre = dir[0].getName();
        if(opre.indexOf("_") > 1)
             opre = opre.substring(0, opre.indexOf("_"));
        else opre = opre.substring(0, opre.lastIndexOf("."));
        
        //slice filtering
        String[] slcmat = slcflt.GetString(0).split("%");
        int[] slcrng = new int[2];
        if((slcsel.GetSelected() == 1) && (slcmat.length > 2)){
            for(i = 0; (i < slcmat.length - 1) && (i < 2); i++)
                try{
                    slcrng[i] = Integer.parseInt(slcmat[i + 1]);
                }catch(Exception ex){}
        }
        int slcidx = (int)slcflt.GetValue(0);
        if(slcidx < 0) slcidx = 0;
        
        //array of slices with matching label
        ArrayList<ImageProcessor> prlist = new ArrayList<>();
        ImagePlus img;
        ImageStack stck;
        for(File f : dir)
            if((f.isDirectory() == false) &&
               (f.getName().toLowerCase().endsWith(".tiff") || f.getName().toLowerCase().endsWith(".tiff"))){
                
                img = new ImagePlus(f.getPath());
                stck = img.getStack();
                
                //index matching
                if(slcsel.GetSelected() == 0)
                   prlist.add(stck.getProcessor(slcidx + 1));
                //filter matching
                else
                    for(i = 1; i <= stck.getSize(); i++)
                         if(DoesMatch(stck.getSliceLabel(i), slcmat[0], slcrng))
                                prlist.add(stck.getProcessor(i));

            }
        if(prlist.isEmpty()){
            IJ.showMessage("no matching slices");
            return;
        }

        //vertical flip
        if(vrflip.GetValue(0) == 1)
            for(ImageProcessor ip : prlist)
                ip.flipVertical();
   
        //calculate y positions
        int mgap = (int)gapwid.GetValue(0);      //gap
        ImageProcessor stack_ip, leading_ip;     //stack image processor
        int[] ypos = new int[prlist.size()];     //y positions
        float[] score;
        int limg = -1;      //leading segment
        int le = 0;         //leading edge (y poistion)
        int w = 0;          //montage width
        int cp; 
        for(j = 0; j < prlist.size(); j++){

            //segment processor
            stack_ip = prlist.get(j);

            //calculate y position
            //...stitch-mode and not first segment
            if((mgap < 0) && (limg != -1)){

                //leading segment
                leading_ip = prlist.get(limg);

                //find contact line current segment and leading segment
                score = new float[stack_ip.getHeight()/2];
                cp = 0;     //contact point
                for(y = 0; y < score.length; y++){
                    for(x = 0; (x < stack_ip.getWidth()) && (x < leading_ip.getWidth()); x++)
                        score[y] += Math.abs(stack_ip.getf(x, y) - leading_ip.getf(x, leading_ip.getHeight() - 1));
                    if(score[y] < score[cp]) cp = y;
                }
                ypos[j] = le - cp;

            //...otherwise
            } else ypos[j] = le;

            //update montage width
            if(stack_ip.getWidth() > w) w = stack_ip.getWidth();

            //update leading segment and edge
            limg = j;
            le = ypos[j] + stack_ip.getHeight();
            if(mgap > 0) le += mgap;

        }

        //create stitched image
        ByteProcessor montage_ip = new ByteProcessor(w, le);
        Overlay ovly = new Overlay(); //marker overlay
        Roi mbox; 
        int domark = (int)vrflip.GetValue(1);
        for(j = 0; j < prlist.size(); j++){

            stack_ip = prlist.get(j);
            for(x = 0; x < stack_ip.getWidth(); x++)
                for(y = 0; y < stack_ip.getHeight(); y++)
                    montage_ip.set(x, ypos[j] + y, stack_ip.get(x, y));
            
            if(domark > 0){
                mbox = new Roi(0, ypos[j], stack_ip.getWidth(), stack_ip.getHeight());
                mbox.setStrokeColor(Color.RED);
                ovly.add(mbox);
            }
    
        }

        //show image
        ImagePlus montage_img = new ImagePlus(opre + "_" + slcflt.GetString(0), montage_ip);
        if(domark > 0) montage_img.setOverlay(ovly);
        montage_img.show();

        
    }
 
    private boolean DoesMatch(String label, String pref, int[] rng){
        
        //prefix
        int u = label.indexOf(pref);
        if(u < 0) return false;
        
        //range
        try{
            double pos = Double.parseDouble(label.substring(u + pref.length()));
            if((pos >= rng[0]) && (pos <= rng[1])) return true;
        }catch(Exception ex){}

        return false;
    }
 
}
