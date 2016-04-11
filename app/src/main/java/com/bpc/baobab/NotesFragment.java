package com.bpc.baobab;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by hp on 2/2/2016.
 */

public class NotesFragment extends ListFragment {

    private static final String LOGGER = "bpc_notes"; // Log.d(LOGGER, "no arguments!");
    public static final String MEMBER = "Member To Edit";
    private static final String NOTE_SEP = "##";

    private noteListener mCallback;
    private Member theMember;
    private TextView mNames;
    private TextView mDates;
    private EditText mNote;
    private ImageView genderImage;
    private String mOldNotes;
    private View myView;

    // Container Activity must implement this interface
    public interface noteListener {
        public void noOp();
    }

    public NotesFragment() {
    } // generic constructor

    @Override
    public void onStart() {
        super.onStart();
        try {
            mCallback = (noteListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement noteListener");
        }

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = savedInstanceState != null
                ? savedInstanceState
                : getArguments();
        if (args != null) {
            theMember = (Member) args.getSerializable(MEMBER);
        } else {
            Log.d(LOGGER, "no arguments!");
        }
        setRetainInstance(true); // solves basic config change issues
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.edit_notes, container, false);

        mNames = (TextView) myView.findViewById(R.id.edit_name);
        mDates = (TextView) myView.findViewById(R.id.edit_date);
        mNote = (EditText) myView.findViewById(R.id.edit_note);
        genderImage = (ImageView) myView.findViewById(R.id.edit_gender);

        Button confirmButton = (Button) myView.findViewById(R.id.edit_button);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mNote.getText().toString().isEmpty()) {
                    Toast.makeText(getActivity(),
                            "please enter a note", Toast.LENGTH_LONG).show();
                } else {
                    finish();
                }
            }
        });

        ArrayList<String> notesList = loadView();
        StableArrayAdapter listAdapter
                = new StableArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, notesList);
        setListAdapter(listAdapter);
        return myView;
    }


    private ArrayList<String> loadView() {

        ArrayList<String> notesList = new ArrayList<String>();
        if (theMember == null) return notesList;

        String fullname = theMember.getFirst() + " " + theMember.getLast();
        mNames.setText(fullname);
        //set gender indicator
        if (theMember.getGender().equals("M")) genderImage.setImageResource(R.drawable.male_64);

        mDates.setText(theMember.getDates());

        mOldNotes = new Family(getActivity()).getNotes(theMember); //theMember.getNotes();
        if (null != mOldNotes) {
            String[] notes = mOldNotes.split(NOTE_SEP);
            Collections.addAll(notesList, notes);
        }
        return notesList;
    }


    // fragments do not have a finish() method! this calls a popstack in main
    private void finish() {

        String newNote = mNote.getText().toString();
        if (!newNote.isEmpty()) {
            Member toUpdate = new Member(theMember.getID());
            if (null != mOldNotes) {
                toUpdate.setNote(mOldNotes)
                        .addNote(newNote);
            } else {
                toUpdate.setNote(newNote);
            }
            new Family(getActivity()).updateMember(toUpdate);
        }
        mCallback.noOp();
        // we must also remove the softkeyboard - this is why the view was a field
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(myView.getWindowToken(), 0);
    }


    // the array adapter used for teh mOldNotesLV of notes
    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId, List<String> notes) {
            super(context, textViewResourceId, notes);

            for (int i = 0; i < notes.size(); ++i) {
                mIdMap.put(notes.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }


}