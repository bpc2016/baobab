package com.bpc.baobab;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.bpc.baobab.contentprovider.FtContentProvider;
import com.bpc.baobab.database.MemberTable;
import com.bpc.baobab.database.PageTable;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by hp on 3/2/2016.
 */
public class Family {
    public static final String EMPTY = "0";
    public static final String SEARCH_LAST_NAME
            = MemberTable.COLUMN_LAST + " LIKE ?";
    private Context ctx;
    private Member theMember;
    private Page thePage;

    //    private ContentResolver resolver;
    private ContentValues values = new ContentValues();
    private String spouse;

    private Family() {
    }

    public Family(Context context) {
        this.ctx = context;
    }



    /*

        set values for Member - assumes that theMember <> null

     */


    //update member

    private void transferMember(Member member){
        String data="";
        values.clear();
        data = member.getGender();
        if (null!= data && !data.isEmpty()) values.put(MemberTable.COLUMN_GENDER, data);
        data = member.getFirst();
        if (null!= data && !data.isEmpty()) values.put(MemberTable.COLUMN_FIRST, data);
        data = member.getLast();
        if (null!= data && !data.isEmpty()) values.put(MemberTable.COLUMN_LAST, data);
        data = member.getYob();
        if (null!= data && !data.isEmpty()) values.put(MemberTable.COLUMN_YOB, data);
        data = member.getYod();
        if (null!= data && !data.isEmpty()) values.put(MemberTable.COLUMN_YOD, data);
        data = member.getMyPages();
        if (null!= data && !data.isEmpty()) values.put(MemberTable.COLUMN_MY_PAGES, data);
        data = member.getPar();
        if (null!= data && !data.isEmpty()) values.put(MemberTable.COLUMN_PAR_PAGES, data);
        data = member.getNote();
        if (null!= data && !data.isEmpty()){
            String today = " -date?- ";
            Cursor cr = ctx.getContentResolver().query(
                    FtContentProvider.EMPTY_DB_URI, null, null, null, null);
            if (cr != null) {
                cr.moveToFirst();
                today = cr.getString(cr
                        .getColumnIndexOrThrow(FtContentProvider.THE_DATE));
                cr.close();
            }
            data = "(" + today + ")  " + data;
            values.put(MemberTable.COLUMN_NOTES, data);
        }
    }



    public Family updateMember(Member member) {
        ContentResolver resolver = ctx.getContentResolver();
        String the_id = member.getID();
        transferMember(member);
        if (!the_id.isEmpty() && values.size() > 0) {
            resolver.update(
                    Uri.parse(FtContentProvider.MEMBER_URI + "/" + the_id)
                    , values, null, null);
        }
        return this;
    }

/*
    public Family updateMemberOLD() {
        ContentResolver resolver = ctx.getContentResolver();
        String the_id = theMember.getID();
        transferMember(theMember);
        if (!the_id.isEmpty() && values.size() > 0) {
            resolver.update(
                    Uri.parse(FtContentProvider.MEMBER_URI + "/" + the_id)
                    , values, null, null);
        }
        return this;
    }
*/


    // create member and return member_id - we use what we have setup in thePage
    public String createMember(Member member) {
        ContentResolver resolver = ctx.getContentResolver();
        transferMember(member);
        if (0==values.size()) return ""; //shouldnt happen
        Uri uri = resolver.insert(FtContentProvider.MEMBER_URI, values);
        String newID = null != uri ? uri.getLastPathSegment() : "";
        member.setID(newID); // side effect
        return newID;
    }



