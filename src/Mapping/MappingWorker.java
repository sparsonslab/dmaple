/********************************************************************************
/////////////////////////////////////////////////////////////////////////////////
April 2017, Sean Parsons

Swing worker for diameter mapping.
- Takes input parameters from MapperDialog.java
- Opens file dialog and expands list of files
- for each file in list creates diameter maps

Options:
- diameter or radius mapping (radius mapping with smooth spine)
- light detection
- bounding box for intestine detection and measurement
- time range
- top-bottom and left-right orientation
- relative and absolute thresholds
- logging
- auto save and close

/////////////////////////////////////////////////////////////////////////////////
********************************************************************************/
package Mapping;

import Misc.*;

//mine
import Param.Param;

//ImageJ
import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.frame.Editor;
import ij.process.*;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;

//Java standard
import java.awt.image.BufferedImage;
import javax.swing.SwingWorker;
import java.io.*;
import java.util.*;
import javax.swing.JFileChooser;
import java.awt.*;
import javax.swing.JOptionPane;

//Java CV
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

public class MappingWorker extends SwingWorker<Boolean, Void> {

    //PARENT DIALOG
    MapperDialog parent;
    File[] dir;

    //PROCESSORS AND CONVERTORS, see ::MakeMap(File fpath)
    FFmpegFrameGrabber grabber;
    Frame frm = null;
    Java2DFrameConverter frameconvertor = new Java2DFrameConverter();
    BufferedImage bfimg = null;
    ColorProcessor cproc = null;
    ByteProcessor bproc = null;
    
    /////////////////////////////////////////////////////////////////////////////
    //CNSTR
    
    public MappingWorker(MapperDialog parentarg, File[] dirarg){
        parent = parentarg;
        dir = dirarg;
    }
    
    /////////////////////////////////////////////////////////////////////////////
    //SWING WORKER
    
    @Override
    public Boolean doInBackground(){
        
        //get expanded file list
        ArrayList<File> flist = FileList.GetExpandedFileList(dir);
        if(flist == null) return true;
   
        //switch off dialog for input
        parent.SetState(false);

        //start log
        Editor ProgressFrame = new Editor();
        ProgressFrame.setFont(new Font("Monospaced", Font.PLAIN, 12));
        boolean doshowprogress = false;
        Date dt = new Date();
        if(parent.automt.GetValue(1) == 1){
            doshowprogress = true;
            ProgressFrame.setSize(600, 200);
            ProgressFrame.display("Log", "");
            //write out parameters
            ProgressFrame.append(dt.toString() + "\r\n\r\nPARAMETERS:\r\n");
            Param[] allwidgets = Param.GetSortedList(parent);
            for(Param x: allwidgets)
                ProgressFrame.append(x.Write());
            ProgressFrame.append("\r\nMAPS:\r\n");
        }
      
        //for each file: try to make map and show progress
        String mssg = "";
        for(File f : flist){
            //make map
            mssg = MakeMap(f);
            //log
            if((mssg.isEmpty() == false) && doshowprogress){
                dt = new Date();
                ProgressFrame.append(dt.toString() + "\t" + mssg + "...\t" + f.getAbsolutePath() + "\r\n");
            }
        }
        
        //autosave progress
        if((parent.automt.GetValue(0) == 1) && (parent.automt.GetValue(1) == 1)){
            try{
                FileWriter fout = new FileWriter(flist.get(0).getParent() + File.separator + "progress.txt");
                fout.write(ProgressFrame.getText());
                fout.close();
            }
            catch (Exception e){}
        }
        
        //switch on dialog for input
        parent.SetState(true);
    
        return true;
        
    }
 
    @Override
    public void done(){
        //call parent function to unfreeze dialog, etc
        parent.WhenWorkerDone();
    }
    
    /////////////////////////////////////////////////////////////////////////////
    //MAPPING
    
