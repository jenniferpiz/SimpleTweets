package com.codepath.apps.restclienttemplate.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.codepath.apps.restclienttemplate.R;
import com.codepath.apps.restclienttemplate.adapters.TweetAdapter;
import com.codepath.apps.restclienttemplate.apps.TwitterApp;
import com.codepath.apps.restclienttemplate.fragments.TweetFragment;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.apps.restclienttemplate.utils.EndlessRecyclerViewScrollListener;
import com.codepath.apps.restclienttemplate.utils.PostsDatabaseHelper;
import com.codepath.apps.restclienttemplate.utils.TwitterClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import cz.msebera.android.httpclient.Header;

import static com.codepath.apps.restclienttemplate.utils.TwitterClient.maxTweets;

public class TimelineActivity extends AppCompatActivity implements TweetFragment.FinishTweetListener {

    private TwitterClient client;
    private TweetAdapter tweetAdapter;
    private ArrayList<Tweet> tweets;
    private RecyclerView rvTweets;
    private TextView tvBody;
    private EndlessRecyclerViewScrollListener scrollListener;
    static private PostsDatabaseHelper db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        //setup database
        db = PostsDatabaseHelper.getInstance(this);

        client = TwitterApp.getRestClient();

        // initialize tweets
        initTweets();

        rvTweets = (RecyclerView) findViewById(R.id.rvTweet);
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        rvTweets.addItemDecoration(itemDecoration);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvTweets.setLayoutManager(linearLayoutManager);

        tweetAdapter = new TweetAdapter(tweets, new TweetAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(View view, int position) {
                Tweet tweet = tweets.get(position);

                AlertDialog.Builder b = new AlertDialog.Builder(TimelineActivity.this);
                b.setMessage(tweet.body);
                b.setTitle("@"+tweet.user.screenName);
                b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null)  {
                            dialog.dismiss();
                        }
                    }

                });
                b.show();
            }
        });

        rvTweets.setAdapter(tweetAdapter);

        // Attach the listener to the AdapterView onCreate
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (!isOnline()) {
                    Toast.makeText(getApplicationContext(), "No internet detected!",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                final int curSize = tweetAdapter.getItemCount();

                populateTimeline(tweets.get(curSize-1).uid);

                view.post(new Runnable() {
                    @Override
                    public void run() {
                        tweetAdapter.notifyItemRangeInserted(curSize, tweets.size() - 1);

                    }
                });
            }
        };

        rvTweets.addOnScrollListener(scrollListener);

        tvBody = (TextView)findViewById(R.id.tvBody);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tweet, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.home:
                return true;

            case R.id.twit:
                showComposeDlg();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public boolean isOnline() {

        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }
        return false;
    }


    private void initTweets() {
        ArrayList<Tweet> dbTweets;

        tweets = new ArrayList<Tweet>();

        dbTweets = (ArrayList<Tweet>)db.getAllPosts();
        if (dbTweets.size() > 1) {
            // reverse order
            Collections.reverse(dbTweets);
            tweets.addAll(dbTweets);
        }

        // if online, get updates then insert in the tweets
        if (isOnline()) {
            if (dbTweets.size() == 0) {
                populateTimeline(1);
            } else {
                // pull newest tweets
                populateTimeline(- dbTweets.get(0).uid);
            }
        }
    }

    /*
    params
    id : a negative value means get tweets newer than id
         a positive value means we get tweets older than id
     */
    private void populateTimeline (final long id) {

        client.getHomeTimeline(id, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("TwitterClient", response.toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d("TwitterClient", response.toString());

                ArrayList<Tweet> newTweets = new ArrayList<Tweet>();

                for (int i = 0; i < response.length(); i++) {
                    Tweet tweet = null;
                    try {
                        tweet = Tweet.fromJSON(response.getJSONObject(i));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    newTweets.add(tweet);
                }

                //add to our list
                addTweets(newTweets, id < 0);

            }

            private void addTweets(ArrayList<Tweet> newTweets, boolean isPrepend) {

                if (isPrepend) {

                    int n_tweets = newTweets.size();

                    // if too many new tweets, we just clear everything and start fresh
                    if (n_tweets > (maxTweets - 1)) {
                        tweets.clear();
                        db.deleteAllPostsAndUsers();
                        tweetAdapter.notifyItemRangeRemoved(0, tweets.size() -1 );
                    }

                    if (n_tweets >0 ) {
                        tweets.addAll(0, newTweets);
                        db.addAllPosts(newTweets);
                        tweetAdapter.notifyItemRangeInserted(0, n_tweets);
                    }

                } else {
                    //check if first = last
                    if (newTweets.size() > 0 && tweets.size() > 0 &&
                            newTweets.get(0).uid == tweets.get(tweets.size()-1).uid) {
                        //remove first tweet
                        newTweets.remove(0);
                    }

                    // append new tweets to our list
                    int n_tweets = newTweets.size();
                    int oldSize = tweets.size();
                    tweets.addAll(newTweets);
                    db.addAllPosts(newTweets);
                    tweetAdapter.notifyItemRangeInserted(oldSize, n_tweets);
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.d("TwitterClient", responseString);
                new Throwable().printStackTrace();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.d("TwitterClient", errorResponse.toString());
                new Throwable().printStackTrace();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.d("TwitterClient", errorResponse.toString());
                new Throwable().printStackTrace();
            }
        });
    }

    private void showComposeDlg() {
        FragmentManager fm = getSupportFragmentManager();
        TweetFragment tweetFragment = TweetFragment.newInstance("Some Title");
        tweetFragment.show(fm, "fragment_tweet");
    }



    @Override
    public void onPassTweetMsg(String s) {
        String tweetMsg = s;

        client.postNewTweet(s, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    // update first before adding new one to top of the list
                    populateTimeline(- tweets.get(0).uid);

                    // add new tweet
                    Tweet t = Tweet.fromJSON(response);
                    tweets.add(0, t);

                    tweetAdapter.notifyItemInserted(0);

                    // make sure we can view it on top of home timeline
                    rvTweets.scrollToPosition(0);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                throwable.printStackTrace();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                throwable.printStackTrace();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
               throwable.printStackTrace();
            }

        });
    }
}
