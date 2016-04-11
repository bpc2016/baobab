package com.bpc.baobab;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by hp on 1/14/2016.
 */
public class SearchFragment extends ListFragment {

    private static final String LOGGER = "bpc_search"; // Log.d(LOGGER, "no arguments!");
    public static final String QUERY = "Query String";

    private searchListener mCallback;
    private String mQuery;
    private ArrayList<Member> myList = new ArrayList<>() ;
    private searchAdapter mAdapter;


    public SearchFragment() {
    } // generic constructor


    // Container Activity must implement this interface
    public interface searchListener {
        void toPageSpecial(String target, Member me);
        void noOp();
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            mCallback = (searchListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement searchListener");
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mQuery = args.getString(QUERY);
        } else {
            Log.d(LOGGER, "no arguments!");
        }
        if (mQuery == null) mCallback.noOp();
//        setRetainInstance(true); // solves basic config change issues
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.search_fragment, container, false);


        TextView results = (TextView) v.findViewById(R.id.results);

//        int count = fillView(mQuery);

        int count = new Family(getActivity()).fillView(
                Family.SEARCH_LAST_NAME
                ,new String[] {mQuery+"%"}
                ,myList
        );
        String out = count + " results for  '" + mQuery + "'";
        results.setText(out);

        mAdapter = new searchAdapter(getActivity(), R.layout.search_fragment);
        setListAdapter(mAdapter);

        return v;
    }

    /*
    // populate the list based on query
    private int fillView(String query) {
        String selection = MemberTable.COLUMN_LAST + " LIKE ?";
        String[] selectionArgs = new String[] {query+"%"};

        Cursor cr = getActivity().getContentResolver().query(
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
                String yob = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_YOB));
                String yod = cr.getString(cr.getColumnIndex(MemberTable.COLUMN_YOD));
                String dates = yob; if (!yod.isEmpty()){dates += "-"+yod;}
                m.setYob(dates); // fake it here
                myList.add(m);
            }
        }
        return myList.size();
    }
*/


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String target = mAdapter.getPage(position);
        if(target.isEmpty()) {
            Toast.makeText(getActivity(), "Error! please try another", Toast.LENGTH_LONG).show();
        } else {
            Member me = myList.get(position);
            mCallback.toPageSpecial(target,me);
        }
    }



    // the array adapter used for the listview of  members
    private class searchAdapter extends ArrayAdapter<String> {

        public searchAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // assign the view we are converting to a local variable
            View v = convertView;
            // first check to see if the view is null. if so, we have to inflate it.
            if (v == null) {
                LayoutInflater inflater
                        = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.search_row, null);
            }
            TextView textViewName = (TextView) v.findViewById(R.id.name);
            TextView textViewDate = (TextView) v.findViewById(R.id.date);

            // fill its contained textviews etc
            Member m = myList.get(position);

            if (m != null) {
                // use its data
                String names = m.getFirst()+" "+m.getLast();
                textViewName.setText(names);
                textViewDate.setText(m.getYob());
            }
            // the view must be returned to our activity
            return v;
        }// end of overidden getView

        @Override
        public int getCount() {
            return myList.size();
        }

        public String getPage(int position) {
            Member me = myList.get(position);
            String page_id= me.getPar();
            if(page_id.isEmpty()) page_id = me.getMyPages(); // may be a spouse with no parent
            if(!page_id.isEmpty() && page_id.contains(" ")){
                page_id = page_id.replaceFirst(" (.*)","");
            }
            return page_id;
        }
    }

}

