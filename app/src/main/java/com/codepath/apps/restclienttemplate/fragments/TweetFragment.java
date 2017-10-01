package com.codepath.apps.restclienttemplate.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.codepath.apps.restclienttemplate.R;

/**
 * Created by jennifergodinez on 9/27/17.
 */

public class TweetFragment extends DialogFragment {

    private EditText etTweetMsg;
    private TextView tvCounter;
    private static int maxLength = 140;


    public TweetFragment() {
    }

    public static TweetFragment newInstance(String title) {

        TweetFragment fragment = new TweetFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        fragment.setArguments(args);

        return fragment;
    }

    public interface FinishTweetListener {
        void onPassTweetMsg(String s);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tweet, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        tvCounter = (TextView) view.findViewById(R.id.tvCounter);
        etTweetMsg = (EditText) view.findViewById(R.id.etTweetMsg);

        // Show soft keyboard automatically and request focus to field
        etTweetMsg.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        etTweetMsg.addTextChangedListener(
            new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    //This sets a textview to the current length
                    tvCounter.setText(String.valueOf(maxLength - s.length()));
                }

                public void afterTextChanged(Editable s) {
                }
            });


        Button btn = (Button)view.findViewById(R.id.btnTweet);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                FinishTweetListener l = (FinishTweetListener)getActivity();
                l.onPassTweetMsg(etTweetMsg.getText().toString());
                // Close the dialog and return back to the parent activity
                dismiss();
            }
        });
    }
}


