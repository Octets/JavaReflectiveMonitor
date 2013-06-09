package ca.etsmtl.octets.visualmonitor.db;

import liquibase.Liquibase;
import liquibase.database.DatabaseConnection;
import liquibase.database.core.SQLiteDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CommandLineParsingException;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.Main;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import org.apache.log4j.Logger;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteJDBCLoader;

import java.io.IOException;
import java.sql.DriverManager;
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

         liquibase = new Liquibase(getClass().getResource("liquibase-changelog.xml").getPath(), new ClassLoaderResourceAccessor(getClass().getClassLoader()),databaseConnection);
         liquibase.getDatabase().commit();
         liquibase.update(null);

//         SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
//         sqLiteDataSource.setUrl(DB_PATH);
//         sqLiteDataSource.getConnection().commit();
//
//
//
//         SQLiteDatabase database = new SQLiteDatabase();
//         database.setConnection(databaseConnection);
//
//         SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
//         sqLiteDataSource.
//
//
//         org.sqlite.SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
//         sqLiteDataSource.setConfig();
//
//         Main.main(new String[] {
//                 "--driver=org.sqlite.JDBC",
//                 "--url=\"" + DB_PATH +"\"",
//                 "--changeLogFile="+ this.getClass().getResource("liquibase-changelog.xml").getPath(),
//                 "migrate"});


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
