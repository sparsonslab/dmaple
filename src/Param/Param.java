/********************************************************************************
/////////////////////////////////////////////////////////////////////////////////
April 2017, Sean Parsons

Describes a set of parameters as represented in a swing panel.

Three parameter/panel types: (see PType.java)
VALUES          text boxes
RADIO           radio buttons
RADIO_EX        radio button group
DROPDOWN        combo box

- Static methods for loading and saving Param members of an object.
- Static methods for returning a panel of grouped and labeled Param GUIs.

/////////////////////////////////////////////////////////////////////////////////
********************************************************************************/
package Param;

import Misc.MyFileFilterEnds;

import ij.IJ;
import ij.io.OpenDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.lang.reflect.*;


public class Param {
    
    private String id;          //unique id (for text file and macro representation)
    private ParamType type;     //type of GUI component (text boxes, radio buttons, combo box, etc)
    private String label;       //label
    public JButton but;         //function button
    private JPanel panel;       //panel
    private JComponent[] comp;  //components of panel
    
    ///////////////////////////////////////////////////////////////////////////
    //CNSTR
    
    /*cnstr
    valarg is comma delimited string of values:
    val$ann,val$ann,..etc
    
    For VALUES type: val = value shown in text box, ann = label at left of text box
    For RADIO and RADIO_EX types: val = label, ann = whether button is selected (1) or not (0)
    For DROPDOWN type: val = text item, ann = to mark item for selection (any string allowed).
    */
    public Param(String idarg, ParamType typearg, String labelarg, String valarg, ActionListener blarg){
        
        //id, type and label
        if(idarg.isEmpty()) id = labelarg;
            else id = idarg;
        type = typearg;
        label = labelarg;
        
        //function button
        but = null;
        if(blarg != null){
            but = new JButton("...");
            but.addActionListener(blarg);
        }
        
        //values
        String[] values = valarg.split(",");
        int n = values.length;
        
        String[] vlp;  
        int i, k;
        switch(type){
            
            case VALUES:
                //look for %, indicating text box labels
                int nb = n;
                for(i = 0; i < n; i++) if(values[i].contains("%")) nb = n * 2;
   
                panel = new JPanel(new GridLayout(1, nb));
                comp = new JTextField[n];
                for(i = 0; i < n; i++){
                    vlp = values[i].split("%");
                    comp[i] = new JTextField(vlp[0]);
                    if(nb != n)
                        if(vlp.length == 1) panel.add(new JLabel(""));
                        else panel.add(new JLabel(vlp[1] + " ", SwingConstants.RIGHT));
                    panel.add(comp[i]);
                }
                break;
                
            case RADIO:
                panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
                comp = new JRadioButton[n];
                for(i = 0; i < n; i++){
                    vlp = values[i].split("%");
                    if(vlp.length == 1) comp[i] = new JRadioButton(vlp[0], true);
                                   else comp[i] = new JRadioButton(vlp[0], IsOn(vlp[1]));
                    panel.add(comp[i]);
                }
                break;
            
            case RADIO_EX:
                panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
                comp = new JRadioButton[n];
                ButtonGroup grp = new ButtonGroup();
                for(i = 0; i < n; i++){
                    vlp = values[i].split("%");
                    if(vlp.length == 1) comp[i] = new JRadioButton(vlp[0], true);
                                   else comp[i] = new JRadioButton(vlp[0], IsOn(vlp[1]));
                    panel.add(comp[i]);
                    grp.add((JRadioButton)comp[i]);
                }
                break;
                
            case DROPDOWN:
                panel = new JPanel(new GridLayout(1, 1));
                JComboBox cb = new JComboBox();
                comp = new JComboBox[1];
                comp[0] = cb;
                k = 0;
                for(i = 0; i < n; i++){
                    vlp = values[i].split("%");
                    if(vlp.length > 1) k = i;
                    cb.addItem(vlp[0]);
                }
                cb.setSelectedIndex(k);
                panel.add(comp[0]);
            
        }
        
    }
    
    private boolean IsOn(String str){
        return (str.matches("1") || str.contains("t"));
    }
    
    ///////////////////////////////////////////////////////////////////////////
    //GET VALUES
    
    //get parameter value
    public double GetValue(int i){
        double[] vals = GetValues();
        if(i < vals.length) return vals[i];
        return 0;
    }
   
    //get parameter values
    public double[] GetValues(){
        
        int i;
        double[] vals = new double[comp.length];
        
        switch(type){
            
            case VALUES:
                for(i = 0; i < comp.length; i++){
                    JTextField tf = (JTextField)comp[i];
                    try{
                        vals[i] = Double.parseDouble(tf.getText());
                    }catch(Exception ex){
                        vals[i] = 0;
                    }
                }
                break;
   
            case RADIO:
                for(i = 0; i < comp.length; i++){
                    JRadioButton rb = (JRadioButton)comp[i];
                    if(rb.isSelected()) vals[i] = 1;
                }
                break;
  
            case RADIO_EX:
                for(i = 0; i < comp.length; i++){
                    JRadioButton rb = (JRadioButton)comp[i];
                    if(rb.isSelected()) vals[i] = 1;
                }
                break;
                
            case DROPDOWN:
                JComboBox cb = (JComboBox)comp[0];
                vals = new double[cb.getItemCount()];
                for(i = 0; i < cb.getItemCount(); i++)
                    if(i == cb.getSelectedIndex()) vals[i] = 1;
                break;
                
        }
        
        return vals;
        
    }
    
