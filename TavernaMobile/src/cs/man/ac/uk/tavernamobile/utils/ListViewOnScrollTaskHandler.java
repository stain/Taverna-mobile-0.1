package cs.man.ac.uk.tavernamobile.utils;

import cs.man.ac.uk.tavernamobile.MainPanelActivity;
import android.os.Handler;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

public class ListViewOnScrollTaskHandler {
	
	// flag that help with preventing multiple
	// enter of the block that execute a search.
	// since every single change on the list trigger the
	// scroll event...
	public boolean taskInProgress = false;
	
	// flag that used to stop executing the task
	// depends on business logic e.g. no more results to load
	public boolean disableTask = false;
	
	// The onScroll gets called even when the user
	// not actually scrolling (consider a bug in the
	// framework) - hence need a flag to indicate actual
	// scrolling from user
	private boolean userScrolled = false;
	
	private SystemStatesChecker systemStatesChecker;
	
	private ListView theList;
	private CallbackTask loadingTask;
	
	private MainPanelActivity mainPanel;
	
	public ListViewOnScrollTaskHandler(MainPanelActivity activity, ListView list, CallbackTask task){
		theList = list;
		loadingTask = task;
		systemStatesChecker = new SystemStatesChecker(theList.getContext());
		mainPanel = activity;
	}
	
	public void setOnScrollLoading(){
		theList.setOnScrollListener(new OnScrollListener(){
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {

				// ">=" - in case there are any padding
				boolean reachTheEnd = firstVisibleItem + visibleItemCount >= totalItemCount &&
						totalItemCount > visibleItemCount;
						// if scroll reach the end of the list AND
						// if we are already in the process of doing
						// a new search... (since every change made to the list layout
						// triggers the invocation of onScroll())
						if(reachTheEnd && userScrolled && !taskInProgress && !disableTask) {
							taskInProgress = true; // lock
							userScrolled = false;
							// Check Network Connection
							if(!systemStatesChecker.isNetworkConnected()){
								return;
							}
							// slightly delay in order to add footer smoothly
							new Handler().postDelayed(new Runnable() {
								public void run() {
									loadingTask.onTaskInProgress();
								}},1000);
						}
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if(scrollState == SCROLL_STATE_TOUCH_SCROLL){
					userScrolled = true;
					mainPanel.showPoweredBy();
				}
				else if(scrollState == SCROLL_STATE_IDLE){
					new Handler().postDelayed(new Runnable() {
						public void run() {
							mainPanel.hidePoweredBy();
						}
					},200);
				}
			}
		});
	}
	
	// method to initialize variables related
	// to the search state
	public void initializeSearchState() {
		this.taskInProgress = false;
		// this.newSearch = true;
		this.disableTask = false;
		this.userScrolled = false;
	}
}
