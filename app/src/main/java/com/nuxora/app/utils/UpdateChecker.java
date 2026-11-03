package com.nuxora.app.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.nuxora.app.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class UpdateChecker {

    private static final String TAG = "NuxoraUpdateChecker";
    private static final String GITHUB_API_LATEST_RELEASE =
            "https://api.github.com/repos/sunandar3221/Nuxora/releases/latest";

    public static class ReleaseInfo {
        public final String tagName;
        public final String cleanVersion;
        public final String title;
        public final String body;
        public final String htmlUrl;
        public final String downloadUrl;

        public ReleaseInfo(String tagName, String cleanVersion, String title, String body, String htmlUrl, String downloadUrl) {
            this.tagName = tagName;
            this.cleanVersion = cleanVersion;
            this.title = title;
            this.body = body;
            this.htmlUrl = htmlUrl;
            this.downloadUrl = downloadUrl;
        }
    }

    public interface UpdateCallback {
        void onUpdateAvailable(ReleaseInfo releaseInfo);
        void onAlreadyLatest(String currentVersion);
        void onError(String errorMessage);
    }

    public static void check(Context context, UpdateCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final String currentVersion = BuildConfig.VERSION_NAME;

        new Thread(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build();

            Request request = new Request.Builder()
                    .url(GITHUB_API_LATEST_RELEASE)
                    .header("User-Agent", "Nuxora-Android-App/" + currentVersion)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    postError(mainHandler, callback, "Respon server tidak berhasil (HTTP " + response.code() + ")");
                    return;
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    postError(mainHandler, callback, "Respon server kosong");
                    return;
                }

                String jsonStr = responseBody.string();
                JSONObject json = new JSONObject(jsonStr);

                String tagName = json.optString("tag_name", "");
                String title = json.optString("name", tagName);
                String body = json.optString("body", "");
                String htmlUrl = json.optString("html_url", "https://github.com/sunandar3221/Nuxora/releases");
                String downloadUrl = htmlUrl;

                JSONArray assets = json.optJSONArray("assets");
                if (assets != null && assets.length() > 0) {
                    String signedApkUrl = null;
                    String generalApkUrl = null;

                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String name = asset.optString("name", "");
                        String browserDownloadUrl = asset.optString("browser_download_url", "");

                        if (name.endsWith(".apk")) {
                            if (name.toLowerCase().contains("signed") && !name.toLowerCase().contains("unsigned")) {
                                signedApkUrl = browserDownloadUrl;
                            } else if (!name.toLowerCase().contains("unsigned")) {
                                generalApkUrl = browserDownloadUrl;
                            }
                        }
                    }

                    if (signedApkUrl != null) {
                        downloadUrl = signedApkUrl;
                    } else if (generalApkUrl != null) {
                        downloadUrl = generalApkUrl;
                    }
                }

                String cleanLatest = cleanVersionString(tagName);
                String cleanCurrent = cleanVersionString(currentVersion);

                int cmp = compareVersions(cleanLatest, cleanCurrent);
                if (cmp > 0) {
                    ReleaseInfo releaseInfo = new ReleaseInfo(tagName, cleanLatest, title, body, htmlUrl, downloadUrl);
                    mainHandler.post(() -> {
                        if (callback != null) callback.onUpdateAvailable(releaseInfo);
                    });
                } else {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onAlreadyLatest(currentVersion);
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "Koneksi gagal saat memeriksa pembaruan", e);
                postError(mainHandler, callback, "Koneksi internet bermasalah. Silakan periksa jaringan Anda.");
            } catch (Exception e) {
                Log.e(TAG, "Gagal memproses data pembaruan", e);
                postError(mainHandler, callback, "Gagal memproses data pembaruan: " + e.getMessage());
            }
        }).start();
    }

    private static void postError(Handler handler, UpdateCallback callback, String message) {
        handler.post(() -> {
            if (callback != null) callback.onError(message);
        });
    }

    public static String cleanVersionString(String raw) {
        if (raw == null) return "0.0.0";
        return raw.trim().replaceAll("^[vV]", "");
    }

    public static int compareVersions(String v1, String v2) {
        String clean1 = cleanVersionString(v1);
        String clean2 = cleanVersionString(v2);

        String[] parts1 = clean1.split("[.\\-_]");
        String[] parts2 = clean2.split("[.\\-_]");

        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int num1 = 0;
            int num2 = 0;

            if (i < parts1.length) {
                num1 = parseLeadingInt(parts1[i]);
            }
            if (i < parts2.length) {
                num2 = parseLeadingInt(parts2[i]);
            }

            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }

        return 0;
    }

    private static int parseLeadingInt(String part) {
        if (part == null || part.isEmpty()) return 0;
        StringBuilder sb = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            } else {
                break;
            }
        }
        if (sb.length() == 0) return 0;
        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
