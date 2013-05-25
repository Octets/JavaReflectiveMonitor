package ca.etsmtl.octets.visualmonitor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

public class Main extends Application {
   private final static Logger logger = Logger.getLogger(Main.class);


   @Override
   public void start(Stage primaryStage) throws Exception {
      Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
      primaryStage.setTitle("Visual Monitor");
      primaryStage.setScene(new Scene(root, 640, 480));

      primaryStage.show();
      logger.debug("MainForm started.");
   }


   public static void main(String[] args) {
      launch(args);
   }
}
