package com.gee12.mytetroid.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.Spanned;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

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
    public static final int RECORDS_LIST_VIEW_NUM = 0;
    public static final int RECORD_DETAILS_VIEW_NUM = 1;

    private DrawerLayout drawerLayout;
    private RecordsListAdapter listAdapter;
    private ListView recordsListView;
    private MultiLevelListView nodesListView;
    private TetroidNode currentNode;
    private ViewSwitcher viewSwitcher;
    private TextView recordContentTextView;

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
        drawerLayout.openDrawer(GravityCompat.START);
        toggle.syncState();

        // загружаем данные
        DataManager.init();

        // список веток
        nodesListView = (MultiLevelListView) findViewById(R.id.nodes_list_view);
        NodesListAdapter listAdapter = new NodesListAdapter(this, onNodeHeaderClickListener);
        nodesListView.setAdapter(listAdapter);
        nodesListView.setOnItemClickListener(onNodeClickListener);
        listAdapter.setDataItems(DataManager.getRootNodes());

        // список записей
        recordsListView = (ListView)findViewById(R.id.records_list_view);
        TextView emptyTextView = (TextView)findViewById(R.id.text_view_empty);
        recordsListView.setOnItemClickListener(onRecordClicklistener);
        recordsListView.setEmptyView(emptyTextView);
        registerForContextMenu(recordsListView);
        this.listAdapter = new RecordsListAdapter(this);

        viewSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);
        recordContentTextView = (TextView) findViewById(R.id.text_view_record_content);
    }

    /**
     * Отображение ветки => список записей
     * @param node
     */
    private void showNode(TetroidNode node)
    {
        this.currentNode = node;
        Toast.makeText(getApplicationContext(), node.getName(), Toast.LENGTH_SHORT).show();
        if (viewSwitcher.getDisplayedChild() == RECORD_DETAILS_VIEW_NUM)
            viewSwitcher.showPrevious();
        drawerLayout.closeDrawers();

        this.listAdapter.reset(node.getRecords());
        recordsListView.setAdapter(listAdapter);
        setTitle(node.getName());
        Toast.makeText(this, "Открытие " + node.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Отображение записи
     * @param position Индекс записи в списке записей ветки
     */
    private void showRecord(int position) {
        TetroidRecord record = (TetroidRecord)currentNode.getRecords().get(position);
        showRecord(record);
    }

    /**
     * Отображение записи
     * @param record Запись
     */
    private void showRecord(TetroidRecord record) {
        Spanned recordContent = record.getContent();
        recordContentTextView.setText(recordContent);
        viewSwitcher.showNext();
        setTitle(record.getName());
        Toast.makeText(this, "Открытие " + record.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Обработчик клика на заголовке ветки с подветками
     */
    NodesListAdapter.OnNodeHeaderClickListener onNodeHeaderClickListener = new NodesListAdapter.OnNodeHeaderClickListener() {
        @Override
        public void onClick(TetroidNode node) {
            showNode(node);
        }
    };

    /**
     * Обработчик клика на "конечной" ветке (без подветок)
     */
    private OnItemClickListener onNodeClickListener = new OnItemClickListener() {

        @Override
        public void onItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
            showNode((TetroidNode)item);
        }

        @Override
        public void onGroupItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
            // это событие обрабатывается с помощью OnNodeHeaderClickListener, чтобы разделить клик
            // на заголовке и на стрелке раскрытия/закрытия ветки
        }
    };

    /**
     * Обработчик клика на записи
     */
    private AdapterView.OnItemClickListener onRecordClicklistener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            showRecord(position);
        }
    };

    /**
     * Обработчик нажатия кнопки Назад
     */
    @Override
    public void onBackPressed() {
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (viewSwitcher.getDisplayedChild() == RECORD_DETAILS_VIEW_NUM) {
            viewSwitcher.showPrevious();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Обработчик создания системного меню
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Обработчик выбора пунктов системного меню
     * @param item
     * @return
     */
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

    /**
     * Обработчик создания контекстного меню при долгом тапе на записи
     * @param menu
     * @param v
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(Menu.NONE, OPEN_RECORD_MENU_ITEM_ID, Menu.NONE, "Открыть");
    }

    /**
     * Обработчик выбора пунктов контекстного меню записи
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case OPEN_RECORD_MENU_ITEM_ID:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                showRecord(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

}
