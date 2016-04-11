package com.bpc.baobab;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bpc.baobab.contentprovider.FtContentProvider;
import com.bpc.baobab.database.MemberTable;
import com.bpc.baobab.database.PageTable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

//import com.bpc.baobab.database.FtDatabaseHelper;


/*
* allows to enter a new note 
* or to change an existing
*/

public class FileFragment extends Fragment {

    public static final String adamLastName = "1234";
    public static final String adamField = MemberTable.COLUMN_LAST;
    private static final String SEP_CH = "~";   // alternative to csv
    private static final String SEP_CHRE = "\\~\\n";   // for removing extra at eol
    private static final String MEMBERS_TXT = "member.txt";
    private static final String MEMBERS_BAK = "member.bak";
    private static final String PAGES_TXT = "page.txt";
    private static final String PAGES_BAK = "page.bak";
    private static final String BAOBAB_TXT = "baobab.txt";
    private static final String BAOBAB_BAK = "baobab.bak";
    private int result_code = -1; //RESULT_CANCELED;

//    private static final int IMPORT_DATA = 1;
//    private static final int EXPORT_DATA = 2;
    private static final int EXPORT_BOTH = 3;
    private static final int IMPORT_BOTH = 4;

    private final String[] membersCols = {
            MemberTable.COLUMN_ID, MemberTable.COLUMN_LAST,
            MemberTable.COLUMN_FIRST, MemberTable.COLUMN_YOB,
            MemberTable.COLUMN_YOD, MemberTable.COLUMN_GENDER,
            MemberTable.COLUMN_MY_PAGES, MemberTable.COLUMN_PAR_PAGES,
            MemberTable.COLUMN_PREF, MemberTable.COLUMN_NOTES
    };

    private final String[] pageCols = {
            PageTable.COLUMN_ID, PageTable.COLUMN_OWNER,
            PageTable.COLUMN_SPOUSE, PageTable.COLUMN_KIDS
    };

    // android 6.0 requires extra security checks

    private static final int REQUEST_EXTERNAL_STORAGE = 200; // was 1;
    private static String[] PERMISSIONS_STORAGE = {  // TODO sdk 23 permissions nonsense
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    fileActionsListener mCallback;

    // Container Activity must implement this interface
    public interface fileActionsListener {
        void startPages();
    }

    private ProgressDialog progressDialog;
    private boolean isTaskRunning = false;

    // see https://androidresearch.wordpress.com/2013/05/10/dealing-with-asynctask-and-screen-orientation/
    private class AsyncTaskPBar extends AsyncTask<String, Integer, String> {

        private final Integer whichTask; // allow various tasks

        public AsyncTaskPBar(Integer whichTask) {
            this.whichTask = whichTask;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(getActivity());
            String message = "";
            switch (whichTask) {
                case IMPORT_BOTH:
                    message = "Loading data ...";
                    progressDialog.setMax(sumLength);
                    break;
                case EXPORT_BOTH:
                    message = "Backing up database ...";
                    progressDialog.setMax(sumdbSizes);
                    break;
            }
            isTaskRunning = true;
            //setup progressDialog
            progressDialog.setMessage(message);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setIndeterminate(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setProgress(0);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            int mProgressStatus = 1;
            switch (whichTask) {
                case IMPORT_BOTH:
                    while (mProgressStatus < sumLength) {
                        // do our background work here
                        mProgressStatus = doWork(IMPORT_BOTH);
                        progressDialog.setProgress(mProgressStatus);
                    }
                    // once finished
                    if (mProgressStatus > 0 ) { //  == 100
                        mCallback.startPages();
                    } // else fail silently
                    break;
                case EXPORT_BOTH:
                    while (mProgressStatus < sumdbSizes){ //100 && mProgressStatus > 0) {
                        // do our background work here
                        mProgressStatus = doWork(EXPORT_BOTH);
                        progressDialog.setProgress(mProgressStatus);
                    }
                    // once finished
                    if (mProgressStatus > 0 ) { //== 100) {
                        mCallback.startPages();
                    } // else fail silently
                    break;
            }
            return null;    //return "All Done!"; - this String return is forced by the override
        }

        @Override
        protected void onPostExecute(String result) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            isTaskRunning = false;
        }
    }


    //====================================================================

	/*
      @Override
	  protected void onCreate(Bundle bundle) {
		  super.onCreate(bundle);
		  // android 6.0 requires extra work with permissions
		  if(Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1){
			  verifyStoragePermissions(FileFragment.this);
		  } else
			  setContentView(R.layout.ftfile);
	  }  */

    // recommended from asynctask

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // If we are returning here from a screen orientation
        // and the AsyncTask is still working, re-create and display the
        // progress dialog. the attachment to mProgressStatus is in teh asynctask background
        if (isTaskRunning) {
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setIndeterminate(false);
            progressDialog.show();
        }
    }

