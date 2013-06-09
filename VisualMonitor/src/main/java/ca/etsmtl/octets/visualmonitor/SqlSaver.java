package ca.etsmtl.octets.visualmonitor;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

public class SqlSaver {
   private SQLiteConfig sqLiteConfig = new SQLiteConfig();
   private SQLiteDataSource sqLiteDataSource;

   public SqlSaver() {

      sqLiteDataSource = new SQLiteDataSource(sqLiteConfig);

   }
}
