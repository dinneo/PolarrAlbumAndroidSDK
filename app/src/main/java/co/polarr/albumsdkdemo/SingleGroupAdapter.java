package co.polarr.albumsdkdemo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.util.List;

import co.polarr.utils.ImageLoadUtil;

/**
 * Created by Colin on 2017/3/9.
 * picky layout title adapter
 */

public class SingleGroupAdapter extends RecyclerView.Adapter<SingleGroupAdapter.LayoutViewHolder> {
    private final List<File> mPhotos;
    private Context mContext;
    private LayoutInflater mInflater;

    public SingleGroupAdapter(Context context, List<File> photoFiles) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mPhotos = photoFiles;
    }

    @Override
    public LayoutViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.single_group_item, parent, false);

        return new LayoutViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mPhotos.size();
    }

    @Override
    public void onBindViewHolder(LayoutViewHolder holder, int position) {
        holder.updateView(position);
    }

    class LayoutViewHolder extends RecyclerView.ViewHolder {
        private ImageView photoCon;

        public LayoutViewHolder(View itemView) {
            super(itemView);
            photoCon = (ImageView) itemView.findViewById(R.id.iv_photo);
        }

        void updateView(int index) {
            File photo = mPhotos.get(index);
            photoCon.setImageBitmap(ImageLoadUtil.decodeThumbBitmapForFile(photo.getPath(), 200, 200, ImageLoadUtil.getImageOrientation(photo.getPath())));
        }
    }
}