    //make map return message
    private String MakeMap(File fpath){
        
        int i, j, k;
        
        //initiate grabber
        grabber = new FFmpegFrameGrabber(fpath.getAbsolutePath());
        try{
            grabber.start();
        }catch(FrameGrabber.Exception fe){
            StopAndRelease();
            return "";
        }
        
        //frame range
        int n_frm = grabber.getLengthInFrames();
        double[] trng = parent.timerg.GetValues();
        if((n_frm < 10) || (trng.length < 2)){
            StopAndRelease();
            return "";
        }
        int[] frange = new int[2];
        frange[0] = (int)Math.floor(trng[0] * grabber.getFrameRate());
        if((frange[0] < 0) || (frange[0] >= n_frm))  frange[0] = 0;
        frange[1] = (int)Math.ceil(trng[1] * grabber.getFrameRate());
        if((frange[1] <= frange[0]) || (frange[1] >= n_frm)) frange[1] = n_frm - 1;
        int nframes = frange[1] - frange[0] - 1;
    
        //first frame
        try{
            if(frange[0] != 0) grabber.setFrameNumber(frange[0]);
        }catch(Exception ex){
            StopAndRelease();
            return "";
        }
        GrabProcessor();
        if(bproc == null){
            StopAndRelease();
            return "";
        }
        String imtitle = fpath.getName();
        k = imtitle.lastIndexOf(".");
        if(k > 0) imtitle = imtitle.substring(0, k);
        ImagePlus img = new ImagePlus(imtitle + "_frame", bproc);
        img.show();
  
        //frame bounds
        Rectangle bounds = GetCheckedBounds(bproc, parent.framex.GetValues());
        img.setRoi(bounds);

        //light detection bounds
        Rectangle lightbox = GetCheckedBounds(bproc, parent.lgtbox.GetValues());
        int ldetection = parent.lgtoff.GetSelected();
        if(ldetection > 0) img.setRoi(lightbox);
      
        //seed side
        k = parent.detect.GetSelected();
        Side seedside = Side.TOP;
        if(k == 0)
            if(SumSide(bproc, bounds, Side.TOP) > SumSide(bproc, bounds, Side.BOTTOM))
                seedside = Side.TOP; else seedside = Side.BOTTOM;
        else if(k == 1) seedside = Side.TOP;   
        else if(k == 2) seedside = Side.BOTTOM; 
        else if(k == 3)
            if(SumSide(bproc, bounds, Side.LEFT) > SumSide(bproc, bounds, Side.RIGHT))
                seedside = Side.LEFT; else seedside = Side.RIGHT;
        else if(k == 4) seedside = Side.LEFT;   
        else if(k == 5) seedside = Side.RIGHT; 

        //thresholds
        float thresh_s = (float)parent.thseed.GetValue(0);
        float thresh_i = (float)parent.thints.GetValue(0);
        //...fractional
        if(parent.thtype.GetSelected() == 0){
            thresh_s = GetAbsoluteThreshold(bproc, bounds, seedside, thresh_s);
            thresh_i = GetAbsoluteThreshold(bproc, bounds, seedside, thresh_i);
        }
        
        //spine smoothing (for radius measurement)
        int smw = 0;
        if(parent.measre.GetSelected() == 1) smw = (int)Math.ceil(parent.smooth.GetValue(0));
     
        //seed spines
        int minwidth = (int)parent.minwid.GetValue(0);
        int mgap = (int)parent.maxgap.GetValue(0);
        ArrayList<GutSpine> spines = new ArrayList<GutSpine>();
        SeedSpines(bproc, bounds, seedside, (int)thresh_s, (int)thresh_i, mgap, minwidth, spines);
        if(spines.isEmpty()){
            StopAndRelease();
            return "no intestines found";
        }
        
        //draw spines
        int col_s = (int)parent.colspn.GetValue(0);
        int col_b = (int)parent.colbnd.GetValue(0);
        DrawSpines(img.getProcessor(), spines, col_s, col_b);
        img.updateAndDraw();
        
        //data array
        //[map][map x][map y]
        //[map] order = D1,D2,....[L]  or R1, L1, R2, L2,.....[L]
        int measuremode = parent.measre.GetSelected();
        int nmaps = ((1 + measuremode) * spines.size()) + ((ldetection > 0) ? 1 : 0);
        int spinelength = spines.get(0).spine.length;
        int[][][] data = new int[nmaps][nframes][spinelength];
        
        //loop through frames
        int count_total = 0;        //total number of grabbed frames
        int count = 0;              //number of grabbed frames that appear to be video frames
        double lightmean;
        while(count_total < nframes){

            //grab processor
            GrabProcessor();
            if(bproc == null) {
                count_total++;
                continue;
            }
            IJ.showProgress(count, nframes);
            
            //calculate and draw spines
            UpdateSpines(bproc, (int)thresh_i, mgap, minwidth, smw, spines);
            DrawSpines(bproc, spines, col_s, col_b);
            
            //diameter
            if(measuremode == 0)
                for(k = 0; k < spines.size(); k++)
                    for(j = 0; j < spinelength; j++)
                        data[k][count][j] = spines.get(k).ubound[j] - spines.get(k).lbound[j];   
            //radius
            else
               for(k = 0; k < spines.size(); k++)
                    for(j = 0; j < spinelength; j++){
                        data[k * 2][count][j] = spines.get(k).ubound[j] - spines.get(k).spine[j];
                        data[(k * 2) + 1][count][j] = spines.get(k).spine[j] - spines.get(k).lbound[j];
                    }
   
            //light-detection
            if(ldetection == 1){
                for(j = 0; (j < lightbox.height) && (j < spinelength); j++){
                    lightmean = 0;
                    for(i = 0; i < lightbox.width; i++) lightmean += bproc.get(i, j);
                    data[nmaps - 1][count][j] = (int)(lightmean / (double)lightbox.width);
                }        
            }
            else if(ldetection == 2){
                for(i = 0; (i < lightbox.width)  && (i < spinelength); i++){
                    lightmean = 0;
                    for(j = 0; j < lightbox.height; j++) lightmean += bproc.get(i, j);
                    data[nmaps - 1][count][i] = (int)(lightmean / (double)lightbox.height);
                }  
            }
            else if(ldetection == 3){
                lightmean = 0;
                for(j = 0; j < lightbox.height; j++)
                    for(i = 0; i < lightbox.width; i++)
                        lightmean += bproc.get(i, j);
                lightmean /= (double)(lightbox.width * lightbox.height);
                for(j = 0; j < spinelength; j++)
                    data[nmaps - 1][count][j] = (int)lightmean; 
            }
   
            //image
            img.setProcessor(bproc);
            img.updateAndDraw();
            
            //count frame
            count++;
            count_total++;
            
        }

        //finish frame grabbing
        IJ.showProgress(1.5);
        img.deleteRoi();
        StopAndRelease();
        
        //maps
        int outtype = parent.outtyp.GetSelected();
        ImageStack stck = new ImageStack(nframes, spinelength);
        String seedp;
        String[] prefs = {"R", "L"};
        if((measuremode == 0) && ((seedside == Side.LEFT) || (seedside == Side.RIGHT))){
            prefs[0] = "A"; prefs[1] = "B";
        }
        //...diameter/radius
        for(k = 0; k < spines.size(); k++){
            seedp = Integer.toString(spines.get(k).seedposition);
            if(measuremode == 0) 
                stck.addSlice("D" + seedp, GetProcessor(data[k], outtype));
            else {
                stck.addSlice(prefs[0] + seedp, GetProcessor(data[k * 2], outtype));
                stck.addSlice(prefs[1] + seedp, GetProcessor(data[(k * 2) + 1], outtype));
            }
        }

        //...light
        if(ldetection > 0)
            stck.addSlice("light", GetProcessor(data[nmaps - 1], outtype));
        //...image
        ImagePlus maps = new ImagePlus(imtitle + "_maps", stck);
        maps.show();

        //lightbox and detection frame annotation
        img.getProcessor().setColor(parent.colfrm.GetValue(0));
        img.getProcessor().draw(new Roi(bounds));
        if(ldetection > 0){
            img.getProcessor().setColor(parent.collgt.GetValue(0));
            img.getProcessor().draw(new Roi(lightbox));
        }
        img.updateAndDraw();

        //autosave and close
        if(parent.automt.GetValue(0) == 1){
            String path = fpath.getParent() + File.separator;
            IJ.saveAsTiff(img, path + img.getTitle() + ".tiff");
            img.close();
            IJ.saveAsTiff(maps, path + maps.getTitle() + ".tiff");
            maps.close();
        }
     
        //finish      
        return "finished mapping";
   
    }
    
