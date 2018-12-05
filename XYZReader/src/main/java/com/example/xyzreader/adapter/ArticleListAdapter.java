package com.example.xyzreader.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.ArticleDetailActivity;
import com.example.xyzreader.ui.DynamicHeightNetworkImageView;
import com.example.xyzreader.ui.ImageLoaderHelper;
import com.example.xyzreader.utility.Utils;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ArticleViewHolder> {

    private Cursor mCursor;
    private Context mContext;
    private int mResource;

    public ArticleListAdapter(Cursor cursor, Context context, int resource) {
        mCursor = cursor;
        mContext = context;
        mResource = resource;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }


    @Override
    public ArticleViewHolder onCreateViewHolder(final ViewGroup viewGroup, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(mResource, viewGroup, false);

        final ArticleViewHolder vh = new ArticleViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = vh.getAdapterPosition();
                ActivityOptionsCompat activityOptionsCompat
                        = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        (Activity) mContext,
                        vh.thumbnailView,
                        vh.thumbnailView.getTransitionName());

                Intent intent = new Intent(mContext, ArticleDetailActivity.class);
                intent.putExtra(Utils.CURRENT_ARTICLE_POSITION, position);
                intent.putExtra(Utils.MUTED_COLOR_VALUE, vh.mutedColor);
                intent.putExtra(Utils.CURRENT_ARTICLE_TRANSITION_NAME, vh.thumbnailView.getTransitionName());
//                mContext.startActivities(new Intent[]{intent}, activityOptionsCompat.toBundle());
                mContext.startActivity(intent);
            }
        });

        return vh;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onBindViewHolder(@NonNull final ArticleViewHolder articleViewHolder, int position) {
        mCursor.moveToPosition(position);
        articleViewHolder.thumbnailView.setTransitionName(mContext.getString(R.string.book_image) + position);
        articleViewHolder.thumbnailView.setTag(mContext.getString(R.string.book_image) + position);

        String title = mCursor.getString(ArticleLoader.Query.TITLE);
        String date = DateUtils.getRelativeTimeSpanString(
                mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL).toString();
        String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
        final String image = mCursor.getString(ArticleLoader.Query.THUMB_URL);

        try {
            articleViewHolder.bitmap = new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... voids) {
                    try {
                        return Picasso.get().load(image)
                                .get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute().get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        articleViewHolder.titleView.setText(title);
        articleViewHolder.bookDate.setText(date);
        articleViewHolder.authorName.setText(author);
        articleViewHolder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

        ImageLoader imageLoader = ImageLoaderHelper.getInstance(mContext).getImageLoader();
        articleViewHolder.thumbnailView.setImageUrl(image, imageLoader);
        imageLoader.get(image, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                Bitmap bitmap = imageContainer.getBitmap();
                if (bitmap != null) {
                    Palette p = Palette.from(bitmap).generate();
                    int mMutedColor = p.getDarkVibrantColor(Utils.DEFAULT_COLOR);
                    articleViewHolder.mutedColor = mMutedColor;
                    articleViewHolder.itemView.setBackgroundColor(mMutedColor);
                    articleViewHolder.thumbnailView.setBackgroundColor(mMutedColor);
                }
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    class ArticleViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.thumbnail)
        DynamicHeightNetworkImageView thumbnailView;
        @BindView(R.id.article_title)
        TextView titleView;
        @BindView(R.id.article_subtitle_author_name)
        TextView authorName;
        @BindView(R.id.article_subtitle_book_date)
        TextView bookDate;

        CardView cardView;
        Bitmap bitmap = null;
        int mutedColor;

        ArticleViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            ButterKnife.bind(this, itemView);
        }
    }
}
