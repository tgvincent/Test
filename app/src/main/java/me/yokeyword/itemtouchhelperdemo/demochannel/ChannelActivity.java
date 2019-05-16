package me.yokeyword.itemtouchhelperdemo.demochannel;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.yokeyword.itemtouchhelperdemo.R;
import me.yokeyword.itemtouchhelperdemo.helper.DataManager;
import me.yokeyword.itemtouchhelperdemo.helper.ItemDragHelperCallback;

/**
 * 频道 增删改查 排序
 * Created by Panda on 19/4/28.
 */
public class ChannelActivity extends AppCompatActivity {

    private RecyclerView mRecy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        mRecy = (RecyclerView) findViewById(R.id.recy);
        init();
    }

    private void init() {
        //数据1
        final List<ChannelEntity> items = new ArrayList<>();
        for (int i = 0; i < DataManager.sStringList.size(); i++) {
            ChannelEntity entity = new ChannelEntity();
            entity.setName(DataManager.sStringList.get(i));
            items.add(entity);
        }

        //数据2
        final List<ChannelEntity> otherItems = new ArrayList<>();
        for (int i = 0; i < DataManager.bottomString.size(); i++) {
            ChannelEntity entity = new ChannelEntity();
            entity.setName(DataManager.bottomString.get(i));
            otherItems.add(entity);
        }

        GridLayoutManager manager = new GridLayoutManager(this, 4);//4列
        mRecy.setLayoutManager(manager);

        ItemDragHelperCallback callback = new ItemDragHelperCallback();
        final ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(mRecy);

        final ChannelAdapter adapter = new ChannelAdapter(this, helper, items, otherItems);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = adapter.getItemViewType(position);
                //我的频道和其他频道标题的Item占4列空间, 其他Item1列空间
                return viewType == ChannelAdapter.TYPE_MY || viewType == ChannelAdapter.TYPE_OTHER ? 1 : 4;
            }
        });
        mRecy.setAdapter(adapter);
        adapter.setOnMyChannelItemClickListener(new ChannelAdapter.OnMyChannelItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {

                Toast.makeText(ChannelActivity.this, items.get(position).getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
