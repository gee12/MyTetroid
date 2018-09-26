package com.gee12.mytetroid;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.gee12.mytetroid.data.NodesManager;
import com.gee12.mytetroid.data.TetroidNode;

import pl.openrnd.multilevellistview.ItemInfo;
import pl.openrnd.multilevellistview.MultiLevelListView;
import pl.openrnd.multilevellistview.OnItemClickListener;

public class MainActivity extends AppCompatActivity {
//        implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    ExpandableListAdapter expListAdapter;
//    ExpandableListView expListView;
//    List<TetroidNode> listDataHeader;
//    HashMap<TetroidNode, List<TetroidNode>> listDataChild;
//    NodesManager nodesManager = new NodesManager();
    private MultiLevelListView mNodesListView;

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

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // обработчики выбора ветки/подветки
/*        ExpandableListView expListView = (ExpandableListView) findViewById(R.id.exp_list_view);
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
//                Toast.makeText(getApplicationContext(),
//                        listDataHeader.get(groupPosition).getName()+ " : "
//                                + listDataChild.get(listDataHeader.get(groupPosition)).get(
//                                childPosition).getName(),
//                        Toast.LENGTH_SHORT).show();
//                drawerLayout.closeDrawers();
                return false;
            }
        });
        ExpandableListAdapter.OnNodeNameClickListener onNodeNameClickListener = new ExpandableListAdapter.OnNodeNameClickListener() {
            @Override
            public void onClick(int groupPosition) {
//                   Toast.makeText(getApplicationContext(),
//                        listDataHeader.get(groupPosition).getName(),
//                        Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawers();
            }
        };

        NodesManager.init();
//        initNodes();

        expListAdapter = new ExpandableListAdapter(this, NodesManager.getNodes(), expListView, onNodeNameClickListener);
        expListView.setAdapter(expListAdapter);*/





        NodesManager.init();
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

        listAdapter.setDataItems(NodesManager.getNodes());
    }


    private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
            showNode((TetroidNode)item);
        }

        @Override
        public void onGroupItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
//            showNode((TetroidNode)item);
        }
    };


    private void showNode(TetroidNode node)
    {
        Toast.makeText(getApplicationContext(), node.getName(), Toast.LENGTH_SHORT).show();
        drawerLayout.closeDrawers();
    }

//    private void initNodes() {
//        listDataHeader = new ArrayList<TetroidNode>();
//        listDataChild = new HashMap<TetroidNode, List<TetroidNode>>();

        // Adding data header
//        listDataHeader.add(new TetroidNode("heading1"));
//        listDataHeader.add(new TetroidNode("heading2"));
//        listDataHeader.add(new TetroidNode("heading3"));
//
//        // Adding child data
//        List<TetroidNode> heading1= new ArrayList<TetroidNode>();
//        heading1.add(new TetroidNode("Submenu of item 1"));
//
//
//        List<TetroidNode> heading2= new ArrayList<TetroidNode>();
//        heading2.add(new TetroidNode("Submenu of item 2"));
//        heading2.add(new TetroidNode("Submenu of item 2"));
//        heading2.add(new TetroidNode("Submenu of item 2"));
//
//
//        listDataChild.put(listDataHeader.get(0), heading1);
//        listDataChild.put(listDataHeader.get(1), heading2);
//    }

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

//    @SuppressWarnings("StatementWithEmptyBody")
//    @Override
//    public boolean onNavigationItemSelected(MenuItem item) {
//        // Handle navigation view item clicks here.
//        int id = item.getItemId();
//
//        if (id == R.id.nav_camera) {
//            // Handle the camera action
//        } else if (id == R.id.nav_gallery) {
//
//        } else if (id == R.id.nav_slideshow) {
//
//        } else if (id == R.id.nav_manage) {
//
//        } else if (id == R.id.nav_share) {
//
//        } else if (id == R.id.nav_send) {
//
//        }
//
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);
//        return true;
//    }

//    private void setupDrawerContent(NavigationView navigationView) {
//        navigationView.setNavigationItemSelectedListener(
//                new NavigationView.OnNavigationItemSelectedListener() {
//                    @Override
//                    public boolean onNavigationItemSelected(MenuItem menuItem) {
//                        menuItem.setChecked(true);
//                        drawerLayout.closeDrawers();
//                        return true;
//                    }
//                });
//    }
}
