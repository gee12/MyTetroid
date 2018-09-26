package com.gee12.mytetroid.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.ContextMenu;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidNode;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.views.NodesListAdapter;
import com.gee12.mytetroid.views.RecordsListAdapter;

import pl.openrnd.multilevellistview.ItemInfo;
import pl.openrnd.multilevellistview.MultiLevelListView;
import pl.openrnd.multilevellistview.OnItemClickListener;

public class MainActivity extends AppCompatActivity {

    public static final int OPEN_RECORD_MENU_ITEM_ID = 1;

    private DrawerLayout drawerLayout;
    private RecordsListAdapter listAdapter;
    private ListView mRecordsListView;
    private MultiLevelListView mNodesListView;
    private TetroidNode currentNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        // панель
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // загружаем данные
        DataManager.init();

        // список веток
        NodesListAdapter.OnNodeNameClickListener onNodeNameClickListener2 = new NodesListAdapter.OnNodeNameClickListener() {
            @Override
            public void onClick(TetroidNode node) {
                showNode(node);
            }
        };

        mNodesListView = (MultiLevelListView) findViewById(R.id.nodes_list_view);
        NodesListAdapter listAdapter = new NodesListAdapter(this, onNodeNameClickListener2);

        mNodesListView.setAdapter(listAdapter);
        mNodesListView.setOnItemClickListener(mOnItemClickListener);

        listAdapter.setDataItems(DataManager.getRootNodes());

        // список записей
        mRecordsListView = (ListView)findViewById(R.id.records_list_view);
        registerForContextMenu(mRecordsListView);
        this.listAdapter = new RecordsListAdapter(this);
    }


    private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
            showNode((TetroidNode)item);
        }

        @Override
        public void onGroupItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
        }
    };


    private void showNode(TetroidNode node)
    {
        this.currentNode = node;
        Toast.makeText(getApplicationContext(), node.getName(), Toast.LENGTH_SHORT).show();
        drawerLayout.closeDrawers();

        this.listAdapter.reset(node.getRecords());
        mRecordsListView.setAdapter(listAdapter);

        Toast.makeText(this, "Открытие " + node.getName(), Toast.LENGTH_SHORT).show();
    }

    private void showRecord(TetroidRecord record) {
        Toast.makeText(this, "Открытие " + record.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(Menu.NONE, OPEN_RECORD_MENU_ITEM_ID, Menu.NONE, "Открыть");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case OPEN_RECORD_MENU_ITEM_ID:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                TetroidRecord record = (TetroidRecord)currentNode.getRecords().get(info.position);
                showRecord(record);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

}
