package com.codepath.apps.restclienttemplate.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;

import com.codepath.apps.restclienttemplate.R;
import com.codepath.apps.restclienttemplate.TwitterApp;
import com.codepath.apps.restclienttemplate.TwitterClient;
import com.codepath.apps.restclienttemplate.adapters.TweetAdapter;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.apps.restclienttemplate.utils.EndlessRecyclerViewScrollListener;
import com.codepath.apps.restclienttemplate.utils.PostsDatabaseHelper;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class TimelineActivity extends AppCompatActivity {

    private TwitterClient client;
    private TweetAdapter tweetAdapter;
    private ArrayList<Tweet> tweets;
    private RecyclerView rvTweets;
    private EndlessRecyclerViewScrollListener scrollListener;
    static private PostsDatabaseHelper db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

/*
//TESTING
        PostsDatabaseHelper databaseHelper = PostsDatabaseHelper.getInstance(this);

        databaseHelper.deleteAllPostsAndUsers();

        // Create sample data
        User sampleUser = new User();
        sampleUser.name = "Miranda";
        sampleUser.profileImageUrl = "https://i.imgur.com/tGbaZCY.jpg";

        Tweet samplePost = new Tweet();
        samplePost.createdAt = "Sun Apr 12";
        samplePost.user = sampleUser;
        samplePost.body = "Miranda won !";
        samplePost.uid = 123;

        databaseHelper.addPost(samplePost);

        sampleUser = new User();
        sampleUser.name = "Steph";
        sampleUser.profileImageUrl = "https://i.imgur.com/tGbaZCY.jpg";

        Tweet samplePost2 = new Tweet();
        samplePost2.user = sampleUser;
        samplePost2.body = "Won won!";
        samplePost2.createdAt = "Mon Aug 6";
        samplePost2.uid = 456;

        databaseHelper.addPost(samplePost);
        databaseHelper.addPost(samplePost2);

        // Get all posts from database
        List<Tweet> posts = databaseHelper.getAllPosts();
        Log.d("DEBUG", "Start of List");
        for (Tweet post : posts) {
            Log.d("DEBUG", post.body);
        }

        if (true) return;
        //END OF TESTING
*/


        //setup database
        db = PostsDatabaseHelper.getInstance(this);

        //TODO for debugging only
        db.deleteAllPostsAndUsers();


        client = TwitterApp.getRestClient();

        rvTweets = (RecyclerView) findViewById(R.id.rvTweet);

        // initializie tweets (get from db)
        tweets = new ArrayList<Tweet>();
        tweetAdapter = new TweetAdapter(tweets);

        ArrayList<Tweet> dbTweets = (ArrayList<Tweet>)db.getAllPosts();

        // if online, get updates then insert in the tweets
        if (isOnline()) {
            if (dbTweets.size() == 0) {
                populateTimeline(1);
            } else {
                populateTimeline(0 - dbTweets.get(0).uid);
                // to make it less complicated, only add dbTweets if total new tweets < 25
                if (tweets.size() < 25) {
                    tweets.addAll(dbTweets);
                }
            }
        } else {
            tweets = dbTweets;
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvTweets.setLayoutManager(linearLayoutManager);
        rvTweets.setAdapter(tweetAdapter);

        // Attach the listener to the AdapterView onCreate
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
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


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tweet, menu);
        return true;
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


    private void populateTimeline (final long id) {

        client.getHomeTimeline(id, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("TwitterClient", response.toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d("TwitterClient", response.toString());

                int startIndex = 1; // we exclude the first in the list since it duplicates previous query
                //start when we started querying from Twitter?
                if (tweetAdapter.getItemCount() == 0) {
                    // initially, there won't be a duplicate so we can set this to 0
                    startIndex = 0;

                    //reset
                    tweets.clear();
                    tweetAdapter.notifyDataSetChanged();
                    scrollListener.resetState();

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
}
