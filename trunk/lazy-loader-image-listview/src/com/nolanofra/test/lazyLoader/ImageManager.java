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
 * 
 * This is a fork of Codehenge project:
 * https://github.com/cacois/TweetView
 * http://codehenge.net/blog/2011/06/android-development-tutorial-asynchronous-lazy-loading-and-caching-of-listview-images/
 */

package com.nolanofra.test.lazyLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Stack;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

public class ImageManager {
	
	//Max cache dir size, in Bytes
	private final long MAX_CACHE_DIR_SIZE = 1500000; //1,5 MB
	
	
	// Just using a hashmap for the cache. SoftReferences would 
	// be better, to avoid potential OutOfMemory exceptions
	private HashMap<String, Bitmap> imageMap = new HashMap<String, Bitmap>();
	
	private File cacheDir;
	private ImageQueue imageQueue = new ImageQueue();
	private Thread imageLoaderThread = new Thread(new ImageQueueManager());
	
	public ImageManager(Context context) {
		// Make background thread low priority, to avoid affecting UI performance
		imageLoaderThread.setPriority(Thread.NORM_PRIORITY-1);

		// Find the dir to save cached images
		String sdState = android.os.Environment.getExternalStorageState();
		Log.d("sdState: ", sdState);
		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			cacheDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
			Log.d("cacheDir: ", cacheDir.getAbsolutePath());		
		}
		else
			cacheDir = context.getCacheDir();
		
