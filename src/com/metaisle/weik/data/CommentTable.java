package com.metaisle.weik.data;

import android.database.sqlite.SQLiteDatabase;

import com.metaisle.util.Util;

public class CommentTable extends BaseTable {
	public static final String COMMENT_TABLE = "COMMENT_TABLE";
	
	public static final String CREATED_AT = "created_at";
	public static final String COMMENT_ID = "status_id";
	public static final String COMMENT_TEXT = "status_text";
	public static final String AUTHOR_ID = "author_id";
	public static final String REPLIED_STATUS = "REPLIED_STATUS";
	public static final String REPLIED_COMMENT = "REPLIED_COMMENT";

	public static final String IS_TO_ME = "IS_TO_ME";
	public static final String IS_MENTIONS = "IS_MENTIONS";
	
	
	public static void onCreate(SQLiteDatabase db){
		
			Util.log("Create table " + COMMENT_TABLE);
			db.execSQL("CREATE TABLE " + COMMENT_TABLE + " (" 
					+ _ID				+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ CREATED_AT 		+ " INTEGER NOT NULL," 
					+ COMMENT_ID 		+ " INTEGER UNIQUE NOT NULL," 
					+ COMMENT_TEXT	 	+ " TEXT NOT NULL," 
					+ AUTHOR_ID 		+ " INTEGER NOT NULL,"
					+ REPLIED_STATUS 		+ " INTEGER,"
					+ REPLIED_COMMENT 	+ " INTEGER,"
					+ IS_TO_ME 	+ " BOOLEAN,"
					+ IS_MENTIONS 	+ " BOOLEAN"
					+ ");");
	}
	
	
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Util.log("onUpgrade");
		db.execSQL("DROP TABLE IF EXISTS " + COMMENT_TABLE);
		onCreate(db);
	}
}
