package com.bpc.baobab.contentprovider;


import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.bpc.baobab.database.FtDatabaseHelper;
import com.bpc.baobab.database.MemberTable;
import com.bpc.baobab.database.PageTable;

import java.util.HashMap;

public class FtContentProvider extends ContentProvider {

    // database
    private FtDatabaseHelper database;

    // Used for the UriMacher
    private static final int MEMBERS = 10;
    private static final int MEMBER_ID = 20;
    private static final int PARENT_ID = 21;
    private static final int SPOUSES = 22;
    private static final int OWNERS = 23;
    private static final int SORTER1 = 24;
    private static final int SORTER2 = 25;
    private static final int PREFS = 26;
    private static final int EMPTY = 27;  // really sample of rawquery
    private static final int PAGESIZE = 28;  // really sample of rawquery
    private static final int MEMBERSIZE = 29;  // really sample of rawquery
    private static final int PAGES = 30;
    private static final int PAGE_ID = 40;
    private static final int PAGE_SORT_ID = 41;

    private static final String AUTHORITY = "com.bpc.baobab.contentprovider";

    private static final String M_BASE_PATH = "member";
    public static final Uri MEMBER_URI = Uri.parse("content://" + AUTHORITY
            + "/" + M_BASE_PATH);

    //    public static final String MEMBER_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
//            + "/members";
//    public static final String MEMBER_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
//            + "/member";
    private static final String J_BASE_PATH = "join";
    public static final Uri PARENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + J_BASE_PATH);

    private static final String SP_BASE_PATH = "spouses";
    public static final Uri SPOUSES_URI = Uri.parse("content://" + AUTHORITY
            + "/" + SP_BASE_PATH);

    private static final String O_BASE_PATH = "owners";
    public static final Uri OWNERS_URI = Uri.parse("content://" + AUTHORITY
            + "/" + O_BASE_PATH);

    private static final String S_BASE_PATH = "sort1";
    // is the same as member_uri
//    public static final Uri SORTER_URI = Uri.parse("content://" + AUTHORITY
//            + "/" + S_BASE_PATH);
//    public static final String SORTER_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
//            + "/sort";
    private static final String S2_BASE_PATH = "ssort";
    // is the same as member_uri
