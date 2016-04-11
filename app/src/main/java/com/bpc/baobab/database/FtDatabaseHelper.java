package com.bpc.baobab.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class FtDatabaseHelper extends SQLiteOpenHelper {

  public static final String DATABASE_NAME = "ftree.db";
  private static final int DATABASE_VERSION = 1;


  public FtDatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }


  // Method is called during creation of the database
  @Override
  public void onCreate(SQLiteDatabase database) {
    MemberTable.onCreate(database);
    PageTable.onCreate(database);
  }



  // Method is called during an upgrade of the database,
  // e.g. if you increase the database version
  @Override
  public void onUpgrade(SQLiteDatabase database, int oldVersion,
      int newVersion) {
    MemberTable.onUpgrade(database, oldVersion, newVersion);
    PageTable.onUpgrade(database, oldVersion, newVersion);
  }

}

