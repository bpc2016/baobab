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


/**
 * Created by hp on 2/2/2016.
 */
public class BioFragment extends Fragment {

    private static final String LOGGER = "bpc_editor"; // Log.d(LOGGER, "no arguments!");
    public static final String MEMBER = "Member To Edit";

    private editListener mCallback;
    private Member theMember;
    private EditText mFirstText;
    private EditText mLastText;
    private EditText myobText;
    private EditText myodText;
    private ImageButton genderButton;
    private View myView;
    private String gender;


    // Container Activity must implement this interface
    public interface editListener {
        public void noOp();
    }

    public BioFragment() {
    } // generic constructor

    @Override
    public void onStart() {
        super.onStart();
        try {
            mCallback = (editListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement editListener");
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.edit_biodata, container, false);

        mFirstText = (EditText) v.findViewById(R.id.member_edit_first);
        mLastText = (EditText) v.findViewById(R.id.member_edit_last);
        myobText = (EditText) v.findViewById(R.id.member_edit_yob);
        myodText = (EditText) v.findViewById(R.id.member_edit_yod);

        genderButton = (ImageButton) v.findViewById(R.id.genderBtn);

        Button confirmButton = (Button) v.findViewById(R.id.member_edit_button);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mFirstText.getText().toString().isEmpty()) {
                    Toast.makeText(getActivity(),
                            "please enter a first name", Toast.LENGTH_LONG).show();
                } else {
                    finish();
                }
            }
        });

        genderButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                gender = gender.equals("F") ? "M" : "F";
                int newImage = gender.equals("M") ? R.drawable.male_64 : R.drawable.female_64;
                genderButton.setImageResource(newImage);
            }
        });

        loadView(theMember);

        mFirstText.requestFocus();
        InputMethodManager imgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imgr.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        myView = v;
        return v;
    }


    private void loadView(Member m) {
        if (m == null) return;

        mFirstText.setText(m.getFirst());
        mLastText.setText(m.getLast()); // need to worry when this is not supplied
        myobText.setText(m.getYob());
        myodText.setText(m.getYod());
        gender = "F"; // default
        // gender icon button
        if (m.getGender().equals("M")) {
            genderButton.setImageResource(R.drawable.male_64); // change it
            gender = "M";
        }
    }


    // fragments do not have a finish() method! this calls a popstack in main
    private void finish() {  //todo - take only differences; ensure that there is nonempty first+last
        theMember.setFirst(mFirstText.getText().toString())
                .setLast(mLastText.getText().toString())
                .setYob(myobText.getText().toString())
                .setYod(myodText.getText().toString())
                .setGender(gender);

        new Family(getActivity())
                .updateMember(theMember);

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(myView.getWindowToken(), 0);
        mCallback.noOp();
    }

}