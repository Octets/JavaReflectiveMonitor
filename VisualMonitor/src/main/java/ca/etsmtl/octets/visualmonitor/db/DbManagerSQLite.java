package ca.etsmtl.octets.visualmonitor.db;

import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.core.SQLiteDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import org.apache.log4j.Logger;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class DbManagerSQLite {

   private static final Logger logger = Logger.getLogger(DbManagerSQLite.class);
   private static final String DB_PATH = "jdbc:sqlite:data.db";

   private Liquibase liquibase;

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

         DatabaseChangeLog databaseChangeLog = new DatabaseChangeLog(getClass().getResource("dbchangelog.xml").getPath());


         SQLiteDatabase database = new SQLiteDatabase();
         database.setConnection(databaseConnection);
         database.checkDatabaseChangeLogLockTable();
         database.checkDatabaseChangeLogTable(true,databaseChangeLog,new String[]{"RElEASE"});
         database.commit();


         //liquibase = new Liquibase(getClass().getResource("db-changelog.xml").getPath(),new FileSystemResourceAccessor(),databaseConnection);
         //liquibase.getDatabase().checkDatabaseChangeLogLockTable();
         //liquibase.getDatabase().checkDatabaseChangeLogTable(true,databaseChangeLog, new String[]{ "RELEASE" });
         //liquibase.listUnrunChangeSets("RELEASE");
         //liquibase.update("RELEASE");

//         DatabaseChangeLog databaseChangeLog = new DatabaseChangeLog(getClass().getResource("db-changelog.xml").getPath());
//
//         SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
//         sqLiteDatabase.setConnection(databaseConnection);
//         sqLiteDatabase.setAutoCommit(true);
//         sqLiteDatabase.checkDatabaseChangeLogTable(true,databaseChangeLog,null);
//         sqLiteDatabase.checkDatabaseChangeLogLockTable();




      } catch (ClassNotFoundException e) {
         logger.error("Error to initial the DB connection.",e);
         liquibase = null;
      } catch (LiquibaseException e) {
         logger.error("Error getting liquibase object.",e);
      } catch (SQLException e) {
         logger.error("Error creater databaseConnection", e);
      }

   }

   public Liquibase getLiquibase() {
      return liquibase;
   }
}
