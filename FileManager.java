//Elliot K. Goldman
//To a T

import javax.swing.*; //JFileChooser
import java.util.*; //ArrayList, Date
import java.io.*; //File
import java.text.*; //Dateformat
import javax.swing.filechooser.*; //FileNameExtensionFilter

//Manages files for TextUI 

public class FileManager{
  
  //Intstance Variables
  private String fileName;
  private String folderNameAS; //To save folder name for autosaves
  private JFileChooser fileChooser;
  private Date lastSavedTime;
  PrintWriter out;
  
  public FileManager(){
    fileChooser = new JFileChooser();
    fileName="";
    
    //Autosave FileManagement
    File saveFolder = new File("Auto Saves");
    DateFormat dateF = new SimpleDateFormat("yyyy:MM:dd");
    Date date = new Date();
    String folderName = String.format("Auto Saves/%s", dateF.format(date));
    folderNameAS = folderName+"/";
    File saveFiles = new File(folderName);
    //Create folder for save states if one does not already exist
    if(!saveFolder.exists()){
      saveFolder.mkdir();
    }
    //Create folder for dated save states
    if(!saveFiles.exists()){
      saveFiles.mkdir();
    }
    
  }
  
  //For auto saves
  public void autoSave(JTextArea textArea){
    System.out.println("Autosaving...");
    if(!textArea.getText().equals("")){
      DateFormat dateF = new SimpleDateFormat("HH.mm.ss");
      Date date = new Date();
      String filename = String.format("%s%s.txt",folderNameAS, dateF.format(date));
      try{
        out = new PrintWriter(filename);
        textArea.write(out);
        out.close();
      }catch(FileNotFoundException e){
        System.err.println("FileNotFoundException: " + e.getMessage());
      }catch(IOException f){
        System.err.println("IOException: " + f.getMessage());
      }
    }
  }
  
  //returns 0 for completed successfully, -1 for unsuccessfully
  public int saveAs(JTextArea textArea){
    FileNameExtensionFilter filter = new FileNameExtensionFilter(".txt", ".txt");
    fileChooser.setFileFilter(filter);
    int returnVal = fileChooser.showSaveDialog(textArea);
    if(returnVal == JFileChooser.CANCEL_OPTION){
      return -1; 
    }else{
      //Add .txt if it's not already present
      fileName = fileChooser.getSelectedFile().getAbsolutePath();
      if(fileName.indexOf(".txt")==-1){ //If the user didn't select .txt
        fileName+=".txt";
      }
      File doesExist = new File(fileName);
      //Make sure File isn't a duplicate
      while(doesExist.exists()){ //Filename Already exists
        String message = String.format("%s already exists. Would you like to replace it?", fileChooser.getSelectedFile().getAbsolutePath());
        int n = JOptionPane.showOptionDialog(textArea, message, "Filename", 
                                             JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, 
                                             null, null, null);
        if(n==0){
          break;
        }else if(n==1){
          returnVal = fileChooser.showSaveDialog(textArea);
          if(returnVal == JFileChooser.APPROVE_OPTION){
            //Add .txt if it's not already present
            fileName = fileChooser.getSelectedFile().getAbsolutePath();
            if(fileName.indexOf(".txt")==-1){ //If the user didn't select .txt
              fileName+=".txt";
            }
            doesExist = new File(fileName);
          }else{
            return -1;
          }
        }else{
          return -1;
        }
      }
      //Everything is okay, save file
      try{
        out = new PrintWriter(fileName);
        textArea.write(out);
        out.close();
        return 0; //File saved correctly
      }catch(FileNotFoundException e){
        System.err.println("FileNotFoundException: " + e.getMessage());
        return -1;
      }catch(IOException f){
        System.err.println("IOException: " + f.getMessage());
        return -1;
      }
    }
  }
  
  //For deliberate saves by the user returns 0 for completed successfully, -1 for unsuccessfully
  public int save(JTextArea textArea){
    if(fileName.equals("")){
      return saveAs(textArea);
    }else{
      try{
        out = new PrintWriter(fileName);
        textArea.write(out);
        out.close();
        return 0;
      }catch(FileNotFoundException e){
        System.err.println("FileNotFoundException: " + e.getMessage());
        return -1;
      }catch(IOException f){
        System.err.println("IOException: " + f.getMessage());
        return -1;
      }
    }
  }
  
  //For when the user sloes the window, but the file isn't saved
  public int close(JTextArea textArea){
    //JOptionPane(Object message, int messageType, int optionType)
    //Show dialogue
    String message;
    if(fileName.equals("")){
      message= "Save changes to the document before closing?";
    }else{
      message = String.format("Save changes to document \"%s\" before closing?", fileName); 
    }
    Object[] possibleChoices = {"Save","Cancel", "Close without saving"};
    int n = JOptionPane.showOptionDialog(textArea, message, "Save?", 
                                         JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, 
                                         null, possibleChoices, possibleChoices[0]);
    if(n==2){ //No
      return 0; //To indicate to close
    }else if(n==1){ //Cancel
      return -1; //To indicate not to close
    }else{//Yes
      int i = save(textArea);
      if(i==-1){
       return -1; //Don't close because it wasn't saved 
      }else{
        return 1; //To indicate to save
      }
    }
  }
  
  public String open(JTextArea textArea){
    fileChooser.setAcceptAllFileFilterUsed(false);
    FileNameExtensionFilter filter = new FileNameExtensionFilter(".txt", "txt");
    if(fileChooser.getFileFilter()==null){
      fileChooser.setFileFilter(filter);
      System.out.println(fileChooser.getFileFilter().toString());
    }
    
    int returnVal = fileChooser.showOpenDialog(textArea);
    File file;
    if(returnVal == JFileChooser.APPROVE_OPTION){
      file = fileChooser.getSelectedFile();
      try{
        if(file.toString().indexOf(".txt")!=-1){
          Scanner in = new Scanner(file);
          String text="";
          while(in.hasNext()){
            text += in.nextLine();
            if(in.hasNext())text+="\n";
          }
          fileName=file.toString();
          return text;
        }else{
          return ""; 
        }
      }catch(FileNotFoundException e){
        System.err.println("FileNotFoundException: " + e.getMessage());
        return "";
      }
      
    }else{
      return "";
    }
  }
}
