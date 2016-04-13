package com.bpc.baobab;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * this is LineageFragment, controls the viewpager adapter
 * that displays trees using TreeFrgament and trees grown using Tree
 */
public class LineageFragment extends Fragment {

    public static final String MEMBER_ID = "MAIN MEMBER";

    private static final String LOGGER = "bpc_lineage"; //Log.d(LOGGER, "page = " + real_id + ", member = " + member_id);
    public static final int SOURCES = 0;
    public static final int RELATION = 1;
    public static final String TYPE = "TYPE"; // of search we want - either sources or relation

    private String memID;
    private ArrayList<Integer> mNodes = new ArrayList<>();
    private ArrayList<String> mTitle = new ArrayList<>();
    private Tree myTree, oTree;
    private HashMap<Integer, Boolean> mRev = new HashMap<>(); // whether we are reversed or not (for teh first line)
    private HashMap<Integer, String> mFirstLine = new HashMap<>(); // encoded first line per src
    private HashMap<Integer, Integer> mOsrc = new HashMap<>();  // the src address from Otree
    private boolean getRelation = false; // whether we are comparing two or not ...
    private int mType;

    @Override
    public void onCreate(Bundle savedInstanceState) { // essential to allow correct position in viewer after rotation
        super.onCreate(savedInstanceState);
        Bundle args = savedInstanceState != null
                ? savedInstanceState
                : getArguments();
        if (args != null) {
            memID = args.getString(MEMBER_ID);
            mType = args.getInt(TYPE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the current page member ID in case we need to recreate the fragment (on rotate!)
        outState.putString(MEMBER_ID, memID);
        outState.putInt(TYPE, mType);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.pages_fragment, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // The activity is about to become visible.
        if (null == myTree) { // prevent duplicate trees
            myTree = new Tree(getActivity(), memID);
            ArrayList<Tree.Node> myLeaves = myTree.distinctLeaves(); //omit some duplicates
            getRelation = mType==RELATION;
            if (!getRelation) {
                parentage(myLeaves);
            } else {
                //get the second tree - mine
                oTree = new Tree(getActivity(), MainActivity.myOwner);
                ArrayList<Tree.Node> tLeaves = oTree.distinctLeaves(); //omit some duplicates

                ArrayList<Tree.Node> myLeaves_2 = reduce(oTree, myLeaves); // get common vertices
                ArrayList<Tree.Node> tLeaves_2 = reduce(myTree, tLeaves); // get common vertices

        Log.d(LOGGER, "number of choices : " + myLeaves_2.size());

//                ArrayList<Tree.Node> newLeaves = new ArrayList<>(); // will replace Leaves_2

                // determine the least common
                for (int i = 0; i < myLeaves_2.size(); i++) {
        Log.d(LOGGER, "trying choice : " + i);
                    //fix the path on myTree
                    ArrayList<Tree.Node> p = myTree.path(myLeaves_2.get(i).getMyad());
                    //and find corresponding  on new path in tLeaves_2
                    Tree.Node nmgood, nm_prev, nt_prev,
                            nt = tLeaves_2.get(i);
                    nm_prev = nt_prev = nt;

                    for (Tree.Node nm : p) {
                        if (!nm.getID().equals(nt.getID())) {
                            break;
                        }
                        nm_prev = nm;
                        nt_prev = nt;
                        nt = oTree.getNode(nt.homewards());
                    }
                    // skip if we have been seen before
                    if (nm_prev.testFlag()) {
                        continue;
                    }
                    nm_prev.setFlag();
                    myTree.getSpouse(nm_prev).setFlag(); // skip the spouse too ? what if there isnt one??
//                    newLeaves.add(nm_prev); // keep the idntical node
                    //proceed ..
                    nmgood = myTree.getNode(nm_prev.homewards());
        Log.d(LOGGER, "same M : " + nm_prev.getID());
        Log.d(LOGGER, "same T : " + nt_prev.getID() + " : " + nt_prev.print());
        Log.d(LOGGER, "diff M : " + nmgood.getID() + " : " + nmgood.print());
        Log.d(LOGGER, "diff T : " + nt.getID() + " : " + nt.print());

                    // determine size, location of siblings
                    String t_sibs = nt.getSiblings();
        Log.d(LOGGER, "siblings : " + t_sibs);
                    String nt_id = nt.getID(), nm_id = nmgood.getID();
                    int nm_src = nm_prev.getMyad();
                    String[] myAr = t_sibs.split(" ");
                    int i_t = -1, i_m = -1, num = myAr.length;
                    for (int j = 0; j < num; j++) {
                        if (myAr[j].equals(nt_id)) {
                            i_t = j;
                        }
                        if (myAr[j].equals(nm_id)) {
                            i_m = j;
                        }
                    }
                    if (i_m < 0 ) { // only one that can be -1
                        Log.d(LOGGER, "not direct siblings! m : ");
                        // try step relations
                        //                String spouses = nm_prev.getSpouses();
                        String m_sibs = nmgood.getSiblings();
                        Log.d(LOGGER, "other siblings : " + m_sibs);
                        myAr = m_sibs.split(" ");
                        // fix i_m this time ..
                        int m_num = myAr.length;
                        for (int j = 0; j < m_num; j++) {
                            if (myAr[j].equals(nm_id)) {
                                i_m = j;
                            }
                        }
//      return(num + "," + index + "," + T.nodeNames(m));
                        String t_str = num + "," + i_t + "," + oTree.nodeNames(nt),
                                m_str = m_num + "," + i_m + "," + myTree.nodeNames(nmgood);
                        Log.d(LOGGER, "line_1 data : t = " + t_str + ", m  == " + m_str);
                        // still have to decide the order m<t or t<m ? comes from deciding 1st row
                        String mypages = nm_prev.getMyPages();
                        String m_par = nmgood.getPar();
                        String t_par = nt.getPar();
                        myAr = mypages.split(" ");
                        int z_num = myAr.length;
                        for (int j = 0; j < z_num; j++) {
                            if (myAr[j].equals(t_par)) {
                                i_t = j;
                            }
                            if (myAr[j].equals(m_par)) {
                                i_m = j;
                            }
                        }
                        Log.d(LOGGER, "pages: " + mypages + ", t -> " + t_par + ", m -> " + m_par + " z_num = " + z_num);
                        String encoding = z_num + ",";//L.add(num + "," + index + "," + myTree.nodeNames(m));
                        // encoding for tree single lines, multiple expandables uses ':'
                        if (i_t < i_m) {
                            encoding += i_t + ":" + i_m + "," + oTree.nodeNames(nt_prev) + ":" + myTree.nodeNames(nm_prev);
                        } else
                            encoding += i_m + ":" + i_t + "," + myTree.nodeNames(nm_prev) + ":" + oTree.nodeNames(nt_prev); // the default
                        //mFirstLine.put(nm_src, encoding); // map the path source to encoding for first line
        Log.d(LOGGER, "encoding = " + encoding);
                    } else { // encode
                        // decide left, right, default is M left, T right
        Log.d(LOGGER, "num, i_m, i_t : " + num + ", " + i_m + ", " + i_t);
                        mRev.put(nm_src, i_m < i_t); // map the path source to the ordering of trees
        Log.d(LOGGER, "src =  " + nm_src);
                        String encoding = num + ",";//L.add(num + "," + index + "," + myTree.nodeNames(m));
                        // encoding for tree single lines, multiple expandables uses ':'
                        if (i_t < i_m) {
                            encoding += i_t + ":" + i_m + "," + oTree.nodeNames(nt) + ":" + myTree.nodeNames(nmgood);
                        } else
                            encoding += i_m + ":" + i_t + "," + myTree.nodeNames(nmgood) + ":" + oTree.nodeNames(nt); // the default
                        mFirstLine.put(nm_src, encoding); // map the path source to encoding for first line
        Log.d(LOGGER, "encoding = " + encoding);
                        // deal with it
                        mOsrc.put(nm_src,nt_prev.getMyad()); // assign the int address for tree Otree here
                        mNodes.add(nm_src);
                        mTitle.add(myTree.nodeNames(nm_prev));
                    }
                }
            }
        }
    }


    /**
     * reduce leaves L to the intersection with those of tree T
     * @param T : second tree, aside from myTree
     * @param L : leaves from another tree which must be interesected
     */
    ArrayList<Tree.Node> reduce(Tree T, ArrayList<Tree.Node> L){
        ArrayList<Tree.Node> Leaves_1 = T.distinctLeaves();
        ArrayList<Tree.Node> outLeaves = new ArrayList<>();
        for(Tree.Node n : L){
            String n_id = n.getID();
            for(Tree.Node m : Leaves_1){
                if (m.getID().equals(n_id)){ outLeaves.add(n); break;}
            }
        }
        return outLeaves;
    }


    /**
     * populate mNodes. mTitles with data from the given leaves
     * here the single tree is used
     *
     * @param leafNodes : subset of nodes on tree from which to derive paths
     */
    void parentage(ArrayList<Tree.Node> leafNodes) {
        //then populate nodes with the following
        for (Tree.Node n : leafNodes) {
            mNodes.add(n.getMyad());
            mTitle.add(myTree.nodeNames(n));
        }
    }

    /**
     * @param src : address of leaf node
     * @return : a list of the path from the node to my own node, encoded for TreeFragment
     */
    public ArrayList<String> encodedList(int src) {
        ArrayList<String> L = new ArrayList<>();
        ArrayList<Tree.Node> p = myTree.path(src);
        int k = 0;
        String sibs;
        for (Tree.Node m : p) {
            if (0 == k) {
                L.add(myTree.nodeNames(m));
            } else {
                sibs = m.getSiblings();
                String[] myAr = sibs.split(" ");
                int index = 0, num = myAr.length;
                for (int i = 0; i < num; i++) {
                    if (myAr[i].equals(m.getID())) {
                        index = i;
                        break;
                    }
                }
                L.add(num + "," + index + "," + myTree.nodeNames(m)); // encode for tree
            }
            k++;
        }
        return L;
    }


    String getStrip(Tree T,Tree.Node m){
        String sibs = m.getSiblings();
        String[] myAr = sibs.split(" ");
        int index = 0, num = myAr.length;
        for (int i = 0; i < num; i++) {
            if (myAr[i].equals(m.getID())) {
                index = i;
                break;
            }
        }
        return(num + "," + index + "," + T.nodeNames(m)); // encode for tree
    }

    public ArrayList<String> encodedList(boolean wantRelation,int src) {
        if(!wantRelation) return encodedList(src); // use the other
        int o_src = mOsrc.get(src);
        ArrayList<String> L = new ArrayList<>();
        ArrayList<Tree.Node> Lp, Rp;
        Tree LT,RT;
        if(mRev.get(src)){
            LT=myTree; RT=oTree;
            Lp = myTree.path(src);
            Rp = oTree.path(o_src);
        } else {
            LT=oTree; RT=myTree;
            Lp = oTree.path(o_src);
            Rp = myTree.path(src);
        }
        Tree.Node ln,rn;
        String lstr, rstr;
        int k=0,
            lsize = Lp.size(), rsize = Rp.size();
        int msize = Math.max(lsize, rsize);
        boolean go_on = true, two_column=true;
        while(go_on) {
            ln = k<lsize? Lp.get(k): null; // ln == left node ...
            rn = k<rsize? Rp.get(k): null;
            if (k == 0) {
                L.add(LT.nodeNames(ln)); // we either present just the names - OR (if contains a comma)
            } else if (k == 1) {
                L.add(mFirstLine.get(src)); // special first line - has a single strip
            } else {
                lstr = null==ln? "" : getStrip(LT,ln);
                if(lstr.isEmpty() && two_column){
                    two_column=false;
                    lstr = "skip&left"; // we want to skip the first xs
                }
                rstr = null==rn? "" : getStrip(RT,rn);
                if(rstr.isEmpty() && two_column){
                    two_column=false; // we simply ignore the second xs
                    lstr = "right&"+lstr;
                }
                if(!lstr.isEmpty() && !rstr.isEmpty()){
                    L.add(lstr+"&"+rstr);
                } else  if (!lstr.isEmpty()){
                    L.add(lstr);
                } else if (!rstr.isEmpty()){
                    L.add(rstr);
                }
    Log.d(LOGGER, "LL: " + L.get(L.size()-1));
            }
            //decide whether to proceed
            k++;
            go_on = k<msize;
        }
        return L;
    }

    @Override
    public void onResume() {
        super.onResume();
        // this is to allow resume on rotation - not really after sleep/ moving away from the activity ...
        // try getChildFragmentManager ??  getActivity().getSupportFragmentManager()
        LineageAdapter mAdapter = new LineageAdapter(getChildFragmentManager());
        ViewPager mPager = (ViewPager) getActivity().findViewById(R.id.viewpager);
        mPager.setAdapter(mAdapter);
    }


    /**
     * our pageadapter for the viewpager - we use the same layout as in general ..
     * uses globals mNodes and mTtitle - could have been passed as extra parameters ...
     */
    private class LineageAdapter extends FragmentStatePagerAdapter {
        //private ArrayList<Integer> nodes; // source members
        public LineageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (mNodes == null) {// shouldn't happen
                return null;
            }
            // otherwise - deal with nodes at position
            return TreeFragment.newInstance(encodedList(getRelation,mNodes.get(position)));
        }

        @Override
        public int getCount() {
            return (null == mNodes) ? 0 : mNodes.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitle.get(position); //pages.get(position).getName();
        }
    }

}
