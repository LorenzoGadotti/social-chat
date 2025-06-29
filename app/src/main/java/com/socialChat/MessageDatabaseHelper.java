package com.socialChat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class MessageDatabaseHelper {

    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_IS_USER = "isUser";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    private final AppDatabaseHelper dbHelper;

    public MessageDatabaseHelper(Context context) {
        dbHelper = new AppDatabaseHelper(context);
    }

    public void addMessage(Message message) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, message.content);
        values.put(COLUMN_IS_USER, message.isUser ? 1 : 0);
        values.put(COLUMN_TIMESTAMP, message.timestamp);
        db.insert(TABLE_MESSAGES, null, values);
        db.close();
    }

    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MESSAGES + " ORDER BY " + COLUMN_TIMESTAMP + " ASC", null);

        if (cursor.moveToFirst()) {
            do {
                Message message = new Message();
                message.id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                message.content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
                message.isUser = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_USER)) == 1;
                message.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                messages.add(message);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return messages;
    }

    public void clearAllMessages() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_MESSAGES, null, null);
        db.close();
    }
}
