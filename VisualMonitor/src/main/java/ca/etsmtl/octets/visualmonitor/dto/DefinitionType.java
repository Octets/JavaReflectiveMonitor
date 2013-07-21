package ca.etsmtl.octets.visualmonitor.dto;

/**
 * User: maxime
 * Date: 20/07/13
 * Time: 1:55 PM
 */
public class DefinitionType {
   private Long id;
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
