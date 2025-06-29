package com.socialChat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.mindrot.jbcrypt.BCrypt;

public class UserDatabaseHelper {

    private static final String TABLE_USERS = "users";
    private static final String COL_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASS_HASH = "password_hash";
    private static final String COL_PROMPT = "system_prompt";
    private static final String COL_PARENT_PASS_HASH = "parent_password_hash";

    private final AppDatabaseHelper dbHelper;

    public UserDatabaseHelper(Context context) {
        dbHelper = new AppDatabaseHelper(context);
    }

    public long addUser(String username, String plainPassword, String prompt, String parentPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USERNAME, username);
        cv.put(COL_PASS_HASH, BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
        cv.put(COL_PROMPT, prompt);
        cv.put(COL_PARENT_PASS_HASH, BCrypt.hashpw(parentPassword, BCrypt.gensalt()));
        long id = db.insert(TABLE_USERS, null, cv);
        db.close();
        return id;
    }

    public User login(String username, String plainPassword) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, null, COL_USERNAME + "=?", new String[]{username}, null, null, null);
        if (c.moveToFirst()) {
            String hash = c.getString(c.getColumnIndexOrThrow(COL_PASS_HASH));
            if (BCrypt.checkpw(plainPassword, hash)) {
                User u = new User(
                        c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                        username,
                        hash,
                        c.getString(c.getColumnIndexOrThrow(COL_PROMPT)),
                        c.getString(c.getColumnIndexOrThrow(COL_PARENT_PASS_HASH))
                );
                c.close();
                db.close();
                return u;
            }
        }
        c.close();
        db.close();
        return null;
    }

    public boolean checkParentPassword(long userId, String plainPassword) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_PARENT_PASS_HASH}, COL_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, null);
        if (c.moveToFirst()) {
            String hash = c.getString(c.getColumnIndexOrThrow(COL_PARENT_PASS_HASH));
            boolean result = BCrypt.checkpw(plainPassword, hash);
            c.close();
            db.close();
            return result;
        }
        c.close();
        db.close();
        return false;
    }

    public void updatePrompt(long userId, String prompt) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PROMPT, prompt);
        db.update(TABLE_USERS, cv, COL_ID + "=?", new String[]{String.valueOf(userId)});
        db.close();
    }

    public User getUserById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, null, COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        if (c.moveToFirst()) {
            User u = new User(
                    id,
                    c.getString(c.getColumnIndexOrThrow(COL_USERNAME)),
                    c.getString(c.getColumnIndexOrThrow(COL_PASS_HASH)),
                    c.getString(c.getColumnIndexOrThrow(COL_PROMPT)),
                    c.getString(c.getColumnIndexOrThrow(COL_PARENT_PASS_HASH))
            );
            c.close();
            db.close();
            return u;
        }
        c.close();
        db.close();
        return null;
    }
}
