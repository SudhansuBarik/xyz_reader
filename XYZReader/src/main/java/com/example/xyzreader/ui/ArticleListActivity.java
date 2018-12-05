package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.widget.Toast;

import com.example.xyzreader.R;
import com.example.xyzreader.adapter.ArticleListAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.utility.NetworkChangeReceiver;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mRecyclerView = findViewById(R.id.recycler_view);

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
        ArticleListAdapter adapter = new ArticleListAdapter(cursor, this, R.layout.list_item_article);
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
}
