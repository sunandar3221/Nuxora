package com.anlinux.app.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.anlinux.app.data.model.ExecutionMode;
import com.anlinux.app.data.model.InstalledDistro;

import java.util.ArrayList;
import java.util.List;

public class DistroDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "anlinux.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_INSTALLED = "installed_distros";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_DISTRO_KEY = "distro_key";
    public static final String COL_ROOTFS_PATH = "rootfs_path";
    public static final String COL_EXECUTION_MODE = "execution_mode";
    public static final String COL_IS_RUNNING = "is_running";
    public static final String COL_MOUNT_SDCARD = "mount_sdcard";
    public static final String COL_INSTALLED_AT = "installed_at";
    public static final String COL_DISK_USAGE = "disk_usage";
    public static final String COL_CUSTOM_SHELL = "custom_shell";

    private static DistroDatabaseHelper instance;

    public static synchronized DistroDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DistroDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DistroDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_INSTALLED + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT, " +
                COL_DISTRO_KEY + " TEXT, " +
                COL_ROOTFS_PATH + " TEXT, " +
                COL_EXECUTION_MODE + " TEXT, " +
                COL_IS_RUNNING + " INTEGER, " +
                COL_MOUNT_SDCARD + " INTEGER, " +
                COL_INSTALLED_AT + " INTEGER, " +
                COL_DISK_USAGE + " TEXT, " +
                COL_CUSTOM_SHELL + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INSTALLED);
        onCreate(db);
    }

    public synchronized long insertDistro(InstalledDistro distro) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME, distro.getName());
        cv.put(COL_DISTRO_KEY, distro.getDistroKey());
        cv.put(COL_ROOTFS_PATH, distro.getRootfsPath());
        cv.put(COL_EXECUTION_MODE, distro.getExecutionMode().name());
        cv.put(COL_IS_RUNNING, distro.isRunning() ? 1 : 0);
        cv.put(COL_MOUNT_SDCARD, distro.isMountSdcard() ? 1 : 0);
        cv.put(COL_INSTALLED_AT, distro.getInstalledAt());
        cv.put(COL_DISK_USAGE, distro.getDiskUsage());
        cv.put(COL_CUSTOM_SHELL, distro.getCustomShell());
        return db.insert(TABLE_INSTALLED, null, cv);
    }

    public synchronized void updateRunningStatus(long id, boolean isRunning) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_IS_RUNNING, isRunning ? 1 : 0);
        db.update(TABLE_INSTALLED, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public synchronized void updateDiskUsage(long id, String usage) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DISK_USAGE, usage);
        db.update(TABLE_INSTALLED, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public synchronized void deleteDistro(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_INSTALLED, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public synchronized List<InstalledDistro> getAllInstalledDistros() {
        List<InstalledDistro> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_INSTALLED, null, null, null, null, null, COL_ID + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                InstalledDistro distro = new InstalledDistro();
                distro.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
                distro.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)));
                distro.setDistroKey(cursor.getString(cursor.getColumnIndexOrThrow(COL_DISTRO_KEY)));
                distro.setRootfsPath(cursor.getString(cursor.getColumnIndexOrThrow(COL_ROOTFS_PATH)));
                String modeStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXECUTION_MODE));
                distro.setExecutionMode(ExecutionMode.valueOf(modeStr));
                distro.setRunning(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_RUNNING)) == 1);
                distro.setMountSdcard(cursor.getInt(cursor.getColumnIndexOrThrow(COL_MOUNT_SDCARD)) == 1);
                distro.setInstalledAt(cursor.getLong(cursor.getColumnIndexOrThrow(COL_INSTALLED_AT)));
                distro.setDiskUsage(cursor.getString(cursor.getColumnIndexOrThrow(COL_DISK_USAGE)));
                distro.setCustomShell(cursor.getString(cursor.getColumnIndexOrThrow(COL_CUSTOM_SHELL)));
                list.add(distro);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public synchronized InstalledDistro getDistroById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_INSTALLED, null, COL_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            InstalledDistro distro = new InstalledDistro();
            distro.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
            distro.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)));
            distro.setDistroKey(cursor.getString(cursor.getColumnIndexOrThrow(COL_DISTRO_KEY)));
            distro.setRootfsPath(cursor.getString(cursor.getColumnIndexOrThrow(COL_ROOTFS_PATH)));
            distro.setExecutionMode(ExecutionMode.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_EXECUTION_MODE))));
            distro.setRunning(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_RUNNING)) == 1);
            distro.setMountSdcard(cursor.getInt(cursor.getColumnIndexOrThrow(COL_MOUNT_SDCARD)) == 1);
            distro.setInstalledAt(cursor.getLong(cursor.getColumnIndexOrThrow(COL_INSTALLED_AT)));
            distro.setDiskUsage(cursor.getString(cursor.getColumnIndexOrThrow(COL_DISK_USAGE)));
            distro.setCustomShell(cursor.getString(cursor.getColumnIndexOrThrow(COL_CUSTOM_SHELL)));
            cursor.close();
            return distro;
        }
        if (cursor != null) cursor.close();
        return null;
    }
}