    //get parameter values as strings
    public String GetString(int i){
        String[] vals = GetStrings();
        if(i < vals.length) return vals[i];
        return "";
    }
    
    //get parameter values as strings
    public String[] GetStrings(){
        
        int i;
        String[] vals = new String[comp.length];
        
        switch(type){
            
            case VALUES:
                for(i = 0; i < comp.length; i++){
                    JTextField tf = (JTextField)comp[i];
                    vals[i] = tf.getText();
                }
                break;
   
            case RADIO:
                for(i = 0; i < comp.length; i++){
                    JRadioButton rb = (JRadioButton)comp[i];
                    if(rb.isSelected()) vals[i] = "true";
                    else vals[i] = "false";
                }
                break;
  
            case RADIO_EX:
                for(i = 0; i < comp.length; i++){
                    JRadioButton rb = (JRadioButton)comp[i];
                    if(rb.isSelected()) vals[i] = "true";
                    else vals[i] = "false";
                }
                break;
                
            case DROPDOWN:
                JComboBox cb = (JComboBox)comp[0];
                vals = new String[cb.getItemCount()];
                for(i = 0; i < cb.getItemCount(); i++)
                    vals[i] = (String)cb.getSelectedItem();
                break;
           
        }
        
        return vals;
        
    }
    
    //get selected value (button group and combo box)
    public int GetSelected(){
        
        int i;
   
        switch(type){
    
            case RADIO_EX:
                for(i = 0; i < comp.length; i++){
                    JRadioButton rb = (JRadioButton)comp[i];
                    if(rb.isSelected()) return i;
                }
                break;
                
            case DROPDOWN:   
                JComboBox cb = (JComboBox)comp[0];
                return cb.getSelectedIndex();
                
        }
        
        return -1;
        
    }
    
    ///////////////////////////////////////////////////////////////////////////
    //RETURN MEMBERS
    
    //match id
    public boolean ismatch(String idarg){return id.matches(idarg);}
    
    public String GetLabel(){return label;}
    
    public String GetID(){return id;}
    
    public JPanel GetPanel(){return panel;}
   
    public void EnableComponents(boolean doenable){
        for(JComponent x : comp) x.setEnabled(doenable);
    }
    
    ///////////////////////////////////////////////////////////////////////////
    /* LOAD AND SAVE PARAMETER LISTS
    - open file open/save dialog
    - load/save list of Param objects associated with an object, represented as a string file:
    OBJECT1_ID      values
    OBJECT2_ID      values
    ...etc
    */
    
    //load from file
    public static void LoadParameters(Object cob){

        File f = GetFile(false);
     
        Param[] allwidgets = GetSortedList(cob);

        try{
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            for(Param x : allwidgets){
                line = br.readLine();
                if(line == null) break;
                x.Read(line);
            }
            br.close();
        }catch(Exception ex){
            IJ.showMessage("Could not open " + f.getName());
        } 
        
    }
    
    //save to file
    public static void SaveParameters(Object cob){

        File f = GetFile(true);
        
        Param[] allwidgets = GetSortedList(cob);
 
        try{
            BufferedWriter br = new BufferedWriter(new FileWriter(f));
            for(Param x : allwidgets)
                br.write(x.Write());
            br.close();
        }catch(Exception ex){
            IJ.showMessage("Could not write to " + f.getName());
        }
          
    }

    //load from whole string
    public static void LoadParametersFromString(Object cob, String str){
        
        Param[] allwidgets = GetSortedList(cob);
        
        String[] paras = str.split("\n");
        for(int i = 0; (i < paras.length) && (i < allwidgets.length); i++)
                allwidgets[i].Read(paras[i]);
        
    }

    //get list of Param members of a class, sortyed by id
    public static Param[] GetSortedList(Object cob){
        
        ArrayList<Param> plist = new ArrayList<>();
       
        //fields
        Field[] filds = cob.getClass().getDeclaredFields();
        Object member;
        for(Field f : filds){
            f.setAccessible(true);
            try{
                //if field object is a Param, add to list
                member = f.get(cob);
                if(member.getClass().equals(Param.class))
                    plist.add((Param)member);
            }catch(Exception ex){}
        }
        
        //sort by id
        Collections.sort(plist, new ParamComparator());
  
        //convert to array
        Param[] parray = new Param[plist.size()];
        for(int i = 0; i < plist.size(); i++)
            parray[i] = plist.get(i);
    
        return parray;
        
    }

