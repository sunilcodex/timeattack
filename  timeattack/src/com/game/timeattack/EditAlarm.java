package com.game.timeattack;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.game.timeattack.provider.TimeAttack.Attack;
import com.game.timeattack.provider.TimeAttack.Fleet;

public class EditAlarm extends Activity implements OnClickListener {

	final static String TAG = "EditAlarm";
	EditText mH, mM, mS;
	Button cancel, ok;
	private int CODE_OK = 1;
	int groupId, childId;
	Cursor attackCursor, fleetCursor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.edit_alarm);
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras == null) {
			throw new IllegalArgumentException(
					"Need groupId, childId and code to launch");
		}
		groupId = extras.getInt("groupId");
		childId = extras.getInt("childId");
		Uri uri = Uri.withAppendedPath(Attack.CONTENT_URI, "" + groupId);
		attackCursor = getContentResolver().query(uri, null, null, null, null);
		attackCursor.moveToFirst();
		uri = Uri.withAppendedPath(Fleet.CONTENT_URI, "" + childId);
		getContentResolver().query(uri, null, null, null, null);

		mH = (EditText) findViewById(R.id.h);
		mM = (EditText) findViewById(R.id.m);
		mS = (EditText) findViewById(R.id.s);

		mH.setText("0");
		mM.setText("5");
		mS.setText("0");

		cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(this);
		ok = (Button) findViewById(R.id.ok);
		ok.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.cancel:
			setResult(-1, null);
			this.finish();
			break;
		case R.id.ok:
			checkTextValues();
			Intent data = getIntent();

			setResult(CODE_OK, data);
			finish();
			break;
		default:
			throw new IllegalArgumentException("Wrong Button");
		}
	}

	private void checkTextValues() {
		Editable h, m, s;
		h = mH.getText();
		m = mM.getText();
		s = mS.getText();
		if (h.length() == 0) {
			h.insert(0, "0");
		}
		if (m.length() == 0) {
			m.insert(0, "0");
		}
		if (s.length() == 0) {
			s.insert(0, "0");
		}
		if (Utils.sToI(m.toString()) > 60) {
			m.clear();
			m.insert(0, "0");
		}
		if (Utils.sToI(s.toString()) > 60) {
			s.clear();
			s.insert(0, "0");
		}
	}
}