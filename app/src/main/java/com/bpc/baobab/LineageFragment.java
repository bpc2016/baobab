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
    public static final String TREE_TYPE = "TREE TYPE"; // either single or double

    private static final String LOGGER = "bpc_lineage"; //Log.d(LOGGER, "page = " + real_id + ", member = " + member_id);
    public static final int SOURCES = 0;
    public static final int RELATION = 1;
    public static final String TYPE = "TYPE"; // of search we want - either sources or relation

    private String memID;
    private ArrayList<Integer> mNodes = new ArrayList<>();
    private ArrayList<String> mtNodes = new ArrayList<>(); // alternate when we compare two nodes
    private ArrayList<String> mTitle = new ArrayList<>();
    private Tree myTree, oTree;
    private HashMap<Integer, Boolean> mRev = new HashMap<>(); // whether we are reversed or not (for teh first line)
    private HashMap<Integer, String> mLineOne = new HashMap<>(); // encoded first line per src
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
                    String sibs = nt.getSiblings();
        Log.d(LOGGER, "siblings : " + sibs);
                    String nt_id = nt.getID(), nm_id = nmgood.getID();
                    int nm_src = nm_prev.getMyad();
                    String[] myAr = sibs.split(" ");
                    int i_t = -1, i_m = -1, num = myAr.length;
                    for (int j = 0; j < num; j++) {
                        if (myAr[j].equals(nt_id)) {
                            i_t = j;
                        }
                        if (myAr[j].equals(nm_id)) {
                            i_m = j;
                        }
                    }
                    if (i_m < 0 || i_t < 0) {
        Log.d(LOGGER, "need to use a higher level - not direct siblings! ");
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
                        mLineOne.put(nm_src, encoding); // map the path source to encoding for first line
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
     *  return the least common node down myTree and along T_1 from n
     * @param T : second tree
     * @param n : the common leaf node to start from
     * @return : the desired final common
     */
    Tree.Node getLeast(Tree T, Tree.Node n){
        // descend to least common
        Tree.Node n0=n,n1=n;
        Tree.Node nn0=null,nn1=null;
        boolean agree=true;
        while(agree){
            nn0= myTree.getNode(n0.homewards());
            nn1=T.getNode(n1.homewards());
            agree= nn1.getID().equals(nn0.getID());
            if(agree) {n0=nn0; n1=nn1;}
        }
        Log.d(LOGGER, "starting from : " + n.print());
        Log.d(LOGGER, "final common: " + n0.print());
        // see how the two compare here
//        Log.d(LOGGER, "myTree: " + n0.print());
//        Log.d(LOGGER, "T: " + n1.print());
        Log.d(LOGGER, "M: " + myTree.nodeNames(n0));
        Log.d(LOGGER, "T: " + T.nodeNames(n1));
        ArrayList<Tree.Node> q = T.path(n1.getMyad());
        for (Tree.Node m:q){
            Log.d(LOGGER, "T: " + T.nodeNames(m));
        }
        return n0;
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
        String sibs = "";
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
                L.add(LT.nodeNames(ln)); //doesn't matter here
            } else if (k == 1) {
                L.add(mLineOne.get(src));
            } else {
                lstr = null==ln? "" : getStrip(LT,ln);
                if(lstr.isEmpty() && two_column){
                    two_column=false;
                    lstr = "skip"; // we want to skip the first xs
                }
                rstr = null==rn? "" : getStrip(RT,rn);
                if(rstr.isEmpty() && two_column){
                    two_column=false; // we simply ignore the second xs
                }
                if(!lstr.isEmpty() && !rstr.isEmpty()){
                    L.add(lstr+"&"+rstr);
                } else  if (!lstr.isEmpty()){
                    L.add(lstr);
                } else if (!rstr.isEmpty()){
                    L.add(rstr);
                }
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
