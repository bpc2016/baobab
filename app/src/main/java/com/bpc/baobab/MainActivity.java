package com.bpc.baobab;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bpc.baobab.database.FtDatabaseHelper;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        FileFragment.fileActionsListener,
        OnePageFragment.pageActionsListener,
        NotesFragment.noteListener,
        BioFragment.editListener,
        AddParentsFragment.addparentListener,
        SorterFragment.sortListener,
        SearchFragment.searchListener,
        NewMemberFragment.newmemberListener {

    private static final String LOGGER = "bpc_main"; //Log.d(LOGGER, "page = " + real_id + ", member = " + member_id);

    public static String myPid;
    public static String myOwner = "";

    private String myPar = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/ //todo implement the snack bar feature

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout maybe
        if (findViewById(R.id.fragment_container) != null) {
            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Get the intent, verify the action and get the query
            Intent intent = getIntent();

            //respond to search: here so that we distinguish from config change indeced intent!
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                handleIntent(intent);
                return;
            }

                /* this is  how we go from page --> owner --> fregment of views */
            File database = MainActivity.this.getDatabasePath(FtDatabaseHelper.DATABASE_NAME);

            if (!database.exists()) {
                // new File Fragment to be placed in the activity layout
                FileFragment fileOps = new FileFragment();
                // Add the fragment to the 'fragment_container' FrameLayout
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, fileOps).commit();
            } else {
                getFirstPage();  // this is called provided a db exists

                // new File Fragment to be placed in the activity layout
                PagesFragment thePages = new PagesFragment();
                // first page?
                Bundle args = new Bundle();
                args.putString(PagesFragment.PAGE, myPid);
                args.putStringArray(PagesFragment.SIBLINGS, getSiblings(myOwner));
                args.putString(PagesFragment.PARID, myPar); // which has been set by getsiblings()
                thePages.setArguments(args);
                // Add the fragment to the 'fragment_container' FrameLayout
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, thePages).commit();
            }
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        getSupportFragmentManager().popBackStack(); // to make back button work ok
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doMySearch(query);
        }
    }

    private void doMySearch(String query) {
        SearchFragment toFind = new SearchFragment();
        Bundle args = new Bundle();

        args.putString(SearchFragment.QUERY, query);
        toFind.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, toFind)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);


        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.file) {  // start file activity
            FileFragment fileOps = new FileFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fileOps).addToBackStack(null).commit();
            return true;
        }

        if (id == R.id.exit) {  // quit
            finish();
            return true;
        }

        if (id == R.id.home) {  // go home
            toPage(new Family(this).getFirstPid());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //-----------------   implementing interfaces  -----------------------------------------------

    //-----------  fileoptionslistener

    @Override
    public void startPages() {
        getFirstPage();  // this is called provided a db exists
        //getSupportFragmentManager().popBackStack(); // we dont want to recall our call to file
        // Create fragment and move there
        toPage(myPid, myOwner);
    }

    //-----------   pageactionlistener

    /*
    * member - the spouse
    * owner - the owner
    * page - current page (ID)
    */
    public void newSpouse(Member spouse, Member owner, String page) {
        NewMemberFragment toCreate = new NewMemberFragment();
        Bundle args = new Bundle();

        args.putSerializable(NewMemberFragment.SPOUSE, spouse);
        args.putSerializable(NewMemberFragment.OWNER, owner);
        args.putInt(NewMemberFragment.TYPE, OnePageFragment.SPOUSE);
        args.putString(NewMemberFragment.PAGEID, page);
        toCreate.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, toCreate)
                .addToBackStack(null)
                .commit();
    }


    public void newChild(Member member, Page page) {  // member is the new member, page current one
        NewMemberFragment toCreate = new NewMemberFragment();
        Bundle args = new Bundle();

        args.putSerializable(NewMemberFragment.CHILD, member);
        args.putSerializable(NewMemberFragment.PAGE, page);
        args.putInt(NewMemberFragment.TYPE, OnePageFragment.CHILD);
        toCreate.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, toCreate)
                .addToBackStack(null)
                .commit();
    }


    public void editMember(Member member) {
        BioFragment toEdit = new BioFragment();
        // then set its arguments
        Bundle args = new Bundle();
        args.putSerializable(BioFragment.MEMBER, member);
        toEdit.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, toEdit)
                .addToBackStack(null)
                .commit();
    }

    public void notesMember(Member member) {
        NotesFragment toEdit = new NotesFragment();
        // then set its arguments
        Bundle args = new Bundle();
        args.putSerializable(NotesFragment.MEMBER, member);
        toEdit.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, toEdit)
                .addToBackStack(null)
                .commit();
    }


    public void lineAge(String mem_id, int type) {
        LineageFragment toView = new LineageFragment();
        // then set its arguments
        Bundle args = new Bundle();
        args.putString(LineageFragment.MEMBER_ID, mem_id);
        args.putInt(LineageFragment.TYPE, type);
        toView.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, toView)
                .addToBackStack(null)
                .commit();
    }



    @Override
    public void newPage(Member member, Member other, int type) {
        if (type == OnePageFragment.CHILD) { // pre-existing child, now wants onw page ...
            String member_id = member.getID();
            Family fh = new Family(this);
            String page_id = fh.createPage(
                    new Page()
                            .setOwner(member_id)
            );
            //update my member
            member.setMyPages(page_id);
            fh.updateMember(member);

            toPage(page_id, member_id); // go there
            return; // done
        }
        //else can only be -- new parents ...
        if (type == OnePageFragment.FATHER) {
            AddParentsFragment toCreate = new AddParentsFragment();
            Bundle args = new Bundle();
            args.putSerializable(AddParentsFragment.CHILD, member);
            args.putSerializable(AddParentsFragment.FATHER, null);
            toCreate.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, toCreate)
                    .addToBackStack(null)
                    .commit();
            return;
        }
        //otherwise, its the second call: mother
        getSupportFragmentManager().popBackStack();

        AddParentsFragment toCreate = new AddParentsFragment();
        Bundle args = new Bundle();
        args.putSerializable(AddParentsFragment.CHILD, member);
        args.putSerializable(AddParentsFragment.FATHER, other);
        toCreate.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, toCreate)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void toPage(String page_id, String member_id) {
        //fix case of empty member
        if (member_id.isEmpty()) {
            Family fh = new Family(this);
            Cursor cr = fh.getPageCursor(page_id);
            member_id = fh.getOwner(cr);
        }
        if (member_id.isEmpty())
            return; // no good!
        // Create fragment and give it arguments
        PagesFragment thePages = new PagesFragment();
        Bundle args = new Bundle();
        args.putString(PagesFragment.PAGE, page_id);
        args.putStringArray(PagesFragment.SIBLINGS, getSiblings(member_id));
        args.putString(PagesFragment.PARID, myPar); // which has been set by getsiblings()
        thePages.setArguments(args);
        // Replace whatever is in the fragment_container view with this fragment,
        // and addDigest the transaction to the back stack so the user can navigate back
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, thePages)
                .addToBackStack(null)         // we DO go back there with BACK button
                .commitAllowingStateLoss();     // rather than .commit because of call from sync process
    }


    @Override
    public void toPage(String page_id) {
        if (page_id.isEmpty()) return;
        Cursor p_cr = new Family(this).getPageCursor(page_id);
        // fetch the value
        if (p_cr == null) {
            Toast.makeText(this, "error retrieving address! ", Toast.LENGTH_LONG).show();
            return;
        }
        String owner = new Family(this).getOwner(p_cr);

        toPage(page_id, owner);
    }

    @Override
    public void swapPage(String page_id, String owner, String spouse) {
        Page thePage = new Page(page_id)
                .setOwner(spouse)
                .setSpouse(owner);
        Boolean ok = (!owner.isEmpty() && !spouse.isEmpty());
        String msg = ok ? "Swap made permanent on this page" : "Operation failed!";
        if (ok) {
            new Family(this)
                    .updatePage(thePage);
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }


    @Override
    public void deleteSpouse(Member theSpouse, Page thePage, Member theOwner) {
        String msg = "Delete successful";
        String spouse_id = theSpouse.getID();
        String next_pid = thePage.getID(); //this_pid;

        if (theSpouse.getMyPages().contains(" ")) {
            msg = "Failed: delete this spouses other spouses first ";
        } else {
            String page_owner = thePage.getOwner();
            String kids = thePage.getKids();
            if (!page_owner.equals(spouse_id)) {
                if (!kids.isEmpty())
                    msg = "Failed: delete children first";
            } else
                msg = "Failed: cannot delete page owner";
        }
        if (msg.equals("Delete successful")) {
            //do it
            Family fh = new Family(this);
            String my_pages = theOwner.getMyPages();
            boolean single = (my_pages.isEmpty()) || !my_pages.contains(" ");
            //special case when the spouse is unique!
            if (single) {
                fh.delMember(spouse_id)
                        .updatePage(
                                thePage.setSpouse(Family.EMPTY)
                        );
            } else {
                String this_pid = thePage.getID();
                if (this_pid.equals(myPid)) { // homepage!
                    msg = "Failed: please change home page first";
                } else {
                    theOwner.remMyPages(this_pid);
                    fh.delMember(spouse_id)
                            .updateMember(theOwner)
                            .delPage(this_pid);
                    next_pid = theOwner.nextPage(); // set above
                }
            }
        }
        toPage(next_pid, theOwner.getID());
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void deleteChild(Member cHild, Member oWner, Page thePage) {
        String msg = "Delete successful";
        String page_id = cHild.getMyPages();
        Family fh = new Family(this);

        if (!page_id.isEmpty()) {
            msg = "Sorry, this member cannot be deleted yet";
            //check if we have any data
            if (!page_id.contains(" ")) { // only one page - no multi spouses
                msg = fh.noDependents(page_id)
                        ? "Delete successful"
                        : "Failed! examine this member's dependents ";
            }
        }
        if (msg.equals("Delete successful")) {
            // do it
            if (thePage.hasManyKids()) {
                fh.delMember(cHild.getID()) //delete the child
                        .updatePage(thePage //remove from page
                                        .remKid(cHild.getID())
                        );
            } else {
                fh.delMember(cHild.getID())
                        .updatePage(thePage
                                .setKids(Family.EMPTY)  //delete the child
                        );
            }
            //do we need to delete a page too?
            if (!page_id.isEmpty()) {
                fh.delPage(page_id);
            }
            //refresh ... this is why we have the second parameter
            toPage(thePage.getID(), oWner.getID());  // was parent
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }


    //-----------  newpagelistener, sortpagelistner

    @Override
    public void toPageSpecial(String pageid, Member member) {
        getSupportFragmentManager().popBackStack();
        //get owner member ID
/*        String memberID = "";
        Family fh = new Family(this);
        String p_Id = fh.getFirstPid(); // adam ...

        if (!p_Id.isEmpty()) {
            Cursor p_cr = fh.getPageCursor(p_Id);
            // fetch the value
            if (p_cr != null) {
                memberID = fh.getOwner(p_cr);
            }
        }
        Tree T_0 = new Tree(this, memberID); // my personal tree - based on my memberID
        Tree T_1 = new Tree(this, member.getID());
        ArrayList<Tree.Node> Leaves_0 = T_0.leaves();
        ArrayList<Tree.Node> Leaves_1 = T_1.leaves();
        Log.d(LOGGER, "leaves 0 size: " + Leaves_0.size() + ", leaves_1: " + Leaves_1.size());
        boolean found = false;
        Tree.Node nn1 = null, nn0 = null;
        for (Tree.Node n1 : Leaves_1) {
            String m1 = n1.getID();
            for (Tree.Node n0 : Leaves_0) {
                found = n0.getID().equals(m1);
                if (found) {
                    nn0 = n0;
                    nn1 = n1;
                    break;
                }
            }
            if (found) break;
        }
        if (found) {
            Log.d(LOGGER, "common: " + nn0.print());
            //descend from there
            Tree.Node nnn0=null,nnn1=null;
            boolean agree=true;
            while(agree){
                nnn0=T_0.getNode(nn0.homewards());
                nnn1=T_1.getNode(nn1.homewards());
                agree= nnn1.getID().equals(nnn0.getID());
                if(agree) {nn0=nnn0; nn1=nnn1;}
            }
            Log.d(LOGGER, "final common: " + nn0.print());
        } else Log.d(LOGGER, "nothing common!");*/
        // we need to go to this page
        toPage(pageid);
    }


    @Override
    public void toPageSpecial(String pageid) {
        getSupportFragmentManager().popBackStack();
        // we need to go to this page
        toPage(pageid);
    }
    //---------- newmemnberlistener


    @Override
    public void sortAfterNew(Member Owner, Page page, int type) {
        // return from newmemberfragment ...
        getSupportFragmentManager().popBackStack();
        // then sort them
        doSort(Owner, page, type);
    }

    //-------------------------   utilities   -----------------------------------------------


    public void doSort(Member member, Page page, int type) {
        SorterFragment toSort = new SorterFragment();
        // then set its arguments
        Bundle args = new Bundle();
        args.putSerializable(SorterFragment.MEMBER, member);
        args.putSerializable(SorterFragment.MPAGE, page);  //
        args.putInt(SorterFragment.TYPE, type);  // whether child|spouse
        toSort.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, toSort)
                .addToBackStack(null)
                .commit();
    }


//miscellaneous


    /*
       return the first page - either from Adam, or if not, create Adam and point to the last item
       requires that the database already exists!
    */

    private void getFirstPage() {
        Family fh = new Family(this);
        String p_Id = fh.getFirstPid(); // adam ...

        if (!p_Id.isEmpty()) {
            myPid = p_Id;
            Cursor p_cr = fh.getPageCursor(p_Id);
            // fetch the value
            if (p_cr != null) {
                myOwner = fh.getOwner(p_cr);
            }
        }
    }


    private String[] getSiblings(String member) {
        Family fh = new Family(this);
        String[] res = new String[]{member};  // default result
        Cursor cr = fh.getMemCursor(member);
        if (cr == null) return res;
        myPar = fh.getPar(cr);
        if (myPar.isEmpty()) return res;
        // fetch the siblings ...
        cr = fh.getPageCursor(myPar);
        if (cr == null) return res; // shouldnt happen
        String mems = fh.getKids(cr);
        return mems.split(" ");    // assigned to mSiblings in pageFrag
    }

    //------------------------ noops -------------------------------

    @Override
    public void noOp() {
        getSupportFragmentManager().popBackStack();
    }


}// --- end


