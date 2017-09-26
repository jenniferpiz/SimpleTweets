package com.codepath.apps.restclienttemplate.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jennifergodinez on 9/25/17.
 */

public class Tweet {
    public String body;
    public long uid;
    public String createdAt;
    public User user;

    public static Tweet fromJSON(JSONObject jsonObject) throws JSONException {
        Tweet tweet = new Tweet();

        tweet.body = jsonObject.getString("text");
        tweet.createdAt = jsonObject.getString("created_at");
        tweet.uid = jsonObject.getLong("id");
        tweet.user = User.fromJSON(jsonObject.getJSONObject("user"));

        return tweet;

    }
}
