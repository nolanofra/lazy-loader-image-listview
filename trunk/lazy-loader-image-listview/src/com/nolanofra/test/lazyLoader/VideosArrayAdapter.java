/**
 * @author FRANCESCO NOLANO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

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
			if (video.thumbnailSQDefault != null && !video.thumbnailSQDefault.equals(""))
			{
				holder.image.setTag(video.thumbnailSQDefault);
				imageManager.displayImage(video.thumbnailSQDefault, activity, holder.image);
			}
			else
			{
				holder.image.setImageResource(R.drawable.icon);
				holder.image.setTag(null);
			}
		}
		return v;
	}
	
	public void clearCacheImageManager()
	{
		this.imageManager.clearCache();
	}
}
