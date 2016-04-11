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
 * Created by hp on 1/14/2016.
 */
public class AddParentsFragment extends Fragment {

    private static final String LOGGER = "bpc_new_member"; // Log.d(LOGGER, "no arguments!");
    public static final String CHILD = "child";
    public static final String FATHER = "father";

    private EditText mFirstText;
    private EditText mLastText;
    private EditText myobText;
    private EditText myodText;
    private ImageButton genderButton;
    private Member mChild = null, mFather = null; //, mMother = null;
    private addparentListener mCallback;
    private EditText mNoteText;
    private Button mConfirmButton;
    private View myView;

    public AddParentsFragment() {
    } // generic constructor


    // Container Activity must implement this interface
    public interface addparentListener {
        void noOp(); // simply popstack

        void toPageSpecial(String pageid); // the resulting id

        void newPage(Member member, Member other, int type);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            mCallback = (addparentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement addparentListener");
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = savedInstanceState != null
                ? savedInstanceState
                : getArguments();
        if (args != null) {
            mChild = (Member) args.getSerializable(CHILD);
            mFather = (Member) args.getSerializable(FATHER);
        } else {
            Log.d(LOGGER, "no arguments!");
//            finish();
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
        mConfirmButton = (Button) myView.findViewById(R.id.confirm_button);


        loadView();

        mFirstText.requestFocus();
        //.. and show keyboard
        InputMethodManager imgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imgr.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        return myView;
    }

    private void loadView() {
        if (mFather == null) { // first time, prepare for father details
            genderButton.setImageResource(R.drawable.male_64);
            mLastText.setText(mChild.getLast()); // make this assumption - can be changed
            mConfirmButton.setText("Save Father Details ...");
            mConfirmButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (mFirstText.getText().toString().isEmpty()) {
                        Toast.makeText(getActivity(),
                                "Please enter father's names", Toast.LENGTH_LONG).show();
                    } else
                        saveFather();
                }
            });
        } else { //mother
            genderButton.setImageResource(R.drawable.female_64);
            mConfirmButton.setText("Save Parents");
            mConfirmButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (mFirstText.getText().toString().isEmpty() || mLastText.getText().toString().isEmpty()) {
                        Toast.makeText(getActivity(),
                                "Please enter both mother's names", Toast.LENGTH_LONG).show();
                    } else
                        finish();
                }
            });
        }
    }


    private void saveFather() {
        mFather = new Member();
        mFather.setFirst(mFirstText.getText().toString());
        mFather.setLast(mLastText.getText().toString());
        mFather.setYob(myobText.getText().toString());
        mFather.setYod(myodText.getText().toString());
        mFather.setNote(mNoteText.getText().toString());
        mFather.setGender("M");
        //go for the mother now
        mCallback.newPage(mChild, mFather, OnePageFragment.MOTHER);//last parameter
    }


    private void finish() {
        mFather.setGender("M"); // not already set?

        Family famHelper = new Family(getActivity()); // load from the mFather member

        String father_id = famHelper.createMember(mFather);

        // mFather.setID(father_id);

        Member mMother = new Member()
                .setGender("F")
                .setFirst(mFirstText.getText().toString())
                .setLast(mLastText.getText().toString())
                .setYob(myobText.getText().toString())
                .setYod(myodText.getText().toString())
                .setNote(mNoteText.getText().toString());

        String mother_id = famHelper.createMember(mMother);

        String parent_id = famHelper.createPage(
                new Page()
                        .setOwner(father_id)
                        .setSpouse(mother_id)
                        .setKids(mChild.getID())
        );

        // set our par_id to this and the my_pages fields on both parents
        mChild.setPar(parent_id);
        mFather.setMyPages(parent_id);
        mMother.setMyPages(parent_id);

        famHelper.updateMember(mChild)
                .updateMember(mFather)
                .updateMember(mMother);


        // return the new pageId
        mCallback.toPageSpecial(parent_id);
        // we must also remove the softkeyboard - this is why the view was a field
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(myView.getWindowToken(), 0);
    }

}
