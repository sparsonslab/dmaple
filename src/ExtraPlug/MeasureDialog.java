/*****************************************************************************
//////////////////////////////////////////////////////////////////////////////
 Author:    Sean Parsons
 Date:      May 2014

Dialog for image calibration and measurement.

August 2015 - add conversion from metadata calibration.
October 2016 - add copying of overlay during cropping.

///////////////////////////////////////////////////////////////////////////////
******************************************************************************/

package ExtraPlug;

import Misc.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.awt.geom.*;

import ij.gui.*;
import ij.*;
import ij.process.*;
import ij.measure.*;
import ij.io.*;
import ij.text.*;
import ij.measure.Calibration;


public class MeasureDialog  extends JDialog 
        implements MouseMotionListener, ActionListener, WindowFocusListener, ImageListener {

    
    String prefix = "Measure: ";        //title of dialog

    //dialog number formating
    int ndec = 6;       //no. of decimal places
    int nzeros = 2;     //truncation after n zeros

    //loading of metadata calibration (matches dimensions of tfCalData)
    String[][] metatags = {{"X_ORIGIN", "X_RES", "X_UNIT"},
                           {"Y_ORIGIN", "Y_RES", "Y_UNIT"},
                           {"Z_ORIGIN", "Z_RES", "Z_UNIT"},
                           {"A_ORIGIN", "A_RES", "A_UNIT"}};

    ////////////////////////////////////////////////////////////////////////////
    //ELEMENTS
    
    //measure panel
    JTextField[][] tfMeasure;
    JComboBox cbRatio;
    JButton btToTable;
    JButton btFromTable;
    JButton btCrop;
    JButton btSetROI;
    
    //calibration panel
    JTextField[][] tfCalData;
    JButton btCalSetImage;
    JButton btCalLoad;
    JButton btMetaCal;  //*** added August 2015
    
    //measurements table
    ResultsTable mtable;
    String mtable_title = "measurements";

    //////////////////////////////////////////////////////////////////////////
    //CNSTR

    public MeasureDialog(Frame parent){
        
        
        //dialog set up
        super(parent, "Measure", Dialog.ModalityType.MODELESS);
        setSize(300,180);
        setResizable(false);
        
        int i, j;

        //window and mouse listener
        int[] wid = WindowManager.getIDList();
        ImagePlus img;
        if(wid != null) for(i = 0; i < wid.length; i++){
            img = WindowManager.getImage(wid[i]);
            img.getWindow().addWindowFocusListener(this);
            img.getCanvas().addMouseMotionListener(this);
        }
               
        //image listener
        ImagePlus.addImageListener(this);


        //MEASURE PANEL
        
        //origin, distance and ratio data
        tfMeasure = new JTextField[4][3];
        for(i = 0; i < 4; i++){
            tfMeasure[i][0] = new JTextField(10); //origin
            tfMeasure[i][1] = new JTextField(10); //distance
            tfMeasure[i][2] = new JTextField(10); //ratio
            tfMeasure[i][0].setHorizontalAlignment(SwingConstants.LEFT);
            tfMeasure[i][1].setHorizontalAlignment(SwingConstants.LEFT);
            tfMeasure[i][2].setHorizontalAlignment(SwingConstants.LEFT);
        }

        //ratio drop down list
        String[] ops = {"d/dx", "d/dy", "d/dz", "d/da"};
        cbRatio = new JComboBox(ops);
        cbRatio.addActionListener(this);

        //buttons
        btToTable = new JButton(">Table");
        btToTable.addActionListener(this);
        btFromTable = new JButton("<Table");
        btFromTable.addActionListener(this);
        btCrop = new JButton("Crop");
        btCrop.addActionListener(this);
        btSetROI = new JButton("ROI");
        btSetROI.addActionListener(this);
        
        //panel
        JPanel pnMeasure = new JPanel();
        pnMeasure.setLayout(new GridLayout(6, 4));
        pnMeasure.setLayout(new GridLayout(6, 4));
        pnMeasure.add(new JLabel("dimension"));
        pnMeasure.add(new JLabel("origin"));
        pnMeasure.add(new JLabel("d"));
        pnMeasure.add(cbRatio);
        for(i = 0; i < 4; i++){
            if(i == 0) pnMeasure.add(new JLabel("x", SwingConstants.CENTER));
            else if(i == 1) pnMeasure.add(new JLabel("y", SwingConstants.CENTER));
            else if(i == 2) pnMeasure.add(new JLabel("z", SwingConstants.CENTER));
            else if(i == 3) pnMeasure.add(new JLabel("a", SwingConstants.CENTER));
            for(j = 0; j < 3; j++) pnMeasure.add(tfMeasure[i][j]);
        }
        pnMeasure.add(btToTable);
        pnMeasure.add(btFromTable);
        pnMeasure.add(btCrop);
        pnMeasure.add(btSetROI);

        
        //CALIBRATION PANEL
        
        //origin, resolution and unit data
        tfCalData = new JTextField[4][3];
        for(i = 0; i < 4; i++){
            tfCalData[i][0] = new JTextField(10); //origin
            tfCalData[i][1] = new JTextField(10); //resolution
            tfCalData[i][2] = new JTextField(10); //unit
            tfCalData[i][0].setHorizontalAlignment(SwingConstants.LEFT);
            tfCalData[i][1].setHorizontalAlignment(SwingConstants.LEFT);
            tfCalData[i][2].setHorizontalAlignment(SwingConstants.LEFT);
        }
        
        //buttons
        btCalSetImage = new JButton("Set");
        btCalSetImage.addActionListener(this);
        btCalLoad = new JButton("Load");
        btCalLoad.addActionListener(this);
        btMetaCal = new JButton("Meta>");
        btMetaCal.addActionListener(this);

        //panel
        JPanel pnCalibration = new JPanel();
        pnCalibration.setLayout(new GridLayout(6, 4));
        pnCalibration.add(new JLabel("dimension"));
        pnCalibration.add(new JLabel("origin"));
        pnCalibration.add(new JLabel("unit/pixel"));
        pnCalibration.add(new JLabel("unit"));
        for(i = 0; i < 4; i++){
            if(i == 0) pnCalibration.add(new JLabel("x", SwingConstants.CENTER));
            else if(i == 1) pnCalibration.add(new JLabel("y", SwingConstants.CENTER));
            else if(i == 2) pnCalibration.add(new JLabel("z", SwingConstants.CENTER));
            else if(i == 3) pnCalibration.add(new JLabel("a", SwingConstants.CENTER));
            for(j = 0; j < 3; j++) pnCalibration.add(tfCalData[i][j]);
        }
        pnCalibration.add(new JLabel(""));
        pnCalibration.add(btCalSetImage);
        pnCalibration.add(btCalLoad);
        pnCalibration.add(btMetaCal);

        //ADD ELEMENTS TO DIALOG
        JTabbedPane tpPane = new JTabbedPane();
        tpPane.add("Measure", pnMeasure);
        tpPane.add("Calibration", pnCalibration);
        add(tpPane);

        //update
        GetMeasureData(WindowManager.getCurrentImage());
        GetCalData(WindowManager.getCurrentImage());
        
        //MEASUREMENTS TABLE
        mtable = new ResultsTable();
        mtable.showRowNumbers(false);

    }
    
    public MeasureDialog(){
        
    
    }
    
    /////////////////////////////////////////////////////////////////////////
    //MEASURE DATA
    void GetMeasureData(ImagePlus img){

        //default
        if((img == null) || (img.getCalibration() == null)){

            for(int i = 0; i < 4; i++){
                tfCalData[i][0].setText("0.00"); //origin
                tfCalData[i][1].setText("1.00"); //resolution
                tfCalData[i][2].setText("cm"); //unit
            }

            return;
        }
   
        //image
        ImageProcessor proc = img.getProcessor();   //processor
        Calibration cal = img.getCalibration();     //calibration
        Rectangle rec = new Rectangle(0, 0, img.getWidth(), img.getHeight());//roi bounds
        //if(img.getRoi() != null) rec = img.getRoi().getBounds();
        //...replaced (3/12/15) with following to deal with arrows (whose bounding rectangle includes head)
        Roi roi = img.getRoi();
        if(roi != null){
            rec = roi.getBounds();
            if(roi.getClass().equals(Arrow.class) == true){
                Arrow arrow = (Arrow)roi;
                Line line = new Line(arrow.x1d, arrow.y1d, arrow.x2d, arrow.y2d);
                rec = line.getBounds();
            }
        }

        //amplitudes
        double p1 = proc.getf(rec.x , rec.y);
        double p2 = proc.getf(rec.x + rec.width - 1, rec.y + rec.height - 1);
        double[] coeff = cal.getCoefficients();
        if((coeff != null) && (cal.getFunction() == Calibration.STRAIGHT_LINE)){
            p1  = (p1 + coeff[0]) * coeff[1];
            p2  = (p2 + coeff[0]) * coeff[1];
        }

        //ratio denominator
        double denom = 0;
        int item = cbRatio.getSelectedIndex();
        if(item == 0)      denom = rec.width * cal.pixelWidth;
        else if(item == 1) denom = rec.height * cal.pixelHeight;
        else if(item == 2) denom = 1.0;                     //?????????????????????????
        else if(item == 3) denom = p2 - p1;
        
        //x
        tfMeasure[0][0].setText(DblString.LooseZeros((-cal.xOrigin + rec.x) * cal.pixelWidth, ndec, nzeros));
        tfMeasure[0][1].setText(DblString.LooseZeros(rec.width * cal.pixelWidth, ndec, nzeros));
        tfMeasure[0][2].setText(DblString.DblToStr(rec.width  * cal.pixelWidth / denom, ndec));
        
        //y
        tfMeasure[1][0].setText(DblString.LooseZeros((-cal.yOrigin + rec.y) * cal.pixelHeight, ndec, nzeros));
        tfMeasure[1][1].setText(DblString.LooseZeros(rec.height * cal.pixelHeight, ndec, nzeros));
        tfMeasure[1][2].setText(DblString.DblToStr(rec.height * cal.pixelHeight / denom, ndec));
        
        //z
        //???????????????????????
        
        //a
        tfMeasure[3][0].setText(DblString.LooseZeros(p1, ndec, nzeros));
        tfMeasure[3][1].setText(DblString.LooseZeros(p2 - p1, ndec, nzeros));
        tfMeasure[3][2].setText(DblString.DblToStr((p2 - p1)/denom, ndec));

    }

    void SetROI(ImagePlus img){

        //image
        if(img == null) return;

        //calibration
        if(img.getCalibration() == null) SetCalData(img);
        Calibration cal = img.getCalibration(); 
        
        //bounds
        Rectangle rec = new Rectangle();

        //x
        try {
            rec.x = (int)Math.floor(cal.xOrigin + (Double.parseDouble(tfMeasure[0][0].getText())/cal.pixelWidth));
            rec.width = (int)Math.round(Double.parseDouble(tfMeasure[0][1].getText())/cal.pixelWidth);
        }catch(NumberFormatException e){}
        
        //y
        try {
            rec.y = (int)Math.floor(cal.yOrigin + (Double.parseDouble(tfMeasure[1][0].getText())/cal.pixelHeight));
            rec.height = (int)Math.round(Double.parseDouble(tfMeasure[1][1].getText())/cal.pixelHeight);
        }catch(NumberFormatException e){}

        //roi
        int type = 0;
        if((img.getRoi() != null) && (img.getRoi().getType() == 5))
            img.setRoi(new Line(rec.x, rec.y, rec.x + rec.width, rec.y + rec.height), true);
        else
            img.setRoi(new Roi(rec), true);

        //set
        GetMeasureData(img);
        
    }

    public void Crop(ImagePlus img){
        
        //image
        if(img == null) return;

        //calibration
        if(img.getCalibration() == null) SetCalData(img);
        Calibration cal = img.getCalibration().copy();
        
        //roi
        if(img.getRoi() == null) SetROI(img);
        Roi roi = img.getRoi();
        Rectangle rec = roi.getBounds();
        String roiname = roi.getName();
        if(roiname == null) roiname  = "crop";
   
        //image
        if(roi.getType() != 0) img.setRoi(rec);
        ImageProcessor ip = img.getProcessor().crop();
        ImagePlus nimg = new ImagePlus(img.getShortTitle() + "_" + roiname, ip);  //**14/10/16**_crop suffix
        
        //origins
        cal.xOrigin = -(Double.parseDouble(tfMeasure[0][0].getText())/cal.pixelWidth);
        cal.yOrigin = -(Double.parseDouble(tfMeasure[1][0].getText())/cal.pixelHeight);
        nimg.setCalibration(cal);
        
        //overlay (added 12/10/16)
        Overlay ovly = img.getOverlay();
        if(ovly != null){
            ovly = img.getOverlay().duplicate();
            Overlay ovly_new = new Overlay();
            Polygon poly; Polygon poly2; PointRoi pnt; PointRoi pnt2; Line ln; Line2D.Double ln2d; Rectangle roibnds;
            Roi[] rois = ovly.toArray();
            for(Roi x: rois){
                //...point
                if(x.getType() == Roi.POINT){
                    pnt = (PointRoi)x;
                    poly = pnt.getPolygon();
                    poly2 = new Polygon();
                    for(int i = 0; i < poly.npoints; i++)
                        if(rec.contains(poly.xpoints[i], poly.ypoints[i]) == true)
                            poly2.addPoint(poly.xpoints[i] - rec.x, poly.ypoints[i] - rec.y);
                    if(poly2.npoints > 0){
                        pnt2 = new PointRoi(poly2);
                        pnt2.setSize(pnt.getSize());
                        pnt2.setPointType(pnt.getPointType());
                        pnt2.setStrokeColor(pnt.getStrokeColor());
                        //...more properties????
                        ovly_new.add(pnt2);
                    }
                }
                //...line
                else if(x.getType() == Roi.LINE){
                    ln = (Line)x;
                    ln2d = new Line2D.Double(ln.x1, ln.y1, ln.x2, ln.y2);
                    if(ln2d.intersects(rec.x, rec.y, rec.width, rec.height)){
                        roibnds = x.getBounds();
                        x.setLocation(roibnds.x - rec.x, roibnds.y - rec.y);
                        ovly_new.add(x);
                    }
                }
                //other shape
                else{
                    poly = x.getPolygon();
                    if(poly.intersects(rec.x, rec.y, rec.width, rec.height)){
                        roibnds = x.getBounds();
                        x.setLocation(roibnds.x - rec.x, roibnds.y - rec.y);
                        ovly_new.add(x);
                    }
                }
            }
            nimg.setOverlay(ovly_new);
        }

        //show cropped image
        nimg.show();
        
        //reset roi (from crop rectangle)
        img.setRoi(roi);
 
    }

    //static version of Crop, for use with Call Function **14/10/2016***
    public static void Crop2(ImagePlus img){
        
        //image
        if(img == null) return;

        //calibration
        Calibration cal = new Calibration();
        if(img.getCalibration() != null) cal = img.getCalibration().copy();
        
        //roi
        if(img.getRoi() == null) return;
        Roi roi = img.getRoi();
        Rectangle rec = roi.getBounds();
   
        //image
        if(roi.getType() != 0) img.setRoi(rec);
        ImageProcessor ip = img.getProcessor().crop();
        ImagePlus nimg = new ImagePlus(img.getShortTitle() + "_crop", ip);  //**14/10/16**_crop suffix
        
        //origins
        cal.xOrigin =- rec.x;
        cal.yOrigin =- rec.y;
        nimg.setCalibration(cal);
        
        //overlay (added 12/10/16)
        Overlay ovly = img.getOverlay().duplicate();
        Overlay ovly_new = new Overlay();
        Polygon poly; Polygon poly2; PointRoi pnt; PointRoi pnt2; Line ln; Line2D.Double ln2d; Rectangle roibnds;
        Roi[] rois = ovly.toArray();
        for(Roi x: rois){
            //...point
            if(x.getType() == Roi.POINT){
                pnt = (PointRoi)x;
                poly = pnt.getPolygon();
                poly2 = new Polygon();
                for(int i = 0; i < poly.npoints; i++)
                    if(rec.contains(poly.xpoints[i], poly.ypoints[i]) == true)
                        poly2.addPoint(poly.xpoints[i] - rec.x, poly.ypoints[i] - rec.y);
                if(poly2.npoints > 0){
                    pnt2 = new PointRoi(poly2);
                    pnt2.setSize(pnt.getSize());
                    pnt2.setPointType(pnt.getPointType());
                    pnt2.setStrokeColor(pnt.getStrokeColor());
                    //...more properties????
                    ovly_new.add(pnt2);
                }
            }
            //...line
            else if(x.getType() == Roi.LINE){
                ln = (Line)x;
                ln2d = new Line2D.Double(ln.x1, ln.y1, ln.x2, ln.y2);
                if(ln2d.intersects(rec.x, rec.y, rec.width, rec.height)){
                    roibnds = x.getBounds();
                    x.setLocation(roibnds.x - rec.x, roibnds.y - rec.y);
                    ovly_new.add(x);
                }
            }
            //other shape
            else{
                poly = x.getPolygon();
                if(poly.intersects(rec.x, rec.y, rec.width, rec.height)){
                    roibnds = x.getBounds();
                    x.setLocation(roibnds.x - rec.x, roibnds.y - rec.y);
                    ovly_new.add(x);
                }
            }
        }
        nimg.setOverlay(ovly_new);

        //show cropped image
        nimg.show();
        
        //reset roi (from crop rectangle)
        img.setRoi(roi);
 
    }

   
    //////////////////////////////////////////////////////////////////////////
    //RESULTS TABLE

    void GetResults(ResultsTable table){

        //table
        Frame frm = WindowManager.getFrame(mtable_title);
        if((frm == null) || ((frm instanceof ij.text.TextWindow) == false))
            return;

        //selected row
        TextWindow tw = (TextWindow)frm;
        int i = tw.getTextPanel().getSelectionStart();
        if(i < 0) return;
        
        //origin and distance
        tfMeasure[0][0].setText(mtable.getStringValue("x0", i));
        tfMeasure[0][1].setText(mtable.getStringValue("dx", i));
        
        tfMeasure[1][0].setText(mtable.getStringValue("y0", i));
        tfMeasure[1][1].setText(mtable.getStringValue("dy", i));
        
        tfMeasure[3][0].setText(mtable.getStringValue("a0", i));
        tfMeasure[3][1].setText(mtable.getStringValue("da", i));

        //derivatives
        //set by SetRoi() in action listener of btFromTable button.

    }

    void SetResults(ResultsTable table, ImagePlus img){

        mtable.incrementCounter();

        //image title
        String imgtitle = "";
        if(img != null) imgtitle = img.getTitle();
        mtable.addValue("image", imgtitle);

        //derivative prefix
        String dv = (String)cbRatio.getSelectedItem();
        dv = dv.substring(dv.indexOf("/"));

        //measurement data
        mtable.addValue("x0", tfMeasure[0][0].getText());
        mtable.addValue("dx", tfMeasure[0][1].getText());
        mtable.addValue("dx" + dv, tfMeasure[0][2].getText());

        mtable.addValue("y0", tfMeasure[1][0].getText());
        mtable.addValue("dy", tfMeasure[1][1].getText());
        mtable.addValue("dy" + dv, tfMeasure[1][2].getText());

        mtable.addValue("a0", tfMeasure[3][0].getText());
        mtable.addValue("da", tfMeasure[3][1].getText());
        mtable.addValue("da" + dv, tfMeasure[3][2].getText());

        //update
        mtable.show(mtable_title);

    }

    //////////////////////////////////////////////////////////////////////////
    //CALIBRATION DATA

    void GetCalData(ImagePlus img){
       
        //default
        if((img == null) || (img.getCalibration() == null)){

            for(int i = 0; i < 4; i++){
                tfCalData[i][0].setText("0.00"); //origin
                tfCalData[i][1].setText("1.00"); //resolution
                tfCalData[i][2].setText("cm"); //unit
            }

            //title
            this.setTitle(prefix);

            return;
        }

        //image calibration
        Calibration cal = MyCalibration.GetCalibration(img);

        //x
        tfCalData[0][0].setText(DblString.LooseZeros(-cal.xOrigin * cal.pixelWidth, ndec, nzeros));
        tfCalData[0][1].setText(DblString.LooseZeros(cal.pixelWidth, ndec, nzeros));
        tfCalData[0][2].setText(cal.getXUnit());

        //y
        tfCalData[1][0].setText(DblString.LooseZeros(-cal.yOrigin * cal.pixelHeight, ndec, nzeros));
        tfCalData[1][1].setText(DblString.LooseZeros(cal.pixelHeight, ndec, nzeros));
        tfCalData[1][2].setText(cal.getYUnit());
        
        //z
        tfCalData[2][0].setText(DblString.LooseZeros(-cal.zOrigin * cal.pixelDepth, ndec, nzeros));
        tfCalData[2][1].setText(DblString.LooseZeros(cal.pixelDepth, ndec, nzeros));
        tfCalData[2][2].setText(cal.getZUnit());

        //a
        double[] coeff = cal.getCoefficients();
        if((coeff == null) || (cal.getFunction() != Calibration.STRAIGHT_LINE)){
            tfCalData[3][0].setText("0.00");
            tfCalData[3][1].setText("1.00");
            tfCalData[3][2].setText("cm");
        }
        else{
            tfCalData[3][0].setText(DblString.LooseZeros(coeff[0], ndec, nzeros));
            tfCalData[3][1].setText(DblString.LooseZeros(coeff[1], ndec, nzeros));
            tfCalData[3][2].setText(cal.getValueUnit());
        }

        //title
        this.setTitle(prefix + img.getTitle());

    }

    void SetCalData(ImagePlus img){

        if(img == null) return;

        int i, j;
        double or, res;

        //image calibration
        Calibration cal = img.getCalibration();

        //x
        try{
            or = Double.parseDouble(tfCalData[0][0].getText());
            res = Double.parseDouble(tfCalData[0][1].getText());
            cal.xOrigin = -or/res;
            cal.pixelWidth = res;
        }catch(NumberFormatException e){}
        cal.setXUnit(tfCalData[0][2].getText());

        //y
        try{
            or = Double.parseDouble(tfCalData[1][0].getText());
            res = Double.parseDouble(tfCalData[1][1].getText());
            cal.yOrigin = -or/res;
            cal.pixelHeight = res;
        }catch(NumberFormatException e){}
        cal.setYUnit(tfCalData[1][2].getText());

        //z
        try{
            or = Double.parseDouble(tfCalData[2][0].getText());
            res = Double.parseDouble(tfCalData[2][1].getText());
            cal.zOrigin = -or/res;
            cal.pixelDepth = res;
        }catch(NumberFormatException e){}
        cal.setZUnit(tfCalData[2][2].getText());

        //a
        try{
            or = Double.parseDouble(tfCalData[3][0].getText());
            res = Double.parseDouble(tfCalData[3][1].getText());
            double[] coeff = {or, res};
            cal.setFunction(Calibration.STRAIGHT_LINE, coeff, tfCalData[3][2].getText());
        }catch(NumberFormatException e){}
        cal.setValueUnit(tfCalData[3][2].getText());

        //set
        MyCalibration.SetCalibration(img, cal);


    }
    
    //get calibration data from old MetaData format ****(added Aug 2015)
    void GetMetaCal(ImagePlus img){

        //image calibration
        if(img == null) return;
        Calibration cal = img.getCalibration();
        if(cal == null) return;

        //list of tags
        String[] info = img.getOriginalFileInfo().info.split("\n");
        ArrayList<String[]> tags = new ArrayList<String[]>();
        String[] line;
        for(String str : info){
            line = str.split("\t");
            if(line.length == 2) tags.add(line);
        }

        //get tag information
        int i, j, k;
        for(i = 0; i < 4; i++)
            for(j = 0; j < 3; j++)
                for(k = 0; k < tags.size(); k++)
                    if(tags.get(k)[0].matches(metatags[i][j])){
                        tfCalData[i][j].setText(tags.get(k)[1]);
                        break;
                    }

    }

    ////////////////////////////////////////////////////////////////////////////
    //MOUSE LISTENER
    
    public void mouseDragged(MouseEvent me){

        Window win = WindowFinder.findWindow(me.getComponent());
        if(win == null) return;
        if(win.equals(WindowManager.getCurrentWindow())){
            if(WindowManager.getCurrentImage().getRoi() == null) return;
            GetMeasureData(WindowManager.getCurrentImage());
        }
        
    }
    
    public void mouseMoved(MouseEvent me){}

    ////////////////////////////////////////////////////////////////////////////
    //ACTION LISTENER
    
    public void actionPerformed(ActionEvent ae){

        int i, j;

        //calibration: set image
        if(ae.getSource() == btCalSetImage){

            //active image
            ImagePlus img = WindowManager.getCurrentImage();
            SetCalData(img);
            img.updateAndRepaintWindow(); //***added aug 2015
            //save
            FileInfo fi = img.getOriginalFileInfo();
            if((fi.directory == null) || (fi.directory.isEmpty())) fi.directory = IJ.getDirectory("current");
            IJ.save(img, fi.directory + img.getOriginalFileInfo().fileName);

        }
        //calibration: load
        else if(ae.getSource() == btCalLoad){

            //pick file
            JFileChooser fdialog = new JFileChooser();
            fdialog.setMultiSelectionEnabled(false);
            MyFileFilterEnds filt = new MyFileFilterEnds(".txt");
            fdialog.addChoosableFileFilter(filt);
            fdialog.setCurrentDirectory(new File(IJ.getDirectory("current")));
            int returnVal = fdialog.showOpenDialog(IJ.getInstance());
            if(returnVal != JFileChooser.APPROVE_OPTION) return;
            File f = fdialog.getSelectedFile();
            OpenDialog.setDefaultDirectory(f.getParent());
        
            //read
            try{

                BufferedReader br = new BufferedReader(new FileReader(f));
                String line = "";
                String[] vals;
                for(i = 0; i < 4; i++){
                    line = br.readLine();
                    if(line == null) break;
                    vals = line.split("\t");
                    for(j = 0; j < vals.length; j++) tfCalData[i][j].setText(vals[j]);
                }
                
                br.close();
                
            }
            //cannot open stream, etc
            catch(IOException e){
                
                return;
            }
            catch(IllegalArgumentException e){
                
                return;
            }

        }
        //calibration: load from meta data ****(added Aug 2015)
        else if(ae.getSource() == btMetaCal){

            GetMetaCal(WindowManager.getCurrentImage());

        }
        //measure: write measurments to table
        else if(ae.getSource() == btToTable){

            SetResults(ResultsTable.getResultsTable(), WindowManager.getCurrentImage());
            SetROI(WindowManager.getCurrentImage());

        }
        //measure: get measurements from table
        else if(ae.getSource() == btFromTable){

            GetResults(ResultsTable.getResultsTable());
            SetROI(WindowManager.getCurrentImage());


        }
        //measure: make cropped image
        else if(ae.getSource() == btCrop){
            
            Crop(WindowManager.getCurrentImage());
            
        }
        //measure: set ROI
        else if(ae.getSource() == btSetROI){

            SetROI(WindowManager.getCurrentImage());

        }
        //measure: change ration (drop down list)
        else if(ae.getSource() == cbRatio){
            
            GetMeasureData(WindowManager.getCurrentImage());

        }
        
 
        
    }

    ////////////////////////////////////////////////////////////////////////////
    //WINDOW LISTENER
    
    public void windowGainedFocus(WindowEvent we){
        
        int[] wid = WindowManager.getIDList();
        if(wid != null)
           for(int i = 0; i < wid.length; i++)
               if(WindowManager.getImage(wid[i]).getWindow().equals(we.getWindow())){
                   this.setTitle(prefix + WindowManager.getImage(wid[i]).getTitle());
                   GetCalData(WindowManager.getImage(wid[i]));
                   GetMeasureData(WindowManager.getImage(wid[i]));
                   break;
               }
               
    }

    public void windowLostFocus(WindowEvent we){}

    /////////////////////////////////////////////////////////////////////////////
    //IMAGE LISTENER (IMAGEJ)
    
    public void imageClosed(ImagePlus imp){}
   
    public void imageOpened(ImagePlus imp){
        imp.getWindow().addWindowFocusListener(this);
        imp.getCanvas().addMouseMotionListener(this);
    }
            
    public void imageUpdated(ImagePlus imp){} 

}
