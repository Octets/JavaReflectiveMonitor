package ca.etsmtl.octets.visualmonitor.model.hibernate;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

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
}