    @Override
    public void onDetach() {
        // All dialogs should be closed before leaving the activity in order to avoid
        // the: Activity has leaked window com.android.internal.policy... exception
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDetach();
    }

    //-----------------------------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.file_fragment, container, false);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (fileActionsListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement fileActionsListener");
        }
        // set up our menu of buttons
        doButtons();
    }



    private void importBoth(){ // uses the fields pages_import,members_import);
        String msg = "";
        //get file lengths (number of lines)
        pageLength = countBefore(import_both);
        if (pageLength < 0) {
            msg = "baobab.txt file missing?";
            // try the Download directory
            import_both = "Download/"+import_both;
            pageLength = countBefore(import_both);
            if(pageLength>0) msg="";
        }
        int memLength = countLines(import_both) - pageLength - 1;
        if (memLength < 0) msg += "baobab.txt file missing??";
        if (msg.isEmpty()) {
            sumLength = pageLength + memLength;
            //delete the two tables;
            getActivity().getContentResolver().delete(FtContentProvider.EMPTY_DB_URI, null, null);
            //use an asynctask to display progressdialog
            AsyncTaskPBar asyncTask = new AsyncTaskPBar(IMPORT_BOTH);
            asyncTask.execute();
        } else {
            show(msg);
        }
    }


    // called in the onclick handlers in dobuttons
    private void exportBoth(boolean backup){
        String msg = "";
        //get file lengths (number of lines)
        pagedbSize = countRecords(PageTable.TABLE_PAGE);
        if (pagedbSize < 0) msg = "page database error!";
        int memdbSize = countRecords(MemberTable.TABLE_MEMBER);
        if (memdbSize < 0) msg += " member database error!";

        if (msg.isEmpty()) {
            sumdbSizes = pagedbSize + memdbSize;
            String sdcd = Environment.getExternalStorageDirectory().getPath();
            String theFile = backup ? BAOBAB_BAK : BAOBAB_TXT;
            total_dump = new File(sdcd + "/" + theFile);
            // truncate or create the files
            try {
                new FileOutputStream(total_dump);  // open, truncate if exists
                //use an asynctask to display progressdialog
                AsyncTaskPBar asyncTask = new AsyncTaskPBar(EXPORT_BOTH);
                asyncTask.execute();
                /*
                altenative syntax: anonymous
                    new AsyncTaskPBar(
                        EXPORT_BOTH
                    ).execute();
                 */
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            show(msg);
        }
    }


    private int soFar = 0;
    private int pageLength;
    private int sumLength;

    private int pagedbSize;
    private int sumdbSizes;
    private  File total_dump;
    private  String  import_both;

    private int doWork(int which) {
        int retVal=0;
        switch (which) {
            case IMPORT_BOTH:  // assumes we have set max at sumLength
                if (soFar < pageLength) {
                    soFar += uploadTable(FtContentProvider.PAGE_URI, import_both, pageCols, 0);
                } else if(soFar == pageLength){
                    soFar += 1; // skip the one line uploadTable(FtContentProvider.MEMBER_URI, members_import, membersCols, pageLength);
                } else { // otherwise do second step
                    soFar += uploadTable(FtContentProvider.MEMBER_URI, import_both, membersCols,0);
                }
                retVal = soFar;
                break;
            case EXPORT_BOTH:
                if (soFar < pagedbSize) {
                    soFar += dumpTable(FtContentProvider.PAGE_URI, total_dump, pageCols, 0);
                } else if (soFar == pagedbSize) { // otherwise do second step
                    soFar += dumpTable(null, total_dump, null, pagedbSize);
                } else {
                    soFar += dumpTable(FtContentProvider.MEMBER_URI, total_dump, membersCols, pagedbSize+1);
                }
                retVal = soFar;
                break;
        }
        return retVal;
    }


    private void doButtons() {
        getActivity().findViewById(R.id.btn_fresh)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //delete the two tables;
                        getActivity().getContentResolver().delete(FtContentProvider.EMPTY_DB_URI, null, null);
                        //set fresh ones
                        ContentValues values = new ContentValues();
                        values.put(adamField, adamLastName);
                        values.put(MemberTable.COLUMN_ID, "1");
                        values.put(MemberTable.COLUMN_MY_PAGES, "1"); // the only option!
                        getActivity().getContentResolver().insert(FtContentProvider.MEMBER_URI, values);
                        values.put(MemberTable.COLUMN_ID, "2");
                        values.put(MemberTable.COLUMN_FIRST, "First_Names");
                        values.put(MemberTable.COLUMN_LAST, "Last_Name");
                        values.put(MemberTable.COLUMN_GENDER, "M");
                        getActivity().getContentResolver().insert(FtContentProvider.MEMBER_URI, values);
                        values.clear();
                        values.put(PageTable.COLUMN_ID, "1");
                        values.put(PageTable.COLUMN_OWNER, "2");
                        getActivity().getContentResolver().insert(FtContentProvider.PAGE_URI, values);
                        mCallback.startPages();
                    }
                });

        getActivity().findViewById(R.id.btn_clear)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean ok = clearSrc();
                    }
                });

        getActivity().findViewById(R.id.btn_backup)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!isTaskRunning) {
                            exportBoth(true);
                        }
                    }
                });


        getActivity().findViewById(R.id.btn_restore)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        import_both = BAOBAB_BAK;
                        importBoth();
                    }
                });

        getActivity().findViewById(R.id.btn_import)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!isTaskRunning) {
                            import_both = BAOBAB_TXT;
                            importBoth();
                        }
                    }
                });

        getActivity().findViewById(R.id.btn_export)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!isTaskRunning) {
                            exportBoth(false); // NOT .bak
                        }
                    }
                });


    }


	/*
      @Override
	  public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
		  Boolean readAccepted = false;
		  Boolean writeAccepted = false;
		  switch(permsRequestCode){
		  case 200:
			  readAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED;
			  writeAccepted = grantResults[1]==PackageManager.PERMISSION_GRANTED;
			  break;
		  }
		  if (! readAccepted ){
			  show("read permissions denied. Please reinstall");
			  finish();
		  }
		  if (! writeAccepted ){
			  show("write permissions denied. Please reinstall");
			  finish();
		  }
		  // only if both true ...
		  show("passed permissions test ...");
		  setContentView(R.layout.ftfile);
	  }


	  @SuppressLint("NewApi")
	  public static void verifyStoragePermissions(Activity activity) {
		  // Check if we have write permission
		  int permission = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

		  if (permission != PackageManager.PERMISSION_GRANTED) {
			  // We don't have permission so prompt the user
			  activity.requestPermissions(PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
		  }
	  }
	*/

    public int countRecords(String dbname){
        Uri uri = Uri.parse(FtContentProvider.SIZE_URI + "/" + dbname);
        Cursor cr = getActivity().getContentResolver().query(uri, null, null, null, null);
        if (cr != null) {
            int size = cr.getCount();
            cr.close();
            return size;
        } else
            return -1;
    }

    public int countBefore(String filename) {
        int count = 0;
        try {
            String sdcd = Environment.getExternalStorageDirectory().getPath();
            File myFile = new File(sdcd + "/" + filename);
            InputStream is = new BufferedInputStream(new FileInputStream(myFile));
            byte[] c = new byte[1024];
            int readChars = 0;
            boolean goon=true;
            while ( goon && ( readChars = is.read(c)) != -1 ) { // we break with a blank line
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                        if(i<1023 && c[i+1]=='\n') { goon = false; break;}
                    }
                }
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (count==0) count = -1;
        return count;
    }

    // strictly for the TXT file
    private boolean clearSrc(){
        String sdcd = Environment.getExternalStorageDirectory().getPath();
        File myFile = new File(sdcd + "/" + BAOBAB_TXT);
        boolean deleted =  myFile.delete();
        myFile = new File(sdcd + "/Download/" + BAOBAB_TXT);
        if (!deleted) deleted =  myFile.delete();
        if (deleted) {
            show("Success: data source file cleared!");
            return true;
        } else {
            show("Failed: data source file not deleted!");
            return false;
        }

    }

    public int countLines(String filename) {
        int count = 0;
        try {
            String sdcd = Environment.getExternalStorageDirectory().getPath();
            File myFile = new File(sdcd + "/" + filename);
            InputStream is = new BufferedInputStream(new FileInputStream(myFile));
            byte[] c = new byte[1024];
            int readChars = 0;
            while ((readChars = is.read(c)) != -1) {
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (count==0) count = -1;
        return count;
//        return (count == 0 && !empty) ? 1 : count;
    }


    private final static int cHunk = 50;

    private int uploadTable(Uri uri, String fromFile, String[] Cols, int baseLine) {
        //Boolean success = false;
        int toSkip = 0, extra = 0;
        // write on SD card file data in the text file my_todo_data.txt
        try {
            String sdcd = Environment.getExternalStorageDirectory().getPath();
            File myFile = new File(sdcd + "/" + fromFile);
            FileInputStream fIn = new FileInputStream(myFile);
            BufferedReader myReader = new BufferedReader(
                    new InputStreamReader(fIn));
            String aDataRow = "";
            while ((aDataRow = myReader.readLine()) != null) {
                if (toSkip < soFar - baseLine) {
                    toSkip++;
                    continue;
                }
                // split the datarow into pieces, place in the Cols
                gather(uri, aDataRow, Cols);
                extra++;
                if (extra == cHunk) break;
            }
            myReader.close();
        } catch (Exception e) {
        }
        return extra; // the exact amount added
    }


    private void gather(Uri uri, String line, String[] Cols) {
        String[] csv = TextUtils.split(line, SEP_CH);
        ContentValues values = new ContentValues();
        for (int i = 0; i < csv.length; i++) {
            values.put(Cols[i], csv[i]);
        }
        getActivity().getContentResolver().insert(uri, values);
    }



    /*  called
     * 	dumpTable(FtContentProvider.MEMBER_URI,
     * 			"oufile.txt",
    //			new String[] {MemberTable.COLUMN_ID,MemberTable.COLUMN_LAST}
     * 	)
     */
    private int dumpTable(Uri uri, File outFile, String[] Cols, int baseLine) {
        int toSkip = 0, extra = 0;

        // write on SD card file data in the text box
        try {
            FileOutputStream fOut = new FileOutputStream(outFile,true);
            OutputStreamWriter myOutWriter =
                    new OutputStreamWriter(fOut);
            if(uri==null){                  // signals placea break
                myOutWriter.append("\n");   // insert a blank line
                extra = 1; // the single line
            } else {
                String buffer = "";
                Cursor cr = getActivity().getContentResolver().query(uri, null, null, null, null);
                if (cr != null) {
                    cr.moveToFirst();
                    while (!cr.isAfterLast()) {
                        if (toSkip < soFar - baseLine) {
                            toSkip++;
                            cr.moveToNext();
                        } else {
                            buffer = assemble(cr, Cols)
                                    .replaceAll("null", "");
                            myOutWriter.append(buffer);
                            extra++;
                            if (extra == cHunk) break;
                            cr.moveToNext();
                        }
                    }
                    cr.close();
                }
            }
            myOutWriter.close();
            fOut.close();
//            show("Done writing to SD : " + outFile);
        } catch (Exception e) {
//            show(e.getMessage()); //e.printStackTrace();
//            show("FAILED!");
        }
        return extra; // the exact amount added
    }// end dumpTable



    private String assemble(Cursor cr, String[] Cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Cols.length; i++) {
            sb.append(cr.getString(cr.getColumnIndex(Cols[i])) + SEP_CH);
        }
        sb.append("\n");
        //return sb.toString().replaceAll("\\"+SEP_CH+"\\n","\n");
        //return sb.toString().replaceAll("\\~\\n","\n");
        return sb.toString().replaceAll(SEP_CHRE, "\n");  // re with eol
    }


    private void show(String what) {
        //String what = where + ": " + String.valueOf(selectedItem) ;
        Toast.makeText(getActivity(),
                what, Toast.LENGTH_LONG).show();
    }


    //================================== maintenance ========================

    // the following is called by
    /*
            if (id == R.id.action_settings) {

//            new LongOperation().execute("");

            return true;
        }
     */

    private class LongOperation extends AsyncTask<String, Void, String> { //todo - move this to a maintenance class
        @Override
        protected String doInBackground(String... params) {
//            for(int i=0;i<2;i++) {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    // Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
            checkDB(); // this is our long standing task here
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
//            Toast.makeText(MainActivity.this, "finish", Toast.LENGTH_SHORT).show();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Gone through " +counter+" records, bad="+bad+", last="+name);
            builder.setCancelable(true);

            builder.setPositiveButton(
                    "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
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

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


    private int counter=0, bad=0;
    private String name="";

    private void checkDB(){
//        String msg="";
        counter=0; bad=0;
        Cursor data = getActivity().getContentResolver().query(FtContentProvider.PAGE_URI, null, null, null, null);
        if (data==null) return;
        while (data.moveToNext()) { // effectively cover all the members of the page ...
            String myid = data.getString(data.getColumnIndex(PageTable.COLUMN_ID));
            String kids = data.getString(data.getColumnIndex(PageTable.COLUMN_KIDS));
            if(!kids.isEmpty()){
//                Toast.makeText(this, "down to id="+myid, Toast.LENGTH_SHORT).show();
                String[] kidsArr = kids.split(" ");
                for(String k : kidsArr){
                    if(k.isEmpty()) continue;
                    Uri uri = Uri.parse(FtContentProvider.MEMBER_URI + "/" + k);
                    Cursor m_cr = getActivity().getContentResolver().query(uri, null, null, null, null);
                    // fetch the value
                    if (m_cr != null) {
                        m_cr.moveToFirst();
                        String par = m_cr.getString(m_cr.getColumnIndex(MemberTable.COLUMN_PAR_PAGES)); // ownerfield
                        String first = m_cr.getString(m_cr.getColumnIndex(MemberTable.COLUMN_FIRST)); // ownerfield
                        String last = m_cr.getString(m_cr.getColumnIndex(MemberTable.COLUMN_LAST)); // ownerfield
                        m_cr.close();
                        if(!par.equals(myid)) { // fix this ... found 91 cases!
//                            counter++;
                            if (!par.isEmpty()) { name = first+" "+last+"; "+myid+"."+par; bad++;
                            } else {
                                counter++; // counts how many we have changed
                                ContentValues values = new ContentValues();
                                values.put(MemberTable.COLUMN_PAR_PAGES, myid);
                                getActivity().getContentResolver().update(uri, values, null, null);
                            }
                        }
                    }
                }
            }
        }
    }




}//================================= end of class ==========================
