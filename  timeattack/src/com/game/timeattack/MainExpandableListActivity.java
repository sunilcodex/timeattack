package com.game.timeattack;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Vector;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import com.game.timeattack.provider.TimeAttack.Attack;
import com.game.timeattack.provider.TimeAttack.Fleet;

public class MainExpandableListActivity extends ExpandableListActivity {
	private static String TAG = "MainExpandedListActivity";
	private static final int EDITION_GROUP = 1;
	private static final int EDITION_CHILD = 2;
	private static final int EDITION_ADD_GROUP = 3;
	private static final int EDITION_ADD_CHILD = 4;
	ArrayList<Integer> expandedGroups = null;
	private static boolean KILL_ALL_THREADS = false;
	public static final int ADD_UPDATE_DATA = 1;
	public static final int START_UPDATE = 2;
	public static final int STOP_UPDATE = 3;
	public static final int RESTART_UPDATE = 4;
	public static final int CLEAR_UPDATE_DATA = 5;
	UpdateThread thread;
	MyHandler mHandler = new MyHandler();

	private class MyHandler extends Handler {
		Vector<UpdateData> updateDatas = new Vector<UpdateData>();

		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "Message received:" + msg.what);
			switch (msg.what) {
			case ADD_UPDATE_DATA:
				boolean alreadyIn = updateDatas.contains((UpdateData) msg.obj);
				Log.d(TAG, "Object already inside : " + alreadyIn);
				if (!alreadyIn) {
					updateDatas.add((UpdateData) msg.obj);
				} else {
					updateDatas.remove((UpdateData) msg.obj);
					updateDatas.add((UpdateData) msg.obj);
				}
				break;
			case START_UPDATE:
				KILL_ALL_THREADS = false;
				removeCallbacks(thread);
				thread = new UpdateThread(updateDatas);
				postDelayed(thread, 100);
				break;
			case STOP_UPDATE:
				KILL_ALL_THREADS = true;
				break;
			case RESTART_UPDATE:
				sendEmptyMessage(STOP_UPDATE);
				sendEmptyMessage(START_UPDATE);
				break;
			case CLEAR_UPDATE_DATA:
				updateDatas.clear();
				break;
			default:
				throw new IllegalArgumentException("Message not handled");
			}
		}
	}

	private class UpdateThread implements Runnable {
		Vector<UpdateData> updateDatas;

		public UpdateThread(Vector<UpdateData> datas) {
			updateDatas = datas;
		}

		@Override
		public void run() {
			for (Iterator<UpdateData> iterator = updateDatas.iterator(); iterator
					.hasNext();) {
				UpdateData tmp = (UpdateData) iterator.next();
				calcRemainingTime(tmp);
				mHandler.removeCallbacks(this);
			}
			if (!KILL_ALL_THREADS) {
				mHandler.postDelayed(this, 0);
			}
		}
	}

	public class MyCursorTreeAdapter extends CursorTreeAdapter implements
			OnTouchListener {
		Context mContext;
		Cursor mCursor;

		public MyCursorTreeAdapter(Cursor cursor, Context context) {
			super(cursor, context);
			mContext = context;
			mCursor = cursor;
		}

		@Override
		protected void bindChildView(View v, Context context, Cursor cursor,
				boolean isLastChild) {
			ChildViewHolder holder = (ChildViewHolder) v.getTag();
			Calendar cal;
			if (holder == null) {
				holder = new ChildViewHolder();
				holder.name = (TextView) v.findViewById(R.id.name);
				holder.delta = (TextView) v.findViewById(R.id.delta);
				holder.timeToLaunch = (TextView) v
						.findViewById(R.id.time_to_launch);
				holder.arrivalTime = (TextView) v
						.findViewById(R.id.arrival_time);
				holder.remainingTime = (TextView) v
						.findViewById(R.id.remaining_time);
				holder.alarm = (ImageView) v.findViewById(R.id.alarm_icon);
			}
			int id = Utils.getIntFromCol(cursor, Fleet._ID);
			int groupId = Utils.getIntFromCol(cursor, Fleet.GROUP_ID);
			String name = Utils.getStringFromCol(cursor, Fleet.NAME);
			String delta = Utils.getStringFromCol(cursor, Fleet.DELTA);
			long time = Utils.getLongFromCol(cursor, Fleet.LAUNCH_TIME);
			Boolean alarmActivated = new Boolean(Utils.getStringFromCol(cursor,
					Fleet.ALARM_ACTIVATED));
			if (alarmActivated) {
				holder.alarm.setVisibility(View.VISIBLE);
			} else {
				holder.alarm.setVisibility(View.INVISIBLE);
			}
			cal = Calendar.getInstance();
			cal.setTimeInMillis(time);
			String launchTime = Utils.getFromCalendar(cal, Utils
					.formatCalendar(cal, Utils.LOCALIZED_MONTH_ABR)
					+ " "
					+ Utils.formatCalendar(cal, Utils.DAY_2_DIGITS)
					+ " "
					+ Utils.formatCalendar(cal, Utils.FULL_12H_TIME));

			holder.id = id;
			holder.groupId = groupId;
			holder.name.setText(name);
			holder.delta.setText(delta);
			holder.timeToLaunch.setText(launchTime);

			int fleetDelta = Utils.sToI(delta);

			String[] projection = { Attack.ATTACK_TIME };
			Cursor attackCursor = getContentResolver().query(
					Attack.CONTENT_URI, projection, Attack._ID + "=" + groupId,
					null, null);
			attackCursor.moveToFirst();
			long attackTime = Utils.getLongFromCol(attackCursor,
					Attack.ATTACK_TIME);
			/**
			 * Arrival time
			 */
			cal = Calendar.getInstance();
			cal.setTimeInMillis(attackTime);
			cal.add(Calendar.SECOND, -fleetDelta);
			String arrivalTime = Utils.getFromCalendar(cal, Utils
					.formatCalendar(cal, Utils.LOCALIZED_MONTH_ABR)
					+ " "
					+ Utils.formatCalendar(cal, Utils.DAY_2_DIGITS)
					+ " "
					+ Utils.formatCalendar(cal, Utils.FULL_12H_TIME));
			holder.arrivalTime.setText(arrivalTime);

			/**
			 * launchtime
			 */
			long launchTime2 = Utils.getLongFromCol(cursor, Fleet.LAUNCH_TIME);

			/**
			 * update thread
			 */
			TextView remainingTimeView = holder.remainingTime;
			UpdateData updateData = new UpdateData(remainingTimeView,
					launchTime2);
			Message msg = new Message();
			msg.what = ADD_UPDATE_DATA;
			msg.obj = updateData;
			mHandler.sendMessage(msg);

			v.setTag(holder);
		}

		@Override
		protected void bindGroupView(View v, Context context, Cursor cursor,
				boolean isExpanded) {

			ParentViewHolder holder = (ParentViewHolder) v.getTag();
			if (holder == null) {
				holder = new ParentViewHolder();
				holder.name = (TextView) v.findViewById(R.id.a_name);
				holder.time = (TextView) v.findViewById(R.id.a_time);
				holder.date = (TextView) v.findViewById(R.id.a_date);
				holder.edit = (ImageView) v.findViewById(R.id.edit_group);
				holder.del = (ImageView) v.findViewById(R.id.del_group);
				holder.add = (ImageView) v.findViewById(R.id.add);
			}

			int groupId = Utils.getIntFromCol(cursor, Attack._ID);
			String name = Utils.getStringFromCol(cursor, Attack.NAME);
			long attackTime = Utils.getLongFromCol(cursor, Attack.ATTACK_TIME);

			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(attackTime);
			holder.time.setText(""
					+ Utils.getFromCalendar(cal, Utils.FULL_12H_TIME));
			holder.date.setText(""
					+ Utils.getFromCalendar(cal, Utils.FULL_DATE));

			holder.groupId = groupId;
			holder.name.setText(name);
			holder.edit.setOnTouchListener(this);
			holder.del.setOnTouchListener(this);
			holder.add.setOnTouchListener(this);
			holder.add.setTag(holder);
			holder.edit.setTag(holder);
			holder.del.setTag(holder);

			v.setTag(holder);
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			int viewId = v.getId();
			Holder h = (Holder) v.getTag();
			Intent intent;
			if (action == MotionEvent.ACTION_UP) {
				switch (viewId) {
				case R.id.del:

					int delete3 = getContentResolver().delete(
							Fleet.CONTENT_URI, Fleet._ID + "=" + h.getId(),
							null);
					Log.d(TAG, "deleted rows=" + delete3);
					break;
				case R.id.add:
					intent = new Intent(getApplicationContext(), Edit1.class);
					intent.putExtra("groupId", h.getGroupId());
					intent.putExtra("code", EDITION_ADD_CHILD);
					intent.putExtra("childId", -2);
					intent.putExtra("HIDE_DATE", true);
					startActivity(intent);
					break;
				case R.id.edit:
					intent = new Intent(getApplicationContext(), Edit1.class);
					intent.putExtra("groupId", h.getGroupId());
					intent.putExtra("childId", h.getId());
					intent.putExtra("code", EDITION_CHILD);
					Log.d(TAG, "edited Group=" + h.getGroupId() + " child="
							+ h.getId());
					startActivity(intent);
					break;
				case R.id.edit_group:
					intent = new Intent(getApplicationContext(), Edit1.class);
					intent.putExtra("groupId", h.getGroupId());
					intent.putExtra("childId", -1);
					intent.putExtra("code", EDITION_GROUP);
					Log.d(TAG, "edited Group=" + h.getGroupId() + " child="
							+ -1);
					startActivity(intent);
					break;
				case R.id.del_group:
					int delete = getContentResolver().delete(Fleet.CONTENT_URI,
							Fleet.GROUP_ID + "=" + h.getGroupId(), null);
					int delete2 = getContentResolver().delete(
							Attack.CONTENT_URI,
							Attack._ID + "=" + h.getGroupId(), null);
					Log.d(TAG, "deleted rows=" + (delete + delete2));
					break;
				default:
					throw new IllegalArgumentException("View not handled");
					// intent = new Intent(getApplicationContext(),
					// FleetDetails.class);
					// intent.putExtra("groupId", h.getGroupId());
					// startActivity(intent);
				}
			}
			Log.d(TAG, "action=" + action + " View id=" + viewId);
			return true;
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			Uri.Builder builder = Fleet.CONTENT_URI.buildUpon();
			return managedQuery(builder.build(), new String[] { Fleet._ID,
					Fleet.GROUP_ID, Fleet.NAME, Fleet.H, Fleet.M, Fleet.S,
					Fleet.DELTA, Fleet.LAUNCH_TIME, Fleet.ALARM_ACTIVATED },
					Fleet.GROUP_ID
							+ "=="
							+ groupCursor.getInt(groupCursor
									.getColumnIndexOrThrow(Fleet._ID)), null,
					Fleet.DEFAULT_SORT_ORDER);
		}

		@Override
		protected View newChildView(Context context, Cursor cursor,
				boolean isLastChild, ViewGroup parent) {
			final LayoutInflater inflater = LayoutInflater.from(context);
			View v = (ViewGroup) inflater.inflate(R.layout.child_layout,
					parent, false);
			return v;
		}

		@Override
		protected View newGroupView(Context context, Cursor cursor,
				boolean isExpanded, ViewGroup parent) {

			final LayoutInflater inflater = LayoutInflater.from(context);
			View v = inflater.inflate(R.layout.head, parent, false);
			return v;
		}
	}

	public class UpdateData {
		public TextView v;
		long launchTime;

		public UpdateData(TextView v, long launchTime) {
			super();
			this.v = v;
			this.launchTime = launchTime;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (o == this) {
				return true;
			}
			if (o.getClass() != getClass()) {
				return false;
			}
			UpdateData u = (UpdateData) o;
			if (v != (u.v)) {
				return false;
			}
			if (v == null) {
				if (u.v != null) {
					return false;
				}
			} else if (!v.equals(u.v)) {
				return false;
			}
			return true;
		}

	}

	public interface Holder {
		int getId();

		int getGroupId();
	}

	static class ParentViewHolder implements Holder {
		int groupId;
		TextView name, time, date;
		ImageView edit, del, add;

		@Override
		public int getId() {
			return -1;
		}

		@Override
		public int getGroupId() {
			return groupId;
		}
	}

	static class ChildViewHolder implements Holder {
		int id, groupId;
		TextView name, delta, timeToLaunch, arrivalTime, remainingTime;
		ImageView alarm;

		@Override
		public int getGroupId() {
			return groupId;
		}

		@Override
		public int getId() {
			return id;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.expandablelist);

		Cursor cursor = managedQuery(Attack.CONTENT_URI, new String[] {
				Attack._ID, Attack.NAME, Attack.ATTACK_TIME }, null, null, null);

		MyCursorTreeAdapter adapter = new MyCursorTreeAdapter(cursor, this);
		setListAdapter(adapter);
		registerForContextMenu(getExpandableListView());
		getExpandableListView().setOnChildClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = new Intent(this, Edit1.class);
		intent.putExtra("code", EDITION_ADD_GROUP);
		intent.putExtra("groupId", -1);
		intent.putExtra("childId", -1);
		intent.putExtra("HIDE_DELTA", true);
		startActivityForResult(intent, EDITION_ADD_GROUP);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView
				.getPackedPositionType(info.packedPosition);
		MenuInflater inflater;
		switch (type) {
		case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
			inflater = new MenuInflater(this);
			inflater.inflate(R.menu.contextmenuchild, menu);
			break;
		case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
			inflater = new MenuInflater(this);
			inflater.inflate(R.menu.contextmenu, menu);
			break;
		default:
			throw new IllegalArgumentException(
					"Wrong Type in create context menu");
		}

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item
				.getMenuInfo();
		int type = ExpandableListView
				.getPackedPositionType(info.packedPosition);
		int groupPosition = ExpandableListView
				.getPackedPositionGroup(info.packedPosition);
		int childPosition = ExpandableListView
				.getPackedPositionChild(info.packedPosition);
		long groupId = getExpandableListAdapter().getGroupId(groupPosition);
		long childId = getExpandableListAdapter().getChildId(groupPosition,
				childPosition);
		Intent intent;
		Log.d("tag", "contextmenu" + " type=" + type + " groupId=" + groupId
				+ " childId" + childId);
		switch (item.getItemId()) {
		case R.id.add:
			intent = new Intent(this, Edit1.class);
			intent.putExtra("code", EDITION_ADD_CHILD);
			intent.putExtra("groupId", (int) groupId);
			intent.putExtra("childId", -2);
			intent.putExtra("HIDE_DATE", true);
			startActivity(intent);
			break;
		case R.id.editchild:
			Log.d(TAG, "editchild");
			intent = new Intent(this, FleetDetails.class);
			intent.putExtra("groupId", (int) groupId);
			intent.putExtra("childId", (int) childId);
			Log.d(TAG, "edited Group=" + groupId + " child=" + childId);
			startActivity(intent);
			break;
		case R.id.editgroup:
			Log.d(TAG, "editgroup");
			intent = new Intent(this, Edit1.class);
			intent.putExtra("groupId", (int) groupId);
			intent.putExtra("childId", -1);
			intent.putExtra("code", EDITION_GROUP);
			Log.d(TAG, "edited Group=" + groupId + " child=" + childId);
			startActivity(intent);
			break;
		case R.id.del:
			Log.d(TAG, "del");
			switch (type) {
			case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
				int delete = getContentResolver().delete(Fleet.CONTENT_URI,
						Fleet.GROUP_ID + "=" + info.id, null);
				int delete2 = getContentResolver().delete(Attack.CONTENT_URI,
						Attack._ID + "=" + info.id, null);
				Log.d(TAG, "deleted rows=" + (delete + delete2));
				break;

			case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
				int delete3 = getContentResolver().delete(Fleet.CONTENT_URI,
						Fleet._ID + "=" + info.id, null);
				Log.d(TAG, "deleted rows=" + delete3);
				break;
			default:
				throw new IllegalArgumentException("deleted item null");
			}

			break;
		default:
			throw new IllegalArgumentException("Unknown context menu element");
		}
		return false;
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		long groupId = getExpandableListAdapter().getGroupId(groupPosition);
		long childId = getExpandableListAdapter().getChildId(groupPosition,
				childPosition);

		Intent intent = new Intent(this, FleetDetails.class);
		intent.putExtra("groupId", (int) groupId);
		intent.putExtra("childId", (int) childId);
		startActivity(intent);
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		expandedGroups = new ArrayList<Integer>();
		int groupCount = getExpandableListAdapter().getGroupCount();
		for (int i = 0; i < groupCount; i++) {
			if (getExpandableListView().isGroupExpanded(i)) {
				expandedGroups.add(i);
			}
		}
		mHandler.sendEmptyMessage(STOP_UPDATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (expandedGroups != null) {
			for (int i = 0; i < expandedGroups.size(); i++) {
				Integer tmp = expandedGroups.get(i);
				getExpandableListView().expandGroup(tmp);
			}
		}
		mHandler.sendEmptyMessage(RESTART_UPDATE);
	}

	/**
	 * @param day
	 * @param h
	 * @param m
	 * @param s
	 */
	private void calcRemainingTime(UpdateData updateData) {
		updateData.v.setTextColor(getResources().getColor(R.color.black));
		Calendar currentCal = Calendar.getInstance();

		Calendar launchCal = Calendar.getInstance();
		launchCal.setTimeInMillis(updateData.launchTime);
		if (currentCal.after(launchCal)) {
			updateData.v.setTextColor(getResources().getColor(R.color.white));
			updateData.v.setText(getString(R.string.already_launched) + " ");
			return;
		}

		long currentTime = currentCal.getTimeInMillis();
		long launchTime = launchCal.getTimeInMillis();
		long difference = launchTime - currentTime;
		if (difference < 1000 * 60 * 10 && currentCal.before(launchCal)) {
			updateData.v.setTextColor(getResources().getColor(R.color.red));
		}

		Calendar newCal = Calendar.getInstance();
		newCal.clear();
		newCal.set(Calendar.DAY_OF_MONTH, 1);
		newCal.set(Calendar.HOUR, 0);
		newCal.set(Calendar.MINUTE, 0);
		newCal.set(Calendar.SECOND, 0);
		newCal.add(Calendar.MILLISECOND, (int) difference);

		int newDay = Utils.sToI(Utils.getFromCalendar(newCal,
				Utils.DAY_2_DIGITS)) - 1;
		String time = Utils.getFromCalendar(newCal, Utils.HOUR_OF_DAY_24H)
				+ ":" + Utils.getFromCalendar(newCal, Utils.MINUTES) + ":"
				+ Utils.getFromCalendar(newCal, Utils.SECONDS);
		String result = newDay + "d " + time;
		updateData.v.setText(result + " ");
	}

}
