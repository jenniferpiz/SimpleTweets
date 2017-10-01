package com.codepath.apps.restclienttemplate.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.codepath.apps.restclienttemplate.R;

/**
 * Created by jennifergodinez on 9/27/17.
 */

public class TweetFragment extends DialogFragment {

    private EditText etTweetMsg;


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

        etTweetMsg = (EditText) view.findViewById(R.id.etTweetMsg);

        // Show soft keyboard automatically and request focus to field
        etTweetMsg.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

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
