/********************************************************************************
/////////////////////////////////////////////////////////////////////////////////
April 2017, Sean Parsons

Dialog for MappingWorker. See MappingWorker.java

/////////////////////////////////////////////////////////////////////////////////
********************************************************************************/

package Mapping;

//mine
import Misc.FileList;
import Misc.MyFileFilterEnds;
import Param.Param;
import Param.ParamType;
import ij.*;
import ij.gui.*;
import ij.io.OpenDialog;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;

//Java
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.*;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

public class MapperDialog extends JDialog implements  ActionListener {
    
    
    //parameters
    Param measre = new Param("MEASRE", ParamType.DROPDOWN, "measurement", "diameter%s,radius", null);
    Param outtyp = new Param("OUTTYP", ParamType.DROPDOWN, "pixel format", "byte%s,float", null);
    Param smooth = new Param("SMOOTH", ParamType.VALUES, "spine smooth [pixels]", "0", null);
    Param automt = new Param("AUTOMT", ParamType.RADIO, "automation", "save & close%0,log%1", null);

    Param detect = new Param("DETECT", ParamType.DROPDOWN, "detection edge", "top/bottom,top,bottom,left/right,left,right", null);
    Param framex = new Param("FRAMEX", ParamType.VALUES, "frame x,y,w,h", "200,0,w,h", this);
    Param timerg = new Param("TIMERG", ParamType.VALUES, "time range [s]", "0,e", null);

    Param thtype = new Param("THTYPE", ParamType.RADIO_EX, "threshold type", "fractional%1,absolute%0", null);
    Param thseed = new Param("THSEED", ParamType.VALUES, "detection", "0.5", null);
    Param thints = new Param("THINTS", ParamType.VALUES, "diameter", "0.2", null);
    Param minwid = new Param("MINWID", ParamType.VALUES, "min width [pixels]", "10", null);
    Param maxgap = new Param("MAXGAP", ParamType.VALUES, "max gap [pixels]", "4", null);

    Param lgtoff = new Param("LGTOFF", ParamType.DROPDOWN, "mode", "off%s,width,height,total", null);
    Param lgtbox = new Param("LGTBOX", ParamType.VALUES, "box x,y,w,h [pixels]", "0,0,w,h", this);
    
    Param colspn = new Param("COLSPN", ParamType.VALUES, "spine", "0", null);
    Param colbnd = new Param("COLBND", ParamType.VALUES, "sides", "255", null);
    Param colfrm = new Param("COLFRM", ParamType.VALUES, "frame", "0", null);
    Param collgt = new Param("COLLGT", ParamType.VALUES, "light box", "0", null);

    //actions
    JButton btSingle;
    JButton btRun;
    JButton btSave;
    JButton btLoad;
    
    //////////////////////////////////////////////////////////////////////////
    //CNSTR

