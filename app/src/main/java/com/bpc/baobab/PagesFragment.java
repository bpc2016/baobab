package com.bpc.baobab;

import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bpc.baobab.contentprovider.FtContentProvider;
import com.bpc.baobab.database.MemberTable;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by hp on 1/4/2016.
 */
public class PagesFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // public constants
    public static final String PAGE = "page";
    public static final String SIBLINGS = "SIBLINGS";
    public static final String PARID = "PARENT_ID";

    private static final String PAGES = "PAGES";
    private static final String LOGGER = "bpc_pagesfragment";
    private static final String SPAGE = "PAGER_PAGE";


    // standard constructor
    public PagesFragment() {}

    //private constants & variables
    private final static int BALL_LOADER = 0;
    private final static String[] sibProjection
            = new String[]{MemberTable.COLUMN_ID
            , MemberTable.COLUMN_MY_PAGES
            ,MemberTable.COLUMN_FIRST
            , MemberTable.COLUMN_PAR_PAGES};   //todo - remove this last one

    //    private String mOwner;
    private String mPageID;
    private String mPar;
    private String[] mySiblings;
    private MyAdapter mAdapter;
    private ViewPager pager;
    private ArrayList<String> myPages = new ArrayList<>();
    private int sPos = 0; //, mPos=0;
    private  HashMap<String, String> memsHash = new HashMap<>();

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Bundle args = savedInstanceState != null
//                ? savedInstanceState
//                : getArguments();
//        if (args != null) {
//            mPar = args.getString(PARID);
//            sPos = args.getInt(SPAGE);
//            mySiblings = args.getStringArray(SIBLINGS); // ?? was in onstart
//            if (savedInstanceState != null) myPages = args.getStringArrayList(PAGES);
//        } else {
//            Log.d(LOGGER, "no arguments!");
//        }
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) { // essential to allow correct position in viewer after rotation
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            sPos = savedInstanceState.getInt(SPAGE); // esp. this assignment
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // If activity recreated (such as from screen rotate), restore
        // the previous page selection set by onSaveInstanceState().
        // This is primarily necessary when in the two-pane layout.
        if (savedInstanceState != null) { // model this on NewMemberFragment handling
            myPages = savedInstanceState.getStringArrayList(PAGES);
            mPar = savedInstanceState.getString(PARID);
            sPos = savedInstanceState.getInt(SPAGE);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.pages_fragment, container, false);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the current page position in case we need to recreate the fragment
        outState.putStringArrayList(PAGES, myPages);
        outState.putString(PARID, mPar);
        if (pager != null) {
            outState.putInt(SPAGE, sPos);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // During startup, check if there are arguments passed to the fragment.
        // onStart is a good place to do this because the layout has already been
        // applied to the fragment at this point so we can safely call the methods
        // below that rely on interpreting layouts
        if(mySiblings == null) {  // otherwise, use what we have
            Bundle args = getArguments();
            if (args != null) {
                mPageID = args.getString(PAGE);
                mPar = args.getString(PARID);
                mySiblings = args.getStringArray(SIBLINGS);
            }
        }
    }


    @Override
    public void onResume(){
        super.onResume();
        // this is to allow resume on rotation - not really after sleep/ moving away from the activity ...
        // try getChildFragmentManager ??  getActivity().getSupportFragmentManager()
        mAdapter = new MyAdapter( getChildFragmentManager(), null, null);  // allegedly why it keeps crashing with observable ... error
        pager = (ViewPager) getActivity().findViewById(R.id.viewpager);
        pager.setAdapter(mAdapter);
        pager.addOnPageChangeListener(myPageChangeListener());
        if(myPages.size()==0){
            getLoaderManager().restartLoader(BALL_LOADER, null, this); // 'this' fragment implements a cursorloader
        } else { //Log.d(LOGGER, "## using cache ");
            useCachedData(myPages);
        }
    }

    private ViewPager.OnPageChangeListener myPageChangeListener() {
        ViewPager.OnPageChangeListener OPCL = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                sPos=position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        };
        return OPCL;
    }




    // called from main activity
    // gives pageid,member,parid,# as data - some items may be empty!
    public String[] getCurrentPage(){
        return (myPages.get(sPos)+",#").split(",");
    }

    public String getCurrentPageID(){
        return (myPages.get(sPos)+",#").split(",")[0];
    }


