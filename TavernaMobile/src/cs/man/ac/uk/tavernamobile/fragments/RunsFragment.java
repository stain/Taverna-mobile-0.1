package cs.man.ac.uk.tavernamobile.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import uk.org.taverna.server.client.InputPort;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshExpandableListView;

import cs.man.ac.uk.tavernamobile.R;
import cs.man.ac.uk.tavernamobile.dataaccess.DataProviderConstants;
import cs.man.ac.uk.tavernamobile.dataaccess.DatabaseLoader;
import cs.man.ac.uk.tavernamobile.datamodels.WorkflowBE;
import cs.man.ac.uk.tavernamobile.datamodels.WorkflowRun;
import cs.man.ac.uk.tavernamobile.io.InputsList;
import cs.man.ac.uk.tavernamobile.io.RunMonitorScreen;
import cs.man.ac.uk.tavernamobile.server.WorkflowLaunchHelper;
import cs.man.ac.uk.tavernamobile.server.WorkflowRunManager;
import cs.man.ac.uk.tavernamobile.utils.CallbackTask;
import cs.man.ac.uk.tavernamobile.utils.MessageHelper;
import cs.man.ac.uk.tavernamobile.utils.SystemStatesChecker;

public class RunsFragment extends Fragment {

	private FragmentActivity parentActivity;
	private ActionMode mActionMode;
	private RunsListAdapter mainListAdapter;
	private PullToRefreshExpandableListView refreshableList;

	private static final String runGroups[] = 
		{ "Initialised", "Running", "Finished", "Stopped", "Deleted" };
	// <State, Map<RunID, Workflow_entity>>
	private HashMap<String, HashMap<String, WorkflowRun>> childElements;
	
	// collection that help with set up checkbox states
	private HashMap<String, ArrayList<Boolean>> checkboxesStates;
	
	// try to reuse the same object
	private SystemStatesChecker systemStateChecker;
	private WorkflowRunManager runManager;
	private RunListRetrievingCompletionListener runRetrievalListener;

	private int wfDetailLoaderID = 3;
	private int Activity_Starter_Code = 3;
	private HashMap<String, WorkflowRun> retrievedRuns;
	//private ArrayList<ChildListAdapter> childListAdapters;
	private ArrayList<String> selectedRunIds;

	// for the sake of Listview inside expendableViwe
	//private int childID;
	
	// index of the selected run group
	// help with dynamic action mode menu loading
	private int selectedGroup;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		runRetrievalListener = new RunListRetrievingCompletionListener();
		// Initialize the collection and adapters
		childElements = new HashMap<String, HashMap<String, WorkflowRun>>();
		selectedRunIds = new  ArrayList<String>();
		checkboxesStates = new HashMap<String, ArrayList<Boolean>>();
		//childListAdapters = new ArrayList<ChildListAdapter>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// super.onCreateView(inflater, container, savedInstanceState);
		View wfRunsView = inflater.inflate(R.layout.main_runs, null); //container, false);
		TextView barText = (TextView) wfRunsView.findViewById(R.id.runsList_bar_text);
		barText.setText("Pull to refresh");
		refreshableList = (PullToRefreshExpandableListView) wfRunsView
				.findViewById(R.id.pull_to_refresh_listview);
		// display action bar menu
		setHasOptionsMenu(true);
		return wfRunsView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		parentActivity = this.getActivity();
		runManager = new WorkflowRunManager(parentActivity);
		systemStateChecker = new SystemStatesChecker(parentActivity);
		
		refreshableList.setOnRefreshListener(new OnRefreshListener<ExpandableListView>() {
		    @Override
		    public void onRefresh(PullToRefreshBase<ExpandableListView> refreshView) {
		        prepareListData();
		    }
		});
		
		mainListAdapter = new RunsListAdapter(parentActivity/*, childElements*/);
		refreshableList.setExpandableAdapter(mainListAdapter);
		for(int i = 0; i < mainListAdapter.getGroupCount(); i++){
			refreshableList.expandGroup(i);
		}
		/*refreshableList.setDivider(null);
		refreshableList.setChildDivider(getResources().getDrawable(R.color.transperent));
		refreshableList.setDividerHeight(10);*/

