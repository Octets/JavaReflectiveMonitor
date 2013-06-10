package ca.etsmtl.octets.visualmonitor.db;

import liquibase.Liquibase;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.DatabaseConnection;
import liquibase.database.core.SQLiteDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.resource.FileSystemResourceAccessor;
import org.apache.log4j.Logger;


import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;



public class DbManagerSQLite {

   private static final Logger logger = Logger.getLogger(DbManagerSQLite.class);
   private static final String DB_PATH = "jdbc:sqlite:data.db";

   private static DbManagerSQLite dbManagerSQLite = null;
   private static ReentrantLock reentrantLock = new ReentrantLock();

   public static DbManagerSQLite getInstance() {
      if(dbManagerSQLite == null) {
         reentrantLock.lock();
         if(dbManagerSQLite == null) {
            dbManagerSQLite = new DbManagerSQLite();
         }
         reentrantLock.unlock();
      }
      return dbManagerSQLite;
   }

   private DbManagerSQLite() {
      try {
         Class.forName("org.sqlite.JDBC");

         DatabaseConnection databaseConnection = new JdbcConnection(DriverManager.getConnection(DB_PATH));
         FileSystemResourceAccessor fileSystemResourceAccessor = new FileSystemResourceAccessor();
         DatabaseChangeLog databaseChangeLog = new XMLChangeLogSAXParser().parse(getClass().getResource("db-changelog.xml").getPath(), new ChangeLogParameters(), fileSystemResourceAccessor);

         SQLiteDatabase database = new SQLiteDatabase();
         database.setConnection(databaseConnection);
         database.checkDatabaseChangeLogLockTable();
         database.checkDatabaseChangeLogTable(true,databaseChangeLog,"RELEASE");

         Liquibase liquibase = new Liquibase(databaseChangeLog.getFilePath(), fileSystemResourceAccessor, database);
         liquibase.update("RELEASE");
         database.commit();

      } catch (ClassNotFoundException e) {
         logger.error("Error to initial the DB connection.",e);
      } catch (LiquibaseException e) {
         logger.error("Error getting liquibase object.",e);
      } catch (SQLException e) {
         logger.error("Error creater databaseConnection", e);
      }

   }
}