    /////////////////////////////////////////////////////////////////////////////
    //THRESHOLDING AND SPINES
    
    private double SumSide(ImageProcessor imp, Rectangle b, Side s){
        
        double sum = 0;
        int xe = b.x + b.width - 1;
        int ye = b.y + b.height - 1;

        if(s == Side.TOP)
            for(int i = b.x; i < xe; i++) sum += imp.getf(i, b.y);
        else if(s == Side.BOTTOM)
            for(int i = b.x; i < xe; i++) sum += imp.getf(i, ye);
        else if(s == Side.LEFT)
            for(int i = b.y; i < ye; i++) sum += imp.getf(b.x, i);
        else if(s == Side.RIGHT)
            for(int i = b.y; i < ye; i++) sum += imp.getf(xe , i);
        
        return sum;
        
    }
    
    private float GetAbsoluteThreshold(ImageProcessor imp, Rectangle b, Side s, float relative){
        
        if(relative < 0) relative = 0;
        if(relative > 1) relative = 1;
        
        float abs = 0;
        float min = 255;
        float max = 0;
        int xe = b.x + b.width - 1;
        int ye = b.y + b.height - 1;
        
        float v;
        if(s == Side.TOP)
            for(int i = b.x; i < xe; i++){
                v = imp.getf(i, b.y);
                if(v > max) max = v;
                if(v < min) min = v;
            }
        else if(s == Side.BOTTOM)
            for(int i = b.x; i < xe; i++){
                v = imp.getf(i, ye);
                if(v > max) max = v;
                if(v < min) min = v;
            }
        else if(s == Side.LEFT)
            for(int i = b.y; i < ye; i++){
                v = imp.getf(b.x, i);
                if(v > max) max = v;
                if(v < min) min = v;
            }
        else if(s == Side.RIGHT)
            for(int i = b.y; i < ye; i++){
                v = imp.getf(xe , i);
                if(v > max) max = v;
                if(v < min) min = v;
            }
    
        abs = min + ((max - min) * relative);
        return abs;
        
    }

