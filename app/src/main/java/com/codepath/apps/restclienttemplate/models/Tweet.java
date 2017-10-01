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
    public String url;
    public String displayURL;

    public static Tweet fromJSON(JSONObject jsonObject) throws JSONException {
        Tweet tweet = new Tweet();

        tweet.body = jsonObject.getString("text");
        tweet.createdAt = jsonObject.getString("created_at");
        tweet.uid = jsonObject.getLong("id");
        tweet.user = User.fromJSON(jsonObject.getJSONObject("user"));

        JSONObject urlObj;
        JSONObject entObj = (JSONObject) jsonObject.getJSONObject("entities");
        if (entObj.getJSONArray("urls").length() > 0) {
            urlObj = (JSONObject) entObj.getJSONArray("urls").get(0);
            tweet.url = urlObj.getString("url");
        }

        if (!entObj.isNull("media") && (entObj.getJSONArray("media").length() > 0)) {
            urlObj = (JSONObject) entObj.getJSONArray("media").get(0);
            tweet.displayURL = urlObj.getString("media_url");
        }

        return tweet;

    }
}
