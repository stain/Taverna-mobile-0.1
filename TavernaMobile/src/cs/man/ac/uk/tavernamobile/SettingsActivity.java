package cs.man.ac.uk.tavernamobile;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// UI components
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle("Settings");
		actionBar.setIcon(this.getResources().getDrawable(R.drawable.taverna_wheel_logo_medium));

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingFragments()).commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public static class SettingFragments extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.settings_screen);
		}
	}
	
	@Override
	public void finish(){
		super.finish();
		this.overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
	}
}