    //seed spines
    private void SeedSpines(ImageProcessor imp, Rectangle b, Side s, int t_seed, int t_wid, int minw, int maxgap, ArrayList<GutSpine> list){
       
        int xe = b.x + b.width - 1;
        int ye = b.y + b.height - 1;
        int i, j, k, m, ub, lb, edge, dir;
        GutSpine l;
        
        if((s == Side.TOP) || (s == Side.BOTTOM)){
            
            //initiation edge and extension direction
            edge = b.y;
            dir = 1;
            if(s == Side.BOTTOM){
                edge = ye;
                dir = -1;
            }
      
            //detect
            for(i = b.x; i < xe; i++)
               if(imp.get(i, edge) >= t_seed){
                    //borders
                    lb = FindEdge(imp, Side.LEFT, i, edge, t_wid);
                    ub = FindEdge(imp, Side.RIGHT, i, edge, t_wid);
                    //if > minimum width, add as seed
                    if((ub - lb) > minw)
                        list.add(new GutSpine(s, b.y, ye, lb + ((ub - lb)/2), t_wid, maxgap));
                    //forward search
                    i = ub;
                }
       
            //extend
            for(m = list.size() - 1; m >= 0; m--){ 

                l = list.get(m);        //line
                i = l.seedposition;        //starting coordinates
                j = edge;
                k = 0;                      //gap width

                //loop along line
                while((j >= b.y) && (j <= ye)){

                    //borders
                    lb = FindEdge(imp, Side.LEFT, i, j, t_wid);
                    ub = FindEdge(imp, Side.RIGHT, i, j, t_wid);

                    //spline coordinate
                    if((ub - lb) < minw){
                        l.spine[j - b.y] = i;
                        k++;
                        if(k > maxgap) {
                            if(Math.abs(edge - j) < b.width/6) list.remove(m);
                            break;
                        } 
                    } else {
                        l.spine[j - b.y] = lb + ((ub - lb)/2);
                        k = 0;
                    }

                    //next coordinate
                    i = l.spine[j - b.y];
                    j += dir;

                }

            }//end of extension
            
            

        }
        else{
            
            //initiation edge and extension direction
            edge = b.x;
            dir = 1;
            if(s == Side.RIGHT){
                edge = xe;
                dir = -1;
            }
    
            //detect
            for(i = b.y; i < ye; i++)
               if(imp.get(edge, i) >= t_seed){
                    //borders
                    lb = FindEdge(imp, Side.TOP, edge, i, t_wid);
                    ub = FindEdge(imp, Side.BOTTOM, edge, i, t_wid);
                    //if > minimum width, add as seed
                    if((ub - lb) > minw)
                        list.add(new GutSpine(s, b.x, xe, lb + ((ub - lb)/2), t_wid, maxgap));
                    //forward search
                    i = ub;
                }
  
            //extend
            for(m = list.size() - 1; m >= 0; m--){

                l = list.get(m);        //line
                i = l.seedposition;        //starting coordinates
                j = edge;
                k = 0;                      //gap width

                //loop along line
                while((j >= b.x) && (j <= xe)){

                    //borders
                    lb = FindEdge(imp, Side.TOP, j, i, t_wid);
                    ub = FindEdge(imp, Side.BOTTOM, j, i, t_wid);

                    //spline coordinate
                    if((ub - lb) < minw){
                        l.spine[j - b.x] = i;
                        k++;
                        if(k > maxgap) {
                            if(Math.abs(edge - j) < b.height/6) list.remove(m);
                            break;
                        } 
                    } else {
                        l.spine[j - b.x] = lb + ((ub - lb)/2);
                        k = 0;
                    }

                    //next coordinate
                    i = l.spine[j - b.x];
                    j += dir;

                }

            }//end of extension
      
        }
 
    }

