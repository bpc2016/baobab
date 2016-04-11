package com.bpc.baobab;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hp on 1/4/2016.
 */
public class OnePageFragment extends ListFragment {

    public static final String OWNER = "owner";
    public static final String PAGEID = "page id";

    private static final String LOGGER = "bpc_onepage"; // Log.d(LOGGER, "onepage -  no arguments!");
    public static final int CHILD = 1;
    public static final int FATHER = 2;
    public static final int MOTHER = 3;
    private static final int PARENT = 0;
    public static final int SPOUSE = 2;
    private static final int NEWPARENT = 3;
    private static final int NEWCHILD = 4;
    private static final int FIXPAGE = 5;
    private static final int DELCHILD = 6;
    private static final int DELSPOUSE = 7;

    private pageActionsListener mCallback;
    private String mOwner;
    private String mPageID;
    private TextView oNameText;
    private TextView oDateText;
    private ImageView oImage;
    private TextView sNameText;
    private TextView sDateText;
    private View layout1;
    private View layout2;
    private View leftlayout;

    private Member mOmember = null, mSmember = null;
    private Page mPage = null;
    private ViewAdapter vAdapter;
    private boolean noSpouse = true; // assume that none there

    private ImageButton addChildBtn;
    private ImageButton addSpouseBtn;
    private ImageButton sortMembersBtn;
    private ImageButton setHomeBtn;
    private ImageButton sharePageBtn;

    //---------------------------------------------------------------------------------

    public OnePageFragment() {
    } // generic

    public static final OnePageFragment newInstance(String owner, String pageid) {
        OnePageFragment f = new OnePageFragment();
        Bundle bdl = new Bundle();
        bdl.putString(OWNER, owner);
        bdl.putString(PAGEID, pageid);
        f.setArguments(bdl);
        return f;
    }

    // our interface
    public interface pageActionsListener {
        void toPage(String par_id);

        void toPage(String page_id, String owner);

        void editMember(Member member);

        void notesMember(Member member);

        void newPage(Member member, Member other, int type);

        void swapPage(String page_id, String owner, String spouse);

        void deleteChild(Member child, Member me, Page this_page);

        void deleteSpouse(Member spouse, Page page, Member owner);

        void newSpouse(Member member, Member mOmember, String page);

        void doSort(Member member, Page page, int type);

        void newChild(Member member, Page page);  // member is the new member, page current one

        void lineAge(String mem_id, int type);
    }
    //------------------------------------------------------------------------------------------

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle args = getArguments();
        if (args != null) {
            mOwner = args.getString(OWNER);
            mPageID = args.getString(PAGEID);
        } else {
            Log.d(LOGGER, "onepage -  no arguments!");
        }

        View v = inflater.inflate(R.layout.one_page_frag, container, false);

        layout1 = v.findViewById(R.id.layout1);
        layout2 = v.findViewById(R.id.layout2);
        leftlayout = v.findViewById(R.id.leftSide);
        oNameText = (TextView) v.findViewById(R.id.ow_name);
        oDateText = (TextView) v.findViewById(R.id.o_date);
        oImage = (ImageView) v.findViewById(R.id.o_icon);
        sNameText = (TextView) v.findViewById(R.id.s_name);
        sDateText = (TextView) v.findViewById(R.id.s_date);
        addChildBtn = (ImageButton) v.findViewById(R.id.add_child);
        addSpouseBtn = (ImageButton) v.findViewById(R.id.add_spouse);
        sortMembersBtn = (ImageButton) v.findViewById(R.id.sort_menu);
        sharePageBtn = (ImageButton) v.findViewById(R.id.share_menu);
        setHomeBtn = (ImageButton) v.findViewById(R.id.set_home);

        // this viewadpater is to stand between the db access from cursorloader and display list
        vAdapter = new ViewAdapter(getActivity(), R.layout.one_page_frag, null);  //
        setListAdapter(vAdapter);
        getDetails(); // uses mOwner etc

        return v;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Member theKid = vAdapter.getKid(position);
        String pageID = theKid.getMyPages();
        String owner = theKid.getID();

        if (pageID.isEmpty()) {
            useDialog(NEWCHILD, theKid); //mCallback.newPage(theKid, "child");
            return;
        }
        // typical reg exp under java //
        String REGEX = "\\s.*"; //anything beyond first spaces
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(pageID);
        String actualPage = m.replaceAll("");

