package ca.etsmtl.octets.visualmonitor;

import ca.etsmtl.octets.visualmonitor.db.DbManagerSQLite;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

public class Main extends Application {
   private final static Logger logger = Logger.getLogger(Main.class);

   //private DbManagerSQLite dbManagerSQLite = DbManagerSQLite.getInstance();

   public Main() {

   }

   @Override
   public void start(Stage primaryStage) throws Exception {
      Parent root = FXMLLoader.load(getClass().getResource("main_form.fxml"));
      primaryStage.setTitle("Visual Monitor");
      primaryStage.setScene(new Scene(root, 640, 480));

      primaryStage.show();
      logger.debug("MainForm started.");
      DbManagerSQLite.getInstance();
      //dbManagerSQLite.getLiquibase().getDatabase().requiresUsername();
   }


   public static void main(String[] args) {
      launch(args);
   }
}
