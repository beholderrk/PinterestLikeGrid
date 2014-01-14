package com.agimind.pinterestlikegrid;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.origamilabs.library.views.ScaleImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    private PinterestGrid grid;

    private String urls[] = {
            "http://192.168.0.84:8005/media/comics/2013/11/12/3/COMICS_PNG_1386768242874.png",
            "http://192.168.0.84:8005/media/comics/2013/11/12/3/COMICS_PNG_1386768225069.png",
            "http://192.168.0.84:8005/media/comics/2013/11/12/3/COMICS_PNG_1386768208156.png",
            "http://192.168.0.84:8005/media/comics/2013/10/12/3/COMICS_PNG_1386692781522.png",
            "http://192.168.0.84:8005/media/comics/2013/02/12/3/COMICS_PNG_1385983495724.png",
            "http://192.168.0.84:8005/media/comics/2013/02/12/3/COMICS_PNG_1385979833814.png",
            "http://192.168.0.84:8005/media/comics/2013/02/12/3/COMICS_PNG_1385979833814.png",
//            "http://farm7.staticflickr.com/6101/6853156632_6374976d38_c.jpg",
//            "http://farm8.staticflickr.com/7232/6913504132_a0fce67a0e_c.jpg",
//            "http://farm5.staticflickr.com/4133/5096108108_df62764fcc_b.jpg",
//            "http://farm5.staticflickr.com/4074/4789681330_2e30dfcacb_b.jpg",
//            "http://farm9.staticflickr.com/8208/8219397252_a04e2184b2.jpg",
//            "http://farm9.staticflickr.com/8483/8218023445_02037c8fda.jpg",
//            "http://farm9.staticflickr.com/8335/8144074340_38a4c622ab.jpg",
//            "http://farm9.staticflickr.com/8060/8173387478_a117990661.jpg",
//            "http://farm9.staticflickr.com/8056/8144042175_28c3564cd3.jpg",
//            "http://farm9.staticflickr.com/8183/8088373701_c9281fc202.jpg",
//            "http://farm9.staticflickr.com/8185/8081514424_270630b7a5.jpg",
//            "http://farm9.staticflickr.com/8462/8005636463_0cb4ea6be2.jpg",
//            "http://farm9.staticflickr.com/8306/7987149886_6535bf7055.jpg",
//            "http://farm9.staticflickr.com/8444/7947923460_18ffdce3a5.jpg",
//            "http://farm9.staticflickr.com/8182/7941954368_3c88ba4a28.jpg",
//            "http://farm9.staticflickr.com/8304/7832284992_244762c43d.jpg",
//            "http://farm9.staticflickr.com/8163/7709112696_3c7149a90a.jpg",
//            "http://farm8.staticflickr.com/7127/7675112872_e92b1dbe35.jpg",
//            "http://farm8.staticflickr.com/7111/7429651528_a23ebb0b8c.jpg",
//            "http://farm9.staticflickr.com/8288/7525381378_aa2917fa0e.jpg",
//            "http://farm6.staticflickr.com/5336/7384863678_5ef87814fe.jpg",
//            "http://farm8.staticflickr.com/7102/7179457127_36e1cbaab7.jpg",
//            "http://farm8.staticflickr.com/7086/7238812536_1334d78c05.jpg",
//            "http://farm8.staticflickr.com/7243/7193236466_33a37765a4.jpg",
//            "http://farm8.staticflickr.com/7251/7059629417_e0e96a4c46.jpg",
//            "http://farm8.staticflickr.com/7084/6885444694_6272874cfc.jpg"
    };
    private int[][] sizes = {
            {413, 217},
            {413, 217},
            {413, 217},
            {413, 217},
            {413, 217},
            {413, 507},
            {413, 507},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        grid = (PinterestGrid) findViewById(R.id.grid);
        View footer = View.inflate(this, R.layout.view_loading_footer, null);

        StaggeredAdapter adapter = new StaggeredAdapter(this, urls, sizes);
        ArrayList<ListView.FixedViewInfo> footers = new ArrayList<ListView.FixedViewInfo>();
        ListView.FixedViewInfo footerInfo = new ListView(this).new FixedViewInfo();
        footerInfo.view = footer;
        footers.add(footerInfo);
        HeaderViewListAdapter hfAdapter = new HeaderViewListAdapter(null, footers, adapter);
        grid.setAdapter(hfAdapter);
        adapter.notifyDataSetChanged();
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
        switch (id){
            case R.id.update:
                Intent intent = getIntent();
                finish();
                startActivity(intent);
                return true;
            case R.id.scrollto0:
                grid.scrollTo(0, 0);
                return true;
            case R.id.scrolltoBottom:
                grid.flingTo(379);
//                grid.scrollTo(0, grid.getAreaBottom());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class StaggeredAdapter extends BaseAdapter {

        private final DisplayImageOptions options;
        private ImageLoader mLoader;
        private Context context;
        private ArrayList<ImageItem> items = new ArrayList<ImageItem>();

        public StaggeredAdapter(Context context,
                                String[] objects, int[][] sizes) {
            this.context = context;
            mLoader = ImageLoader.getInstance();
            mLoader.init(new ImageLoaderConfiguration.Builder(context)
                    .build());
            options = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisc(true)
                    .build();
            for (int i = 0; i < objects.length; i++) {
                items.add(new ImageItem(objects[i], sizes[i]));
            }
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public ImageItem getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d("getView", Integer.toString(position));

            final ViewHolder holder;

            if (convertView == null) {
                LayoutInflater layoutInflator = LayoutInflater.from(context);
                convertView = layoutInflator.inflate(R.layout.row_staggered_demo, null);
                holder = new ViewHolder();
                holder.imageView = (ScaleImageView) convertView .findViewById(R.id.imageView1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final ImageItem item = getItem(position);

            holder.imageView.setImageBitmap(null);
            holder.imageView.setBackgroundResource(android.R.color.black);
            ViewGroup.LayoutParams lp =  holder.imageView.getLayoutParams();
            lp.width = item.width;
            lp.height = item.height;


            mLoader.displayImage(item.url, holder.imageView, options, new StaggerImageLoadingListener(holder, item, context));

            return convertView;
        }

        class ViewHolder {
            ScaleImageView imageView;
        }

        class ImageItem {
            String url;
            int width;
            int height;

            ImageItem(String url, int[] size) {
                this.url = url;
                this.width = size[0];
                this.height = size[1];
            }

            public void setDimensions(int width, int height){
                this.width = width;
                this.height = height;
            }
        }

        private static class StaggerImageLoadingListener extends SimpleImageLoadingListener {

            final ViewHolder holder;
            final ImageItem item;
            final Animation fadeIn;

            static final List<String> displaing = Collections.synchronizedList(new LinkedList<String>());

            public StaggerImageLoadingListener(ViewHolder holder, ImageItem item, Context context) {
                this.holder = holder;
                this.item = item;
                this.fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                assert this.fadeIn != null;
            }

            @Override
            public void onLoadingComplete(final String imageUri, View view, Bitmap loadedImage) {
                int[] size = ScaleImageView.calcScaleToWidth(loadedImage.getWidth(), loadedImage.getHeight(), holder.imageView.getWidth(), 0);
                item.setDimensions(size[0], size[1]);

                this.fadeIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        displaing.remove(imageUri);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                boolean in_animation = displaing.contains(imageUri);
                if(!in_animation){
                    view.startAnimation(fadeIn);
                }
            }
        }
    }
}