    public  Family delMember(String id) {
        if (id.isEmpty()){
            Toast.makeText(ctx, "Failed! Error in deleting member", Toast.LENGTH_LONG).show();
            return this;
        }
        ctx.getContentResolver().delete(
                Uri.parse(FtContentProvider.MEMBER_URI + "/" + id)
                , null, null);
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    /*
        set up pages details and save
     */



    private void transferPage(Page page){
        String data="";
        values.clear();
        data = page.getOwner();
        if (null!= data && !data.isEmpty()) values.put(PageTable.COLUMN_OWNER, data);
        data = page.getSpouse();
        if(EMPTY==data) { values.put(PageTable.COLUMN_SPOUSE, "");}
        else if (null!= data && !data.isEmpty()) values.put(PageTable.COLUMN_SPOUSE, data);
        data = page.getKids();
        if(EMPTY==data) { values.put(PageTable.COLUMN_KIDS, "");}
        else if (null!= data && !data.isEmpty()) values.put(PageTable.COLUMN_KIDS, data);
    }


    public void updatePage(Page page) {
        ContentResolver resolver = ctx.getContentResolver();
        String the_id = page.getID();
        transferPage(page);
        if (!the_id.isEmpty() && values.size() > 0) {
            resolver.update(
                    Uri.parse(FtContentProvider.PAGE_URI + "/" + the_id)
                    , values, null, null);
        }
    }


/*
    public Family setSpouse(String data) {
        if (!data.equals(thePage.getSpouse())) {
            thePage.setSpouse(data);
            values.put(PageTable.COLUMN_SPOUSE, data);
        }
        return this;
    }*/

    public String createPage(Page page) {
        transferPage(page);
        if (0==values.size()) return ""; //shouldnt happen
        ContentResolver resolver = ctx.getContentResolver();
        Uri uri = resolver.insert(FtContentProvider.PAGE_URI, values);
        return null != uri ? uri.getLastPathSegment() : "";
    }



/*
    public Family newMemberOLD(String id) {
        values.clear(); // prepare to update id=member_id
        theMember = new Member(id);
        values.put(MemberTable.COLUMN_ID, id);
        return this;
    }
*/


    // this must be loaded here - dont collect Notes elsewhere!
    public String getNotes(Member member) { // use theMember
        String id = member.getID();
        if (id.isEmpty()) return null;
        String notes = null;
        Uri uri = Uri.parse(FtContentProvider.MEMBER_URI + "/" + id);
        Cursor cr = ctx.getContentResolver().query(uri, null, null, null, null);
        // fetch the value
        if (cr != null) {
            cr.moveToFirst();
            notes = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_NOTES));
            cr.close();
        }
        return notes;
    }

    public  void delPage(String id) {
        ctx.getContentResolver().delete(
                Uri.parse(FtContentProvider.PAGE_URI + "/" + id)
                , null, null);
    }
    /////////////////////////////// from onepage ///////////////////////////////////////////

    public void resetHome(String mPageID) {
        // update adam
        Cursor cr = ctx.getContentResolver().query(FtContentProvider.MEMBER_URI, null
                , FileFragment.adamField + " = " + FileFragment.adamLastName      // the selection
                , null, null);
        if (cr != null) {
            cr.moveToFirst();
            String the_id = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_ID));
            cr.close();
            Uri uri = Uri.parse(FtContentProvider.MEMBER_URI + "/" + the_id);
            values.clear();
            values.put(MemberTable.COLUMN_MY_PAGES, mPageID);
            ctx.getContentResolver().update(uri, values, null, null);
            Toast.makeText(ctx, "Success! This is now your new home", Toast.LENGTH_LONG).show();
            MainActivity.myPid = mPageID; // reset this
            uri = Uri.parse(FtContentProvider.PAGE_URI + "/" + mPageID);
            cr = ctx.getContentResolver().query(uri,null,null,null,null);
            if(cr!=null){
                cr.moveToFirst();
                String myOwner = cr.getString(cr.getColumnIndex(PageTable.COLUMN_OWNER));
                cr.close();
                MainActivity.myOwner = myOwner; // reset this
            }
            return;
        }
        Toast.makeText(ctx, "database error", Toast.LENGTH_LONG).show();
    }

    /////////////////////// cursor helper methods //////////////////

    private boolean CRCLOSE = true;

    public Family keepCr(){
        CRCLOSE = false;
        return this;
    }

    public  Cursor getMemCursor(String member){
        Uri uri = Uri.parse(FtContentProvider.MEMBER_URI + "/" + member);
        return ctx.getContentResolver().query(
                uri,
                null, null, null, null);
    }

    public String getMyPages(Cursor cr){
        if (null==cr) return null;
        cr.moveToFirst();
        String result =  cr.getString(cr.getColumnIndex(MemberTable.COLUMN_MY_PAGES));
        if (CRCLOSE) cr.close(); CRCLOSE=true;
        return  result;
    }

    public String getFirst(Cursor cr){
        if (null==cr) return null;
        cr.moveToFirst();
        String result =  cr.getString(cr.getColumnIndex(MemberTable.COLUMN_FIRST));
        if (CRCLOSE) cr.close(); CRCLOSE=true;
        return  result;
    }

    public String getLast(Cursor cr){
        if (null==cr) return null;
        cr.moveToFirst();
        String result = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_LAST));
        if (CRCLOSE) cr.close(); CRCLOSE=true;
        return  result;
    }

    public String getPar(Cursor cr){
        if (null==cr) return null;
        cr.moveToFirst();
        String result =  cr.getString(cr.getColumnIndex(MemberTable.COLUMN_PAR_PAGES));
        if (CRCLOSE) cr.close(); CRCLOSE=true;
        return  result;
    }

    //////// Page Table

    public  Cursor getPageCursor(String page){
        Uri uri = Uri.parse(FtContentProvider.PAGE_URI + "/" + page);
        return ctx.getContentResolver().query(
                uri,
                null, null, null, null);
    }

    public String getOwner(Cursor cr){
        if (null==cr) return null;
        cr.moveToFirst();
        String result =   cr.getString(cr.getColumnIndex(PageTable.COLUMN_OWNER));
        if (CRCLOSE) cr.close(); CRCLOSE=true;
        return  result;
    }


    public String getKids(Cursor cr){
        if (null==cr) return null;
        cr.moveToFirst();
        String result =  cr.getString(cr.getColumnIndex(PageTable.COLUMN_KIDS));
        if (CRCLOSE) cr.close(); CRCLOSE=true;
        return  result;
    }

    public String getSpouse(Cursor cr) {
        if (null==cr) return null;
        cr.moveToFirst();
        String result =  cr.getString(cr.getColumnIndex(PageTable.COLUMN_SPOUSE));
        if (CRCLOSE) cr.close(); CRCLOSE=true;
        return  result;
    }

    ////////// newmember utilities


    // return a memList of spouses
    public ArrayList<String> fetchSpouses(Member mOmember, String mPageID) {
        String myid = mOmember.getID(); // to keep track
        Boolean foundBlank = false;
        String[] projection = new String[]{
                "_pid", // See FtContentProvider definition of the query method
                MemberTable.COLUMN_ID,
                MemberTable.COLUMN_FIRST, MemberTable.COLUMN_LAST,
                MemberTable.COLUMN_YOB, MemberTable.COLUMN_YOD, MemberTable.COLUMN_MY_PAGES};

        String mine = mOmember.getMyPages().replace(" ", ",");
        String selection = PageTable.TABLE_PAGE + "." + PageTable.COLUMN_ID
                + " in (" + mine + ")";
        Cursor cr = ctx.getContentResolver().query(
                FtContentProvider.SPOUSES_URI,
                projection,
                selection,
                null, null);
        // fetch the values
        String dates = "", name = "", yod = "", id = "", foundString = "";
        String mem_id;

        String[] mineAr = mine.split(",");
        HashMap<String, String> reOrder = new HashMap<>();
        if (cr != null) {
            while (cr.moveToNext()) {
                mem_id = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_ID));
                if (mem_id.equals(myid)) {
                    id = cr.getString(cr.getColumnIndex("_pid"));
                    foundString = foundString.isEmpty() ? id : foundString + "," + id;
//                    Log.d(LOGGER, "lost id  = " + id);
                    foundBlank = true;
                } else {
                    dates = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_YOB));
                    yod = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_YOD));
                    if (!yod.isEmpty()) dates = dates + " - " + yod;
                    name = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_FIRST)) + " "
                            + cr.getString(cr.getColumnIndex(MemberTable.COLUMN_LAST));
                    id = cr.getString(cr.getColumnIndex("_pid"));
                    reOrder.put(id, name + "," + dates + "," + id);
                }
            }
            cr.close();
            // now try the other side - only in case we hit a blank!
            if (foundBlank) {
                selection = PageTable.TABLE_PAGE + "." + PageTable.COLUMN_ID
                        + " in (" + foundString + ")";
                cr = ctx.getContentResolver().query(
                        FtContentProvider.OWNERS_URI, null,
                        selection,
                        null, null);
                if (cr != null) {
                    while (cr.moveToNext()) {
                        dates = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_YOB));
                        yod = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_YOD));
                        if (!yod.isEmpty()) dates = dates + " - " + yod;
                        name = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_FIRST)) + " "
                                + cr.getString(cr.getColumnIndex(MemberTable.COLUMN_LAST));
                        id = cr.getString(cr.getColumnIndex("_pid"));
                        // now replace entry in memList
                        reOrder.put(id, name + "," + dates + "," + id);
                    }
                    cr.close();
                }
            }
        }
