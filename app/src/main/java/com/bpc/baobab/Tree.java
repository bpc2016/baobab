package com.bpc.baobab;



import android.content.Context;

import java.util.ArrayList;

/**
 * Created on 3/14/2016.
 * the basic tree class - a tree is a list of nodes; a node is an extension of member
 */
public class Tree {
    private ArrayList<Node> nodes;
    private Family fh; // Family helper
    private static final String LOGGER = "bpc_tree"; //Log.d(LOGGER, "page = " + real_id );


    public Tree(Context ctx, String memID){
        fh = new Family(ctx);
        nodes = new ArrayList<>();
        Node N = new Node();
        fh.loadMember(N,memID);
        N.home_ad=-1; /* stop condition */
        N.my_ad=0;
        N.p_length=1;//N.index=N.p_length=1;
        nodes.add(N);
        //develop the tree
        grow();
    }

    //methods

    public int getCount(){
        return nodes.size();
    }

    public Member getMember(int position){
        return nodes.get(position);
    }

    /**
     *
     * @param node - that we are expanding by adding the two parents, if possible
     *             we have also added extra data: the string of sibling ids
     * @return number of nodes attached: eiither 2 or 0
     */
    private int expand(Node node){
        int parents = 0;
        if (node.is_leaf  && node.unseen){
            String [] parentData = fh.getParents(node.getPar());
            if (null != parentData) {
                node.siblings = parentData[2]; // see Family.getParents
                //use data for expansion ...
                Node N = new Node(),
                father = new Node(), mother = new Node();
                fh.loadMember(N,parentData[0]);
                if(N.getGender().equals("M")){
                    father = N; N = mother;
                } else {
                    mother = N; N = father;
                }
                fh.loadMember(N,parentData[1]);
                this.add(father,node)
                    .add(mother,node);
                parents = 2;
            }
            node.unseen = false;
            node.is_leaf = (parents==0); // false if parents > 0
        }
        return  parents;
    }

    private Tree add(Node parent, Node my){
        parent.home_ad = my.my_ad;
        parent.p_length = my.p_length+1;
        parent.my_ad = nodes.size();
        //attach it
        nodes.add(parent);
        return this;
    }


    /**
     * grow the tree, a node at a time - this is a binary tree
     * so we add two new nodes per existing (unseen) leaf
     * until we can no longer do so.
     *
     * we call 'expand' on each leaf
     */
    private void grow(){
        boolean moreUnseen = true;
        while(moreUnseen) {
            int added = 0;
            for (int i=0;i<nodes.size();i++) {
                added += expand(nodes.get(i));
            }
            moreUnseen = added>0;
        }
    }

    /**
     *
     * @return arraylist of leaves
     *
     * */
    public ArrayList<Node> leaves(){
        ArrayList<Node> list = new ArrayList<>();
        for (int i=0;i<nodes.size();i++) {
            Node thenode = nodes.get(i);
            if (thenode.is_leaf) list.add(thenode);
        }
        if ( list.size()==0 ) return null;
        return list;
    }

    /**
     *
     * @param m : node
     * @return 'name -- spouse_name'  or  'name' (when it is the self)
     */
    public String nodeNames(Node m) {
        if (0==m.my_ad) return  m.getShortname();
        int spouse_ad = m.getGender().equals("M") ? m.my_ad+1 : m.my_ad-1; // we are attached next to each other
        Member spouse = nodes.get(spouse_ad);
        return  m.getShortname()+";"+spouse.getShortname();
    }

    /**
     * as with leaves, but remove duplicates when a leafnode has a spouse that is also a leaf
     * in that case, return only the M one
     * @return :  list of nodes from T
     */
    public ArrayList<Node> distinctLeaves(){
        ArrayList<Node> list = new ArrayList<>();
        for (int i=0;i<nodes.size();i++) {
            Node thenode = nodes.get(i);
            if (thenode.is_leaf) {
                int spouse_ad = thenode.getGender().equals("M") ? thenode.my_ad+1 : thenode.my_ad-1;
                Node spouse = nodes.get(spouse_ad);
                if( ! spouse.is_leaf )list.add(thenode);
                if( spouse.is_leaf && spouse.getGender().equals("F") ) { list.add(thenode); }
            }
        }
        if ( list.size()==0 ) return null;
        return list;
    }


    /**
     * @param n - node to search from
     * @return - the nodes addresses of a path from n to home (backwards)
     */
    public int[] path(Node n){
        int [] ret = new int[n.p_length];
        Node nxt=n;
        for(int i=n.p_length-1;i>0;i--){
            ret[i] = nxt.my_ad;
            nxt = nodes.get(nxt.home_ad);
        }
        return ret;
    }

    /**
     * @param ind - tree index of node to search from
     * @return list of nodes starting at T(ind) down to root = T(0)
     */
    public ArrayList<Node> path(int ind){
        Node n = nodes.get(ind);
        ArrayList<Node> res = new ArrayList<>();
        res.add(n);
        Node nxt=n;
        while(nxt.home_ad != -1){
            nxt = nodes.get(nxt.home_ad);
            res.add(nxt);
        }
        return res;
    }

    public Node getNode(int i) {
        return nodes.get(i);
    }

    public String parents(Node m) {
        if (0==m.my_ad) return "(0) " + m.getFullname();
        int spouse_ad = m.getGender().equals("M") ? m.my_ad+1 : m.my_ad-1; // we are attached next to each other
        Member spouse = nodes.get(spouse_ad);
        int reduced = m.p_length-1;
        return "("+reduced+")" + m.getFullname()+" = "+spouse.getFullname();
    }

    public Node getSpouse(Node m) {
        int spouse_ad = m.getGender().equals("M") ? m.my_ad+1 : m.my_ad-1;
        return nodes.get(spouse_ad);
    }

    protected class Node
            extends Member{
        private boolean is_leaf=true;
        private boolean unseen=true;
        private int my_ad; /*pointer into nodes list */
        private int home_ad; /* the way home: */
        public int p_length; /* path length - to root*/
        public String siblings=""; /* space delimited listing of my siblings - includes myself! */
        private boolean flag = false; /* useful in searches etc */


        /**
         * base constructor
         */
        public  Node(){}

        /**
         *  fetch Member with ID memID and use as a Node
         * @param memID - the ID of teh member to fetch
         */
        public Node(String memID){
            super(memID); // base class is Member
        }

        public int getMyad(){ return my_ad; }
        public int homewards() { return home_ad; }
        public String getSiblings() { return siblings; }

        public boolean testFlag() {
            return flag;
        }

        public void setFlag() {
            flag = true;
        }
    }

}
