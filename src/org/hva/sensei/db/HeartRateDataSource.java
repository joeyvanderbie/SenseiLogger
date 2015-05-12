package org.hva.sensei.db;

import org.hva.sensei.data.HeartRateData;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class HeartRateDataSource {
	// Database fields
		private SQLiteDatabase database;
		private DatabaseHelper dbHelper;
		private String[] allColumns = { Database.HeartRate._ID,
				Database.HeartRate.COLUMN_NAME_RUN_ID,
				Database.HeartRate.COLUMN_NAME_DATETIME,
				Database.HeartRate.COLUMN_NAME_HEARTRATE};

		public HeartRateDataSource(Context context) {
			dbHelper = new DatabaseHelper(context);
		}

		public void open() throws SQLException {
			database = dbHelper.getWritableDatabase();
		}

		public void close() {
			dbHelper.close();
		}

		public void addHeartRateSilent(HeartRateData hr) {
			ContentValues values = new ContentValues();
			values.put(Database.HeartRate.COLUMN_NAME_RUN_ID, hr.getRun_id());
			values.put(Database.HeartRate.COLUMN_NAME_HEARTRATE, hr.getHeart_rate());
			values.put(Database.HeartRate.COLUMN_NAME_DATETIME, hr.getTimestamp());

			database.insert(Database.HeartRate.TABLE_NAME, null, values);
		}
		
		public void addHeartRateRRSilent(HeartRateData rr) {
			ContentValues values = new ContentValues();
			values.put(Database.HeartRateRR.COLUMN_NAME_RUN_ID, rr.getRun_id());
			values.put(Database.HeartRateRR.COLUMN_NAME_HEARTRATE, rr.getHeart_rate());
			values.put(Database.HeartRateRR.COLUMN_NAME_DATETIME, rr.getTimestamp());

			database.insert(Database.HeartRateRR.TABLE_NAME, null, values);
		}
		
		public int getLastHeartRate(long run_id){
			String query = "SELECT "+Database.HeartRate.COLUMN_NAME_HEARTRATE+" from "+Database.HeartRate.TABLE_NAME +" WHERE " + Database.HeartRate.COLUMN_NAME_RUN_ID + " = "+ run_id + " ORDER BY cast("+Database.HeartRate.COLUMN_NAME_DATETIME+" as REAL) DESC LIMIT 1";
			Cursor c = database.rawQuery(query,null);
			int lastId = 0;
			if (c != null && c.moveToFirst()) {
			    lastId = c.getInt(0); //The 0 is the column index, we only have 1 column, so the index is 0
			}
			return lastId;
		}

		
}
