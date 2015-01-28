//             //                           //
//   To a T    //     Elliot K. Goldman     //
//             //                           //


import javax.swing.*;    // JPanel
import java.awt.*;       // Graphics, Graphics2D, Color
import java.awt.geom.*;  // Ellipse2D
import java.awt.event.*; // MouseListener, MouseMotionListener, MouseEvent, KeyListener, KeyEvent
import javax.swing.event.*; 
import javax.swing.undo.*;
import javax.swing.text.*;
import javax.swing.border.EmptyBorder;
import java.awt.print.*; //PrinterException
import java.awt.image.*; //BufferedImage
import java.io.*; //File
import javax.imageio.*;
import java.lang.reflect.*; //For "Method" (Window Transparency)

public class TextUI extends JFrame{
  
  //Action Listeners
  //For overlay so that the overlay never overlaps the main window
  private class WindowO extends WindowAdapter{
    public void windowActivated(WindowEvent e){
      setAlwaysOnTop(true);
    }
    public void windowDeactivated(WindowEvent e){
      setAlwaysOnTop(false);
    }
  }
  
  //For fading the window in
  private class WindowL extends WindowAdapter{
    private int alpha=0;
    private Timer fadeIn; //For gradual fade-in
    

    public void windowClosing(WindowEvent e){
      if(hasSaved){
       fileManager.close(textArea); 
      }
      System.out.print("Window Closing...\n");
    }
    public void windowActivated(WindowEvent e){
      //Text and caret Fade In
      ActionListener caretFadeIn = new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          Color currentShade = new Color(0, 0, 0, alpha);
          textArea.setCaretColor(currentShade);
          textArea.setForeground(currentShade);
          textArea.updateUI();
          alpha++;
          if(alpha>=255){
            alpha=0;
            fadeIn.stop();
          }
        }
      };
      if(fadeIn==null){ //For first time running
        fadeIn = new Timer(5, caretFadeIn);
      }
      if(!fadeIn.isRunning()){//To avoid repeated calls
        fadeIn = new Timer(5, caretFadeIn);
      }
      fadeIn.start();
      textArea.requestFocus();
    }
    public void windowDeactivated(WindowEvent e){
      Color currentShade = new Color(0, 0, 0, 0);
      textArea.setCaretColor(currentShade);
      textArea.setForeground(currentShade);
    }
    public void windowDeiconified(WindowEvent e){//For un-minimizing from a fullscreen state
      if(fullScreen){
        setExtendedState(JFrame.MAXIMIZED_BOTH);
      }
      if(overlayOn && getExtendedState()!=JFrame.MAXIMIZED_BOTH){ //So that it doesn't turn on if fullscreen, but does if it's not
        overlay.setVisible(true);
      }
    }
  }
  
  //For marking changes in the document
  private class DocumentListen implements DocumentListener {
    public void insertUpdate(DocumentEvent e) {
      //Update charachter and word count
      charachterCount++;
      String countString = String.format("%d/%d", charachterCount, wordCount());
      count.setText(countString);
      
      saveLabel.setIcon(new ImageIcon(notSaved)); //Change Save indicator
      repaint();
      changed = true;
      hasSaved = false;
    }
    public void removeUpdate(DocumentEvent e) {
      //Update charachter and word count
      charachterCount--;
      String countString = String.format("%d/%d", charachterCount, wordCount());
      count.setText(countString);
      
      saveLabel.setIcon(new ImageIcon(notSaved)); //Change Save indicator
      repaint();
      changed = true;
      hasSaved = false;
    }
    public void changedUpdate(DocumentEvent e) {}
  }

  //For both bottom info/control panel and making the window draggable
  private class MouseInformant extends MouseAdapter{
    private Point firstPoint;
    public void mousePressed(MouseEvent e){
      //Only draggable by info bar and if not maximized
      if(info.isVisible() && getExtendedState()!=JFrame.MAXIMIZED_BOTH){
        //Get initial position
        firstPoint = e.getPoint();
      }
    }
    public void mouseDragged(MouseEvent e){
      //Only draggable by info bar and if not maximized
      if(info.isVisible() && getExtendedState()!=JFrame.MAXIMIZED_BOTH){
        //Initial location
        int windowX = getLocation().x;
        int windowY = getLocation().y;
        
        //Amount Moved
        int movedX = e.getX() - firstPoint.x;
        int movedY = e.getY() - firstPoint.y;
        
        //set X and Y
        int x = windowX + movedX;
        int y = windowY + movedY;
        setLocation(x, y);
      }
    }
    public void mouseMoved(MouseEvent e){
      //If mouse is at bottom of the screen and info bar isn't already visible
      //Happens if already over info so that it doesn't flicker
      //Also relative to screen so it isn't affected by JScrollbar
      if(e.getYOnScreen()>=(getLocationOnScreen().y+getHeight())-20 && !info.isVisible() || e.getSource() == info){
        info.setVisible(true);
      }else{
        info.setVisible(false);
      }
    }
  }
  
  //Instance Variables
  private JTextArea textArea; //Main text Area
  private JPanel info; //Bottom info/drag panel
  private UndoManager undo;
  private DocumentListen changeListener;
  private FileManager fileManager; //Handles save, printing, opening, autosave, etc.
  private boolean changed; //To keep track of document changes
  private boolean hasSaved; //To keep track of if the document has been saved for when it closes
  private Timer autoSaveTimer;
  private JLabel count; //For charachter and word count
  private int charachterCount;
  private int wordCount;
  //For Save indicator
  private BufferedImage save;
  private BufferedImage notSaved;
  private JLabel saveLabel;
  private JFrame overlay; //Background transparent overlay
  private boolean fullScreen; //To keep track of if the application is in fullscreen mode for when it's minimized
  private boolean overlayOn; //To keep track of if the application has the overlay on for when it's minimized

  
  //Constructor
  public TextUI(){
    fileManager = new FileManager();
    hasSaved = true; //So it won't ask to save if the user hasn't typed anything
    fullScreen = false;
    overlayOn = true;
    
    
    //Formating Text Area
    textArea = new JTextArea("");
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setFont(new Font("Avenir", Font.PLAIN, 14));
    textArea.setTabSize(4);
    textArea.setBorder(new EmptyBorder(10, 10, 10, 10));
    //Change Caret Settings
    DefaultCaret caret = new DefaultCaret();
    caret.setBlinkRate(0);
    textArea.setCaret(caret);
    JScrollPane scroll = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setPreferredSize(new Dimension(600, 500));
    scroll.setBorder(BorderFactory.createEmptyBorder());
    add(scroll,BorderLayout.CENTER);
    
    
    
    
    //Auto Save (every 2.5 minutes if content has changed)
    changed = false;
    changeListener = new DocumentListen();
    textArea.getDocument().addDocumentListener(changeListener);
    autoSaveTimer = new Timer(150000, new AbstractAction() {@Override
      public void actionPerformed(ActionEvent e) {
      if(changed){
        fileManager.autoSave(textArea);
        changed=false;
        saveLabel.setIcon(new ImageIcon(save)); //Change Save indicator
      }
    }
    });
    autoSaveTimer.start();
    
    
    
    //UndoManager
    undo = new UndoManager();
    textArea.getDocument().addUndoableEditListener(undo);
    undo.setLimit(500);
    InputMap inputMap = textArea.getInputMap(JComponent.WHEN_FOCUSED);
    ActionMap actionMap = textArea.getActionMap();
    
    
    //JPanel for FullScreen/WordCount/Autosave indicator/Exit
    info = new JPanel(new BorderLayout());
    info.setPreferredSize(new Dimension(600, 20));
    info.setBackground(new Color(200, 200, 200));
    
    JPanel appButtons = new JPanel(); //for app buttons x - +
    JPanel infoBit = new JPanel();
    infoBit.setBorder(new EmptyBorder(-2, 0, 0, 0));
    infoBit.setBackground(new Color(200, 200, 200));
    
    //Load images and add as JLabels or JButtons
    try{     
      //Close Button
      BufferedImage ex = ImageIO.read(new File("Art Files/x.png"));
      JButton exButton = new JButton(new ImageIcon(ex)); 
      exButton.setBorderPainted(false); //Remove all interior except icon
      exButton.setContentAreaFilled(false); 
      exButton.setFocusPainted(false); 
      exButton.setOpaque(false);
      exButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); //Set cursor
      exButton.setBorder(null); //Remove all padding and border
      exButton.setBorderPainted(false);
      exButton.setMargin(new Insets(0,0,0,0));
      appButtons.add(exButton);
      exButton.addActionListener(new ActionListener() {@Override
        public void actionPerformed(ActionEvent evt) {
        if(hasSaved){  
          System.exit(0);
        }else{
         int n = fileManager.close(textArea); 
         if(n!=-1){
          System.exit(0); 
         }else{
           //Save did not work, go back to document
           //System.exit(0);
         }
        }
      }
      });
      
      //Minimize Button
      BufferedImage minus = ImageIO.read(new File("Art Files/-.png"));
      JButton minusButton = new JButton(new ImageIcon(minus));
      minusButton.setBorderPainted(false); //Remove all interior except icon
      minusButton.setContentAreaFilled(false); 
      minusButton.setFocusPainted(false); 
      minusButton.setOpaque(false);
      minusButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); //Set cursor
      minusButton.setBorder(null); //Remove all padding and border
      minusButton.setBorderPainted(false);
      minusButton.setMargin(new Insets(0,0,0,0));
      appButtons.add(minusButton);
      minusButton.addActionListener(new ActionListener() {@Override
        public void actionPerformed(ActionEvent evt) {
          overlay.setVisible(false);
          setExtendedState(JFrame.ICONIFIED);
      }
      });
      
      //FullScreen Button
      BufferedImage plus = ImageIO.read(new File("Art Files/+.png"));
      JButton plusButton = new JButton(new ImageIcon(plus)); 
      plusButton.setBorderPainted(false); //Remove all interior except icon
      plusButton.setContentAreaFilled(false); 
      plusButton.setFocusPainted(false); 
      plusButton.setOpaque(false);
      plusButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); //Set cursor
      plusButton.setBorder(null); //Remove all padding and border
      plusButton.setBorderPainted(false);
      plusButton.setMargin(new Insets(0,0,0,0));
      appButtons.add(plusButton);
      plusButton.addActionListener(new ActionListener() {@Override
        public void actionPerformed(ActionEvent evt) {
           if(getExtendedState()==JFrame.NORMAL){
             fullScreen=true;
             setExtendedState(JFrame.MAXIMIZED_BOTH);
             //Set border so text is centered more
             textArea.setBorder(new EmptyBorder(50, 300, 10, 300));
             overlay.setVisible(false); //No point of an overlay if it's fullscreen and it slows down the computer when you minimize
           }else{
             //return border to normal
             fullScreen=false;
             setExtendedState(JFrame.NORMAL);
             textArea.setBorder(new EmptyBorder(10, 10, 10, 10));
             if(overlayOn){
               overlay.setVisible(true);
             }
           }
        }
      });
      
      //Add app buttons to info bad
      info.add(appButtons, BorderLayout.WEST);
      
      //Charachter and word count
      count = new JLabel("0/0"); //Charachter Count/Word Count
      count.setFont(new Font("Avenir", Font.PLAIN, 12));
      infoBit.add(count);
      charachterCount = 0;
      wordCount = 0;
      
      /*Save label (whether or not content has been saved or auto saved
       *Label is red if content has been changed and the file hasn't been saved or autosaved
       *Remains green if current version has been saved (deliberate or autoSave) (until a new change is made) */
      save = ImageIO.read(new File("Art Files/save.png"));
      notSaved = ImageIO.read(new File("Art Files/not_saved.png"));
      saveLabel = new JLabel(new ImageIcon(save));
      infoBit.add(saveLabel);
      info.add(infoBit, BorderLayout.EAST);
      
    }catch(IOException e){
      System.err.println(e); 
    }
    add(info,BorderLayout.SOUTH);
    info.setVisible(false);
    
    
    
    
    //Handle all key codes and their corresponding actions
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "undo");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "redo");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "save");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() + KeyEvent.SHIFT_DOWN_MASK), "saveAs");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "open");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "print");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "hide");
    actionMap.put("undo", new AbstractAction() {@Override
      public void actionPerformed(ActionEvent e) {
      try {
        if (undo.canUndo()) {
          undo.undo();
        }
      } catch (CannotUndoException undoException) {}
    }
    });
    actionMap.put("redo", new AbstractAction() {@Override
      public void actionPerformed(ActionEvent e) {
      try {
        if (undo.canRedo()) {
          undo.redo();
        } 
      }catch (CannotUndoException redoException) {}
    }
    });
    actionMap.put("save", new AbstractAction() {@Override
      public void actionPerformed(ActionEvent e) {
      int n = fileManager.save(textArea);
      if(n!=-1){//Saved without error
        saveLabel.setIcon(new ImageIcon(save)); //Change Save indicator
        hasSaved = true;
      }
      repaint();
    }
    });
    actionMap.put("saveAs", new AbstractAction() {@Override
      public void actionPerformed(ActionEvent e) {
      int n = fileManager.saveAs(textArea);
      if(n!=-1){//Saved without error
        hasSaved = true;
        saveLabel.setIcon(new ImageIcon(save)); //Change Save indicator
      }
      repaint();
    }
    });
    actionMap.put("open", new AbstractAction() {@Override
      public void actionPerformed(ActionEvent e) {
      textArea.setText(fileManager.open(textArea));
    }
    });
    actionMap.put("print", new AbstractAction() {@Override
      public void actionPerformed(ActionEvent e) {
      try {
        boolean complete = textArea.print(null, null, true, null, null, false);
        if (complete) {
          System.out.println("Printing...");
        } else {
          System.out.println("Printing canceled...");
          }
      } catch (PrinterException pe) {
        System.err.println("Printer Exception: " + pe.getMessage());
      }
    }
    });
    actionMap.put("hide", new AbstractAction() {@Override //To hide or unhide overlay
      public void actionPerformed(ActionEvent e) {
      if(overlay.isVisible()){
        overlayOn = false;
        overlay.setVisible(false);
      }else{
        overlayOn = true;
        overlay.setVisible(true);
      }
    }
    });
    
    
    
    //Mouse Events
    MouseInformant mouseListener = new MouseInformant();
    textArea.addMouseListener(mouseListener);
    textArea.addMouseMotionListener(mouseListener);
    info.addMouseListener(mouseListener);
    info.addMouseMotionListener(mouseListener);
    
      
    //Window overlay behind window
    /*JFrame.setOpacity(float[from 0-1]) was added in Java 7, but current version of Dr. Java doesn't support later
     * than Java 6 when not run as a .jar and I didn't want to have to change IDEs this late in the game.
     * Future versions should implement a simpler solution using Java 7
     * [Note: Transparency code copied from Oracle's website (With slight modification)]
     */
    overlay = new JFrame();
    overlay.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    overlay.setUndecorated(true);
    overlay.setBackground(new Color(100, 100, 100));
    overlay.setDefaultLookAndFeelDecorated(true);
    //Transparency
    Window overlayWindow = new Window((Frame)overlay);
    try {
      Class<?> awtUtilitiesClass = Class.forName("com.sun.awt.AWTUtilities");
      Method mSetWindowOpacity = awtUtilitiesClass.getMethod("setWindowOpacity", Window.class, float.class);
      mSetWindowOpacity.invoke(null, (Window)overlay, Float.valueOf(0.6f));
    } catch (NoSuchMethodException ex) {
      ex.printStackTrace();
    } catch (SecurityException ex) {
      ex.printStackTrace();
    } catch (ClassNotFoundException ex) {
      ex.printStackTrace();
    } catch (IllegalAccessException ex) {
      ex.printStackTrace();
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
    } catch (InvocationTargetException ex) {
      ex.printStackTrace();
    }
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    overlay.setVisible(true);
    overlay.setExtendedState(JFrame.MAXIMIZED_BOTH);
    WindowListener window = new WindowO();
    overlay.addWindowListener(window);

    
    //Window Maintenance
    window = new WindowL();
    //setTitle("To a T");
    addWindowListener(window);
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    setUndecorated(true);
    setDefaultLookAndFeelDecorated(true);
    pack();
    setVisible(true);
    
    
  }
  
  //For counting number of words in document
  public int wordCount(){
    if(textArea.getText() == "")return 0;
    String[] words = textArea.getText().split(" ");
    //Remove any spaces or empty strings (for repeated spaces)
    int wordCount=0;
    for(String i : words){
      if(!i.equals("")){
       wordCount++; 
      }
    }
    return wordCount;
  }
  
  public static void main(String[] args) {
    TextUI tui = new TextUI();
  }
}