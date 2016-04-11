package com.bpc.baobab;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by hp on 12/22/2015.
 */
public class Page implements Serializable {
    private String id;
    private String owner;
    private String spouse;
    private String kids;

    // extra fields used here!
    private String par;
    private String name;
    private ArrayList<String> digest;
    private String surname = "Unknown";
    private String nextkid = "";

    // constructors

    public Page(){}

    public Page(String id){
        this.id=id;
    }

    //special constructor ... may have to remove!!
    public Page( String id, String owner, String par, String name){
        this.id=id;
        this.owner=owner;
        this.par=par;
        this.name=name;
    }

    // gettters

    public String getOwner(){
        return owner;
    }
    public String getKids() {
        return kids;
    }

    public String getID(){ return id; }

    public String getSpouse() {
        return spouse;
    }

    public String getName(){
        return name;
    }

    public ArrayList<String> getDigests() {
        return digest;
    }


    // setters

    public Page setID(String id) {
        this.id = id;
        return this;
    }

    public Page setSpouse(String spouse) {
        this.spouse = spouse;
        return this;
    }

    public Page setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public Page setKids(String kids) {
        this.kids = kids;
        return this;
    }

    public Page addKid(String child_id) {
        kids = child_id+" "+kids;
        return this;
    }

    public void addDigest(Member m) {
        if(null==digest){
            digest = new ArrayList<>();
        }
//        digest.add(m.getFirst()+","+m.getDates()+","+m.getID());
        digest.add(m.Digest());
    }

    public void setDigests(ArrayList<String> digest) {
        this.digest = digest;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getSurname() {
        return surname;
    }

    public String nextKid() {
        return nextkid;
    }

    //specials

    public boolean hasManyKids(){
        return kids.contains(" ");
    }

    public Page remKid(String id) {
        String [] kidsAr = kids.split(" ");
        String [] replaceAr = new String[kidsAr.length-1];
        int i=0;
        for(String s : kidsAr){
            if (s.equals(id)) continue;
            replaceAr[i++] = s;
        }
        kids = Join(replaceAr," ");
        nextkid = replaceAr[0];
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

}