    //update spines
    private void UpdateSpines(ImageProcessor frame, int t_wid, int minw, int maxgap, int smoothw, ArrayList<GutSpine> linelist){

        //counters, etc
        int i, j, k, ub, lb;

        for(GutSpine l : linelist){

            if((l.seedside == Side.TOP) || (l.seedside == Side.BOTTOM)){

                k = 0; //gap

                for(i = 0; i < l.spine.length; i++) if(l.spine[i] > 0){

                    if(l.spine[i] == 0) continue;

                    //borders
                    lb = FindEdge2(frame, Side.LEFT, l.spine[i], i + l.p0, t_wid, maxgap);
                    ub = FindEdge2(frame, Side.RIGHT, l.spine[i], i + l.p0, t_wid, maxgap);

                    //spine coordinate
                    if((ub - lb) < minw){
                        //(spine coordinate unchanged)
                        k++;
                        if(k > maxgap) break;
                    } else {
                        l.spine[i] = lb + ((ub - lb)/2);
                        k = 0;
                    }

                    //width
                    l.lbound[i] = lb;
                    l.ubound[i] = ub;

                }

            }else{
                
                k = 0; //gap

                for(i = 0; i < l.spine.length; i++) if(l.spine[i] > 0){

                    if(l.spine[i] == 0) continue;

                    //borders
                    lb = FindEdge2(frame, Side.TOP, i + l.p0, l.spine[i], t_wid, maxgap);
                    ub = FindEdge2(frame, Side.BOTTOM, i + l.p0, l.spine[i], t_wid, maxgap);

                    //spine coordinate
                    if((ub - lb) < minw){
                        //(spine coordinate unchanged)
                        k++;
                        if(k > maxgap) break;
                    } else {
                        l.spine[i] = lb + ((ub - lb)/2);
                        k = 0;
                    }

                    //width
                    l.lbound[i] = lb;
                    l.ubound[i] = ub;

                }

            }
 
            //smooth
            if(smoothw < 3) return;
            int sww = smoothw/2;
            for(i = 0; i < l.spine.length; i++) {
                if(l.spine[i] <= 0) continue;
                l.smooth[i] = 0;
                k = 0;
                for(j = i - sww; j <= i + sww; j++)
                    if((j >= 0) && (j < l.spine.length) && (l.spine[j] > 0)){
                        l.smooth[i] += l.spine[j];
                        k++;
                    }
                l.smooth[i] /= k;
            }
            for(i = 0; i < l.spine.length; i++) l.spine[i] = l.smooth[i];

        }//end of spine list


    }

    //draw spines
    private void DrawSpines(ImageProcessor frame, ArrayList<GutSpine> linelist, int draws, int drawb){
        for(GutSpine l : linelist) l.DrawSpine(frame, draws, drawb);
    }

    /////////////////////////////////////////////////////////////////////////////
    //EDGE DETECTION
    
    //find y distance from set point to threshold
    private int FindEdge(ImageProcessor ip, Side towards, int x, int y, int t){

        CheckX(ip, x);
        CheckY(ip, y);

        int diff = 1;
        int p = 0;

        if((towards == Side.BOTTOM) || (towards == Side.TOP)){

            p = y;
            if (towards == Side.TOP) diff = -1;

            while((p >= 0) &&
                  (p < ip.getHeight()) &&
                  (ip.get(x, p) >= t))
                            p += diff;

        }
        else {

            p = x;
            if (towards == Side.LEFT) diff = -1;

            while((p >= 0) &&
                  (p < ip.getWidth()) &&
                  (ip.get(p, y) >= t))
                            p += diff;

        }

        return p - diff;

    }

