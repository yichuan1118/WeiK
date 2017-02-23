package com.metaisle.weik.data;

import android.database.sqlite.SQLiteDatabase;

import com.metaisle.util.Util;

public final class TimelineTable extends BaseTable{
	
	public static final String TIMELINE_TABLE = "timeline_table";
	
	public static final String CREATED_AT = "created_at";
	public static final String STATUS_ID = "status_id";
	public static final String STATUS_TEXT = "status_text";
	public static final String FAVORITED = "favorited";
	
//	public static final String IN_REPLY_TO_STATUS_ID = "in_reply_to_status_id";
//	public static final String IN_REPLY_TO_USER_ID = "in_reply_to_user_id";
//	public static final String IN_REPLY_TO_SCREEN_NAME = "in_reply_to_screen_name";
	public static final String THUMBNAIL_PIC = "thumbnail_pic";
	public static final String BMIDDLE_PIC = "bmiddle_pic";
	public static final String ORIGINAL_PIC = "original_pic";
	public static final String GEO = "geo";
	public static final String REPOSTS_COUNT = "reposts_count";
	public static final String COMMENTS_COUNT = "comments_count";
	public static final String ANNOTATIONS = "annotations";
	
	public static final String AUTHOR_ID = "author_id";
	
	public static final String RETWEETED_STATUS = "retweeted_status";
	
	public static final String CACHED_AT = "cached_at";
	public static final String USER_TIMELINE = "user_timeline";
	public static final String IS_HOME = "is_home";
	public static final String IS_MENTION = "is_mention";
	public static final String DIRECT_REPOST = "STATUS_REPOSTED";
	

	
	
	public static void onCreate(SQLiteDatabase db){
		
			Util.log("Create table " + TIMELINE_TABLE);
			db.execSQL("CREATE TABLE " + TIMELINE_TABLE + " (" 
					+ _ID				+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ CREATED_AT 		+ " INTEGER NOT NULL," 
					+ STATUS_ID 		+ " INTEGER UNIQUE NOT NULL," 
					+ STATUS_TEXT	 	+ " TEXT NOT NULL," 
					+ FAVORITED 		+ " BOOLEAN NOT NULL,"
//					+ IN_REPLY_TO_STATUS_ID	 	+ " TEXT," 
//					+ IN_REPLY_TO_USER_ID	 	+ " TEXT," 
//					+ IN_REPLY_TO_SCREEN_NAME	 	+ " TEXT," 
					+ THUMBNAIL_PIC	 	+ " TEXT," 
					+ BMIDDLE_PIC	 	+ " TEXT," 
					+ ORIGINAL_PIC	 	+ " TEXT," 
					+ GEO	 	+ " TEXT," 
					+ REPOSTS_COUNT	 	+ " INTEGER NOT NULL," 
					+ COMMENTS_COUNT	 	+ " INTEGER NOT NULL," 
					+ ANNOTATIONS	 	+ " TEXT," 
					
					+ AUTHOR_ID 			+ " INTEGER NOT NULL,"
					
					+ RETWEETED_STATUS 			+ " INTEGER,"
					
					+ CACHED_AT + " INTEGER,"
					+ USER_TIMELINE + " INTEGER,"
					+ IS_HOME + " BOOLEAN,"
					+ IS_MENTION + " BOOLEAN,"
					+ DIRECT_REPOST + " INTEGER"
					+ ");");
	}
	
	
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Util.log("onUpgrade");
		db.execSQL("DROP TABLE IF EXISTS " + TIMELINE_TABLE);
		onCreate(db);
	}
}
