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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ListActivity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends ListActivity{

	public Bitmap placeholder;
	
	VideosArrayAdapter videoArrayAdapter;
	ArrayList<Video> videos;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        //ArrayList<Video> videos = getVideos ("https://gdata.youtube.com/feeds/api/users/radiobocconi/favorites?alt=jsonc&v=2&orderby=updated");
        new VideosAsyncTask().execute("https://gdata.youtube.com/feeds/api/users/bocconitv/favorites?alt=jsonc&v=2&orderby=updated");      
                    
        videos = new ArrayList<Video>();
        videoArrayAdapter = new VideosArrayAdapter(this, R.layout.list_item, videos);
        setListAdapter(videoArrayAdapter);  
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (videoArrayAdapter != null)
			videoArrayAdapter.clearCacheImageManager();
	}
	
	private class VideosAsyncTask extends AsyncTask <String, Void, Void>
	{
		@Override
		protected Void doInBackground(String... urls) {
			
			for (String url : urls)
				videos = getVideos(url);
					
			return null;
		}
		
		@Override
		protected void onPostExecute(Void x)
		{
			if (videos != null && !videos.isEmpty())
			{
				videoArrayAdapter.clear();
								
				for (Video v : videos)
					videoArrayAdapter.add(v);
				
				videoArrayAdapter.notifyDataSetChanged();							
			}
		}
	}
	
	private ArrayList<Video> getVideos (String url)
	{
		ArrayList<Video> videos = new ArrayList<Video>();
		
		String jsonString = executeGet(url);						
				
		JSONTokener tokener = new JSONTokener(jsonString);
		
		JSONObject objectMain = null; 
		
		try {
			objectMain = new JSONObject(tokener);
		} catch (JSONException e) {
			e.printStackTrace();
		}
				
		JSONObject data = null;
		JSONArray itemsArray = null;
		try {
			data = objectMain.getJSONObject("data");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		try
		{
			itemsArray = data.getJSONArray("items");
			
			for (int i = 0; i < itemsArray.length(); i++)
			{
				JSONObject videoRoot= itemsArray.getJSONObject(i);
				JSONObject video = videoRoot.getJSONObject("video");
				Video v = new Video();
				v.title = video.getString("title");
				if (!video.isNull("description"))
					v.description = video.getString("description");
				if (!video.isNull("thumbnail"))
				{
					JSONObject thumbnail =  video.getJSONObject("thumbnail");
					v.thumbnailHQDefault = thumbnail.getString("hqDefault");
					v.thumbnailSQDefault = thumbnail.getString("sqDefault");
				}
				
				videos.add(v);
			}					
		}
		catch (Exception e)
		{
			Log.d("error", e.getMessage());
		}		
		
		
		return videos;
	}
	
	
	public String executeGet (String url)
	{	
		int timeout = 5000;
		HttpGet get;
		InputStream instream;
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);		
		HttpClient client = new DefaultHttpClient();		
		
		String jsonText = "";
		get = new HttpGet(url);
		HttpResponse response = null;
		instream = null;
				
		try
		{		
			try 
			{
				response = client.execute(get);
			} 
			catch (ClientProtocolException e1)
			{
				e1.printStackTrace();		
			} 
			catch (IOException e1) 
			{
				e1.printStackTrace();			
			}					
			int status = response.getStatusLine().getStatusCode();
			
			if (status == HttpStatus.SC_OK) 
			{
				HttpEntity entity = response.getEntity();
				
				try
				{
					instream = entity.getContent();			
				}
				catch (IllegalStateException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();			
				} 
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();			
				}
				
				jsonText = convertStreamToString(instream);
			}			
		}
		catch (Exception e)
		{			
		}
		return jsonText;
	}
		
	public String  convertStreamToString(InputStream is)
	{
		BufferedReader reader = null;		
        InputStreamReader inputStreamReader = new InputStreamReader(is); 
		reader = new BufferedReader(inputStreamReader);
		//String encoding = inputStreamReader.getEncoding();
		
        StringBuilder sb = new StringBuilder();
 
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
            	inputStreamReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }        
        try {

        	return sb.toString();
        	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
}
