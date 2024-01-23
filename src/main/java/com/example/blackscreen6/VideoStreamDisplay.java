package com.example.blackscreen6;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

public class VideoStreamDisplay {
    public static void main(String[] args) throws Exception {
        String streamUrl = "udp://@224.2.2.2:1234"; // Replace with your stream URL
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(streamUrl);
        grabber.start();

        CanvasFrame canvasFrame = new CanvasFrame("Video Stream", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        canvasFrame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        Frame frame;

        while (canvasFrame.isVisible() && (frame = grabber.grabImage()) != null) {
            long startTime = System.nanoTime();
            grabber.setOption("hwaccel", "cuvid");
            canvasFrame.showImage(frame);
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);
            double durationInMilliseconds = duration / 1_000_000.0;

            System.out.println("Duration of loop iteration: " + durationInMilliseconds + " ms");
        }

        grabber.stop();
        canvasFrame.dispose();
    }
}