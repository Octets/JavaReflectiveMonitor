package ca.etsmtl.octets.visualmonitor.db;

import liquibase.Liquibase;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.DatabaseConnection;
import liquibase.database.core.SQLiteDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import org.apache.log4j.Logger;
import org.sqlite.SQLiteDataSource;


import java.sql.SQLException;
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
            //dbManagerSQLite = new DbManagerSQLite();
         }
         reentrantLock.unlock();
      }
      return dbManagerSQLite;
   }

   private DbManagerSQLite() {
      try {
         Class.forName("org.sqlite.JDBC");

         //DatabaseConnection databaseConnection = new JdbcConnection();
         SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
         sqLiteDataSource.setUrl(DB_PATH);

         DatabaseConnection databaseConnection = new JdbcConnection(sqLiteDataSource.getConnection());
         DatabaseChangeLog databaseChangeLog = new DatabaseChangeLog(getClass().getResource("db-changelog.xml").getPath());
         databaseConnection.setAutoCommit(true);

         SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
         sqLiteDatabase.setConnection(databaseConnection);
         sqLiteDatabase.setAutoCommit(true);
         sqLiteDatabase.checkDatabaseChangeLogTable(true,databaseChangeLog,null);
         sqLiteDatabase.checkDatabaseChangeLogLockTable();




      } catch (SQLException | ClassNotFoundException e) {
         logger.error("Error to initial the DB connection.",e);
         liquibase = null;
      } catch (LiquibaseException e) {
         logger.error("Error getting liquibase object.",e);
      }

   }

   public Liquibase getLiquibase() {
      return liquibase;
   }
}
