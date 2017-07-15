package co.polarr.albumsdkdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.io.File;
import java.util.List;

import co.polarr.utils.MemoryCache;

public class GroupingActivity extends AppCompatActivity {

    private RecyclerView groupCon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grouping);

        groupCon = (RecyclerView) findViewById(R.id.rv_photos);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(GroupingActivity.this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        groupCon.setLayoutManager(linearLayoutManager);

        List<List<File>> photoFiles = (List<List<File>>) MemoryCache.get("group_files");

        groupCon.setAdapter(new GroupPhotoAdapter(GroupingActivity.this, photoFiles));
    }
}