    //find y distance from set point to threshold: includes gap skip
    private int FindEdge2(ImageProcessor ip, Side towards, int x, int y, int t, int maxgap){

        CheckX(ip, x);
        CheckY(ip, y);

        int diff = 1;
        int p = 0;
        int g = 0;

        if((towards == Side.BOTTOM) || (towards == Side.TOP)){

            p = y;
            if (towards == Side.TOP) diff = -1;

            while((p > 0) &&
                  (p < ip.getHeight() - 1) &&
                  (g < maxgap)){

                p += diff;
                if(ip.get(x, p) >= t) g = 0; else g++;

            }

        }
        else {

            p = x;
            if (towards == Side.LEFT) diff = -1;

            while((p > 0) &&
                  (p < ip.getWidth() - 1) &&
                  (g < maxgap)){

                p += diff;
                if(ip.get(p, y) >= t) g = 0; else g++;
            }

        }

        return p - (diff * g);

    }

    //check range of an x value
    private void CheckX(ImageProcessor ip, int arg){
        if (arg < 0) {arg = 0; return;}
        if (arg >= ip.getWidth()) {arg = ip.getWidth() - 1; return;}
    }

    //check range of an y value
    private void CheckY(ImageProcessor ip, int arg){
        if (arg < 0) {arg = 0; return;}
        if (arg >= ip.getHeight()) {arg = ip.getHeight() - 1; return;}
    }
    
    /////////////////////////////////////////////////////////////////////////////
    //FRAME GRABER
    
    private boolean StopAndRelease(){
        try{
            grabber.stop();
            grabber.release();
            return true;
        }catch(Exception ex){
            return false;
        }
    }
    
    private void GrabProcessor(){
        
        bproc = null;
        
        try{
            frm = grabber.grabImage();

            if((frm != null) && (frm.image != null) && (frm.imageWidth > 10)){
                bfimg = frameconvertor.getBufferedImage(frm);
                if(bfimg == null) return;
                if(bfimg.getType() == BufferedImage.TYPE_BYTE_GRAY){
                    bproc = new ByteProcessor(bfimg);
                }else{
                    cproc = new ColorProcessor(bfimg);
                    bproc = cproc.convertToByteProcessor();
                }
            }

        }catch(Exception ex){}

    }

    /////////////////////////////////////////////////////////////////////////////
    //MISC
    
    //get bounding area within processor
    private Rectangle GetCheckedBounds(ImageProcessor imp, double[] fb){
        
        Rectangle bounds = new Rectangle(0, 0, imp.getWidth() - 1, imp.getHeight() - 1);
        if((fb[0] > 0) && (fb[0] < imp.getWidth())) bounds.x = (int)fb[0];
        if((fb[1] > 0) && (fb[1] < imp.getHeight())) bounds.y = (int)fb[1];
        if((fb[2] > 0) && (fb[2] + fb[0] < imp.getWidth()))
            bounds.width = (int)fb[2]; else bounds.width = imp.getWidth() - bounds.x;
        if((fb[3] > 0) && (fb[3] + fb[1] < imp.getHeight()))
            bounds.height = (int)fb[3]; else bounds.height = imp.getHeight() - bounds.y;
        
        return bounds;
        
    }

    //get byte or float processor from int data
    private ImageProcessor GetProcessor(int[][] dat, int isfloat){
        if(isfloat == 1) return new FloatProcessor(dat);
        ByteProcessor bp = new ByteProcessor(dat.length, dat[0].length);
        for(int j = 0; j < bp.getHeight(); j++)
            for(int i = 0; i < bp.getWidth(); i++)
                bp.set(i, j, dat[i][j]);
        return bp;
    }
    
    //for 4-textbox Parameter GUI:
    //if contains "s" or "S" return manually selected ROI
    private int GetManualFrame(Param para, ImagePlus img, Rectangle rec){
        

        //find "S"
 
        String[] sstr = para.GetStrings();
        for(String x : sstr) if(x.toLowerCase().contains("s") == true)
            try{

                //dialog
                Object[] options = {"okay", "cancel"};
                int op = JOptionPane.showOptionDialog(para.GetPanel(), "Make a rectangle selection.", "Bounds",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                        options, options[0]);
                
                //get roi
                
                return op;
         
            }catch(Exception ex){}
            
             


        return -1;
        
        
    }
    
}
