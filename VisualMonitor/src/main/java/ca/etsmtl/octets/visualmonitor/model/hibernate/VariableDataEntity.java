package ca.etsmtl.octets.visualmonitor.model.hibernate;

import javax.persistence.Column;
import javax.persistence.Table;
import java.util.Date;

/**
 * User: maxime
 * Date: 19/07/13
 * Time: 4:33 PM
 */
@Table(name = "variable_data")
public class VariableDataEntity {

   @Column(name = "id")
   private Long id;

   @Column(name = "variable_id")
   private VariableEntity variableEntity;

   @Column(name = "data_value")
   private String value;

   @Column(name = "data_time")
   private Date createdDate;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public VariableEntity getVariableEntity() {
      return variableEntity;
   }

   public void setVariableEntity(VariableEntity variableEntity) {
      this.variableEntity = variableEntity;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public Date getCreatedDate() {
      return createdDate;
   }

   public void setCreatedDate(Date createdDate) {
      this.createdDate = createdDate;
   }
}
