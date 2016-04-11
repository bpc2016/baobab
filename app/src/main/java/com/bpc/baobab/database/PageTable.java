package com.bpc.baobab.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class PageTable {
	// Database table
		  public static final String TABLE_PAGE = "page";
		  public static final String COLUMN_ID = "_id";
		  public static final String COLUMN_OWNER = "owner";
		  public static final String COLUMN_SPOUSE = "spouse";
		  public static final String COLUMN_KIDS = "kids";
		  
		  public static final String COLUMN_SUMMARY = "summary"; // we will use this is a join sql

		// Database creation SQL statement
		  private static final String DATABASE_CREATE = "create table " 
		      + TABLE_PAGE
		      + "(" 
		      + COLUMN_ID + " integer primary key autoincrement, "
		      + COLUMN_OWNER + " text not null default '', "
		      +	COLUMN_SPOUSE + " text default '', "
		      + COLUMN_KIDS + " text default '' " 
		      + ");";
		  		  
		  public static void onCreate(SQLiteDatabase database) {
			    database.execSQL(DATABASE_CREATE);
			  };
			  
		  public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			      int newVersion) {
			    Log.w(PageTable.class.getName(), "Upgrading database from version "
			        + oldVersion + " to " + newVersion
			        + ", which will destroy all old data");
			    database.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGE);
			    onCreate(database);
		  };	 
}