//        Log.d(LOGGER, "mine = " + mine);

        ArrayList<String> memList = new ArrayList<>();
        for (int i = 0; i < mineAr.length; i++) {
            memList.add(reOrder.get(mineAr[i]));
            if (mineAr[i].equals(mPageID)) mOmember.setPosition(i); // = i;
        }
        String mlist = "";
        for (String x : memList) {
            String xid = x.split(",")[2];
            mlist = mlist + ":" + xid;
        }
//        Log.d(LOGGER, "memlist = " + mlist);
//        Log.d(LOGGER, "position = " + pos_on_sort);
        return memList;
    }

    /**
     *
     * @param mem : member ID
     * @return : comma separated list of
     */
    public String getSpouses(String mem) {
        return "";
    }


        /**
         *  the data fetched for Members fro te db. Note we have excluded notes
         */
    private  String[] memProjn = {MemberTable.COLUMN_ID, MemberTable.COLUMN_FIRST, MemberTable.COLUMN_LAST,
            MemberTable.COLUMN_GENDER, MemberTable.COLUMN_YOB, MemberTable.COLUMN_YOD,
            MemberTable.COLUMN_MY_PAGES, MemberTable.COLUMN_PAR_PAGES};



    /**
     * load pre-exisiting Member m with db data from id=id,
     * requires a prior call of new Member (or by upcasting, any extended class, such as Node)
     * @param m
     * @param mem_id
     */
    public void loadMember(Member m, String mem_id){
        // fetch the data
        Cursor data = ctx.getContentResolver().query(FtContentProvider.MEMBER_URI,
                memProjn,  // the projection
                MemberTable.COLUMN_ID + " = " + mem_id // selection
                , null, null);
        if (null!=data){
            while (data.moveToNext()) {
                String id = data.getString(0); // this is "_id"
                for (int k = 0; k < memProjn.length; k++) {  // note this one also loads id!!
                    m.load(k, data.getString(k));
                }
            }
            data.close();
        }
    }

    // from a member id, return the list of members - epect that the list is space separated
    public ArrayList<Member> getMembers(String member_ids){
        if (member_ids.isEmpty()) return null;
        ArrayList<Member> theList = new ArrayList<>();
        HashMap<String,Integer> ordering = new HashMap<>();
        String[] arr = member_ids.split(" ");
        String memList = member_ids.replaceAll(" ",",");

        for (String id : arr){
            theList.add(new Member(id));
        }

        int i=0;
        for(String id:arr){
            ordering.put(id,i++);
        }

        // fetch the data
        Cursor data = ctx.getContentResolver().query(FtContentProvider.MEMBER_URI,
                memProjn,  // the projection
                MemberTable.COLUMN_ID + " in (" + memList + ")"// selection
                , null, null);
        if (null!=data){
            while (data.moveToNext()) {
                String id = data.getString(0); // this is "_id"
                for (int k = 1; k < memProjn.length; k++) {
                    theList.get(ordering.get(id)).load(k, data.getString(k));
                }
            }
            data.close();
        }
        return  theList;
    }

    public Member getMember(String mem_id){
        ArrayList<Member> members = getMembers(mem_id);
        if (null==members) return null;
        return members.get(0);
    }

    public String getFirstPid(){
        String p_Id = "";
        String adamLastName = FileFragment.adamLastName;
        String adamField = FileFragment.adamField;

        Cursor cr = ctx.getContentResolver().query(
                FtContentProvider.MEMBER_URI, null
                , adamField + " = " + adamLastName      // the selection
                , null, null);
        if (cr != null) {
            cr.moveToFirst();
            p_Id = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_MY_PAGES));
            cr.close();
        }

        return p_Id;
    }

    public boolean noDependents(String page_id){
        Cursor p_cr = ctx.getContentResolver().query(
                Uri.parse(FtContentProvider.PAGE_URI + "/" + page_id),
                null, null, null, null);
        if (p_cr == null) {
            return false; // msg = "Failed! error retrieving address! ";
        } else {
            p_cr.moveToFirst();
            String spouse = p_cr.getString(p_cr.getColumnIndex(PageTable.COLUMN_SPOUSE)); //fh.keepCr().getSpouse(p_cr);
            String kids = p_cr.getString(p_cr.getColumnIndex(PageTable.COLUMN_KIDS)); //fh.getKids(p_cr);
            p_cr.close(); //if (spouse.isEmpty() && kids.isEmpty()) msg = "Delete successful"; //doit
            return (spouse.isEmpty() && kids.isEmpty());
        }
    }

    //from searchfragment

    // populate the list based on query
    /**
     * selection = MemberTable.COLUMN_LAST + " LIKE ?"
     * selectionArgs = new String[] {query+"%"}
     *
     */
    public int fillView(String selection, String[] selectionArgs, ArrayList<Member> myList) {
        if(null == myList) return 0; // need: myList = new ArrayList<>();
        Cursor cr = ctx.getContentResolver().query(
                FtContentProvider.MEMBER_URI,
                null,
                selection,
                selectionArgs, null);
        if(cr!=null){
            while(cr.moveToNext()){
                Member m = new Member();
                m.setID(cr.getString(cr.getColumnIndex(MemberTable.COLUMN_ID)));
                m.setFirst(cr.getString(cr.getColumnIndex(MemberTable.COLUMN_FIRST)));
                m.setLast(cr.getString(cr.getColumnIndex(MemberTable.COLUMN_LAST)));
                m.setMyPages(cr.getString(cr.getColumnIndex(MemberTable.COLUMN_MY_PAGES)));
                m.setPar(cr.getString(cr.getColumnIndex(MemberTable.COLUMN_PAR_PAGES)));
                m.setYob(cr.getString(cr.getColumnIndex(MemberTable.COLUMN_YOB)));
                m.setYod(cr.getString(cr.getColumnIndex(MemberTable.COLUMN_YOD)));
                m.setYob(m.getDates()); // fake it here
                myList.add(m);
            }
        }
        return myList.size();
    }


    /**
     * use page_id=id to fetch the member IDs of the parents.
     * further analysis required to decide the gender!
     * @param id
     * @return
     */
    public String[] getParents(String id) {
        if(id.isEmpty()) return null;
        String [] pars = new String[3];
        // else get the parents
        Cursor p_cr = ctx.getContentResolver().query(
                Uri.parse(FtContentProvider.PAGE_URI + "/" + id),
                null, null, null, null);
        if (p_cr == null) {
            return null; // shouldnt happen
        } else {
            p_cr.moveToFirst();
            pars[0] = p_cr.getString(p_cr.getColumnIndex(PageTable.COLUMN_OWNER));
            pars[1] = p_cr.getString(p_cr.getColumnIndex(PageTable.COLUMN_SPOUSE));
            pars[2] = p_cr.getString(p_cr.getColumnIndex(PageTable.COLUMN_KIDS));
            p_cr.close();
            return pars;
        }
    }
}
