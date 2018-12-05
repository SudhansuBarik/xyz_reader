package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.utility.NetworkChangeReceiver;
import com.example.xyzreader.utility.Utils;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = ArticleListActivity.class.toString();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private static final String RECYCLER_VIEW_STATE_KEY = "RecyclerViewState";
    private NetworkChangeReceiver networkChangeReceiver;
    private IntentFilter connectivityChangeActionIntentFilter;
    private static Toast internetConnectionToastMessage;
    public static boolean mIsDetailsActivityStarted;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            startService(new Intent(this, UpdaterService.class));
        }

        setUpNetworkChangeReceiver();

        IntentFilter filter = new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE);
        filter.addAction(UpdaterService.EXTRA_REFRESHING);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRefreshingReceiver, filter);
    }

    private void setUpNetworkChangeReceiver() {
        networkChangeReceiver = new NetworkChangeReceiver();

        connectivityChangeActionIntentFilter = new IntentFilter();
        connectivityChangeActionIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        networkChangeReceiver.setNetworkStateListener(new NetworkChangeReceiver.NetworkStateListener() {
            @Override
            public void networkStateConnected(boolean status) {

                if (internetConnectionToastMessage == null) {
                    internetConnectionToastMessage = Toast.makeText(ArticleListActivity.this,
                            "Toast Initialized", Toast.LENGTH_LONG);
                }
                if (status) {
                    internetConnectionToastMessage.setText(R.string.internet_connection_restored_message);
                } else {
                    internetConnectionToastMessage.setText(R.string.internet_connection_lost_message);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(networkChangeReceiver, connectivityChangeActionIntentFilter);
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkChangeReceiver);
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                boolean mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
            } else if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mSwipeRefreshLayout.setRefreshing(false);
                Toast.makeText(ArticleListActivity.this, getString(R.string.empty_recycle_view_no_network), Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor, this);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Parcelable state = mRecyclerView.getLayoutManager().onSaveInstanceState();
        outState.putParcelable(RECYCLER_VIEW_STATE_KEY, state);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            Parcelable savedState = savedInstanceState.getParcelable(RECYCLER_VIEW_STATE_KEY);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(savedState);
        }
    }

    @Override
    public void onRefresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;
        private Context mContext;

        Adapter(Cursor cursor, Context context) {
            mCursor = cursor;
            mContext = context;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                }
            });
            return vh;
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            holder.authorName.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.AUTHOR)));
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                holder.bookDate.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()));
            } else {
                holder.bookDate.setText(Html.fromHtml(outputFormat.format(publishedDate)));
            }
            final String image = mCursor.getString(ArticleLoader.Query.THUMB_URL);
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

            try {
                holder.bitmap = new AsyncTask<Void, Void, Bitmap>() {
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

            ImageLoader imageLoader = ImageLoaderHelper.getInstance(mContext).getImageLoader();
            holder.thumbnailView.setImageUrl(image, imageLoader);
            imageLoader.get(image, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                    Bitmap bitmap = imageContainer.getBitmap();
                    if (bitmap != null) {
                        Palette p = Palette.from(bitmap).generate();
                        int mMutedColor = p.getDarkVibrantColor(Utils.DEFAULT_COLOR);
                        holder.mutedColor = mMutedColor;
                        holder.itemView.setBackgroundColor(mMutedColor);
                        holder.thumbnailView.setBackgroundColor(mMutedColor);
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
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
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

        public ViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
            ButterKnife.bind(this, itemView);
        }
    }
}
