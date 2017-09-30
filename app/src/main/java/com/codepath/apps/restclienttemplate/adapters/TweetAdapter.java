package com.codepath.apps.restclienttemplate.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.codepath.apps.restclienttemplate.R;
import com.codepath.apps.restclienttemplate.models.Tweet;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Created by jennifergodinez on 9/25/17.
 */

public class TweetAdapter extends RecyclerView.Adapter<TweetAdapter.ViewHolder> {

    private List<Tweet> mTweets;
    private Context context;
    private String colorCodeStart = "<font color='#2DB7EF'>";  // use any color as  your want
    private String colorCodeEnd = "</font>";

    public TweetAdapter(List<Tweet> mTweets) {
        this.mTweets = mTweets;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View tweetView = inflater.inflate(R.layout.item_tweet_img, parent, false);

        ViewHolder viewHolder = new ViewHolder(tweetView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Tweet tweet = mTweets.get(position);

        holder.tvUserName.setText(tweet.user.name);

        // change color of tags
        String body = tweet.body;
        int start = body.indexOf("#") + 1;
        String prefix = body.substring(0, start);
        String suffix = body.substring(start);
        String newBody = prefix.replace("#", colorCodeStart+"#") + suffix.replaceFirst(" ", colorCodeEnd+" ");
        holder.tvBody.setText(Html.fromHtml(newBody));

        holder.tvTimeStamp.setText(getRelativeTimeAgo(tweet.createdAt));

        Glide.with(context).load(tweet.user.profileImageUrl).into(holder.ivProfileImage);



        if (tweet.displayURL  != null) {
            new MyAsyncTask(holder.ivDisplay).execute(tweet.displayURL);
            //Glide.with(context).load(tweet.url).into(holder.ivDisplay);
        }



    }


    private String getRelativeTimeAgo(String rawJsonDate) {
        String twitterFormat = "EEE MMM dd HH:mm:ss ZZZZZ yyyy";
        SimpleDateFormat sf = new SimpleDateFormat(twitterFormat, Locale.ENGLISH);
        sf.setLenient(true);

        String relativeDate = "";
        try {
            long dateMillis = sf.parse(rawJsonDate).getTime();
            relativeDate = DateUtils.getRelativeTimeSpanString(dateMillis,
                    System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // shorten relative time
        String str;
        if ("Yesterday".equals(relativeDate)) {
            str = "1d";
        } else {
            int cutIndex = relativeDate.indexOf(' ') + 1;
            str = relativeDate.replace(" ", "").substring(0, cutIndex);
        }

        return str;
    }


    @Override
    public int getItemCount() {
        return mTweets.size();
    }


    private class MyAsyncTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        MyAsyncTask(ImageView imageView) {
            this.imageView = imageView;
        }

        protected void onPreExecute() {
            // Runs on the UI thread before doInBackground
            // Good for toggling visibility of a progress indicator
            //progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        protected Bitmap doInBackground(String... urls) {
            String urlStr = urls[0];

            Bitmap bmp = null;
            
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection  = (HttpURLConnection) url.openConnection();

                InputStream in = connection.getInputStream();
                //InputStream in = new java.net.URL(url).openStream();

                bmp = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }

            return bmp;
        }



        protected void onPostExecute(Bitmap result) {
            // This method is executed in the UIThread
            // with access to the result of the long running task
            super.onPostExecute(result);

            imageView.setImageBitmap(result);
            // Hide the progress bar
            //progressBar.setVisibility(ProgressBar.INVISIBLE);
        }
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivProfileImage;
        public TextView tvUserName;
        public TextView tvBody;
        public TextView tvTimeStamp;
        public ImageView ivDisplay;
        //public TextView tvBody;


        public ViewHolder (View itemView) {
            super(itemView);

            ivProfileImage = (ImageView)itemView.findViewById(R.id.ivProfileImage);
            tvUserName = (TextView)itemView.findViewById(R.id.tvUserName);
            tvBody = (TextView)itemView.findViewById(R.id.tvBody);
            tvTimeStamp = (TextView)itemView.findViewById(R.id.tvTimeStamp);
            ivDisplay = (ImageView)itemView.findViewById(R.id.ivDisplay);

        }

    }
}
