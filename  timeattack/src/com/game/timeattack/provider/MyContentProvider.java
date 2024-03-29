package com.game.timeattack.provider;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.game.timeattack.AlarmReceiver;
import com.game.timeattack.Utils;
import com.game.timeattack.provider.TimeAttack.Attack;
import com.game.timeattack.provider.TimeAttack.Fleet;

public class MyContentProvider extends ContentProvider {
	private AlarmManager mAlarmManager;

	public static final String PROVIDER_NAME = "com.game.timeattack.provider.MyContentProvider";

	public static final Uri CONTENT_URI = Uri.parse("content://"
			+ PROVIDER_NAME);

	private static final String pre = "DBOpenHelper: ";
	private static final String DATABASE_NAME = "timeattackdb";
	private static final int DATABASE_VERSION = 13;

	private static final UriMatcher uriMatcher;

	private static final Map<String, String> sAttackProjectionMap;
	private static final Map<String, String> sFleetProjectionMap;

	private static final int ATTACKS = 1;
	private static final int ATTACK_ID = 2;
	private static final int FLEETS = 3;
	private static final int FLEET_ID = 4;

	private static final String TAG = "MyContentProvider";

	private static final int ALARM_ID = 1;

	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("kk:mm:ss");

	public static class MyDbOpenHelper extends SQLiteOpenHelper {
		public MyDbOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(TAG, pre + "Creating a new DB");

			db.execSQL(Attack.TABLE_CREATE);
			db.execSQL(Fleet.TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, pre + "Upgrading from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + Attack.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + Fleet.TABLE_NAME);
			onCreate(db);
		}
	}

