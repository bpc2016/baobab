package com.bpc.baobab;

import java.io.Serializable;

/**
 * Created by hp on 1/1/2016.
 */
public class Member implements Serializable{


    private static final String NOTE_SEP = "##";

    private String id;
    private String last;
    private String first;
    private String yob;
    private String yod;
    private String gender;
    private String my_pages;
    private String par_pages;
    private String note;

    // aditional fields
    private int position = 0; // where the current page lies among my_pages
    private String nextpage = ""; //top of the my_pages


    public Member() {
    } // generic constructor

    public Member( String id) {
        this.id = id;
    }

    public Member( Family fh, String id) { }

    // getters

    public String getID() {
        return id;
    }

    public String getYob() {
        return yob;
    }

    public String getYod() {
        return yod;
    }

    public String getMyPages() {
        return my_pages;
    }

    public String getFirst() {
        return first;
    }

    public String getGender() {
        return gender;
    }

    public String getNotes() {
        return note;
    }

    public String nextPage() {
        return nextpage;
    }
/*
    String[] memProjn = {MemberTable.COLUMN_ID,MemberTable.COLUMN_FIRST, MemberTable.COLUMN_LAST,
            MemberTable.COLUMN_GENDER, MemberTable.COLUMN_YOB, MemberTable.COLUMN_YOD,
            MemberTable.COLUMN_MY_PAGES, MemberTable.COLUMN_PAR_PAGES};
*/


    public void load(int k, String input) {
        switch (k) {
            case 0:
                this.id = input;
                break;
            case 1:
                this.first = input;
                break;
            case 2:
                this.last = input;
                break;
            case 3:
                this.gender = input;
                break;
            case 4:
                this.yob = input;
                break;
            case 5:
                this.yod = input;
                break;
            case 6:
                this.my_pages = input;
                break;
            case 7:
                this.par_pages = input;
                break;
            case 8:
                this.note = input;
                break;
        }
    }

    //formatted dates
    public String getDates(){
        String result = yob;
        if (!yod.isEmpty()) result = result + " - " + yod;
        return result;
    }

    public int getPosition() {
        return position;
    }

    ///////////////// getters

    public String getPar() {
        return par_pages;
    }

    public String getLast() {
        return last;
    }

    public String getNote() {
        return note;
    }

    //setters, so that we can chain them ..

    public void setID(String ID) {
        this.id = ID;
    }

    public Member setFirst(String first) {
        this.first = first;
        return this;
    }

    public Member setLast(String last) {
        this.last = last;
        return this;
    }

    public Member setYob(String yob) {
        this.yob = yob;
        return this;
    }

    public Member setYod(String yod) {
        this.yod = yod;
        return this;
    }

    public Member setNote(String note) {
        this.note = note;
        return this;
    }

    public Member setMyPages(String myPages) {
        this.my_pages = myPages;
        return this;
    }

    public Member setGender(String gender) {
        this.gender = gender;
        return this;
    }

    public Member setPar(String par) {
        this.par_pages = par;
        return this;
    }

    public Member setPosition(int position) {
        this.position = position;
        return this;
    }

    public String Digest() {
        return first+","+this.getDates()+","+id;
    }

    public Member addNote(String newNote) {
        note =  newNote + NOTE_SEP + note;
        return this;
    }

    public Member addMyPages(String page_id) {
        my_pages = page_id +" "+my_pages;
        return this;
    }

    public Member remMyPages(String id) {
        String [] spouseAr = my_pages.split(" ");
        String [] replaceAr = new String[spouseAr.length-1];
        int i=0;
        for(String s : spouseAr){
            if (s.equals(id)) continue;
            replaceAr[i++] = s;
        }
        my_pages = Join(replaceAr," ");
        nextpage = replaceAr[0];
        return this;
    }

    private String Join(String[] s, String glue) {
        int k = s.length;
        if (k == 0)
            return null;
        StringBuilder out = new StringBuilder();
        out.append(s[0]);
        for (int x = 1; x < k; ++x)
            out.append(glue).append(s[x]);
        return out.toString();
    }

    public String getFullname() {
        return first+" "+last;
    }

    /**
     * keep only the first of the first names
     * @return : fullname with only one first name
     */
    public String getShortname() {
        String[] allfirstAr = first.split(" ");
        String just_one = allfirstAr[0];
        return just_one+" "+last.toUpperCase();
    }

} /////-------------- end of class ----------------------

/*

public static final String COLUMN_ID = "_id";
	  public static final String COLUMN_LAST = "last";
	  public static final String COLUMN_FIRST = "first";
	  public static final String COLUMN_YOB = "yob";
	  public static final String COLUMN_YOD = "yod";
	  public static final String COLUMN_GENDER = "gender";
	  public static final String COLUMN_MY_PAGES = "my_pages";
	  public static final String COLUMN_PAR_PAGES = "par_pages";
 */