package ca.etsmtl.octets.visualmonitor;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData.VarData;
import static ca.etsmtl.octets.visualmonitor.Controller.PathVar;
import static ca.etsmtl.octets.visualmonitor.Controller.TableRowVar;

public class DisplayMapper implements Connector.IListenFrame {
   private final static Logger logger = Logger.getLogger(DisplayMapper.class);
   private final Hashtable<String,VarData> hashMap = new Hashtable<>(100);
   private final List<String> root = new ArrayList<>();
   private final ObservableList<TableRowVar> displayedData = FXCollections.observableArrayList();

   private final Controller controller;

   private String currentPath = "";

   public DisplayMapper(Controller controller) {
      this.controller = controller;
   }

   public static String getName(final String path) {
      String name = path;
      if(name.contains(".")) {
         String temps[] = name.split("\\.");
         name = temps[temps.length -1];
      }
      return name;
   }

   public void updateTableContent() {

      controller.tblVar.setEditable(true);

      controller.tbcVisibility.setCellValueFactory(new PropertyValueFactory<String, TableRowVar>(TableRowVar.TABLE_PROPERTY.VISIBILITY));
      controller.tbcName.setCellValueFactory(new PropertyValueFactory<String, TableRowVar>(TableRowVar.TABLE_PROPERTY.NAME));
      controller.tbcMode.setCellValueFactory(new PropertyValueFactory<String, TableRowVar>(TableRowVar.TABLE_PROPERTY.MODE));
      controller.tbcPath.setCellValueFactory(new PropertyValueFactory<String, TableRowVar>(TableRowVar.TABLE_PROPERTY.PATH));
      controller.tbcValue.setCellValueFactory(new PropertyValueFactory<String, TableRowVar>(TableRowVar.TABLE_PROPERTY.VALUE));
      controller.tbcType.setCellValueFactory(new PropertyValueFactory<String, TableRowVar>(TableRowVar.TABLE_PROPERTY.TYPE));

      controller.tblVar.setItems(displayedData);

      controller.tblVar.setRowFactory(new Callback<TableView<TableRowVar>, TableRow<TableRowVar>>() {
         @Override
         public TableRow<TableRowVar> call(TableView<TableRowVar> tableRowVarTableView) {
            TableRow<TableRowVar> tableRow = new TableRow<>();
            tableRow.setOnMouseClicked(new EventHandler<MouseEvent>() {
               @Override
               public void handle(MouseEvent mouseEvent) {
                  if(mouseEvent.getClickCount() > 1) {
                     setCurrentPath(((TableRowVar) ((TableRow) mouseEvent.getSource()).getItem()).getVarPath());
                     controller.getConnector().requestPath(getCurrentPath());
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
      updateCurrentPath();
      updatePathFlowPanel();
   }

   private void updateCurrentPath() {
      for(VarData varData : hashMap.values()) {
         checkForUpdate(varData);
      }
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
            currentPath.length() + 1 < varData.getPath().length() &&
            !varData.getPath().substring(currentPath.length() + 1).contains(".")) {
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
               new SimpleStringProperty(""),
               new SimpleStringProperty(varData.getType().getName())));
      }
   }

   public void updatePathFlowPanel() {
      ObservableList<Node> nodes = controller.varFlow.getChildren();

      nodes.clear();
      nodes.add(controller.btnRoot);
      String[] pathList = currentPath.split("\\.");

      /*EventHandler<MouseEvent> eventEventHandler = new EventHandler<MouseEvent>() {
         @Override
         public void handle(MouseEvent mouseEvent) {
            setCurrentPath(((PathVar) ((ChoiceBox) mouseEvent.getSource()).getValue()).getPath());
         }
      };*/

      ChangeListener<PathVar> changeListener = new ChangeListener<PathVar>() {
         @Override
         public void changed(ObservableValue<? extends PathVar> observableValue, PathVar pathVar, PathVar pathVar2) {
            setCurrentPath(observableValue.getValue().getPath());
         }
      };

      if(!currentPath.equalsIgnoreCase("") && pathList.length > 0) {
         ChoiceBox<PathVar> choiceRootFirst = new ChoiceBox<>();
         nodes.add(choiceRootFirst);
         for(String rootPath : root) {
            PathVar pathVar = new PathVar(rootPath);
            choiceRootFirst.getItems().add(pathVar);
            if(rootPath.equals(pathList[0])) {
               choiceRootFirst.setValue(pathVar);
            }
         }
         choiceRootFirst.getSelectionModel().selectedItemProperty().addListener(changeListener);
         //choiceRootFirst.setOnMouseReleased(eventEventHandler);
         //choiceRootFirst.setOnMouseClicked(eventEventHandler);
      }

      String fullPath;
      if(pathList.length > 0 && !pathList[0].equals("")) {
         fullPath = pathList[0];
         {
            ChoiceBox<PathVar> choiceBox = new ChoiceBox<>();
            for(PathVar pathVar : getNameFromPath(fullPath)) {
               choiceBox.getItems().add(pathVar);
               if(pathList.length >1 && pathVar.getName().equals(pathList[1])) {
                  choiceBox.setValue(pathVar);
               }
            }
            if(choiceBox.getItems().size() > 0) {
               nodes.add(choiceBox);
            }
            choiceBox.getSelectionModel().selectedItemProperty().addListener(changeListener);
            //choiceBox.setOnMouseReleased(eventEventHandler);
         }
         for(int i = 1; i < pathList.length -1; ++i) {
            fullPath += "." + pathList[i];
            ChoiceBox<PathVar> choiceBox = new ChoiceBox<>();
            for(PathVar pathVar : getNameFromPath(fullPath)){
               choiceBox.getItems().add(pathVar);
               if(i+1 < pathList.length && pathVar.getName().equals(pathList[i+1])) {
                  choiceBox.setValue(pathVar);
               }
            }
            if(choiceBox.getItems().size() > 0) {
               nodes.add(choiceBox);
            }
            choiceBox.getSelectionModel().selectedItemProperty().addListener(changeListener);
            //choiceBox.setOnMouseReleased(eventEventHandler);
         }
      }
   }

   public List<PathVar> getNameFromPath(String current) {
      List<PathVar> pathVars = new ArrayList<>();
      Enumeration<String> enumeration = hashMap.keys();
      while(enumeration.hasMoreElements()) {
         String path = enumeration.nextElement();
         if(isFromPath(path,current)) {
            pathVars.add(new PathVar(path));
         }
      }

      return pathVars;
   }

   private boolean isFromPath(String fullPath, String checkPath) {
      return fullPath.contains(checkPath) &&
            checkPath.length() + 1 < fullPath.length() &&
            !fullPath.substring(checkPath.length() + 1).contains(".");
   }
}