        mCallback.toPage(actualPage, owner); // go there
    }


    @Override
    public void onStart() {
        super.onStart();
        try {
            mCallback = (pageActionsListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement pageActionsListener");
        }

        // this is the only way to get a longitemclicklistener for listfragments
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
                //Get the one to edit
                Member mem = vAdapter.getKid(position);
                if (mem == null) {
                    Toast.makeText(getActivity(),
                            "empty member - really a long click?", Toast.LENGTH_LONG).show();
                    return true;
                }
                longClickDialog(CHILD, mem);
                return true;
            }
        });

        addChildBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                if (mPageID.isEmpty()) {
                    Toast.makeText(getActivity(), "Please create a page for this member - navigate up first", Toast.LENGTH_LONG).show();
                    Log.d(LOGGER, "footer failed to get pageID");
                    return;
                }
                if (noSpouse) {
                    Toast.makeText(getActivity(), "You cannot add children without a spouse record!", Toast.LENGTH_LONG).show();
                    return;
                }
                useDialog(CHILD, null);
            }
        });

        addSpouseBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mPageID.isEmpty()) {
                    Toast.makeText(getActivity(),
                            "Failed: please create a page for this member first", Toast.LENGTH_LONG).show();
                    return;
                }
                useDialog(SPOUSE, null);
            }
        });

        sortMembersBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mPageID.isEmpty()) {
                    Toast.makeText(getActivity(), "Page error!", Toast.LENGTH_LONG).show();
                    return;
                }
                sortDialog();
            }
        });

        sharePageBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mPageID.isEmpty()) {
                    Toast.makeText(getActivity(), "Page error!", Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(getActivity(), "Share this page - coming soon :-)", Toast.LENGTH_LONG).show();
            }
        });


        setHomeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mPageID.isEmpty()) {
                    Toast.makeText(getActivity(), "Please create a page for this member - navigate up first", Toast.LENGTH_LONG).show();
                    Log.d(LOGGER, "footer failed to get pageID");
                    return;
                }
                new Family(getActivity()).resetHome(mPageID);
            }
        });
    }


    private void createChild() {
        Member member = new Member();
        // set up first child
        member.setLast(mPage.getSurname())
                .setPar(mPageID)
                .setGender("F"); // par we are sending forward the kernel new member
        mCallback.newChild(member, mPage);
    }


    private void createSpouse() {
        Member member = new Member();
        member.setGender(mOmember.getGender().equals("M") ? "F" : "M");
        if (noSpouse) { // set up first spouse
            member.setMyPages(mPageID); // mypages
            noSpouse = false;
        }
        mCallback.newSpouse(member, mOmember, mPageID);
    }

