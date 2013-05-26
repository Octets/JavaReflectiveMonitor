package ca.etsmtl.octets.visualmonitor;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import org.apache.log4j.Logger;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData.VarData;
import static ca.etsmtl.octets.visualmonitor.Controller.TableRowVar;

public class DisplayMapper implements Connector.IListenFrame {
   private final static Logger logger = Logger.getLogger(DisplayMapper.class);
   private final Hashtable<String,VarData> hashMap = new Hashtable<>(100);
   private final List<String> root = new ArrayList<>();
   private final ObservableList<TableRowVar> displayedData = FXCollections.observableArrayList();
   private String currentPath = "";

   public static String getName(final String path) {
      String name = path;
      if(name.contains(".")) {
         String temps[] = name.split("\\.");
         name = temps[temps.length -1];
      }
      return name;
   }

   public void updateTableContent(Controller fxController) {

      fxController.tblVar.setEditable(true);

      fxController.tbcVisibility.setCellValueFactory(new PropertyValueFactory<String, TableRowVar>(TableRowVar.TABLE_PROPERTY.VISIBILITY));
      fxController.tbcName.setCellValueFactory(new PropertyValueFactory<String, TableRowVar>(TableRowVar.TABLE_PROPERTY.NAME));
      fxController.tbcMode.setCellValueFactory(new PropertyValueFactory<String, TableRowVar>(TableRowVar.TABLE_PROPERTY.MODE));
      fxController.tbcPath.setCellValueFactory(new PropertyValueFactory<String, TableRowVar>(TableRowVar.TABLE_PROPERTY.PATH));
      fxController.tbcValue.setCellValueFactory(new PropertyValueFactory<String, TableRowVar>(TableRowVar.TABLE_PROPERTY.VALUE));

      fxController.tblVar.setItems(displayedData);

      fxController.tblVar.setRowFactory(new Callback<TableView<TableRowVar>, TableRow<TableRowVar>>() {
         @Override
         public TableRow<TableRowVar> call(TableView<TableRowVar> tableRowVarTableView) {
            TableRow<TableRowVar> tableRow = new TableRow<TableRowVar>();
            tableRow.setOnMouseClicked(new EventHandler<MouseEvent>() {
               @Override
               public void handle(MouseEvent mouseEvent) {
                  if(mouseEvent.getClickCount() > 1) {

                     setCurrentPath(((TableRow<TableRowVar>)mouseEvent.getSource()).getItem().getVarPath());
                  }
               }
            });

            return tableRow;
         }
      });
   }

   public String getCurrentPath() {
      return currentPath;
   }

   public void setCurrentPath(String currentPath) {
      this.currentPath = currentPath;
      displayedData.clear();
   }

   @Override
   public void onFrameReaded(VarData varData) {
      String path = varData.getPath();
      if(!path.contains(".") && !root.contains(path))
         root.add(path);
      hashMap.put(varData.getPath(), varData);
      checkForUpdate(varData);
   }

   private void checkForUpdate(VarData varData) {
      if(currentPath.equalsIgnoreCase("") && !varData.getPath().contains(".")) {
         updateVar(varData);
      } else if(varData.getPath().toLowerCase().contains(currentPath.toLowerCase()) &&
            currentPath.length() < varData.getPath().length() &&
            !varData.getPath().substring(currentPath.length()).contains(".")) {
         updateVar(varData);
      }
   }

   private void updateVar(VarData varData) {
      boolean found = false;
      for(TableRowVar tableRowVar : displayedData) {
         if(tableRowVar.getVarPath().equalsIgnoreCase(varData.getPath())){
            found = true;

            tableRowVar.setVarName(getName(varData.getPath()));
            tableRowVar.setVarPath(varData.getPath());
            tableRowVar.setVarValue(varData.getData().getValue());
            tableRowVar.setVarMode("");
            tableRowVar.setVarVisibility("");
         }
      }
      if(!found) {
         displayedData.add(new TableRowVar(
               new SimpleStringProperty(getName(varData.getPath())),
               new SimpleStringProperty(varData.getData().getValue()),
               new SimpleStringProperty(""),
               new SimpleStringProperty(varData.getPath()),
               new SimpleStringProperty("")));
      }
   }
}
