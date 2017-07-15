package co.polarr.albumsdkdemo;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Created by Colin on 2017/3/9.
 * picky layout title adapter
 */

public class GroupPhotoAdapter extends RecyclerView.Adapter<GroupPhotoAdapter.LayoutViewHolder> {
    private final List<List<File>> mPhotoFiles;
    private Context mContext;
    private LayoutInflater mInflater;

    public GroupPhotoAdapter(Context context, List<List<File>> photoFiles) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mPhotoFiles = photoFiles;
    }

    @Override
    public LayoutViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.group_photo_item, parent, false);

        return new LayoutViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mPhotoFiles.size();
    }

    @Override
    public void onBindViewHolder(LayoutViewHolder holder, int position) {
        holder.updateView(position);
    }

    class LayoutViewHolder extends RecyclerView.ViewHolder {
        private RecyclerView photosCon;
        private TextView tvGroupName;

        public LayoutViewHolder(View itemView) {
            super(itemView);
            photosCon = (RecyclerView) itemView.findViewById(R.id.rv_photos);
            tvGroupName = (TextView) itemView.findViewById(R.id.tv_groupName);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
            linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            photosCon.setLayoutManager(linearLayoutManager);
        }

        void updateView(int index) {
            List<File> files = mPhotoFiles.get(index);
            tvGroupName.setText(String.format(Locale.ENGLISH, "Group %d:", index + 1));
            SingleGroupAdapter singleGroupAdapter = new SingleGroupAdapter(mContext, files);
            photosCon.setAdapter(singleGroupAdapter);
        }
    }
}