//==================================================================================================
    // dialogs

    public void useDialog(final Integer type, final Member mem) {
        String question = "";
        switch (type) {
            case CHILD:
                question = "Add a child?";
                break;
            case SPOUSE:
                question = "Add a spouse?";
                break;
            case NEWPARENT:
                question = "This will create a new parents page. Continue?";
                break;
            case NEWCHILD:
                question = "This will create a new child page. Continue?";
                break;
            case DELCHILD:
                question = "Delete this child?";
                break;
            case DELSPOUSE:
                question = "Delete this spouse?";
                break;
            case FIXPAGE:
                question = "Keep this swap?";
                break;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(question);
        builder.setCancelable(true);

        builder.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        switch (type) {
                            case CHILD:
                                createChild();
                                break;
                            case SPOUSE:
                                createSpouse();
                                break;
                            case NEWPARENT:
                                mCallback.newPage(mem, null, FATHER);
                                break;
                            case NEWCHILD:
                                mCallback.newPage(mem, null, CHILD);
                                break;
                            case DELCHILD:
                                mCallback.deleteChild(mem, mOmember, mPage);
                                break;
                            case DELSPOUSE:
                                Log.d(LOGGER, "thepage spouse = " + mPage.getSpouse());
                                mCallback.deleteSpouse(mem, mPage, mOmember);  // was mPageID
                                break;
                            case FIXPAGE:
                                mCallback.swapPage(mPageID, mOmember.getID(), mSmember.getID());
                                break;
                        }
                        dialog.cancel();
                    }
                });

        builder.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }


    private void sortDialog() {
        final String[] items = getActivity().getResources().getStringArray(R.array.sort_array); // for locale reasons
        final Integer[] icons = new Integer[]{R.drawable.ic_child_care_black_24dp, R.drawable.ic_supervisor_account_black_24dp};

        TypedArray typedArray = getActivity()
                .obtainStyledAttributes(null, R.styleable.AlertDialog, R.attr.alertDialogStyle, 0);
        DialogAdapter dAdapter = new DialogAdapter(
                getActivity(),
                typedArray.getResourceId(R.styleable.AlertDialog_listItemLayout, 0),
                items,
                icons);


        new AlertDialog.Builder(getActivity())
//                .setTitle(R.string.pick_edit)
//                .setItems(R.array.sort_array, new DialogInterface.OnClickListener() {
                .setAdapter(dAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<String> memList;
                        switch (which) {
                            case 0: // sort kids
                                if (mPage.getDigests().size() < 2) {
                                    Toast.makeText(getActivity(), "You need at least 2 children to perform a sort", Toast.LENGTH_LONG).show();
                                } else
                                    mCallback.doSort(vAdapter.getKid(0), mPage, CHILD);
                                break;
                            case 1: // sort spouses
                                if (mOmember.getMyPages().length() < 2) {
                                    Toast.makeText(getActivity(), "You need at least 2 spouses to perform a sort", Toast.LENGTH_LONG).show();
                                } else
                                    mCallback.doSort(mOmember, mPage, SPOUSE);
                                break;
                        }
                    }
                })
                .create().show();
    }


    private void longClickDialog(final int where, final Member mem) {
        final String[] par_items = getActivity().getResources().getStringArray(R.array.edit_parent_array); // for locale reasons
        final String[] child_items = getActivity().getResources().getStringArray(R.array.edit_choices_array); // for locale reasons
        final Integer[] icons = new Integer[]{R.drawable.ic_edit_black_24dp, R.drawable.ic_event_note_black_24dp
                , R.drawable.ic_timeline_black_24dp,R.drawable.ic_device_hub_black_24dp, R.drawable.ic_share_black_24dp, R.drawable.ic_delete_black_24dp};

        TypedArray typedArray = getActivity()
                .obtainStyledAttributes(null, R.styleable.AlertDialog, R.attr.alertDialogStyle, 0);

        DialogAdapter dAdapter_par = new DialogAdapter(
                getActivity(),
                typedArray.getResourceId(R.styleable.AlertDialog_listItemLayout, 0),
                par_items,
                icons);
        DialogAdapter dAdapter_child = new DialogAdapter(
                getActivity(),
                typedArray.getResourceId(R.styleable.AlertDialog_listItemLayout, 0),
                child_items,
                icons);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setTitle(R.string.pick_edit);
        if (where == PARENT) {
            builder.setAdapter(dAdapter_par, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: // edit biodata
                            mCallback.editMember(mem);
                            break;
                        case 1: // edit notes
                            mCallback.notesMember(mem);
                            break;
                        case 2: // share member
                            Toast.makeText(getActivity(),
                                    "share - coming soon :-)", Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            });
        } else builder.setAdapter(dAdapter_child, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // edit biodata
                        mCallback.editMember(mem);
                        break;
                    case 1: // edit notes
                        mCallback.notesMember(mem);
                        break;
                    case 2: // graph lineage
                        mCallback.lineAge(mem.getID(),LineageFragment.SOURCES);
                        break;
                    case 3: // graph relation
                        mCallback.lineAge(mem.getID(),LineageFragment.RELATION);
                        break;
                    case 4: // share member
                        Toast.makeText(getActivity(),
                                "share - coming soon :-)", Toast.LENGTH_LONG).show();
                        break;
                    case 5: // delete member
                        if (where == SPOUSE) {
                            useDialog(DELSPOUSE, mem);
                        } else
                            useDialog(DELCHILD, mem); //mCallback.deleteChild(mem);
                        break;
                }
            }
        });
        builder.create().show();
    }

    private void MakeTree(Member mem) {
//        Tree T0  = new Tree(getActivity(),)
        Tree T = new Tree(getActivity(),mem.getID());
        ArrayList<Tree.Node> Leaves = T.leaves();
//        if (null != Leaves) for(Tree.Node n : Leaves){
////            if(n.getIndex()%2==0)
//                Log.d(LOGGER, n.print() );
//        }
        if(null!=Leaves) {
            Tree.Node n = Leaves.get(Leaves.size()-1);
            int[] p = T.path(n);
            for(int i : p) Log.d(LOGGER, "i: "+T.getMember(i).getFullname());
        }

        Log.d(LOGGER, " ---- reverse -----");

        if(null!=Leaves) {
            int ind = Leaves.get(Leaves.size()-1).getMyad();
            ArrayList<Tree.Node> p = T.path(ind);
            for (Tree.Node m : p) Log.d(LOGGER, "i: "+T.parents(m) +" >> "+m.getSiblings());
        }

//        Log.d(LOGGER, "logs"); //use crumbs last index is: " + Leaves.get(0).print() + " .. "+ T.NodeAt(Leaves.get(0).getIndex())  );
        Toast.makeText(getActivity(),
                                "tree done, size is: " + T.getCount(), Toast.LENGTH_LONG).show();
    }
