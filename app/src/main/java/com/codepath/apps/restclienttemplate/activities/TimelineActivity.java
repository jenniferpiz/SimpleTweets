package com.codepath.apps.restclienttemplate.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.codepath.apps.restclienttemplate.R;
import com.codepath.apps.restclienttemplate.apps.TwitterApp;
import com.codepath.apps.restclienttemplate.adapters.TweetAdapter;
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

public class TimelineActivity extends AppCompatActivity implements TweetFragment.FinishTweetListener {

    private TwitterClient client;
    private TweetAdapter tweetAdapter;
    private ArrayList<Tweet> tweets;
    private RecyclerView rvTweets;
    private TextView tvBody;
    private ArrayList<Tweet> dbTweets;
    private EndlessRecyclerViewScrollListener scrollListener;
    static private PostsDatabaseHelper db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        //setup database
        db = PostsDatabaseHelper.getInstance(this);
        //TODO enable for debugging only
        //db.deleteAllPostsAndUsers();


        client = TwitterApp.getRestClient();

        // initializie tweets
        initTweets();

        rvTweets = (RecyclerView) findViewById(R.id.rvTweet);
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        rvTweets.addItemDecoration(itemDecoration);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvTweets.setLayoutManager(linearLayoutManager);

        tweetAdapter = new TweetAdapter(tweets);
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
        tweets = new ArrayList<Tweet>();

        dbTweets = (ArrayList<Tweet>)db.getAllPosts();
        if (dbTweets.size() > 1) {
            // reverse order
            Collections.reverse(dbTweets);
        }

        // if online, get updates then insert in the tweets
        if (isOnline()) {
            if (dbTweets.size() == 0) {
                populateTimeline(1);
            } else {
                // query latest tweets
                populateTimeline(- dbTweets.get(0).uid);

            }
        } else {
            tweets = dbTweets;
        }

    }


    private void populateTimeline (final long id) {

        client.getHomeTimeline(id, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("TwitterClient", response.toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d("TwitterClient", response.toString());

                int startIndex;

                if (tweetAdapter.getItemCount() != 0) {
                    // we exclude the first in the list since it duplicates previous query
                    startIndex = 1;
                } else {
                    // initially, there won't be a duplicate so we can set this to 0
                    startIndex = 0;
                }

                for (int i = startIndex; i < response.length(); i++) {
                    Tweet tweet = null;
                    try {
                        tweet = Tweet.fromJSON(response.getJSONObject(i));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    tweets.add(tweet);
                    db.addPost((tweet));
                    tweetAdapter.notifyItemInserted(tweets.size()-1); // TODO: delete this?
                }

                // add dbTweets to the new tweets, if any
                int n_tweets = tweets.size();  // n_tweets is number of new tweets
                if (dbTweets.size() > 0) {
                    // There's a better way of doing this but to simplify, only add dbTweets
                    // if new tweets < maxTweets-1
                    if (n_tweets < (TwitterClient.maxTweets - 1)) {
                        tweets.addAll(dbTweets);
                        tweetAdapter.notifyItemRangeInserted(n_tweets, dbTweets.size()-1);
                    }
                    dbTweets.clear(); // clear so it doesn't get added again
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
