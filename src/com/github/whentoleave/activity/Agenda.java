package com.github.whentoleave.activity;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.github.whentoleave.model.EventEntry;
import com.github.whentoleave.service.AppService;
import com.github.whentoleave.service.AppServiceConnection;

import com.github.whentoleave.R;

/**
 * Activity which shows a list of all events in the next two weeks. Works
 * optimally as a tab for TabbedInterface.
 * 
 * @see TabbedInterface
 */
public class Agenda extends Activity implements Refreshable
{
	/**
	 * Logging tag
	 */
	private static final String TAG = "Agenda";
	/**
	 * Formatted list of events used to create ListView adapter
	 */
	private final ArrayList<HashMap<String, String>> eventHashMapList = new ArrayList<HashMap<String, String>>();
	/**
	 * List of events
	 */
	private final ArrayList<EventEntry> eventList = new ArrayList<EventEntry>();
	/**
	 * Connection to the persistent, authorized service
	 */
	private final AppServiceConnection service = new AppServiceConnection(this);

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.agenda);
		final ListView agendaList = (ListView) findViewById(R.id.agendaList);
		agendaList.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(final AdapterView<?> parent,
					final View view, final int position, final long id)
			{
				final Intent detailsIntent = new Intent(Agenda.this,
						EventDetails.class);
				detailsIntent.putExtra("eventUrl", eventList.get(position)
						.getSelfLink());
				startActivity(detailsIntent);
			}
		});
		// For complex hashmap layout
		final SimpleAdapter adapterForList = new SimpleAdapter(Agenda.this,
				eventHashMapList, R.layout.agenda_item, new String[] { "title",
						"when", "where", "imageUri" }, new int[] {
						R.id.agendaItemTitle, R.id.agendaItemWhen,
						R.id.agendaItemWhere });
		final ListView mainList = (ListView) findViewById(R.id.agendaList);
		mainList.setAdapter(adapterForList);
		// Need to use getApplicationContext as this activity is used as a Tab
		getApplicationContext().bindService(new Intent(this, AppService.class),
				service, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		getApplicationContext().unbindService(service);
	}

	/**
	 * Refresh the data for the MainScreen activity
	 */
	@Override
	public void refreshData()
	{
		// Set the last refreshed to a while refreshing text
		final TextView lastRefreshed = (TextView) findViewById(R.id.lastRefreshed);
		lastRefreshed.setText(getText(R.string.whileRefreshing));
		// Load the data
		eventHashMapList.clear();
		try
		{
			final Calendar twoWeeksFromNow = Calendar.getInstance();
			twoWeeksFromNow.add(Calendar.DATE, 14);
			final Set<EventEntry> events = service
					.getEventsStartingNow(twoWeeksFromNow.getTime());
			for (final EventEntry event : events)
			{
				final HashMap<String, String> calendarEventHashMap = new HashMap<String, String>();
				calendarEventHashMap.put("title", event.title);
				if (event.when != null && event.when.startTime != null)
				{
					final CharSequence time = android.text.format.DateFormat
							.format("hh:mma 'on' EEEE, MMM dd",
									event.when.startTime.value);
					calendarEventHashMap.put("when", time.toString());
				}
				else
					calendarEventHashMap.put("when", "");
				if (event.where != null && event.where.valueString != null
						&& !event.where.valueString.equals(""))
					calendarEventHashMap.put("where", event.where.valueString);
				else
					calendarEventHashMap.put("where", "No Location");
				eventHashMapList.add(calendarEventHashMap);
			}
			eventList.clear();
			eventList.addAll(events);
		} catch (final IOException e)
		{
			Log.e(TAG, "Error while refreshing data", e);
			final HashMap<String, String> calendarEventHashMap = new HashMap<String, String>();
			calendarEventHashMap.put("title", e.getMessage());
			calendarEventHashMap.put("when", "");
			calendarEventHashMap.put("where", "");
			eventHashMapList.add(calendarEventHashMap);
		}
		// Update the last refreshed text
		final CharSequence lastRefreshedBase = getText(R.string.lastRefreshedBase);
		final Date currentDate = new Date();
		final DateFormat dateFormat = DateFormat
				.getDateInstance(DateFormat.SHORT);
		final DateFormat timeFormat = DateFormat
				.getTimeInstance(DateFormat.SHORT);
		lastRefreshed.setText(lastRefreshedBase + " "
				+ dateFormat.format(currentDate) + " "
				+ timeFormat.format(currentDate));
	}
}