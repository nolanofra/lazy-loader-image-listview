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

import java.io.File;
import java.util.Stack;

public class MyLibrary {

	/**
	 * Calculates dir size
	 * @param dir dir who calculate size from
	 * @return dir size
	 */
	public static long dirSize(File dir) {
	    long result = 0;

	    Stack<File> dirlist= new Stack<File>();
	    dirlist.clear();

	    dirlist.push(dir);

	    while(!dirlist.isEmpty())
	    {
	        File dirCurrent = dirlist.pop();

	        File[] fileList = dirCurrent.listFiles();
	        for (int i = 0; i < fileList.length; i++) {

	            if(fileList[i].isDirectory())
	                dirlist.push(fileList[i]);
	            else
	                result += fileList[i].length();
	        }
	    }

	    return result;
	}
}
