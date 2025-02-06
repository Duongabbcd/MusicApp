package com.example.service.download.get;

public interface DownloadManager {
    int BLOCK_SIZE = 512 * 1024;

    int startMission(String url, String name, String id, String source, String referer, String message, String notification);

    void resumeMission(int id);

    void pauseMission(int id);

    void deleteMission(int id);

    DownloadMission getMission(int id);

    int getCount();

    String getLocation();
}