//===================================================================================
//    alert dialog adapter


    private class DialogAdapter extends ArrayAdapter<String> {
        private Integer[] icons;

        public DialogAdapter(Context context, int resource, String[] items, Integer[] icons) {
            super(context, resource, items);
            this.icons = icons;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setCompoundDrawablesWithIntrinsicBounds(icons[position], 0, 0, 0);
            textView.setCompoundDrawablePadding(
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getContext().getResources().getDisplayMetrics()));
            return view;
        }
    }


//==================================================================================================
    // our view adapter


    private class ViewAdapter extends ArrayAdapter<Member> {
        // declaring our ArrayList of sampleItems
        private ArrayList<Member> persons;

        public ViewAdapter(Context context, int textViewResourceId, ArrayList<Member> persons) {
            super(context, textViewResourceId, persons);
            this.persons = persons;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // assign the view we are converting to a local variable
            View v = convertView;
            // first check to see if the view is null. if so, we have to inflate it.
            // to inflate it basically means to render, or show, the view.
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.child_row, null);
            }
            TextView textViewName = (TextView) v.findViewById(R.id.name);
            TextView textViewDate = (TextView) v.findViewById(R.id.date);
            ImageView imageView = (ImageView) v.findViewById(R.id.drag_handle);

            // fill its contained textviews etc
            Member m = persons.get(position);

            if (m != null) {
                // use its data
                textViewDate.setText(m.getDates());
                // do we need expansion?
                textViewName.setTextColor(Color.BLACK); // reset !
                String more = m.getMyPages();
                if (more.length() == 0)
                    textViewName.setTextColor(Color.RED);

                String first = m.getFirst();
                textViewName.setText(first);

                String s = m.getGender();
                if (s.equals("M")) {
                    imageView.setImageResource(R.drawable.male_64);
                } else {
                    imageView.setImageResource(R.drawable.female_64);
                }
            }
            // the view must be returned to our activity
            return v;
        }// end of overidden getView

        @Override
        public int getCount() {
            if (persons == null) {
                return 0;
            } else {
                return persons.size();
            }
        }

        public void swapMembers(ArrayList<Member> ms) {
            if (persons == ms)
                return;

            this.persons = ms;
            notifyDataSetChanged();
        }

        public Member getKid(int pos) {
            return persons.get(pos);
        }
    }


    private void setParents(Member me, Member sp) {
        // use mOmember and mSmember, fields have already been attached in oncreateview
        if (me == null) return;  // shouldnt happen!

        layout1.setOnClickListener(parClickListener(me)); // click handler
        layout1.setOnLongClickListener(parLongClickListener(me));

        layout2.setOnClickListener(parClickListener(sp)); // only one that can be null!!
        layout2.setOnLongClickListener(parLongClickListener(sp));

        leftlayout.setOnClickListener(parSwapListener(sp));
        leftlayout.setOnLongClickListener(parSwapLongListener(sp));

        String fullName = me.getFirst() + " " + me.getLast(); //+" "+me.getID();
        oNameText.setText(fullName);

        String more = me.getPar();
        // do we need expansion?
        if (more.isEmpty())
            oNameText.setTextColor(Color.RED);
        else {
            oNameText.setTextColor(Color.BLACK); // reset !
        }

        oDateText.setText(me.getDates());
        oImage.setImageResource(R.drawable.female_72);
        if (me.getGender().equals("M")) {
            if (null != mPage) mPage.setSurname(me.getLast()); // sometimes, there is no page!!
            oImage.setImageResource(R.drawable.male_72);
        }

        if (sp != null) {
            more = sp.getPar();
            fullName = sp.getFirst() + " " + sp.getLast();
            sNameText.setText(fullName);
            sNameText.setTextColor(Color.BLACK); // reset !
            if (more.isEmpty()) sNameText.setTextColor(Color.RED);

            sDateText.setText(sp.getDates());
            if (sp.getGender().equals("M")) mPage.setSurname(sp.getLast());
        }

    }

    private View.OnClickListener parClickListener(final Member mem) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mem == null) { // only possible with spouse
                    Toast.makeText(getActivity(),
                            "new spouse?", Toast.LENGTH_LONG).show();
                    return;
                }
                String par_id = mem.getPar();
                if (par_id.isEmpty()) { // needs to edit
                    useDialog(NEWPARENT, mem); //mCallback.newPage(mem, "parent");
                    return;
                }
                mCallback.toPage(par_id); // go there
            }
        };
    }

    private View.OnClickListener parSwapListener(final Member sp) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sp == null) {
                    Toast.makeText(getActivity(),
                            "no spouse!", Toast.LENGTH_LONG).show();
                    return;
                }
                mCallback.toPage(mPageID, sp.getID());
            }
        };
    }

    private View.OnLongClickListener parSwapLongListener(final Member sp) {
        return new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                if (sp == null) { // only possible with spouse
                    Toast.makeText(getActivity(),
                            "no spouse!", Toast.LENGTH_LONG).show();
                    return true;
                }
                mCallback.toPage(mPageID, sp.getID());
                useDialog(FIXPAGE, null);
                return true;
            }
        };
    }

    private View.OnLongClickListener parLongClickListener(final Member mem) {
        return new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                if (mem == null) { // only possible with spouse
                    Toast.makeText(getActivity(),
                            "empty member - spouse long click?", Toast.LENGTH_LONG).show();
                    return true;
                }
                if ((mSmember != null) && mem.getID().equals(mSmember.getID())) { // are we the spouse?
                    longClickDialog(SPOUSE, mem);
                } else
                    longClickDialog(PARENT, mem);
                return true;
            }
        };
    }


    //-----------------------------------------------------------
    private void getDetails() {
        // fetch the page members IDs
        //initialise:
        String theSpouse = "", theOwner = "", theChildren = "";
        ArrayList<Member> childrenList = null;
        noSpouse = true; // default

        if (!mPageID.isEmpty()) {
            Family fHelper = new Family(getActivity());
            Cursor cr = fHelper.getPageCursor(mPageID);
            theOwner = fHelper.keepCr().getOwner(cr);
            theSpouse = fHelper.keepCr().getSpouse(cr);
            theChildren = fHelper.getKids(cr);

            // init mPage
            mPage = new Page(mPageID)
                    .setOwner(theOwner)
                    .setSpouse(theSpouse)
                    .setKids(theChildren);

            String wHich = mOwner.equals(theOwner) ? theOwner : theSpouse;
            mOmember = new Family(getActivity())  // First is better
                    .getMembers(wHich).get(0);
            if (!theSpouse.isEmpty()) {
                noSpouse = false;
                mSmember = new Family(getActivity())  // second is better
                        .getMembers(mOwner.equals(theOwner) ? theSpouse : theOwner).get(0);
                childrenList = new Family(getActivity()).getMembers(theChildren); // null if none
                if (null != childrenList) {
                    for (Member m : childrenList) {
                        mPage.addDigest(m);
                    }
                }
            }
        } else { // just have a member w/o a page
            mOmember = new Family(getActivity())  // First is better
                    .getMembers(mOwner).get(0);
        }

        vAdapter.swapMembers(childrenList);  // was mKids
        setParents(mOmember, mSmember);
    }

}
