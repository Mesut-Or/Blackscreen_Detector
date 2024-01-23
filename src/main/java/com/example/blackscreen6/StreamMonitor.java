package com.example.blackscreen6;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// ...

public class StreamMonitor{



    private long lastFrameTime = 0;

    private boolean isActive = false;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::checkStream, 3, 5, TimeUnit.SECONDS);
    }

    public void stopMonitoring() {
        scheduler.shutdownNow();
    }

    private void checkStream() {
        long currentTime = System.currentTimeMillis();
        // Check if it's been more than 5 seconds since the last frame
        if (currentTime - lastFrameTime > 5000) {
            // Handle stream inactivity
            isActive =false;
            System.out.println("Stream appears to be inactive.");

        }else{
            System.out.println("Stream appears to be active.");
            isActive = true;
        }

    }

    public void updateLastFrameTime() {
        lastFrameTime = System.currentTimeMillis();
    }

    public boolean isActive(){
        return isActive;
    }


}