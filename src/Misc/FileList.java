/********************************************************************************
/////////////////////////////////////////////////////////////////////////////////
April 2017, Sean Parsons

file listS

/////////////////////////////////////////////////////////////////////////////////
********************************************************************************/
package Misc;

import ij.IJ;
import ij.io.OpenDialog;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class FileList {
    
    ///////////////////////////////////////////////////////////////////////////////
    //OPEN DIALOG
    
    public static File[] Openlist(boolean allowmulti, boolean fileonly, String ... ends){
        
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
            FileFilter[] filts = fdialog.getChoosableFileFilters();
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
  
    ///////////////////////////////////////////////////////////////////////////////
    //FILE LIST EXPANSION
    
    //take the argument array of File objects, containing both files and folders, 
    //and return a list containing just those files and files from directories
    public static ArrayList<File> GetExpandedFileList(File[] files){   
        ArrayList<File> outlist = new ArrayList<>();
        SearchList(files, outlist);
        return outlist;
    }
    
    //search array for files and directories: if file add to list; if directory, expand
    private static void SearchList(File[] files,  ArrayList<File> list){
        
        //loop through
        for(File x : files){
            //file
            if(x.isFile() == true) list.add(x);
            //directory
            if(x.isDirectory() == true) ExpandDirectory(x, list);
        }

    }
    
    //expand the argument directory into the list
    private static void ExpandDirectory(File dir, ArrayList<File> list){
        if(dir.isDirectory()) SearchList(dir.listFiles(), list);               
    }

    
}
