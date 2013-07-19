package ca.etsmtl.octets.visualmonitor.model.hibernate;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Table(name = "type_definition")
public class DefinitionTypeEntity {

   @Column(name = "id")
   private Long id;

   @Column(name = "classpath")
   private String classpath;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public String getClasspath() {
      return classpath;
   }

   public void setClasspath(String classpath) {
      this.classpath = classpath;
   }
}