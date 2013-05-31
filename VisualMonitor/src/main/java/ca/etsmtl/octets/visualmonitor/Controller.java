package ca.etsmtl.octets.visualmonitor;

import ca.etsmtl.octets.appmonitoring.ClientManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import org.apache.log4j.Logger;

import java.io.IOException;

public class Controller {
   private static final Logger logger = Logger.getLogger(Controller.class);
   public Button btnConnect;
   public TextField txtHostname;
   public TextField txtPort;
   public ProgressIndicator loadingConnection;
   public GridPane mainGrid;
   public TableView<TableRowVar> tblVar;
   public TableColumn<String, TableRowVar> tbcVisibility;
   public TableColumn<String, TableRowVar> tbcName;
   public TableColumn<String, TableRowVar> tbcValue;
   public TableColumn<String, TableRowVar> tbcMode;
   public TableColumn<String, TableRowVar> tbcPath;
   private final Connector connector = new Connector();
   private final DisplayMapper displayMapper = new DisplayMapper(connector,this);
   public FlowPane varFlow;
   public Button btnRoot;
   public TableColumn<String, TableRowVar> tbcType;

   public void onConnectionClick() {
      btnConnect.setDisable(true);
      if(connector.getStates() == Connector.States.DISCONNECTED) {
         loadingConnection.setOpacity(1d);
         String hostname = null;
         int port;

         if(!txtHostname.getText().isEmpty()) {
            hostname = txtHostname.getText();
         }
         if(txtPort.getText().isEmpty()) {
            port = ClientManager.SERVER_PORT;
         } else {
            port = Integer.parseInt(txtPort.getText());
         }
         try {
            if(hostname != null) {
               connector.connect(hostname,port,this);
            }
         } catch (IOException e) {
            logger.error("Fail to initiate connection.",e);
         } finally {
            loadingConnection.setOpacity(0d);
         }
         updateBtnConnectText();
         btnConnect.setDisable(false);
      } else {
         Runnable futureTask = new Runnable() {
            @Override
            public void run() {
               updateBtnConnectText();
               btnConnect.setDisable(false);
            }
         };
         connector.disconnect(futureTask);
      }
   }

   public void updateTableLayout() {
      displayMapper.updateTableContent();
   }

   private void updateBtnConnectText() {
      if(connector.getStates() == Connector.States.CONNECTED) {
         btnConnect.setText("Disconnect");
      } else {
         btnConnect.setText("Connect");
      }
   }

   public DisplayMapper getDisplayMapper() {
      return displayMapper;
   }

   public void onRootClick() {
      displayMapper.setCurrentPath("");
   }

   public static class TableRowVar {
      private final SimpleStringProperty varName;
      private final SimpleStringProperty varValue;
      private final SimpleStringProperty varMode;
      private final SimpleStringProperty varPath;
      private final SimpleStringProperty varVisibility;
      private final SimpleStringProperty varType;
      public TableRowVar(SimpleStringProperty varName, SimpleStringProperty varValue, SimpleStringProperty varMode, SimpleStringProperty varPath, SimpleStringProperty varVisibility, SimpleStringProperty varType) {
         this.varName = varName;
         this.varValue = varValue;
         this.varMode = varMode;
         this.varPath = varPath;
         this.varVisibility = varVisibility;
         this.varType = varType;
      }

      public String getVarType() {
         return varType.get();
      }

      public SimpleStringProperty varTypeProperty() {
         return varType;
      }

      public void setVarType(String varType) {
         this.varType.set(varType);
      }

      public String getVarName() {
         return varName.get();
      }

      public void setVarName(String varName) {
         this.varName.set(varName);
      }

      public SimpleStringProperty varNameProperty() {
         return varName;
      }

      public String getVarValue() {
         return varValue.get();
      }

      public void setVarValue(String varValue) {
         this.varValue.set(varValue);
      }

      public SimpleStringProperty varValueProperty() {
         return varValue;
      }

      public String getVarMode() {
         return varMode.get();
      }

      public void setVarMode(String varMode) {
         this.varMode.set(varMode);
      }

      public SimpleStringProperty varModeProperty() {
         return varMode;
      }

      public String getVarPath() {
         return varPath.get();
      }

      public void setVarPath(String varPath) {
         this.varPath.set(varPath);
      }

      public SimpleStringProperty varPathProperty() {
         return varPath;
      }

      public String getVarVisibility() {
         return varVisibility.get();
      }

      public void setVarVisibility(String varVisibility) {
         this.varVisibility.set(varVisibility);
      }

      public SimpleStringProperty varVisibilityProperty() {
         return varVisibility;
      }

      public interface TABLE_PROPERTY {
         public String NAME = "varName";
         public String VALUE = "varValue";
         public String MODE = "varMode";
         public String PATH = "varPath";
         public String VISIBILITY = "varVisibility";
         public String TYPE = "varType";
      }
   }
   public static class PathVar {
      private String path;

      public PathVar(String path) {
         this.path = path;
      }

      public String getPath() {
         return path;
      }
      public String getName() {
         return DisplayMapper.getName(path);
      }

      @Override
      public String toString() {
         return DisplayMapper.getName(path);
      }
   }
}
