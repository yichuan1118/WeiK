package com.metaisle.weik.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.metaisle.util.Util;

public class Database extends SQLiteOpenHelper {

	public static final String DB_NAME = "weik.db";
	public static final int DB_VERSION = 11;

	public Database(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Util.log("onCreate");
		TimelineTable.onCreate(db);
		UserTable.onCreate(db);
		MessageTable.onCreate(db);
		CommentTable.onCreate(db);
		RelationshipTable.onCreate(db);
		UrlTable.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Util.log("onUpgrade");
		TimelineTable.onUpgrade(db, oldVersion, newVersion);
		UserTable.onUpgrade(db, oldVersion, newVersion);
		MessageTable.onUpgrade(db, oldVersion, newVersion);
		CommentTable.onUpgrade(db, oldVersion, newVersion);
		RelationshipTable.onUpgrade(db, oldVersion, newVersion);
		UrlTable.onUpgrade(db, oldVersion, newVersion);
	}

}