		if(!cacheDir.exists())
			cacheDir.mkdirs();
	}
	   
	public void displayImage(String url, Activity activity, ImageView imageView) {
		if(imageMap.containsKey(url))
			imageView.setImageBitmap(imageMap.get(url));
		else {
			queueImage(url, activity, imageView);
			imageView.setImageResource(R.drawable.icon);
		}
	}

	private void queueImage(String url, Activity activity, ImageView imageView) {
		// This ImageView might have been used for other images, so we clear 
		// the queue of old tasks before starting.
		imageQueue.Clean(imageView);
		ImageRef p=new ImageRef(url, imageView);				

		synchronized(imageQueue.imageRefs) {
			imageQueue.imageRefs.push(p);
			imageQueue.imageRefs.notifyAll();
		}

		// Start thread if it's not started yet
		if(imageLoaderThread.getState() == Thread.State.NEW)			
			imageLoaderThread.start();
	}

	private Bitmap getBitmap(String url) {
		String filename = String.valueOf(url.hashCode());
		File f = new File(cacheDir, filename);

		// Is the bitmap in our cache?
		Bitmap bitmap = BitmapFactory.decodeFile(f.getPath());
		if(bitmap != null)
			return bitmap;
		else
			Log.d("getBitmap", "bitmap isn't in cache");

		// Nope, have to download it
		try {
			bitmap = BitmapFactory.decodeStream(new FlushedInputStream(new URL(url).openConnection().getInputStream()));
			if (bitmap != null)
			{
				//save bitmap to cache for later
				new Thread(new WriteFileManager(bitmap, f)).start();
			}
			else
				Log.d("getBitmap", "dedode stream error: bitmap is null");
			
			return bitmap;
		} catch (Exception ex) {
			Log.d("Error getBitmap: ", ex.getMessage());
			return null;
		}
	}
	
	private class WriteFileManager implements Runnable
	{
		Bitmap bmp;
		File f;

		public WriteFileManager (Bitmap _bmp, File _f)
		{
			bmp = _bmp;
			f = _f;
		}
		
		@Override
		public void run() {
			FileOutputStream out = null;
			
			try {
				out = new FileOutputStream(f);
				bmp.compress(Bitmap.CompressFormat.PNG, 80, out);
			} catch (Exception e) {
				e.printStackTrace();
			}
			finally { 
				try { if (out != null ) out.close(); }
				catch(Exception ex) {} 
			}			
		}		
	}		
	
	/** Classes **/
	
	private class ImageRef {
		public String url;
		public ImageView imageView;
		
		public ImageRef(String u, ImageView i) {
			url=u;
			imageView=i;
		}
	}
	
	//stores list of images to download
	private class ImageQueue {
		private Stack<ImageRef> imageRefs = 
			new Stack<ImageRef>();

		//removes all instances of this ImageView
		public void Clean(ImageView view) {
			
			for(int i = 0 ;i < imageRefs.size();) {
				if(imageRefs.get(i).imageView == view)
					imageRefs.remove(i);
				else ++i;
			}
		}
	}
	
	private class ImageQueueManager implements Runnable {
		@Override
		public void run() {
			try {
				while(true) {
					// Thread waits until there are images in the 
					// queue to be retrieved
					if(imageQueue.imageRefs.size() == 0) {
						synchronized(imageQueue.imageRefs) {							
							imageQueue.imageRefs.wait();							
						}
					}
					
					// When we have images to be loaded
					if(imageQueue.imageRefs.size() != 0) {
						ImageRef imageToLoad;

						synchronized(imageQueue.imageRefs) {
							imageToLoad = imageQueue.imageRefs.get(0);
						}
												
						Bitmap bmp = getBitmap(imageToLoad.url);
						
						if (bmp != null)
						{
							imageQueue.imageRefs.remove(imageToLoad);
							imageMap.put(imageToLoad.url, bmp);
							Object tag = imageToLoad.imageView.getTag();
							
							// Make sure we have the right view - thread safety defender
							if(tag != null && ((String)tag).equals(imageToLoad.url)) {
								BitmapDisplayer bmpDisplayer = 
									new BitmapDisplayer(bmp, imageToLoad.imageView);
								
								Activity a = 
									(Activity)imageToLoad.imageView.getContext();
								
								a.runOnUiThread(bmpDisplayer);
							}
							else
								Log.d("WARNING: ", "img is null");							
						}
					}
					
					if(Thread.interrupted())
					{
						Log.d("imageLoaderThread", "thread interrupted()");
						break;
					}
				}
			} catch (InterruptedException e) {}
		}
	}

	//Used to display bitmap in the UI thread
	private class BitmapDisplayer implements Runnable {
		Bitmap bitmap;
		ImageView imageView;
		
		public BitmapDisplayer(Bitmap b, ImageView i) {
			bitmap=b;
			imageView=i;
		}
		
		public void run() {
			if(bitmap != null)
				imageView.setImageBitmap(bitmap);
			else
				imageView.setImageResource(R.drawable.icon);
		}
	}
	
	public void interruptThread()
	{
		imageLoaderThread.interrupt();
	}
	
	
	private class ClearCacheManager implements Runnable
	{
		@Override
		public void run() {
			try
			{				
	        	Log.d("clearCache", "max dir cache size reached");
	        	File[] files=cacheDir.listFiles();
	            
	            Arrays.sort(files, new Comparator<File>()
	            {        	
	                public int compare(File f1, File f2)
	                {
	                    return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
	                }
	            });
	        	
	            int index = 0;
	            while (MyLibrary.dirSize(cacheDir) > MAX_CACHE_DIR_SIZE)
	            {
	            	files[index].delete();
	            	index ++;
	            }					        
			}
			catch (SecurityException e) {} 
		}
	}
			
	public void clearCache() {
        //clear memory cache
		imageMap.clear();
		//interrupt thread
        imageLoaderThread.interrupt();
        //clear SD cache
        
        Log.d("cache dir size is:  ", Long.toString(MyLibrary.dirSize(cacheDir)));
        
        /*if cache directory size is greater than 1,5 MB => i'll delete oldest file, until reaching cache dir maximum size*/
        if (MyLibrary.dirSize(cacheDir) > MAX_CACHE_DIR_SIZE)
        	new Thread(new ClearCacheManager()).start();                     
    }
	
	/**
	 * seems there was some problem with the stream and the way android handled it.
	 * More information can be found ad: http://code.google.com/p/android/issues/detail?id=6066
	 * android bug n°6066
	 * @author f.nolano
	 *
	 */
	static class FlushedInputStream extends FilterInputStream {
	    public FlushedInputStream(InputStream inputStream) {
	    super(inputStream);
	    }

	    @Override
	    public long skip(long n) throws IOException {
	        long totalBytesSkipped = 0L;
	        while (totalBytesSkipped < n) {
	            long bytesSkipped = in.skip(n - totalBytesSkipped);
	            if (bytesSkipped == 0L) {
	                  int mbyte = read();
	                  if (mbyte < 0) {
	                      break;  // we reached EOF
	                  } else {
	                      bytesSkipped = 1; // we read one byte
	                  }
	           }
	           totalBytesSkipped += bytesSkipped;
	        }
	        return totalBytesSkipped;
	    }
	}

}