//    public static final Uri SORTER2_URI = Uri.parse("content://" + AUTHORITY
//            + "/" + S2_BASE_PATH);
    // for detecting empty db
    private static final String E_BASE_PATH = "empty";
    public static final Uri EMPTY_DB_URI = Uri.parse("content://" + AUTHORITY
            + "/" + E_BASE_PATH);
    ;

    private static final String P_BASE_PATH = "page";
    public static final Uri PAGE_URI = Uri.parse("content://" + AUTHORITY
            + "/" + P_BASE_PATH);

    private static final String PS_BASE_PATH = "pagesort";
    public static final Uri PAGE_SORT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + PS_BASE_PATH);

    public static final String PAGE_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/pages";
    public static final String PAGE_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/page";

    private static final String PR_BASE_PATH = "prefs";
    public static final Uri PREFS_URI = Uri.parse("content://" + AUTHORITY
            + "/" + PR_BASE_PATH);

    public static final String THE_DATE = "the_date";

    private static final String SZ_BASE_PATH = "size";
    public static final Uri SIZE_URI = Uri.parse("content://" + AUTHORITY
            + "/" + SZ_BASE_PATH);

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, M_BASE_PATH, MEMBERS);
        sURIMatcher.addURI(AUTHORITY, M_BASE_PATH + "/#", MEMBER_ID);
        sURIMatcher.addURI(AUTHORITY, J_BASE_PATH + "/#", PARENT_ID);
        sURIMatcher.addURI(AUTHORITY, SP_BASE_PATH, SPOUSES);
        sURIMatcher.addURI(AUTHORITY, O_BASE_PATH, OWNERS);
        sURIMatcher.addURI(AUTHORITY, S_BASE_PATH, SORTER1);
        sURIMatcher.addURI(AUTHORITY, S2_BASE_PATH, SORTER2);
        sURIMatcher.addURI(AUTHORITY, E_BASE_PATH, EMPTY);
        sURIMatcher.addURI(AUTHORITY, P_BASE_PATH, PAGES);
        sURIMatcher.addURI(AUTHORITY, P_BASE_PATH + "/#", PAGE_ID);
        sURIMatcher.addURI(AUTHORITY, PS_BASE_PATH + "/#", PAGE_SORT_ID);
        sURIMatcher.addURI(AUTHORITY, PR_BASE_PATH, PREFS);
        sURIMatcher.addURI(AUTHORITY, SZ_BASE_PATH + "/"+PageTable.TABLE_PAGE, PAGESIZE);
        sURIMatcher.addURI(AUTHORITY, SZ_BASE_PATH + "/"+MemberTable.TABLE_MEMBER, MEMBERSIZE);
    }

    @Override
    public boolean onCreate() {
        database = new FtDatabaseHelper(getContext());
        return false;
    }



    public Cursor squery(Uri uri, String[] projection, String selection,
                         String[] selectionArgs, String sortOrder){

        // also using the following projection:
        HashMap<String,String> SEARCH_PROJECTION_MAP = new HashMap<String, String>();
        SEARCH_PROJECTION_MAP.put( PageTable.COLUMN_ID, PageTable.COLUMN_ID + " as _pid" );
        SEARCH_PROJECTION_MAP.put( MemberTable.COLUMN_ID , MemberTable.COLUMN_ID + " as _id" );

        // so that, presumably, in a call we can set
        // String[] projection = new String[] { "_pid" };


        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        //here is where we invoke the use of the projectionmap
        queryBuilder.setProjectionMap( SEARCH_PROJECTION_MAP );

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {

            case SPOUSES:
                queryBuilder.setTables(PageTable.TABLE_PAGE
                        + " inner join "
                        + MemberTable.TABLE_MEMBER
                        + " on "
                        + PageTable.TABLE_PAGE
                        + "." + PageTable.COLUMN_SPOUSE
                        + " = "
                        + MemberTable.TABLE_MEMBER + "._id ");
                break;  // we set the selection separately -
                // where pagetable_id in a_1, a_2, a_3  : here my_pages
                // gives us "a_1 a_2 a_3" as associated page_id

            case OWNERS:
                queryBuilder.setTables(PageTable.TABLE_PAGE
                        + " inner join "
                        + MemberTable.TABLE_MEMBER
                        + " on "
                        + PageTable.TABLE_PAGE
                        + "." + PageTable.COLUMN_OWNER
                        + " = "
                        + MemberTable.TABLE_MEMBER + "._id ");
                break;  // we set the selection separately -
                // where pagetable_id in a_1, a_2, a_3  : here my_pages
                // gives us "a_1 a_2 a_3" as associated page_id
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // Make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        //queryBuilder.
        return cursor;
    }





    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // also using the following projection:
        HashMap<String,String> SEARCH_PROJECTION_MAP = new HashMap<String, String>();
        SEARCH_PROJECTION_MAP.put( "_pid", PageTable.TABLE_PAGE+"."+PageTable.COLUMN_ID + " as _pid" );
        SEARCH_PROJECTION_MAP.put( "_id" , MemberTable.TABLE_MEMBER+"."+MemberTable.COLUMN_ID + " as _id");
        SEARCH_PROJECTION_MAP.put(MemberTable.COLUMN_FIRST,MemberTable.COLUMN_FIRST);
        SEARCH_PROJECTION_MAP.put(MemberTable.COLUMN_LAST,MemberTable.COLUMN_LAST);
        SEARCH_PROJECTION_MAP.put(MemberTable.COLUMN_YOB,MemberTable.COLUMN_YOB);
        SEARCH_PROJECTION_MAP.put(MemberTable.COLUMN_YOD,MemberTable.COLUMN_YOD);
        SEARCH_PROJECTION_MAP.put(MemberTable.COLUMN_MY_PAGES,MemberTable.COLUMN_MY_PAGES);

        // so that, presumably, in a call we can set
        // String[] projection = new String[] { "_pid" }; - when we invoke it!


        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Check if the caller has requested a column which does not exists
        checkColumns(projection);


        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case MEMBERS:
                queryBuilder.setTables(MemberTable.TABLE_MEMBER);
                break;
            case MEMBER_ID:
                queryBuilder.setTables(MemberTable.TABLE_MEMBER);
                // Adding the ID2CHANGE to the original query
                queryBuilder.appendWhere(MemberTable.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;


            /*
            this is an example of a join: required and for contentproviders, needs to be antcipated and set up
            in advance: cant join contentprovider uris! note what we needed to do to set this up:#
             1. give
            * */
            case PARENT_ID:
                queryBuilder.setTables(
                        PageTable.TABLE_PAGE
                        + " inner join " +
                        MemberTable.TABLE_MEMBER
                        + " on " +
                        PageTable.TABLE_PAGE + "._id = " + MemberTable.COLUMN_PAR_PAGES
                );
                // Adding the ID2CHANGE to the original query
                queryBuilder.appendWhere(MemberTable.TABLE_MEMBER + "." + MemberTable.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;
/*
               rawQuery("select a.COLUMN_FORM_A, b.COLUMN_FROM_B
                        from TABLE_A a, TABLE_B b, TABLE_AB ab
                        where ab.FK_ID_A=a.ID2CHANGE and ab.FK_ID_B=b.ID2CHANGE", null);
 */

    //_id ???
    // SELECT _id as _id FROM page inner join member on page.spouse = member._id  WHERE (page._id in (1,2))

            case SPOUSES:
                queryBuilder.setTables(PageTable.TABLE_PAGE
                        + " inner join "
                        + MemberTable.TABLE_MEMBER
                        + " on "
                        + PageTable.TABLE_PAGE
                        + "." + PageTable.COLUMN_SPOUSE
                        + " = "
                        + MemberTable.TABLE_MEMBER + "._id ");
                //here is where we invoke the use of the projectionmap
                queryBuilder.setProjectionMap(SEARCH_PROJECTION_MAP);

                break;  // we set the selection separately - where pagetable_id in a_1, a_2, a_3  : here my_pages gives us "a_1 a_2 a_3" as associated page_id

            case OWNERS:
                queryBuilder.setTables(PageTable.TABLE_PAGE
                        + " inner join "
                        + MemberTable.TABLE_MEMBER
                        + " on "
                        + PageTable.TABLE_PAGE
                        + "." + PageTable.COLUMN_OWNER
                        + " = "
                        + MemberTable.TABLE_MEMBER + "._id ");
                //here is where we invoke the use of the projectionmap
                queryBuilder.setProjectionMap( SEARCH_PROJECTION_MAP );

                break;  // we set the selection separately - where pagetable_id in a_1, a_2, a_3  : here my_pages gives us "a_1 a_2 a_3" as associated page_id

            case SORTER1:
                queryBuilder.setTables(MemberTable.TABLE_MEMBER
                        + " inner join "
                        + MemberTable.TABLE_SORTER
                        + " on "
                        + MemberTable.TABLE_MEMBER
                        + "._id = "
                        + MemberTable.TABLE_SORTER
                        + ".r_id ");
                break;
            case SORTER2:
                queryBuilder.setTables(MemberTable.TABLE_MEMBER
                        + " inner join "
                        + MemberTable.TABLE_SORTER
                        + " on "
                        + MemberTable.TABLE_MEMBER
                        + "._id = "
                        + MemberTable.TABLE_SORTER
                        + ".r_id ");
                break;
            case PAGES:
                queryBuilder.setTables(PageTable.TABLE_PAGE);
                break;
            case PAGE_ID:
                queryBuilder.setTables(PageTable.TABLE_PAGE);
                // Adding the ID2CHANGE to the original query
                queryBuilder.appendWhere(PageTable.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;
            case PREFS:
                queryBuilder.setTables(MemberTable.TABLE_PREFS);
                break;
            case EMPTY:   // use this to get the date
                Cursor cursor = database.
                        getReadableDatabase().
                        rawQuery("select date() as " + THE_DATE, null);
                return cursor;
            case PAGESIZE:   // use this to get the date
                cursor = database.
                        getReadableDatabase().
                        rawQuery("select _id  from " + PageTable.TABLE_PAGE , null);
                return cursor;
            case MEMBERSIZE:   // use this to get the date
                cursor = database.
                        getReadableDatabase().
                        rawQuery("select _id  from " + MemberTable.TABLE_MEMBER , null);
                return cursor;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // Make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        //queryBuilder.
        return cursor;
    }


    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        String Which_PATH;
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        //int rowsDeleted = 0;
        long id = 0;
        switch (uriType) {
            case MEMBERS:
                Which_PATH = M_BASE_PATH;
                id = sqlDB.insert(MemberTable.TABLE_MEMBER, null, values);
                break;
            case SORTER1:
                Which_PATH = M_BASE_PATH;
                id = sqlDB.insert(MemberTable.TABLE_SORTER, null, values);
                break;
            case SORTER2:
                Which_PATH = M_BASE_PATH;
                id = sqlDB.insert(MemberTable.TABLE_SORTER, null, values);
                break;
            case PREFS:
                Which_PATH = PR_BASE_PATH;
                id = sqlDB.insert(MemberTable.TABLE_PREFS, null, values);
                break;
            case PAGES:
                Which_PATH = P_BASE_PATH;
                id = sqlDB.insert(PageTable.TABLE_PAGE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        //cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return Uri.parse(Which_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted = 0;
        String id;
        switch (uriType) {
            case MEMBERS:
                rowsDeleted = sqlDB.delete(MemberTable.TABLE_MEMBER, selection,
                        selectionArgs);
                break;
            case MEMBER_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(MemberTable.TABLE_MEMBER,
                            MemberTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(MemberTable.TABLE_MEMBER,
                            MemberTable.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            case PREFS:
                rowsDeleted = sqlDB.delete(MemberTable.TABLE_PREFS, selection,
                        selectionArgs);
                break;
            case SORTER1:
                rowsDeleted = sqlDB.delete(MemberTable.TABLE_SORTER, selection,
                        selectionArgs);
                break;
            case SORTER2:
                rowsDeleted = sqlDB.delete(MemberTable.TABLE_SORTER, selection,
                        selectionArgs);
                break;
            case PAGES:
                rowsDeleted = sqlDB.delete(PageTable.TABLE_PAGE, selection,
                        selectionArgs);
                break;
            case PAGE_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(PageTable.TABLE_PAGE,
                            PageTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(PageTable.TABLE_PAGE,
                            PageTable.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            case EMPTY:
                rowsDeleted = sqlDB.delete(PageTable.TABLE_PAGE, selection,
                        selectionArgs);
                rowsDeleted = sqlDB.delete(MemberTable.TABLE_MEMBER, selection,
                        selectionArgs);
                rowsDeleted = sqlDB.delete("SQLITE_SEQUENCE",
                        "name = '" + MemberTable.TABLE_MEMBER + "'",
                        selectionArgs);
                rowsDeleted = sqlDB.delete("SQLITE_SEQUENCE",
                        "name = '" + PageTable.TABLE_PAGE + "'",
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated = 0;
        String id;
        switch (uriType) {
            case MEMBERS:
                rowsUpdated = sqlDB.update(MemberTable.TABLE_MEMBER,
                        values,
                        selection,
                        selectionArgs);
                break;
            case MEMBER_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(MemberTable.TABLE_MEMBER,
                            values,
                            MemberTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(MemberTable.TABLE_MEMBER,
                            values,
                            MemberTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            case PREFS:
                rowsUpdated = sqlDB.update(MemberTable.TABLE_PREFS,
                        values,
                        selection,
                        selectionArgs);
                break;
            case PAGES:
                rowsUpdated = sqlDB.update(PageTable.TABLE_PAGE,
                        values,
                        selection,
                        selectionArgs);
                break;
            case PAGE_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(PageTable.TABLE_PAGE,
                            values,
                            PageTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(PageTable.TABLE_PAGE,
                            values,
                            PageTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            case PAGE_SORT_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(PageTable.TABLE_PAGE,
                            values,
                            PageTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(PageTable.TABLE_PAGE,
                            values,
                            PageTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                return 0; // we dont want to notify, zero return is bogus!
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }


    private void checkColumns(String[] projection) {
        // implement as below for the two tables
    }
  
  
  /*
  private void checkColumns(String[] projection) {
    String[] available = { TodoTable.COLUMN_CATEGORY,
        TodoTable.COLUMN_SUMMARY, TodoTable.COLUMN_DESCRIPTION,
        TodoTable.COLUMN_ID };
    if (projection != null) {
      HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
      HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
      // Check if all columns which are requested are available
      if (!availableColumns.containsAll(requestedColumns)) {
        throw new IllegalArgumentException("Unknown columns in projection");
      }
    }
  }  */
  

  /* 
   *   SQLiteQueryBuilder.setTables(
   * 		"foo LEFT OUTER JOIN bar ON (foo.id = bar.foo_id)"
   *   )
   */
  
  /*
   * exmple use of rawquery
  
  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
      SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
      Cursor cursor = null;
      int uriType = URIMatcher.match(uri);
      switch (uriType) {
          case TABLE_A_URI:
              queryBuilder.setTables("TABLE_A");
              cursor = queryBuilder.query(databaseHelper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
              break;
          case TABLE_B_URI:
              queryBuilder.setTables("TABLE_B");
              cursor = queryBuilder.query(databaseHelper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
              break;
          case TABLE_JOIN_A_B_URI:
               cursor = databaseHelper.
                 getReadableDatabase().
                 rawQuery("select a.COLUMN_FORM_A, b.COLUMN_FROM_B 
                        from TABLE_A a, TABLE_B b, TABLE_AB ab 
                        where ab.FK_ID_A=a.ID2CHANGE and ab.FK_ID_B=b.ID2CHANGE", null);
              break;
          default:
              throw new IllegalArgumentException("Unknown URI");
      }

      cursor.setNotificationUri(getContext().getContentResolver(), uri);
      return cursor;
  }
  
  */


}