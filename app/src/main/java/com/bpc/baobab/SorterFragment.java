package com.bpc.baobab;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by hp on 1/14/2016.
 */
public class SorterFragment extends ListFragment {

    private static final String LOGGER = "bpc_sorter"; // Log.d(LOGGER, "no arguments!");
    public static final String MEMBERS = "Members To Sort";
    public static final String SELECTED = "Selected";
    public static final String ID2CHANGE = "Parent or Spouse id to change";
    public static final String TYPE = "being sorted"; // child | spouse
    public static final String MEMBER = "MEMBER";
    public static final String MPAGE = "MPAGEID";


    private sortListener mCallback;
    private int mSelected;
    private ArrayList<String> unOrderedList;
    private sortAdapter mAdapter;

    private int type;
    private String target = "";
    private Member theMember;
    private Page thePage;


    public SorterFragment() {
    } // generic constructor


    // Container Activity must implement this interface
    public interface sortListener {
        void toPageSpecial(String target); // if empty, do nothing
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            mCallback = (sortListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement sortListener");
        }

        // this is the only way to get a longitemclicklistener for listfragments
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
                //long click selects a new 'currrent item'
                mAdapter.setSelected(position);
                return true;
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.sorter_fragment, container, false);

        mAdapter = new sortAdapter(getActivity(), R.layout.sorter_fragment, unOrderedList); //, adList);
        setListAdapter(mAdapter);

        mAdapter.setSelected(mSelected);


        Button confirmButton = (Button) v.findViewById(R.id.btn_save);
        ImageButton helpButton = (ImageButton) v.findViewById(R.id.btn_help);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                finish();
            }
        });


        helpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Sort your members here.\n Member selected to move is in RED\n Long click to change selection\n Short click on destination slot");
                builder.setCancelable(true);
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        return v;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        mAdapter.moveTo(position);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = savedInstanceState != null
                ? savedInstanceState
                : getArguments();
        if (args != null) {
            theMember = (Member) args.getSerializable(MEMBER);
            thePage = (Page) args.getSerializable(MPAGE);
            type = args.getInt(TYPE);
        } else {
            Log.d(LOGGER, "no arguments!");
            finish();
        }
        unOrderedList = type == OnePageFragment.SPOUSE
                ? new Family(getActivity()).fetchSpouses(theMember, thePage.getID())
                : thePage.getDigests();

        if (null == savedInstanceState) mSelected = theMember.getPosition();

        if (savedInstanceState == null && type == OnePageFragment.SPOUSE) { // right at the start
            target = unOrderedList.get(mSelected).split(",")[2]; // the id at the end of first member
        }

        setRetainInstance(true); // solves basic config change issues
    }

    // fragments do not have a finish() method! this calls a popstack in main
    private void finish() {
        // get the new ordering
        String ordering = mAdapter.getOrdering();
        if (type == OnePageFragment.CHILD) {
            thePage.setKids(ordering);
            new Family(getActivity())
                    .updatePage(thePage);
        } else {
            theMember.setMyPages(ordering);
            new Family(getActivity())
                    .updateMember(theMember);
        }
        mCallback.toPageSpecial(target); // either empty or the new page
    }


    // the array adapter used for the listview of  members
    private class sortAdapter extends ArrayAdapter<String> {
        private ArrayList<String> adList;

        public sortAdapter(Context context, int textViewResourceId, ArrayList<String> aList) {
            super(context, textViewResourceId);
            adList = aList;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // assign the view we are converting to a local variable
            View v = convertView;
            // first check to see if the view is null. if so, we have to inflate it.
            if (v == null) {
                LayoutInflater inflater
                        = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.sorter_row, null);
            }
            TextView textViewName = (TextView) v.findViewById(R.id.name);
            TextView textViewDate = (TextView) v.findViewById(R.id.date);

            // fill its contained textviews etc
            String m = adList.get(position);
            String[] marr = m.split(","); // name,dates,mem_id

            if (marr.length > 0) {
                // use its data
                String names = marr[0];
                textViewName.setText(names);
                String dates = marr[1];
                textViewDate.setText(dates);

                // are we the chosen one?
                textViewName.setTextColor(Color.BLACK); // reset !
                textViewDate.setTextColor(Color.BLACK); // reset !
                if (position == mSelected) {
                    textViewName.setTextColor(Color.RED);
                    textViewDate.setTextColor(Color.RED);
                }
            }
            // the view must be returned to our activity
            return v;
        }// end of overidden getView

        @Override
        public int getCount() {
            return adList.size();
        }

        public void setSelected(int position) {
            mSelected = position;
            notifyDataSetChanged();
        }

        public String getOrdering() {
            String ordering = "";
            for (int i = 0; i < adList.size(); i++) {
                if (i == 0) {
                    ordering = adList.get(i).split(",")[2];
                } else {
                    ordering = ordering + " " + adList.get(i).split(",")[2];
                }
            }
            return ordering;
        }

        public void moveTo(int position) {
            if (position == mSelected) return;
            String temp = adList.get(mSelected);
            if (position > mSelected) {
                for (int i = mSelected; i < position; i++) {
                    adList.set(i, adList.get(i + 1));
                }
            }
            if (position < mSelected) {
                for (int i = mSelected; i > position; i--) {
                    adList.set(i, adList.get(i - 1));
                }
            }
            adList.set(position, temp);
            mSelected = position;
            notifyDataSetChanged();
        }
    }

}