//    public String getCurrentMember() {
//        return currentMember;
//    }
    //cursorloader

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;
        if (id == BALL_LOADER) {
            String comma_str = "(" + Join(mySiblings, ",") + ")";
            cursorLoader = new CursorLoader(getActivity().getApplicationContext(),
                    FtContentProvider.MEMBER_URI,
                    sibProjection,  // the projection
                    "_id in " + comma_str, // selection
                    null, null);
        } // use an else if we have another loader ...
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        if(myPages.size()>0) {
            return;
        }

        HashMap<String, String> namesH = new HashMap<>();

        if (loader.getId() == BALL_LOADER) {
            mAdapter.swapCursor(data);
            ArrayList<Page> pageList = new ArrayList<>();
            int myPos = 0;
            if (data != null) {
                while (data.moveToNext()) { // effectively cover all the members of the page ...
                    String[] out = new String[sibProjection.length];
                    Integer j = 0;
                    out = new String[sibProjection.length]; // TODO - reduce this - remove the par pages
                    for (String c : sibProjection) {
                        out[j] = data.getString(data.getColumnIndex(c));
                        j++;
                    }
                    memsHash.put(out[0], out[1]); // id --> mypages
                    namesH.put(out[0], out[2]); // id --> firstname
                }
                int position = 0;
                for (String m : mySiblings) {
                    String adsList = memsHash.get(m);
                    String firstname = namesH.get(m);
                    String[] theAds = adsList.split(" ");
                    if (theAds.length > 1) {
                        for (String id : theAds) {
                            pageList.add( new Page(id,m,mPar,firstname));
                            myPages.add(id+","+m+","+mPar+","+firstname);
                            if (id.equals(mPageID)) myPos = position;
                            position++;
                        }
                    } else {
                        pageList.add( new Page(adsList,m,mPar,firstname) );
                        myPages.add(adsList+","+m+","+mPar+","+firstname);
                        if (adsList.equals(mPageID)) myPos = position;
                        position++;
                    }
                }
            }
            mAdapter.swapPages(pageList);
            pager.setCurrentItem(myPos); // set this here because we are asynchronous!
        }
    }

    private void useCachedData(ArrayList<String> mypages) {
        // revive the pages and set current item on pos
        ArrayList<Page> pageList = new ArrayList<>();
        for(String item : mypages){
            String xitem = item+",0"; // yuk, need to extend in case last entry is empty
            String[] triple = xitem.split(",");  // id,member,par_id
            pageList.add( new Page(triple[0],triple[1],triple[2],triple[3]) );
        }
        mAdapter.swapPages(pageList);
        pager.setCurrentItem(sPos);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }




    // viewpager

    public class MyAdapter extends FragmentStatePagerAdapter {
        private Cursor cursor;
        private ArrayList<Page> pages;

        public MyAdapter(FragmentManager fm, Cursor cursor, ArrayList<Page> pages) {
            super(fm);
            this.cursor = cursor;
            this.pages = pages;
        }

        @Override
        public Fragment getItem(int position) {
            if (pages == null) {// shouldn't happen
                return null;
            }
            // otherwise - deal with Page(position)
            Page thePage = pages.get(position);
            return OnePageFragment.newInstance(thePage.getOwner(), thePage.getID());
        }

        @Override
        public int getCount() {
            if (pages == null)
                return 0;
            else
                return pages.size();
        }

        public void swapCursor(Cursor c) {
            if (cursor == c)
                return;

            this.cursor = c;
            notifyDataSetChanged();
        }


        public void swapPages(ArrayList<Page> pageList) {
            if (pages == pageList)
                return;

            this.pages = pageList;
            notifyDataSetChanged();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return pages.get(position).getName();
        }
    }


    //miscellaneous

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
