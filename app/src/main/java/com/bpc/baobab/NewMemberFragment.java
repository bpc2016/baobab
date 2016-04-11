package com.bpc.baobab;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by hp on 1/14/2016.
 */
public class NewMemberFragment extends Fragment {

    private static final String LOGGER = "bpc_new_member"; // Log.d(LOGGER, "no arguments!");
    public static final String MEMBER = "theOwner";
    public static final String TYPE = "relation";  // either 'child' or 'spouse'
    public static final String EXTRAS = "extra data";

    public static final String SPOUSE = "SPOUSE_MEMBER";
    public static final String OWNER = "OWNER_MEMBER";
    public static final String PAGEID = "PAGEID";
    public static final String PAGE = "PAGE";
    public static final String CHILD = "CHILD";

    private EditText mFirstText;
    private EditText mLastText;
    private EditText myobText;
    private EditText myodText;
    private ImageButton genderButton;
    private newmemberListener mCallback;
    private EditText mNoteText;
    private int type; // child|spouse
    private String gender;
    private View myView;
    private Member mOwner;
    private Member mSpouse;
    private String mPageID;
    private Page mPage;
    private Member mChild;


    public NewMemberFragment() {
    } // generic constructor


    // Container Activity must implement this interface
    public interface newmemberListener {
        void noOp(); // simply popstack

        void sortAfterNew(Member mChild, Page mPage, int type);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            mCallback = (newmemberListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement newmemberListener");
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = savedInstanceState != null
                ? savedInstanceState
                : getArguments();
        if (args != null) {
            mOwner = (Member) args.getSerializable(OWNER);
            mSpouse = (Member) args.getSerializable(SPOUSE);
            mPageID = args.getString(PAGEID);
            mChild = (Member) args.getSerializable(CHILD);
            mPage = (Page) args.getSerializable(PAGE);
            type = args.getInt(TYPE);
        } else {
            Log.d(LOGGER, "no arguments!");
            finish();
        }

        setRetainInstance(true); // solves basic config change issues
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        myView = inflater.inflate(R.layout.new_member, container, false);

        genderButton = (ImageButton) myView.findViewById(R.id.genderBtn);
        mFirstText = (EditText) myView.findViewById(R.id.member_first);
        mLastText = (EditText) myView.findViewById(R.id.member_last);
        myobText = (EditText) myView.findViewById(R.id.member_yob);
        myodText = (EditText) myView.findViewById(R.id.member_yod);
        mNoteText = (EditText) myView.findViewById(R.id.member_note);

        Button confirmButton = (Button) myView.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mFirstText.getText().toString().isEmpty()) {
                    Toast.makeText(getActivity(),
                            "Please enter a first name", Toast.LENGTH_LONG).show();
                } else {
                    finish();
                }
            }
        });

        genderButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (type == OnePageFragment.SPOUSE) return; // cant change my gender
                gender = gender.equals("F") ? "M" : "F";
                int newImage = gender.equals("M") ? R.drawable.male_64 : R.drawable.female_64;
                genderButton.setImageResource(newImage);
                mChild.setGender(gender); // can only be a child here ...
            }
        });


        if (type == OnePageFragment.SPOUSE) {
            gender = mSpouse.getGender();
            //if(gender.equals("M"))  genderButton.setImageResource(R.drawable.male_64);
        } else {
            mLastText.setText(mChild.getLast());
            gender = mChild.getGender();
        }
        if (gender.equals("M")) genderButton.setImageResource(R.drawable.male_64);

        mFirstText.requestFocus();

        InputMethodManager imgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imgr.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        return myView;
    }


    // fragments do not have a finish() method! this calls a popstack in main
    private void finish() {
        Family fHelper;
        if (type == OnePageFragment.SPOUSE) {
            mSpouse.setFirst(mFirstText.getText().toString())
                    .setLast(mLastText.getText().toString())
                    .setYob(myobText.getText().toString())
                    .setYod(myodText.getText().toString())
                    .setNote(mNoteText.getText().toString());

            if (mPageID.equals(mSpouse.getMyPages())) { // was set on the call! first spouse ..
                mSpouse.setMyPages(mPageID);
                fHelper = new Family(getActivity());
                String spouse_id = fHelper
                        .createMember(mSpouse);

                fHelper.updatePage(
                        new Page(mPageID)
                                .setSpouse(spouse_id)
                );

                // proceed to noOp
                mCallback.noOp();
            } else {
                fHelper = new Family(getActivity());
                String spouse_id = fHelper
                        .createMember(mSpouse);

                Page newPage = new Page()
                        .setOwner(mOwner.getID())
                        .setSpouse(spouse_id);

                String page_id = fHelper
                        .createPage(newPage);
                //update the new member's details
                mSpouse.setMyPages(page_id);
                fHelper.updateMember(mSpouse);
                //prepare spouse addition ...
                mOwner.addMyPages(page_id); // note this one - we have grown the my_pages list
                //go sort the spouses
                mCallback.sortAfterNew(mOwner, new Page(page_id), type);
            }
        } else { // handle child
            mChild.setFirst(mFirstText.getText().toString())
                    .setLast(mLastText.getText().toString())
                    .setYob(myobText.getText().toString())
                    .setYod(myodText.getText().toString())
                    .setNote(mNoteText.getText().toString());

            String child_id = new Family(getActivity())
                    .createMember(mChild);

            if (0 == mPage.getKids().length()) { // first child
                mPage.setKids(child_id);
                new Family(getActivity())
                        .updatePage(mPage);
                mCallback.noOp();
            } else {
                mPage.addKid(child_id); // may be on top of others - we sort after
                //append to the digest
                ArrayList<String> toBeSorted = new ArrayList<>();
                toBeSorted.add(mChild.Digest());
                toBeSorted.addAll(mPage.getDigests());
                mPage.setDigests(toBeSorted);
                //go ahead and sort
                mCallback.sortAfterNew(mChild, mPage, OnePageFragment.CHILD);
            }
        }
        // we must also remove the softkeyboard - this is why the view was a field
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(myView.getWindowToken(), 0);
    }


}