    //get open/save file dialog
    private static File GetFile(boolean showassave){
      
        //file picker
        JFileChooser fdialog = new JFileChooser();
        fdialog.setMultiSelectionEnabled(false);
        MyFileFilterEnds filt = new MyFileFilterEnds(".txt");
        fdialog.addChoosableFileFilter(filt);
        fdialog.setFileFilter(filt);
        fdialog.setCurrentDirectory(new File(IJ.getDirectory("current")));

        int returnVal = JFileChooser.CANCEL_OPTION;
        if(showassave == true) returnVal = fdialog.showSaveDialog(IJ.getInstance());
        else returnVal = fdialog.showOpenDialog(IJ.getInstance());
        if(returnVal != JFileChooser.APPROVE_OPTION) return null;

        //get selected file(s) or files from single selected directory
        File f = fdialog.getSelectedFile();
        OpenDialog.setDefaultDirectory(f.getParent());

        return f;
        
    }
    
    //string representation of this parameter
    public String Write(){
        
        String out = id + "\t";
        
        int i;
        
        switch(type){
            
            case VALUES:
                for(i = 0; i < comp.length; i++){
                    JTextField tf = (JTextField)comp[i];
                    out += tf.getText() + ",";
                }
                break;
                
            case RADIO: 
                for(i = 0; i < comp.length; i++){
                    JRadioButton rb = (JRadioButton)comp[i];
                    if(rb.isSelected()) out += "true,";
                    else out += "false,";
                }
                break;
                
            case RADIO_EX:    
                for(i = 0; i < comp.length; i++){
                    JRadioButton rb = (JRadioButton)comp[i];
                    if(rb.isSelected()){
                        out += Integer.toString(i);
                        break;
                    }             
                }
                break;
                
            case DROPDOWN:
                JComboBox cb = (JComboBox)comp[0];
                out += Integer.toString(cb.getSelectedIndex());
                break;
            
        }
        
        out += "\r\n";
   
        return out;
        
    }
    
    //read string representation of parameter
    public void Read(String input){

        //check null
        if((input == null) || (input.isEmpty())) return;

        //matching id?
        String[] read = input.split("\t");
        if((read.length < 2) || (read[0].matches(id) == false)) return;
      
        int i;
        String[] vals;
        
        switch(type){
            
            case VALUES:
                vals = read[1].split(",");
                for(i = 0; (i < vals.length) && (i < comp.length); i++){
                    JTextField tf = (JTextField)comp[i];
                    tf.setText(vals[i]);
                }
                break;
                
            case RADIO:
                vals = read[1].split(",");
                for(i = 0; (i < vals.length) && (i < comp.length); i++){
                    JRadioButton rb = (JRadioButton)comp[i];
                    if(vals[i].matches("true")) rb.setSelected(true);
                    else rb.setSelected(false);
                }
                break;
                    
            case RADIO_EX:
                try{
                    i = Integer.parseInt(read[1]);
                    if(i < comp.length){
                       JRadioButton rb = (JRadioButton)comp[i];
                       rb.setSelected(true);
                    }
                }catch(Exception ex){}
                break;
                
                
            case DROPDOWN:
                try{
                    i = Integer.parseInt(read[1]);
                    JComboBox cb = (JComboBox)comp[0];
                    if(i < cb.getItemCount()) cb.setSelectedIndex(i);
                }catch(Exception ex){}
                break;
  
        }
   
    }

    ///////////////////////////////////////////////////////////////////////////
    //GROUP PANEL
    
    public static JPanel GetCentredPanel(int pw, int ph, Param ... plist){
        JPanel pnBig = new JPanel();
        pnBig.add(GetBoxPanel(pw, ph, plist), BorderLayout.CENTER);
        return pnBig;
    }
    
    public static JPanel GetBoxPanel(int pw, int ph, Param[] plist){
        
        JPanel pnMain = new JPanel(new GridLayout(plist.length, 1));
        JPanel pnPara; JPanel pnPval;
        
        for(Param p : plist){
            
            //parameter panel
            pnPara = new JPanel();
            pnPara.setLayout(new BoxLayout(pnPara, BoxLayout.X_AXIS));
            
            //glue
            pnPara.add(Box.createHorizontalGlue());
            
            //label
            pnPara.add(new JLabel(p.GetLabel() + "   "));
            
            //function button
            if(p.but != null){
                p.but.setPreferredSize(new Dimension(20, ph));
                p.but.setMaximumSize(new Dimension(20, ph));      
                pnPara.add(p.but);
            }
      
            //values
            pnPval = p.GetPanel();
            pnPval.setPreferredSize(new Dimension(pw, ph));
            pnPval.setMaximumSize(new Dimension(pw, ph));
            pnPval.setMinimumSize(new Dimension(pw, ph));
            pnPara.add(pnPval);
            
            //add to main panel
            pnMain.add(pnPara);
            
        }
       
        return pnMain;
    }
 
}
