package org.hva.sensei.db;

import java.util.ArrayList;
import java.util.List;

import org.hva.sensei.data.AccelData;
import org.hva.sensei.data.CoachData;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class CoachDataSource {
	// Database fields
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private String[] allColumns = { Database.Coach._ID,
			Database.Coach.COLUMN_NAME_RUN_ID,
			Database.Coach.COLUMN_NAME_DATETIME,
			Database.Coach.COLUMN_NAME_STIMULUS,
			Database.Coach.COLUMN_NAME_STIMULUS_LENGTH,
			Database.Coach.COLUMN_NAME_ANSWER,
			Database.Coach.COLUMN_NAME_INSTRUCTIONS,
			Database.Coach.COLUMN_NAME_PLEASURE,
			Database.Coach.COLUMN_NAME_AROUSAL};

	public CoachDataSource(Context context) {
		dbHelper = new DatabaseHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}



	public void add(CoachData coach) {
		ContentValues values = new ContentValues();
		values.put(Database.Coach.COLUMN_NAME_RUN_ID, coach.getRun_id());
		values.put(Database.Coach.COLUMN_NAME_ANSWER, coach.getAnswer());
		values.put(Database.Coach.COLUMN_NAME_AROUSAL, coach.getArousal());
		values.put(Database.Coach.COLUMN_NAME_DATETIME, coach.getDatetime());
		values.put(Database.Coach.COLUMN_NAME_INSTRUCTIONS, coach.getInstructions());
		values.put(Database.Coach.COLUMN_NAME_PLEASURE, coach.getPleasure());
		values.put(Database.Coach.COLUMN_NAME_STIMULUS, coach.getStimulus());
		values.put(Database.Coach.COLUMN_NAME_STIMULUS_LENGTH, coach.getStimulus_length());

		long insertId = database.insert(Database.Coach.TABLE_NAME, null,
				values);
		
	}
}
