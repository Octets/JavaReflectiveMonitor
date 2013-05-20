package ca.etsmtl.octects.visualmonitor;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;

public class MainForm {
   private JXTextField address;
   private JXTextField port;
   private JXButton btnConnect;
   private JXLabel lblPort;
   private JXLabel lblAddress;
   private JXPanel contentPanel;
   private JPanel mainPanel;
   private JXPanel headPanel;
   private JXPanel titlePanel;
   private JXPanel dataPanel;
   private JXLabel lblTitle;


   public static void main(String[] args) {
      JFrame frame = new JFrame("MainForm");
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (ClassNotFoundException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (InstantiationException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (IllegalAccessException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (UnsupportedLookAndFeelException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }

      for(UIManager.LookAndFeelInfo lookAndFeelInfo : UIManager.getInstalledLookAndFeels()) {
         System.out.println(lookAndFeelInfo.getName());
      }



      frame.setContentPane(new MainForm().mainPanel);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
   }
}
