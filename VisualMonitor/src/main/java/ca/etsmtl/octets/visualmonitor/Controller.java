package ca.etsmtl.octets.visualmonitor;

import ca.etsmtl.octets.appmonitoring.ClientManager;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import org.apache.log4j.Logger;

import java.io.IOException;

public class Controller {
   private static final Logger logger = Logger.getLogger(Controller.class);

   public Button btnConnect;
   public TextField txtHostname;
   public TextField txtPort;
   public ProgressIndicator loadingConnection;

   private Connector connector = new Connector();

   public void onConnect() {
      loadingConnection.setOpacity(1d);
      String hostname;
      int port;

      if(txtHostname.getText().isEmpty()) {
         hostname = txtHostname.getPromptText();
      } else {
         hostname = txtHostname.getText();
      }
      if(txtPort.getText().isEmpty()) {
         port = ClientManager.SERVER_PORT;
      } else {
         port = Integer.parseInt(txtPort.getText());
      }
      try {
         connector.connect(hostname,port);
         loadingConnection.setOpacity(0d);
      } catch (IOException e) {
         logger.error("Fail to initiate connection.",e);
      }
   }
}
