package com.nolanofra.test.lazyLoader;

import java.util.ArrayList;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class VideosArrayAdapter extends ArrayAdapter<Video> {
	private ArrayList<Video> videos;
	private Activity activity;
	public ImageManager imageManager;

	public VideosArrayAdapter(Activity a, int textViewResourceId, ArrayList<Video> videos) {
		super(a, textViewResourceId, videos);
		this.videos = videos;
		activity = a;
		
		imageManager = 
			new ImageManager(activity.getApplicationContext());
	}

	public static class ViewHolder{
		public TextView title;
		public TextView description;
		public ImageView image;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		ViewHolder holder;
		if (v == null) {		
			LayoutInflater vi = 
				(LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.list_item, null);
			holder = new ViewHolder();
			holder.title = (TextView) v.findViewById(R.id.title);
			holder.description = (TextView) v.findViewById(R.id.description);
			holder.image = (ImageView) v.findViewById(R.id.avatar);
			v.setTag(holder);
		}
		else
			holder=(ViewHolder)v.getTag();

		final Video video = videos.get(position);
		if (video != null) {
			holder.title.setText(video.title);
			holder.description.setText(video.description);
			holder.image.setTag(video.thumbnailSQDefault);
			imageManager.displayImage(video.thumbnailSQDefault, activity, holder.image);
		}
		return v;
	}
	
	public void clearCacheImageManager()
	{
		this.imageManager.clearCache();
	}
}
