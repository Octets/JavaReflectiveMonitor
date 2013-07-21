package ca.etsmtl.octets.visualmonitor.model.hibernate;

import javax.persistence.*;
import java.util.List;

/**
 * User: maxime
 * Date: 19/07/13
 * Time: 4:28 PM
 */
@Table(name = "variable")
public class VariableEntity {

   @Column(name = "id")
   private long id;

   @Column(name = "path")
   private String path;

   @JoinColumn(name = "type_id")
   private DefinitionTypeEntity typeEntity;

   @OneToMany
   @JoinTable(name = "variable_data", joinColumns =
   @JoinColumn(name = "variable_id", referencedColumnName = "id", updatable = true))
   private List<VariableDataEntity> variableDataEntities;

   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public String getPath() {
      return path;
   }

   public void setPath(String path) {
      this.path = path;
   }

   public DefinitionTypeEntity getTypeEntity() {
      return typeEntity;
   }

   public void setTypeEntity(DefinitionTypeEntity typeEntity) {
      this.typeEntity = typeEntity;
   }

   public List<VariableDataEntity> getVariableDataEntities() {
      return variableDataEntities;
   }

   public void setVariableDataEntities(List<VariableDataEntity> variableDataEntities) {
      this.variableDataEntities = variableDataEntities;
   }
}
