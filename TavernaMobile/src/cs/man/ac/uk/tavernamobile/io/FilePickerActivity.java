package cs.man.ac.uk.tavernamobile.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cs.man.ac.uk.tavernamobile.R;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FilePickerActivity extends ListActivity {
	
	public final static String FILE_PATH = "file_path";
	public final static String SELECTED_FILE_NAME = "selectedFileName";
	private static String INITIAL_DIRECTORY;// = "/mnt/sdcard/";
	
	protected File mDirectory;
	protected ArrayList<File> mFiles;
	protected FilePickerListAdapter mAdapter;
	protected String[] acceptedFileExtensions;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String inputPortName = (String) getIntent().getStringExtra("inputPortName");
		if(inputPortName == null){
			inputPortName = "input port";
		}
		
		// UI components
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle("Select Input file for \""+inputPortName+"\"");
		actionBar.setIcon(this.getResources().getDrawable(R.drawable.taverna_wheel_logo_medium));
		
		// Set the view to be shown if the list is empty
		LayoutInflater inflator = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View emptyView = inflator.inflate(R.layout.file_picker_empty_view, null);
		((ViewGroup)getListView().getParent()).addView(emptyView);
		getListView().setEmptyView(emptyView);
		
		// Initialise initial directory and file List
		INITIAL_DIRECTORY = Environment.getExternalStorageDirectory().getPath();
		mDirectory = new File(INITIAL_DIRECTORY);		
		mFiles = new ArrayList<File>();
		mAdapter = new FilePickerListAdapter(this, mFiles);
		setListAdapter(mAdapter);
		
		// accept all file extensions
		acceptedFileExtensions = new String[] {};
	}
	
	@Override
	protected void onResume() {
		refreshFilesList();
		super.onResume();
	}
	
	@Override
	public void onBackPressed() {
		finish();
		super.onBackPressed();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		File newFile = (File)l.getItemAtPosition(position);
		
		if(newFile.isFile()) {
			// Set result
			Intent extra = new Intent();
			extra.putExtra(FILE_PATH, newFile.getAbsolutePath());
			extra.putExtra(SELECTED_FILE_NAME, newFile.getName());
			setResult(RESULT_OK, extra);
			// close the activity
			finish();
		} else {
			// if it is directory
			mDirectory = newFile;
			// Update the files list
			refreshFilesList();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if(mDirectory.getParentFile() != null) {
				// Go to parent directory
				mDirectory = mDirectory.getParentFile();
				refreshFilesList();
			}
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private class FilePickerListAdapter extends ArrayAdapter<File> {
		
		private List<File> mObjects;
		
		public FilePickerListAdapter(Context context, List<File> objects) {
			super(context, R.layout.file_picker_list_item, android.R.id.text1, objects);
			mObjects = objects;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			if(convertView == null) { 
				LayoutInflater inflater = 
						(LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.file_picker_list_item, parent, false);
			}
			File object = mObjects.get(position);

			ImageView imageView = (ImageView)convertView.findViewById(R.id.file_picker_image);
			TextView textView = (TextView)convertView.findViewById(R.id.file_picker_text);
			textView.setSingleLine(true);
			textView.setText(object.getName());
			
			if(object.isFile()) {
				// Show the file icon
				imageView.setImageResource(R.drawable.file_icon);
			} else {
				// Show the folder icon
				imageView.setImageResource(R.drawable.folder_icon);
			}
			
			return convertView;
		}
	}
	
	protected void refreshFilesList() {
		mFiles.clear();
		// Set the file extension filter
		ExtensionFilenameFilter filter = new ExtensionFilenameFilter(acceptedFileExtensions);
		
		// Get the files in the directory
		File[] files = mDirectory.listFiles(filter);
		if(files != null && files.length > 0) {
			for(File f : files) {
				mFiles.add(f);
			}
			
			Collections.sort(mFiles, new FileComparator());
		}
		mAdapter.notifyDataSetChanged();
	}
	
	private class FileComparator implements Comparator<File> {
	    public int compare(File f1, File f2) {
	    	if(f1 == f2) {
	    		return 0;
	    	}
	    	if(f1.isDirectory() && f2.isFile()) {
	        	// Show directories above files
	        	return -1;
	        }
	    	if(f1.isFile() && f2.isDirectory()) {
	        	// Show files below directories
	        	return 1;
	        }
	    	// Sort the directories alphabetically
	        return f1.getName().compareToIgnoreCase(f2.getName());
	    }
	}
	
	private class ExtensionFilenameFilter implements FilenameFilter {
		private String[] mExtensions;
		
		public ExtensionFilenameFilter(String[] extensions) {
			super();
			mExtensions = extensions;
		}
		
		public boolean accept(File dir, String filename) {
			if(new File(dir, filename).isDirectory()) {
				// Accept all directory names
				return true;
			}
			if(mExtensions != null && mExtensions.length > 0) {
				for(int i = 0; i < mExtensions.length; i++) {
					if(filename.endsWith(mExtensions[i])) {
						// The filename ends with the extension
						return true;
					}
				}
				// The filename did not match any of the extensions
				return false;
			}
			// No extensions has been set. Accept all file extensions.
			return true;
		}
	}
}
