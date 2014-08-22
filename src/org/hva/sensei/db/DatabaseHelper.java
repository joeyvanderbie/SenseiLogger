package org.hva.sensei.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
	// If you change the database schema, you must increment the database
	// version.
	public static final int DATABASE_VERSION = 5;
	public static final String DATABASE_NAME = "SenseiLogger.db";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Database.SQL_CREATE_USER);
//		db.execSQL(Database.SQL_CREATE_AFFECT);
//		db.execSQL(Database.SQL_CREATE_AFFECT_TOOL);
		db.execSQL(Database.SQL_CREATE_MOVEMENT);
//		db.execSQL(Database.SQL_CREATE_GYRO);
//		db.execSQL(Database.SQL_CREATE_ROUTE_RUN);
//		db.execSQL(Database.SQL_CREATE_ROUTE);
//		db.execSQL(Database.SQL_CREATE_LOCATION);
//		db.execSQL(Database.SQL_CREATE_QUEUE);
		db.execSQL(Database.SQL_CREATE_HEARTRATE);
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// This database is only a cache for online data, so its upgrade policy
		// is
		// to simply to discard the data and start over
		db.execSQL(Database.SQL_DELETE_USER);
//		db.execSQL(Database.SQL_DELETE_AFFECT);
//		db.execSQL(Database.SQL_DELETE_AFFECT_TOOL);
		db.execSQL(Database.SQL_DELETE_MOVEMENT);
//		db.execSQL(Database.SQL_DELETE_ROUTE_RUN);
//		db.execSQL(Database.SQL_DELETE_ROUTE);
//		db.execSQL(Database.SQL_DELETE_GYRO);
//		db.execSQL(Database.SQL_DELETE_LOCATION);
//		db.execSQL(Database.SQL_DELETE_QUEUE);
		db.execSQL(Database.SQL_DELETE_HEARTRATE);
		onCreate(db);
	}
	
	public void doSaveDelete(SQLiteDatabase db) {
		// This database is only a cache for online data, so its upgrade policy
		// is
		// to simply to discard the data and start over
		db.execSQL(Database.SQL_DELETE_MOVEMENT);
		db.execSQL(Database.SQL_DELETE_HEARTRATE);
//		db.execSQL(Database.SQL_DELETE_GYRO);
//		db.execSQL(Database.SQL_DELETE_LOCATION);
//		
		db.execSQL(Database.SQL_CREATE_MOVEMENT);
		db.execSQL(Database.SQL_CREATE_HEARTRATE);

//		db.execSQL(Database.SQL_CREATE_GYRO);
//		db.execSQL(Database.SQL_CREATE_LOCATION);
	}

	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

}