    public MapperDialog(Frame parent){
        
        //dialog set up
        super(parent, "Mapper", Dialog.ModalityType.MODELESS);
        setSize(500,190);
        setResizable(true);
   
        //parameter panes
        int pw = 150;
        int ph = 20;
        JTabbedPane tbTabs = new JTabbedPane(JTabbedPane.LEFT); 
        tbTabs.addTab("MAIN", Param.GetCentredPanel(pw, ph, measre, outtyp, smooth, automt));
        tbTabs.addTab("BOUNDS", Param.GetCentredPanel(pw, ph, framex, detect, timerg));
        tbTabs.addTab("THRESHOLDS", Param.GetCentredPanel(pw, ph, thtype, thseed, thints, minwid, maxgap));
        tbTabs.addTab("LIGHT BOX", Param.GetCentredPanel(pw, ph, lgtoff, lgtbox));
        tbTabs.addTab("COLORS", Param.GetCentredPanel(pw, ph, colspn, colbnd, colfrm, collgt));
        
        //actions
        btSingle = new JButton("SINGLE");
        btSingle.addActionListener(this);
        btRun = new JButton("RUN");
        btRun.addActionListener(this);
        btSave = new JButton("SAVE");
        btSave.addActionListener(this);
        btLoad = new JButton("LOAD");
        btLoad.addActionListener(this);
        JPanel pnActions = new JPanel(new GridLayout(1, 4));
        pnActions.add(btSingle);
        pnActions.add(btRun);
        pnActions.add(btSave);
        pnActions.add(btLoad);
        
        //add to dialog
        add(tbTabs, BorderLayout.CENTER);
        add(pnActions, BorderLayout.SOUTH);
           
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //ACTION LISTENER

    public void actionPerformed(ActionEvent ae){
        
        //open single frame
        if(ae.getSource() == btSingle){
            OpenSingleFrame();
        }
        //map
        else if(ae.getSource() == btRun){
            File[] dir = Openlist(true, false, "");
            MappingWorker mapwork = new MappingWorker(this, dir);
            mapwork.execute();
        }
        //load parameters
        else if(ae.getSource() == btLoad)
            Param.LoadParameters(this);
        //save parameters
        else if(ae.getSource() == btSave)
            Param.SaveParameters(this);
        //detect box
        else if(ae.getSource() == framex.but)
            SetFromRoi(framex);
        //light box
        else if(ae.getSource() == lgtbox.but)
           SetFromRoi(lgtbox);
                   
    }
    
    private void SetFromRoi(Param para){
        
        //current roi
        ImagePlus img = WindowManager.getCurrentImage();
        if(img == null) return;
        Roi roi = img.getRoi();
        if(roi == null) return;
        //set values
        Rectangle rec = roi.getBounds();
        String str = para.GetID() + "\t" + rec.x + "," + rec.y + "," + rec.width + "," + rec.height;
        para.Read(str);
        
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //WORKER FINISHED
    
    public void WhenWorkerDone(){
       
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //DIALOG FREEZE/UNFREEZE
    
    public void SetState(boolean ison){
        Param[] allwidgets = Param.GetSortedList(this);
        for(Param x: allwidgets) x.EnableComponents(ison);
        if(ison) btRun.setText("RUN"); else btRun.setText("RUNNING...");
        btSingle.setEnabled(ison);
        btRun.setEnabled(ison);
        btSave.setEnabled(ison);
        btLoad.setEnabled(ison);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //FILE OPENING
    
    private File[] Openlist(boolean allowmulti, boolean fileonly, String ... ends){
        
        //file picker
        JFileChooser fdialog = new JFileChooser();
        fdialog.setMultiSelectionEnabled(allowmulti);
        if(fileonly) fdialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fdialog.setCurrentDirectory(new File(IJ.getDirectory("current")));

        //filter
        int c = 0;
        MyFileFilterEnds filt;
        for(String x : ends){
            if(x.isEmpty()) continue;
            filt = new MyFileFilterEnds(x);
            fdialog.addChoosableFileFilter(filt); 
            c++;
        }

        if(c > 1){
            javax.swing.filechooser.FileFilter[] filts = fdialog.getChoosableFileFilters();
            fdialog.setFileFilter(filts[1]);
        }

        //show dialog
        int returnVal = fdialog.showOpenDialog(IJ.getInstance());
        if(returnVal != JFileChooser.APPROVE_OPTION) return null;

        //get selected file(s) or files from single selected directory
        File[] dir = new File[1];
        if(allowmulti) dir = fdialog.getSelectedFiles();
        else dir[0] = fdialog.getSelectedFile();       
        if(dir == null){
            File f = fdialog.getSelectedFile();
            if(f.isDirectory()) dir = f.listFiles();
            else dir[0] = f;
        }

        //reset ImageJ current directory
        OpenDialog.setDefaultDirectory(dir[0].getParent());
  
        return dir;
        
    }
 
    //open single frames
    private void OpenSingleFrame(){
        
        File[] dir = FileList.Openlist(false, true, "");
       
        Java2DFrameConverter frameconvertor = new Java2DFrameConverter();
        FFmpegFrameGrabber grabber;
        BufferedImage bfimg;
        ImagePlus img;
        for(File x : dir){
            
            //ignore directory
            if(x.isDirectory()) continue;
            
            //open
            grabber = new FFmpegFrameGrabber(x.getAbsolutePath());
            try{
                grabber.start();
                org.bytedeco.javacv.Frame frm = grabber.grabImage();

                if((frm != null) && (frm.image != null) && (frm.imageWidth > 10)){
                    bfimg = frameconvertor.getBufferedImage(frm);
                    if(bfimg.getType() == BufferedImage.TYPE_BYTE_GRAY)
                        img = new ImagePlus(x.getName(), new ByteProcessor(bfimg));
                    else
                        img = new ImagePlus(x.getName(), new ColorProcessor(bfimg));
                    img.show();
                }
     
                grabber.stop();
                grabber.release();
            }catch(FrameGrabber.Exception fe){}
            
            
            
            
        }
        
    }
    
    
    
    
}
