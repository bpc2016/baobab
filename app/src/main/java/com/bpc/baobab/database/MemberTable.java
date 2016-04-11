package com.bpc.baobab.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


public class MemberTable {

	  // Database table
	  public static final String TABLE_MEMBER = "member";
	  public static final String TABLE_SORTER = "sorter";
	  public static final String COLUMN_ID = "_id";
	  public static final String COLUMN_LAST = "last";
	  public static final String COLUMN_FIRST = "first";
	  public static final String COLUMN_YOB = "yob";
	  public static final String COLUMN_YOD = "yod";
	  public static final String COLUMN_GENDER = "gender";
	  public static final String COLUMN_MY_PAGES = "my_pages";
	  public static final String COLUMN_PAR_PAGES = "par_pages";
	  public static final String COLUMN_PREF = "pref";
	  public static final String COLUMN_NOTES = "comments";
	  public static final String COLUMN_REAL_ID ="r_id";
	  public static final String COLUMN_SORT_ID ="s_id";
	  public static final String COLUMN_PAIR="pair";  // keeps track of me,spouse 09/22
	  public static final String TABLE_PREFS ="prefs";
	  public static final String COLUMN_KEY="thekey";
	  public static final String COLUMN_VALUE="thevalue";
	  
	  public static final String PREF_PAGE="page_id";    // locations of 
	  public static final String PREF_PAIR="pair";


	// Database creation SQL statement
	  private static final String DATABASE_CREATE = "create table " 
	      + TABLE_MEMBER
	      + "(" 
	      + COLUMN_ID + " integer primary key autoincrement, " 
	      + COLUMN_LAST + " text not null default '', " 
	      + COLUMN_FIRST + " text not null default '', " 
	      + COLUMN_YOB + " text not null default '', " 
	      + COLUMN_YOD + " text not null default '', " 
	      + COLUMN_GENDER + " char(1) not null default 'F', " 
	      + COLUMN_MY_PAGES + " text  not null default '', " 
	      + COLUMN_PAR_PAGES + " text not null default '', "
	      + COLUMN_PREF + " text not null default '', "
	      + COLUMN_NOTES + " text not null default '' " 
	      + ");";
	  
	  // we will need this for sorting our members
	  private static final String SORTER_CREATE =" create table "
			  +  TABLE_SORTER
			  + "( "
			  + COLUMN_REAL_ID + " integer," 
			  + COLUMN_SORT_ID + " integer," 
			  + COLUMN_PAIR + " text default '')"; //  'me,you' kept here
	  
	  // for starting afresh and keeping track of who and where
	  private static final String PREFS_CREATE =" create table "
			  +  TABLE_PREFS
			  + "( "
		      + COLUMN_ID + " integer primary key autoincrement, " 
			  + COLUMN_KEY + " char(40) not null, " 
			  + COLUMN_VALUE + " text)";
		  
	
	          
	  private static final String PREFS_INIT = "insert into " 
			  + TABLE_PREFS 
			  + " values (1,'page_id','-1')";
	  
	  
	  public static void onCreate(SQLiteDatabase database) {
		    database.execSQL(DATABASE_CREATE);
		    database.execSQL(SORTER_CREATE);
		    database.execSQL(PREFS_CREATE);
		    database.execSQL(PREFS_INIT);
		 
		  };
		  
	  
	  
	  public static void onUpgrade(SQLiteDatabase database, int oldVersion,
		      int newVersion) {
		    Log.w(MemberTable.class.getName(), "Upgrading database from version "
		        + oldVersion + " to " + newVersion
		        + ", which will destroy all old data");
		    database.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMBER);
		    database.execSQL("DROP TABLE IF EXISTS " + TABLE_SORTER);
		    onCreate(database);
	  };	  
}