		super.onActivityCreated(savedInstanceState);
	}
	
	/*@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// remove menu added by previous fragment
		for(int i = 1; i < menu.size(); i ++){
			menu.removeItem(menu.getItem(i).getItemId());
		}
		inflater.inflate(R.menu.runlist_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}*/

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.delete_all_run_menu:
	    	MessageHelper.showOptionsDialog(
    			  refreshableList.getContext(), 
    			  "Delete all your runs on the server ?",
    			  null,
    			  new CallbackTask(){
					@Override
					public Object onTaskInProgress(Object... param) {
						runManager.DeleteAllRun(new CallbackTask(){

							@Override
							public Object onTaskInProgress(Object... param) {
								return null;
							}

							@Override
							public Object onTaskComplete(Object... result) {
								if(result[0] instanceof String){
									Toast.makeText(parentActivity, 
											(String)result[0], Toast.LENGTH_SHORT).show();
									return null;
								} else if((Boolean) result[0]){
									prepareListData();
								}
								return null;
							}
						});
						return null;
					}

					@Override
					public Object onTaskComplete(Object... result){
						return null; 
					}
    			  }, null);
	    	return true;
	    default:
	        break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		// remove menu added by previous fragment
		for(int i = 1; i < menu.size(); i ++){
			menu.removeItem(menu.getItem(i).getItemId());
		}
		parentActivity.getMenuInflater().inflate(R.menu.runlist_menu, menu);
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onPause() {
		if(mActionMode != null){
			mActionMode.finish();
		}
		super.onPause();
	}
	
	@Override
	public void onResume(){
		// since action mode stopped on pause
		// no check box should be checked when resumed
		for(int i = 0; i < runGroups.length; i++){
			ArrayList<Boolean> states = checkboxesStates.get(runGroups[i]);
			if(states != null){
				for(int j = 0; j < states.size(); j++){
					states.set(j, false);
				}
				checkboxesStates.put(runGroups[i], states);
			}
		}
		mainListAdapter.notifyDataSetChanged();
		if(mActionMode != null){
			mActionMode.finish();
		}
		super.onResume();
	}

	private void prepareListData() {
		// check for network connection
		if(!systemStateChecker.isNetworkConnected()){
			return;
		}
		// Initialize the collection and adapters
		childElements = new HashMap<String, HashMap<String, WorkflowRun>>();
		//childListAdapters = new ArrayList<ChildListAdapter>();
		// Do work to refresh the list here.
    	runManager.getRuns(runRetrievalListener);
	}

	// class to process the result of Run List retrieval
	// i.e get Run ID then load relevant workflow details to display
	private class RunListRetrievingCompletionListener implements CallbackTask {
		
		private workflowDetailLoadingListener loadingListener;
		
		public RunListRetrievingCompletionListener(){
			loadingListener = new workflowDetailLoadingListener();
		}

		@Override
		public Object onTaskInProgress(Object... param) { return null; }

		@Override
		public Object onTaskComplete(Object... result) {
			// if message return get returned
			if (result[0] instanceof String) {
				String message = (String) result[0];
				MessageHelper.showMessageDialog(parentActivity, "Attention", message, null);
				// refresh list
				mainListAdapter.notifyDataSetChanged();
				// Mark the current Refresh as complete.
				refreshableList.onRefreshComplete();
			} else {
				retrievedRuns = (HashMap<String, WorkflowRun>) result[0];
				if (retrievedRuns == null || retrievedRuns.size() < 1) {
					// if no runs has been found do nothing
					// message should be return before reaching here
					return null;
				}
				
				/** begin to retrieve details of workflows
				 *  which have runs of which the ID matches those IDs 
				 * 	returned from server
				 * **/
				
				// building the ID collection part of the SQL query
				String theArgs = "";
				Iterator<Entry<String, WorkflowRun>> it = retrievedRuns.entrySet().iterator();
				while (it.hasNext()) {
					HashMap.Entry<String, WorkflowRun> pairs = 
							(HashMap.Entry<String, WorkflowRun>) it.next();
					theArgs += "'" + pairs.getKey() + "', ";
				}
				// remove the last comma
				int lastComma = theArgs.lastIndexOf(",");
				theArgs = theArgs.substring(0, lastComma);
				
				// use the "selection" parameter here as the arguments
				// in contrast to normal query via content provider
				String selection = theArgs;
				Bundle loaderArgs = new Bundle();
				loaderArgs.putString("selection", selection);
				// this JOIN_TABLE URI is only for forcing
				// content resolver invoke the specific query logic in 
				// content provider in order to execute complex SQL query.
				// Not a real content URI
				loaderArgs.putString("tableURI", 
						DataProviderConstants.WF_RUN_JOIN_TABLE_CONTENTURI.toString());

				// create CursorLoader
				getLoaderManager().restartLoader(
						wfDetailLoaderID,
						loaderArgs,
						new DatabaseLoader(parentActivity, loadingListener));
			}
			return null;
		}
	}

	// Class to process workflow details loaded from the database
	// when the loading finished
	private class workflowDetailLoadingListener implements CallbackTask {
		@Override
		public Object onTaskInProgress(Object... param) { return null; }

		@Override
		public Object onTaskComplete(Object... result) {
			if (!(result[0] instanceof Cursor)) {
				return null;
			}
			
			Cursor existingWFRecord = (Cursor) result[0];

			while (existingWFRecord.moveToNext()) {
				String runId = 
						existingWFRecord.getString(
								existingWFRecord.getColumnIndexOrThrow(
										DataProviderConstants.Run_Id));
						
				String workflowTitle =
						existingWFRecord.getString(
								existingWFRecord.getColumnIndexOrThrow(
										DataProviderConstants.WorkflowTitle));

				String workflowVersion = 
						existingWFRecord.getString(
								existingWFRecord.getColumnIndexOrThrow(
										DataProviderConstants.Version));

				String workflowUploaderName = 
						existingWFRecord.getString(
								existingWFRecord.getColumnIndexOrThrow(
										DataProviderConstants.UploaderName));
				
				byte[] avatorData = existingWFRecord
						.getBlob(existingWFRecord
								.getColumnIndexOrThrow(DataProviderConstants.Avatar));
				Bitmap avatorBitmap = BitmapFactory.decodeByteArray(avatorData, 0, avatorData.length);
				
				WorkflowBE wfBE = new WorkflowBE();
				wfBE.setTitle(workflowTitle);
				wfBE.setVersion(workflowVersion);
				wfBE.setUploaderName(workflowUploaderName);
				wfBE.setAvatar(avatorBitmap);
				
				//"Initialised", "Running", "Finished", "Stopped", "Deleted" 
				prepareChildList(wfBE, runId);
			}
			
			// refresh all list
			/*for(ChildListAdapter adapter : childListAdapters){
				adapter.notifyDataSetChanged();
			}*/

			// refresh data
			mainListAdapter.notifyDataSetChanged();
			// mainListAdapter = new RunsListAdapter(parentActivity, childElements);
			// refreshableList.setExpandableAdapter(mainListAdapter);
			// Mark the current Refresh as complete.
			refreshableList.onRefreshComplete();
			
			return null;
		}

		private void prepareChildList(WorkflowBE wfBE, String runId) {
			
			//String state = retrievedRunIdsState.get(runId);
			WorkflowRun theRun = retrievedRuns.get(runId);
			String state = theRun.getRunState();
			
			HashMap<String, WorkflowRun> iniMap = null;
			ArrayList<Boolean> singleGroupCheckState = null;
			// if nothing in the collection (no group)
			// add a new one (group)
			if(childElements.size() < 1){
				iniMap = new HashMap<String, WorkflowRun>();
				singleGroupCheckState = new ArrayList<Boolean>();
			}
			else{
				// try to get the child list at specific group (index) first
				iniMap = childElements.get(state);
				singleGroupCheckState = checkboxesStates.get(state);
				// if there isn't such list (first time)
				// create a new one
				if(iniMap == null){
					iniMap = new HashMap<String, WorkflowRun>();
				}
				
				if(singleGroupCheckState == null){
					singleGroupCheckState = new ArrayList<Boolean>();
				}
			}
			
			// adding other information into the WorkflowRun object
			// e.g title, uploader, avatar, version
			// which are not returned by the server
			theRun.setTitle(wfBE.getTitle());
			theRun.setVersion(wfBE.getVersion());
			theRun.setUploaderName(wfBE.getUploaderName());
			theRun.setAvatar(wfBE.getAvatar());

			// then add the entity in and put it back in the 
			// child elements mapping
			iniMap.put(runId, theRun);
			childElements.put(state, iniMap);
			
			singleGroupCheckState.add(false);
			checkboxesStates.put(state, singleGroupCheckState);
		}
	}
	
	// Adapter of the ExpandableListView (Root layout of the Run fragment)
	private class RunsListAdapter extends BaseExpandableListAdapter {
		private Context myContext;
		//private HashMap<String, HashMap<String, WorkflowRun>> listData;

		public RunsListAdapter(Context context/*, HashMap<String, HashMap<String, WorkflowRun>> data*/) {
			myContext = context;
			//listData = data;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			Object child = childElements.size() <= groupPosition ?
					null : childElements.get(runGroups[groupPosition]).get(childPosition);
			return child;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			long id = getCombinedChildId(groupPosition, childPosition);
			return id;
		}
		
		@Override
		public int getChildrenCount(int groupPosition) {
			int size = childElements.get(runGroups[groupPosition]) == null? 
							0: childElements.get(runGroups[groupPosition]).size();
			return size;
			/*int size = listData.get(runGroups[groupPosition]) == null ? 0 : 1;
			return size;*/
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			
			//childID = childPosition;
			
			if(convertView == null){
				LayoutInflater mInflater = (LayoutInflater) parentActivity.getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
				convertView = mInflater.inflate(R.layout.main_runs_child_singlerow, null);
			}
			
			// get data 
			HashMap<String, WorkflowRun> children = childElements.get(runGroups[groupPosition]);
			// (run ids)
			final String[] mKeys = children.keySet().toArray(new String[children.size()]);
			// WorkflowRun (workflow entity)
			final WorkflowRun workflowEntity = (WorkflowRun) children.get(mKeys[childPosition]);
			
			// UI elements
			TextView wfTitleVersion = (TextView) convertView.findViewById(R.id.runsTitleVersion);
			TextView wfuploaderName = (TextView) convertView.findViewById(R.id.wfRunUploaderName);
			TextView startTimeText = (TextView) convertView.findViewById(R.id.runStartTimeValue);
			TextView endTimeText = (TextView) convertView.findViewById(R.id.runEndTimeValue);
			final CheckBox runCheckbox = (CheckBox) convertView.findViewById(R.id.runList_run_checkbox);
			// data setup
			wfTitleVersion.setText(workflowEntity.getTitle()+" (v"+workflowEntity.getVersion()+")");
			String startTime = workflowEntity.getStartTime();
			String endTime = workflowEntity.getEndTime();
			// hide time info if not available
			startTimeText.setVisibility(8);
			endTimeText.setVisibility(8);
			if(startTime != null){
				startTimeText.setVisibility(0);
				startTimeText.setText(startTime);
			}
			if(endTime != null){
				endTimeText.setVisibility(0);
				endTimeText.setText(endTime);
			}
			// TODO: fixed image scale
			Drawable avatarDrawable = new BitmapDrawable(getResources(),
					Bitmap.createScaledBitmap(workflowEntity.getAvatar(), 80, 80, true));
			wfuploaderName.setCompoundDrawablesWithIntrinsicBounds(null, avatarDrawable, null, null);
			wfuploaderName.setText(workflowEntity.getUploaderName());
			runCheckbox.setChecked(checkboxesStates.get(runGroups[groupPosition]).get(childPosition));
			runCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked){
						// set the selected run group in order to
						// help with loading different action mode menu
						// and prevent selection aross different group etc.
						selectedGroup = groupPosition;
						// save the check state in to the state collection
						ArrayList<Boolean> childStates = checkboxesStates.get(runGroups[groupPosition]);
						childStates.set(childPosition, true);
						checkboxesStates.put(runGroups[groupPosition], childStates);
						
						String runid = (String) mKeys[childPosition];
						selectedRunIds.add(runid);
						
						// uncheck (set state of) checkboxes of other group
						for(int i = 0; i < runGroups.length; i++){
							if(i != selectedGroup){
								ArrayList<Boolean> states = checkboxesStates.get(runGroups[i]);
								if(states != null){
									for(int j = 0; j < states.size(); j++){
										states.set(j, false);
									}
									checkboxesStates.put(runGroups[i], states);
								}
							}
						}
						
						// if select the finished list also uncheck
						// other check box in this list
						if(selectedGroup == 3){
							ArrayList<Boolean> states = checkboxesStates.get(runGroups[selectedGroup]);
							if(states != null){
								for(int j = 0; j < states.size(); j++){
									if(j != childPosition){
										states.set(j, false);
									}
								}
								checkboxesStates.put(runGroups[selectedGroup], states);
							}
						}
						// refresh data set
						mainListAdapter.notifyDataSetChanged();
					} else{
						// remove the check state in to the state collection
						ArrayList<Boolean> childStates = checkboxesStates.get(runGroups[groupPosition]);
						childStates.set(childPosition, false);
						checkboxesStates.put(runGroups[groupPosition], childStates);
						
						String runid = (String) mKeys[childPosition];
						selectedRunIds.remove(runid);
					}
					
					// start the action mode when there are selected runs
					if(mActionMode == null){
						mActionMode = parentActivity.startActionMode(mActionModeCallback);
					}else if(selectedRunIds.size() < 1){
						mActionMode.finish();
					}
				}
				
				private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

				    // Called when the action mode is created; startActionMode() was called
				    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					      MenuInflater inflater = mode.getMenuInflater();
					      inflater.inflate(R.menu.runlist_action_mode_menu, menu);
					      return true;
				    }
		
				    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				    	MenuItem startMenu = menu.findItem(R.id.runList_run_start);
			    		MenuItem stopMenu = menu.findItem(R.id.runList_run_stop);
			    		//MenuItem deleteMenu = menu.findItem(R.id.runList_run_delete);
				    	switch(selectedGroup){
				    	case 0: // Initialized
				    		stopMenu.setEnabled(false);
				    		stopMenu.setVisible(false);
				    		/*deleteMenu.setVisible(true);
					    	deleteMenu.setEnabled(true);*/
						    return true;
				    	case 1: // Running
				    		startMenu.setEnabled(false);
				    		startMenu.setVisible(false);
				    		return true;
				    	case 2: // Finished
				    		stopMenu.setEnabled(false);
				    		stopMenu.setVisible(false);
				    		return true;
				    	case 3: // Stopped
				    		// TODO: interaction not supported
				    		startMenu.setEnabled(false);
				    		startMenu.setVisible(false);
				    		stopMenu.setEnabled(false);
				    		stopMenu.setVisible(false);
				    		return true;
				    	case 4: // Deleted
				    		startMenu.setEnabled(false);
				    		startMenu.setVisible(false);
				    		stopMenu.setEnabled(false);
				    		stopMenu.setVisible(false);
				    		return true;
				    	default:
				    		return false;
				    	}
				    }
		
				    // Called when the user selects a contextual menu item
				    public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
				      switch (item.getItemId()) {
				      case R.id.runList_run_stop:
				    	  MessageHelper.showOptionsDialog(
				    			  refreshableList.getContext(), 
				    			  "Stop selected runs ?",
				    			  null,
				    			  new CallbackTask(){
									@Override
									public Object onTaskInProgress(Object... param) {
										runManager.StopRun("Stopping runs...", selectedRunIds);
										// automatically close action mode when action performed
										mode.finish();
										return null;
									}
		
									@Override
									public Object onTaskComplete(Object... result){
										prepareListData();
										return null; 
									}
				    			  }, null);
				    	  // mode.finish(); 
				        return true;
				      case R.id.runList_run_delete:
				    	  MessageHelper.showOptionsDialog(
				    			  refreshableList.getContext(), 
				    			  "Delete selected runs ?",
				    			  null,
				    			  new CallbackTask(){
									@Override
									public Object onTaskInProgress(Object... param) {
										runManager.DeleteRun("Deleting runs...", selectedRunIds);
										// automatically close action mode when action performed
										mode.finish();
										return null;
									}
		
									@Override
									public Object onTaskComplete(Object... result){
										prepareListData();
										return null; 
									}
				    		  
				    			  }, 
				    			  new CallbackTask(){
									@Override
									public Object onTaskInProgress(Object... param) {
										ArrayList<Boolean> childStates = checkboxesStates.get(runGroups[groupPosition]);
										for(int i=0; i < childStates.size(); i++){
											childStates.set(i, false);
										}
										checkboxesStates.put(runGroups[groupPosition], childStates);
										mainListAdapter.notifyDataSetChanged();
										mode.finish();
										return null;
									}

									@Override
									public Object onTaskComplete(Object... result) { return null; }
				    			  });
				    	  // mode.finish();
				    	  return true;
				      case R.id.runList_run_start:
				    	  	// get launched workflows run ID
							// in order to retrieve its state
							// and then monitor it
							for(String runID : selectedRunIds){
								if (runID != null) {
									WorkflowRunManager manager = new WorkflowRunManager(parentActivity);
									manager.checkRunStateWithID(runID, 
											new RunStateChecker(workflowEntity, runID));
								}
							}
							mode.finish();
				    	  return true;
				      default:
				    	  return false;
				      }
				    }
		
				    // Called when the user exits the action mode
				    public void onDestroyActionMode(ActionMode mode) {
				    	mActionMode = null;
				    }
				}; // end of ActionMode Callback()
			}); // end of onCheckedChangeListener()
			
			/*if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) myContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.main_runs_child, null);
			}
			
			final ChildListAdapter adapter = 
					new ChildListAdapter(childElements.get(runGroups[groupPosition]));
			ListView runList = (ListView) convertView.findViewById(R.id.runsList);
			runList.setAdapter(adapter);
			runList.setOnItemSelectedListener(new OnItemSelectedListener() {

				public void onItemClick(AdapterView<?> parent, View arg1, final int itemIndex, long arg3) {
					
					// get launched workflows run ID
					// in order to retrieve its state
					// and then monitor it
					String runID = (String) adapter.getKey(childPosition);
					final WorkflowBE workflowEntity = (WorkflowBE) adapter.getItem(childPosition);
					
					//final WorkflowBE workflowEntity = (WorkflowBE) adapter.getItem(itemIndex);
					selectedTitle = workflowEntity.getTitle();
					selectedWfVersion = workflowEntity.getVersion();
					selectedWfUploaderName = workflowEntity.getUploaderName();
					
					if (runID != null) {
						WorkflowRunManager manager = new WorkflowRunManager(parentActivity);
						manager.checkRunStateWithID(runID, 
								new RunsListAdapter.RunStateChecker(workflowEntity, runID));
					} else {
						// A run of this workflow has been attempted
						// i.e it has been recorded
						// but the run creation was unsuccessful
						MessageHelper.showOptionsDialog(parentActivity, 
								"There was a problem launching this workflow."
								+"\nDo you want to try again ?", "Attention",
								new CallbackTask() {
									@Override
									public Object onTaskInProgress(Object... param) {
										// check Internet
										SystemStatesChecker sysChecker = new SystemStatesChecker(parentActivity);
										if (!sysChecker.isNetworkConnected()) {
											return null;
										}
										WorkflowBE wfBe = (WorkflowBE) adapter.getItem(itemIndex);

										WorkflowLaunchHelper launchHelper = new WorkflowLaunchHelper(
												parentActivity, wfBe, Activity_Starter_Code);
										launchHelper.launch();
										return null;
									}

									@Override
									public Object onTaskComplete(Object... result) { return null; }
								}, null);
						}
						showLaunchDialog("There was a problem launching this workflow."
										+"\nDo you want to try again ?");
					}

				@Override
				public void onItemSelected(AdapterView<?> parent, View arg1, final int itemIndex, long arg3) {
					// get launched workflows run ID
					// in order to retrieve its state
					// and then monitor it
					String runID = (String) adapter.getKey(childPosition);
					final WorkflowBE workflowEntity = (WorkflowBE) adapter.getItem(childPosition);
					
					//final WorkflowBE workflowEntity = (WorkflowBE) adapter.getItem(itemIndex);
					selectedTitle = workflowEntity.getTitle();
					selectedWfVersion = workflowEntity.getVersion();
					selectedWfUploaderName = workflowEntity.getUploaderName();
					
					if (runID != null) {
						WorkflowRunManager manager = new WorkflowRunManager(parentActivity);
						manager.checkRunStateWithID(runID, 
								new RunsListAdapter.RunStateChecker(workflowEntity, runID));
					} else {
						// A run of this workflow has been attempted
						// i.e it has been recorded
						// but the run creation was unsuccessful
						MessageHelper.showOptionsDialog(parentActivity, 
								"There was a problem launching this workflow."
								+"\nDo you want to try again ?", "Attention",
								new CallbackTask() {
									@Override
									public Object onTaskInProgress(Object... param) {
										// check Internet
										SystemStatesChecker sysChecker = new SystemStatesChecker(parentActivity);
										if (!sysChecker.isNetworkConnected()) {
											return null;
										}
										WorkflowBE wfBe = (WorkflowBE) adapter.getItem(itemIndex);

										WorkflowLaunchHelper launchHelper = new WorkflowLaunchHelper(
												parentActivity, Activity_Starter_Code);
										launchHelper.launch(wfBe, 0);
										return null;
									}

									@Override
									public Object onTaskComplete(Object... result) { return null; }
								}, null);
						}
						showLaunchDialog("There was a problem launching this workflow."
										+"\nDo you want to try again ?");
					}
					
				@Override
				public void onNothingSelected(AdapterView<?> arg0) {}
			});
			// add adapter into the adapters list
			// in order to refresh all list when loading complete
			childListAdapters.add(adapter);*/
			
			return convertView;
		}

		@Override
		public Object getGroup(int groupPosition) {
			Object group = childElements.size() <= groupPosition ?
					null : childElements.get(groupPosition);
			return group;
		}

		@Override
		public int getGroupCount() {
			return runGroups.length;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflater = 
						(LayoutInflater) myContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.main_runs_group, null);
			}

			TextView groupName = (TextView) convertView.findViewById(R.id.runGroupHeaderTextView);
			groupName.setText(runGroups[groupPosition]);
		    
			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		// adaptor for the child(listView) of expendableListView
		/*private class ChildListAdapter extends BaseAdapter{
			private HashMap<String, WorkflowBE> listData;
			private String[] mKeys;
			
			public ChildListAdapter(HashMap<String, WorkflowBE> data){
				listData = data;
		        mKeys = listData.keySet().toArray(new String[data.size()]);
			}
			
			public Object getKey (int index){
				return mKeys[index];
			}
		
			@Override
			public int getCount() {
				return listData.size();
			}
		
			@Override
			public Object getItem(int index) {
				return listData.get(mKeys[index]);
			}
		
			@Override
			public long getItemId(int position) {
				return position;
			}
		
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if(convertView == null){
					LayoutInflater mInflater = 
							(LayoutInflater) parentActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					convertView = mInflater.inflate(R.layout.main_runs_child_singlerow, null);
				}
				
				//WorkflowBE wfBE = (WorkflowBE) getItem(childID);
				WorkflowBE wfBE = (WorkflowBE) getItem(position);
				CheckBox runCheckbox = (CheckBox)convertView.findViewById(R.id.runList_run_checkbox);
				TextView wfTitleVersion = (TextView) convertView.findViewById(R.id.runsTitleVersion);
				TextView wfuploaderName = (TextView) convertView.findViewById(R.id.runsUploader);
				
				wfTitleVersion.setText(wfBE.getTitle()+" (v"+wfBE.getVersion()+")");
				wfuploaderName.setText(wfBE.getUploaderName());
				
				runCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
		
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if(isChecked){
							String runid = (String) getKey(childID);
							selectedRunIds.add(runid);
						}
						else{
							String runid = (String) getKey(childID);
							selectedRunIds.remove(runid);
						}
						
						// start the action mode when there are selected runs
						if(mActionMode == null){
							mActionMode = parentActivity.startActionMode(mActionModeCallback);
						}else if(selectedRunIds.size() < 1){
							mActionMode.finish();
						}
					}
				});
				
				convertView.setOnLongClickListener(new OnLongClickListener(){
					@Override
					public boolean onLongClick(View view) {
				        mActionMode = parentActivity.startActionMode(mActionModeCallback);
						return true;
					}
				});
				
				return convertView;
			}
			
			private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		
			    // Called when the action mode is created; startActionMode() was called
			    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				      MenuInflater inflater = mode.getMenuInflater();
				      inflater.inflate(R.menu.runlist_action_mode_menu, menu);
				      return true;
			    }
		
			    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			    	return false; // Return false if nothing is done
			    }
		
			    // Called when the user selects a contextual menu item
			    public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
			      switch (item.getItemId()) {
			      case R.id.runList_run_stop:
			    	  MessageHelper.showOptionsDialog(
			    			  refreshableList.getContext(), 
			    			  "Stop selected runs ?",
			    			  null,
			    			  new CallbackTask(){
								@Override
								public Object onTaskInProgress(Object... param) {
									runManager.StopRun("Stopping runs...", selectedRunIds);
									// automatically close action mode when action performed
									mode.finish();
									return null;
								}
		
								@Override
								public Object onTaskComplete(Object... result){
									prepareListData();
									return null; 
								}
			    		  
			    			  }, null);
			    	  // mode.finish(); 
			        return true;
			      case R.id.runList_run_delete:
			    	  MessageHelper.showOptionsDialog(
			    			  refreshableList.getContext(), 
			    			  "Delete selected runs ?",
			    			  null,
			    			  new CallbackTask(){
								@Override
								public Object onTaskInProgress(Object... param) {
									runManager.DeleteRun("Deleting runs...", selectedRunIds);
									// automatically close action mode when action performed
									mode.finish();
									return null;
								}
		
								@Override
								public Object onTaskComplete(Object... result){
									prepareListData();
									return null; 
								}
			    		  
			    			  }, null);
			    	  // mode.finish();
			    	  return true;
			      case R.id.runList_run_start:
			    	  	// get launched workflows run ID
						// in order to retrieve its state
						// and then monitor it
						String runID = (String) selectedRunIds.get(0);
						
						if (runID != null) {
							WorkflowRunManager manager = new WorkflowRunManager(parentActivity);
							manager.checkRunStateWithID(runID, 
									new RunsListAdapter.RunStateChecker(workflowEntity, runID));
						} 
			    	  return true;
			      default:
			        return false;
			      }
			    }
		
			    // Called when the user exits the action mode
			    public void onDestroyActionMode(ActionMode mode) {
			    	mActionMode = null;
			    }
			  };
		}*/// end of childListAdapter		
	}
	
	private class RunStateChecker implements CallbackTask {

		private WorkflowBE workflowEntity;
		private String runId;

		public RunStateChecker(WorkflowBE entity, String id) {
			workflowEntity = entity;
			runId = id;
		}

		public Object onTaskInProgress(Object... param) {
			return null;
		}

		public Object onTaskComplete(Object... result) {
			String runState = (String) result[0];
			// if running or finished go to monitor to view progress or output
			if (runState == "Running" || runState == "Finished") {
				MessageHelper.showOptionsDialog(parentActivity, 
					"The run is "+ runState + "\nDo you want to view it ?", 
					null, 
					new CallbackTask(){
						@Override
						public Object onTaskInProgress(Object... param) {
							// go to monitor
							Intent goToMonitor = new Intent(parentActivity, RunMonitorScreen.class);
							Bundle extras = new Bundle();
							extras.putSerializable("workflowEntity", workflowEntity);
							extras.putString("command", "MonitoringOnly");
							goToMonitor.putExtras(extras);
							parentActivity.startActivity(goToMonitor);
							return null;
						}

						@Override
						public Object onTaskComplete(Object... result) {return null;}
					}, null);
				
			} else if (runState == "Initialised") {
				MessageHelper.showOptionsDialog(parentActivity, 
					"Supply inputs for selected runs ?", 
					"Initialised Runs", 
					new CallbackTask(){
						@Override
						public Object onTaskInProgress(Object... param) {
							// go to inputs screen
							WorkflowRunManager manager = new WorkflowRunManager(parentActivity);
							manager.getRunInputs(runId, new CallbackTask(){
								@Override
								public Object onTaskInProgress(Object... param) { return null; }
			
								@Override
								public Object onTaskComplete(Object... result) {
									Map<String, InputPort> inputPorts = 
											(Map<String, InputPort>) result[0];
									WorkflowLaunchHelper launchHelper = 
											new WorkflowLaunchHelper(parentActivity, Activity_Starter_Code);
									Bundle extras = 
											launchHelper.prepareInputs(inputPorts, workflowEntity);
									Intent goToInputList = new Intent(parentActivity, InputsList.class);
									goToInputList.putExtras(extras);
									parentActivity.startActivity(goToInputList);
									return null;
								}
							});
							return null;
						}
						@Override
						public Object onTaskComplete(Object... result) {return null;}
				 }, null);
			} else if (runState == "Deleted" || runState == null) {
				MessageHelper.showMessageDialog(parentActivity, 
						"Run state changed", 
						"The run has been deleted, please refresh the list.",  null);
			}
			return null;
		}
	}// end of RunStateChecker
	
	/*private void showLaunchDialog(String message) {
		MessageHelper.showOptionsDialog(parentActivity, message, "Attention",
				new CallbackTask() {
					@Override
					public Object onTaskInProgress(Object... param) {
						// check Internet
						SystemStatesChecker sysChecker = new SystemStatesChecker(parentActivity);
						if (!sysChecker.isNetworkConnected()) {
							return null;
						}
						reRun();
						return null;
					}

					@Override
					public Object onTaskComplete(Object... result) { return null; }
				}, null);
	}
	
	private void reRun() {
		WorkflowBE workflowEntity = new WorkflowBE();
		workflowEntity.setTitle(selectedTitle);
		workflowEntity.setVersion(selectedWfVersion);
		workflowEntity.setUploaderName(selectedWfUploaderName);

		WorkflowLaunchHelper launchHelper = 
				new WorkflowLaunchHelper(parentActivity, Activity_Starter_Code);
		launchHelper.launch(workflowEntity, 0);
	}*/
}