	private MyDbOpenHelper dbHelper;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (uriMatcher.match(uri)) {
		case ATTACKS:
			count = db.delete(Attack.TABLE_NAME, selection, selectionArgs);
			break;
		case ATTACK_ID:
			String attack_id = uri.getPathSegments().get(1);
			count = db.delete(Attack.TABLE_NAME, Attack._ID
					+ "="
					+ attack_id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		case FLEETS:
			count = db.delete(Fleet.TABLE_NAME, selection, selectionArgs);
			break;

		case FLEET_ID:
			String fleet_id = uri.getPathSegments().get(1);
			count = db.delete(Fleet.TABLE_NAME, Fleet._ID
					+ "="
					+ fleet_id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ATTACKS:
			return Attack.CONTENT_TYPE;
		case ATTACK_ID:
			return Attack.CONTENT_ITEM_TYPE;
		case FLEETS:
			return Fleet.CONTENT_TYPE;
		case FLEET_ID:
			return Fleet.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		// Validate the requested uri
		switch (uriMatcher.match(uri)) {
		case ATTACKS:
		case FLEETS:
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		switch (uriMatcher.match(uri)) {
		case ATTACKS:
			if (values.containsKey(Attack.ATTACK_TIME) == false) {
				values.put(Attack.ATTACK_TIME, "0");
			}
			if (values.containsKey(Attack.NAME) == false) {
				values.put(Attack.NAME, "Attack name");
			}
			break;
		case FLEETS:
			if (values.containsKey(Fleet.GROUP_ID) == false) {
				values.put(Fleet.GROUP_ID, 0);
			}
			if (values.containsKey(Fleet.NAME) == false) {
				values.put(Fleet.NAME, "Fleet name");
			}
			if (values.containsKey(Fleet.H) == false) {
				values.put(Fleet.H, "02");
			}
			if (values.containsKey(Fleet.M) == false) {
				values.put(Fleet.M, "02");
			}
			if (values.containsKey(Fleet.S) == false) {
				values.put(Fleet.S, "02");
			}
			if (values.containsKey(Fleet.DELTA) == false) {
				values.put(Fleet.DELTA, "00");
			}
			if (values.containsKey(Fleet.LAUNCH_TIME) == false) {
				values.put(Fleet.LAUNCH_TIME, "0");
			}
			if (values.containsKey(Fleet.ALARM_DELTA) == false) {
				values.put(Fleet.ALARM_DELTA, "300000");
			}
			if (values.containsKey(Fleet.ALARM_ACTIVATED) == false) {
				values.put(Fleet.ALARM_ACTIVATED, "false");
			}
			break;
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = 0;
		switch (uriMatcher.match(uri)) {
		case ATTACKS:
			rowId = db.insert(Attack.TABLE_NAME, Attack.NAME, values);
			break;
		case FLEETS:
			rowId = db.insert(Fleet.TABLE_NAME, Fleet.NAME, values);
			break;
		}

		if (rowId > 0) {
			switch (uriMatcher.match(uri)) {
			case ATTACKS:
				Uri attackUri = ContentUris.withAppendedId(Attack.CONTENT_URI,
						rowId);
				getContext().getContentResolver().notifyChange(attackUri, null);
				return attackUri;
			case FLEETS:
				Uri fleetUri = ContentUris.withAppendedId(Fleet.CONTENT_URI,
						rowId);
				getContext().getContentResolver().notifyChange(fleetUri, null);
				return fleetUri;
			}
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public boolean onCreate() {
		dbHelper = new MyDbOpenHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String lastPathSegment = uri.getLastPathSegment();

		// qb.setTables(lastPathSegment);
		switch (uriMatcher.match(uri)) {
		case ATTACKS:
			qb.setTables(lastPathSegment);
			qb.setProjectionMap(sAttackProjectionMap);
			break;
		case ATTACK_ID:
			qb.setTables(Attack.TABLE_NAME);
			qb.setProjectionMap(sAttackProjectionMap);
			qb.appendWhere(Attack._ID + "=" + uri.getPathSegments().get(1));
			break;
		case FLEETS:
			qb.setTables(lastPathSegment);
			qb.setProjectionMap(sFleetProjectionMap);
			break;
		case FLEET_ID:
			qb.setTables(Fleet.TABLE_NAME);
			qb.setProjectionMap(sFleetProjectionMap);
			qb.appendWhere(Fleet._ID + "=" + uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase database = dbHelper.getReadableDatabase();
		Cursor cursor = qb.query(database, projection, selection,
				selectionArgs, null, null, sortOrder);

		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (uriMatcher.match(uri)) {
		case ATTACKS:
			count = db.update(Attack.TABLE_NAME, values, selection,
					selectionArgs);
			String[] columns = { Attack._ID };
			Cursor cursor = db.query(Attack.TABLE_NAME, columns, selection,
					null, null, null, null);
			cursor.moveToFirst();
			int agroupId = cursor.getInt(cursor
					.getColumnIndexOrThrow(Attack._ID));
			calcGroup(db, agroupId);
			break;
		case ATTACK_ID:
			String attack_id = uri.getPathSegments().get(1);
			count = db.update(Attack.TABLE_NAME, values, Attack._ID
					+ "="
					+ attack_id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		case FLEETS:
			count = db.update(Fleet.TABLE_NAME, values, selection,
					selectionArgs);
			String[] columns1 = { Fleet._ID, Fleet.GROUP_ID };
			Cursor cursor1 = db.query(Fleet.TABLE_NAME, columns1, selection,
					null, null, null, null);
			cursor1.moveToFirst();
			int achildId = cursor1.getInt(cursor1
					.getColumnIndexOrThrow(Fleet._ID));
			int agroupId1 = cursor1.getInt(cursor1
					.getColumnIndexOrThrow(Fleet.GROUP_ID));
			calcChild(db, agroupId1, achildId);
			break;
		case FLEET_ID:
			String fleet_id = uri.getPathSegments().get(1);
			count = db.update(Fleet.TABLE_NAME, values, Fleet._ID
					+ "="
					+ fleet_id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		uriMatcher.addURI(PROVIDER_NAME, "attack", ATTACKS);
		uriMatcher.addURI(PROVIDER_NAME, "attack/#", ATTACK_ID);

		uriMatcher.addURI(PROVIDER_NAME, "fleet", FLEETS);
		uriMatcher.addURI(PROVIDER_NAME, "fleet/#", FLEET_ID);

		sAttackProjectionMap = new HashMap<String, String>();
		sAttackProjectionMap.put(Attack._ID, Attack._ID);
		sAttackProjectionMap.put(Attack.NAME, Attack.NAME);
		sAttackProjectionMap.put(Attack.ATTACK_TIME, Attack.ATTACK_TIME);

		sFleetProjectionMap = new HashMap<String, String>();
		sFleetProjectionMap.put(Fleet._ID, Fleet._ID);
		sFleetProjectionMap.put(Fleet.GROUP_ID, Fleet.GROUP_ID);
		sFleetProjectionMap.put(Fleet.NAME, Fleet.NAME);
		sFleetProjectionMap.put(Fleet.H, Fleet.H);
		sFleetProjectionMap.put(Fleet.M, Fleet.M);
		sFleetProjectionMap.put(Fleet.S, Fleet.S);
		sFleetProjectionMap.put(Fleet.DELTA, Fleet.DELTA);
		sFleetProjectionMap.put(Fleet.LAUNCH_TIME, Fleet.LAUNCH_TIME);
		sFleetProjectionMap.put(Fleet.ALARM_DELTA, Fleet.ALARM_DELTA);
		sFleetProjectionMap.put(Fleet.ALARM_ACTIVATED, Fleet.ALARM_ACTIVATED);

	}

	public void calcGroup(SQLiteDatabase aDb, int agroupId) {
		String[] projection = { Fleet._ID };
		String selection = Fleet.GROUP_ID + "=" + agroupId;
		Cursor cursor = aDb.query(Fleet.TABLE_NAME, projection, selection,
				null, null, null, null);
		while (cursor.moveToNext()) {
			int id = cursor.getInt(cursor.getColumnIndexOrThrow(Fleet._ID));
			calcChild(aDb, agroupId, id);
		}
	}

	public void calcChild(SQLiteDatabase aDb, int agroupId, int achildId) {
		mAlarmManager = (AlarmManager) getContext().getSystemService(
				Context.ALARM_SERVICE);
		String[] projection = { Attack.ATTACK_TIME };
		Cursor cursor = aDb.query(Attack.TABLE_NAME, projection, Attack._ID
				+ "=" + agroupId, null, null, null, null);
		cursor.moveToFirst();
		long attackTime = Utils.getLongFromCol(cursor, Attack.ATTACK_TIME);

		String[] projection2 = { Fleet.NAME, Fleet.H, Fleet.M, Fleet.S,
				Fleet.DELTA, Fleet.ALARM_DELTA, Fleet.ALARM_ACTIVATED,
				Fleet.LAUNCH_TIME };
		Cursor cursor2 = aDb.query(Fleet.TABLE_NAME, projection2, Fleet._ID
				+ "=" + achildId, null, null, null, null);
		cursor2.moveToFirst();
		int fleetH = Utils.getIntFromCol(cursor2, Fleet.H);
		int fleetM = Utils.getIntFromCol(cursor2, Fleet.M);
		int fleetS = Utils.getIntFromCol(cursor2, Fleet.S);
		int fleetDelta = Utils.getIntFromCol(cursor2, Fleet.DELTA);

		Calendar launchCal = Calendar.getInstance();
		launchCal.setTimeInMillis(attackTime);
		Utils.addToCalendar(launchCal, 0, 0, 0, -fleetH, -fleetM, -fleetS
				- fleetDelta);
		ContentValues values = new ContentValues();
		values.put(Fleet.LAUNCH_TIME, launchCal.getTimeInMillis());

		long launchTime = Utils.getLongFromCol(cursor2, Fleet.LAUNCH_TIME);
		long alarmDelta = Utils.getLongFromCol(cursor2, Fleet.ALARM_DELTA);
		Log.d(TAG, "launchTime=" + launchTime + ", alarmDelta=" + alarmDelta);
		long alarmTime = launchTime - alarmDelta;
		String name = Utils.getStringFromCol(cursor2, Fleet.NAME);
		Intent intent = new Intent(getContext(), AlarmReceiver.class);
		intent.putExtra("fleetName", name);
		intent.putExtra("childId", achildId);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(),
				ALARM_ID + achildId, intent, 0);
		Boolean isActive = new Boolean(Utils.getStringFromCol(cursor2,
				Fleet.ALARM_ACTIVATED));
		if (isActive) {
			mAlarmManager
					.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
		} else {
			mAlarmManager.cancel(pendingIntent);
		}

		int update = aDb.update(Fleet.TABLE_NAME, values, "_id=" + achildId,
				null);
		Log.d(TAG, "number of lines modified after calculation:" + update);

	}
}
