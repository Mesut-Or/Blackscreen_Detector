package com.example.blackscreen6;

import javafx.application.Platform;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;


public class StreamProcessor {

    private final StreamStatusListener statusListener;

    private int streamStartAttemt = 0;

    private int blackFrameCounter = 0;

    private int frozenFrameCounter = 0;

    private final int blackIntensity;



    private boolean alertForBlackScreenSent = false;
    private boolean alertForFrozenFrameSent = false;
    private boolean alertForStreamFailureSent = false;

    private final String udpAddress;

    private ExecutorService executorService;

    StreamMonitor monitor = new StreamMonitor();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    public StreamProcessor(String udpAddress, StreamStatusListener statusListener, int blackIntensity){

        this.udpAddress = udpAddress;
        this.statusListener = statusListener;
        this.blackIntensity = blackIntensity;

    }

    private void updateStatus(boolean isActive) {
        if (statusListener != null) {
            Platform.runLater(() -> statusListener.onStreamStatusChanged(isActive));
        }
    }

    public boolean streamTest() {


        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(udpAddress)) {
            grabber.setOption("stimeout", "10000000");
            grabber.setOption("timeout", "3");
            grabber.start();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public void processStream() {

        executorService.submit(() -> {

                try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(udpAddress)) {
                   // grabber.setOption("stimeout" , "10000000");
                    grabber.setOption("timeout" , "3");
                    grabber.setOption("hwaccel", "cuvid");

                    //grabber.setFrameRate(50);
                    grabber.start();
                    updateStatus(true);
                    streamStartAttemt = 0;

                    String previousHash = null;
                    String currentHash;
                    double sum= 0;
                    double average;
                    int index = 0;

                    // Proceed with frame grabbing if the connection is successful

                    Frame frame;
                    while (!Thread.currentThread().isInterrupted()) {
                        long startTime = System.nanoTime();
                        frame = grabber.grabImage();

                        //currentHash = getMurmurHash(getByteData(frame));
                        currentHash = hashFrame(frame);


                        if (isBlackScreen(frame)) {
                            handleBlackScreen();

                        } else if (isFrozenFrame(currentHash,previousHash)) {
                            handleFrozenFrame();

                        }else {
                            blackFrameCounter=0;
                            resetAlertFlags();
                        }


                        monitor.updateLastFrameTime();
                        previousHash = currentHash;

                         long endTime = System.nanoTime();
                         long duration = (endTime - startTime);
                        double durationInMilliseconds = duration / 1_000_000.0;
                        sum = sum+durationInMilliseconds;
                        index++;



                    }

                    grabber.release();

                    average = sum/index;


                    System.out.println("released");

                    System.out.println("Duration of loop iteration: " + average + " ms");

                } catch (FFmpegFrameGrabber.Exception e) {

                    handleStreamFailure(e);

                } catch (Exception e){

                     handleStreamFailure(e);
                }
        });

    }

    public boolean isBlackScreen(Frame frame) {

        //long startTime = System.nanoTime();

        if (frame == null || frame.image == null) {
            return false; // Not a valid frame
        }

        java.nio.ByteBuffer buffer = (java.nio.ByteBuffer) frame.image[0];
        buffer.position(0); // Reset the buffer's position

        int totalIntensity = 0;
        /*int sampleSize = 100; // Number of pixels to sample
        int stepSize = frame.imageWidth * frame.imageHeight / sampleSize;

        for (int i = 0; i < sampleSize; i++) {
            int position = i * stepSize;
            if (position < buffer.limit()) {
                buffer.position(position);
                int intensity = buffer.get() & 0xFF; // Convert to unsigned
                totalIntensity += intensity;
            }
        }

        double avgIntensity = (double) totalIntensity / sampleSize;*/


        for (int i = 0; i < frame.imageHeight; i++) {
            for (int j = 0; j < frame.imageWidth; j++) {
                int position = i * frame.imageWidth + j;
                buffer.position(position);
                int intensity = buffer.get() & 0xFF;
                totalIntensity += intensity;
            }
        }
        double avgIntensity = (double) totalIntensity / (frame.imageWidth * frame.imageHeight);

       // long endTime = System.nanoTime();
       // long duration = (endTime - startTime);
        //double durationInMilliseconds = duration / 1_000_000.0;

        //System.out.println("Black screen detection took: " + durationInMilliseconds + " milliseconds");

        return avgIntensity <= blackIntensity; // Adjust threshold as needed
    }

    private boolean isFrozenFrame(String currentHash, String previousHash) {
        // Implement logic to compare currentFrame with previousFrame
        // Return true if they are identical or very similar
        if(currentHash == null || previousHash == null) return false;

        return currentHash.equals(previousHash);

    }



    private void handleFrozenFrame() {
        frozenFrameCounter++;
        if(frozenFrameCounter == 500){
            EmailUtil.sendAlert("frozen Frame for 10 seconds");
            alertForFrozenFrameSent = true;
        }


    }

    private void handleStreamFailure(Exception e) {

        streamStartAttemt++;
        updateStatus(false);
        int MAX_ATTEMPTS = 4;
        if (streamStartAttemt == MAX_ATTEMPTS) {
            EmailUtil.sendAlert("Max attempts reached. " + e.getMessage());
            alertForStreamFailureSent = true;
        }
        try {

            Thread.sleep(2500); // Wait before retrying
            if(executorService.isShutdown()) return;
            processStream();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            // Handle interruption
        }


    }
    private void handleBlackScreen() {
        System.out.println("black screen");
        blackFrameCounter++;
        if(blackFrameCounter == 500){
            EmailUtil.sendAlert("black screen for 10 seconds");
            alertForBlackScreenSent= true;
        }
    }

    private void resetAlertFlags() {

        alertForBlackScreenSent = false;
        alertForFrozenFrameSent = false;
        alertForStreamFailureSent= false;
    }

    public void startStreamProcessing() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Shut down the existing executor service
        }
        monitor.startMonitoring();
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::processStream);
    }

    public void stopStreamProcessing() {
        if (executorService != null && !executorService.isShutdown()) {
            statusListener.onStreamStatusChanged(false);
            executorService.shutdownNow(); // Attempt to stop all actively executing tasks
           // monitor.stopMonitoring();
            updateStatus(false);
            monitor.stopMonitoring();


            try {
                // Optionally wait for tasks to terminate
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.out.println("Stream processing tasks did not terminate");
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted during shutdown");
            }
        }
    }

    public boolean isWorking(){
        return executorService != null && !executorService.isShutdown();
    }




    public void shutdown() {
        if (executorService != null) {
            monitor.stopMonitoring();
            executorService.shutdownNow(); // Attempt to stop all actively executing tasks

            try {
                if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public String getMD5Hash(BufferedImage image) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            byte[] imageData = outputStream.toByteArray();

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(imageData);

            // Convert byte array to hex string
            BigInteger number = new BigInteger(1, hash);
            StringBuilder hexString = new StringBuilder(number.toString(16));
            while (hexString.length() < 32) {
                hexString.insert(0, '0');
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getSHA3Hash(byte[] data) {
        try {

            MessageDigest md = MessageDigest.getInstance("SHA3-256", BouncyCastleProvider.PROVIDER_NAME);
            byte[] hashBytes = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getByteData(Frame frame){
        long startTime = System.nanoTime();
        byte[] imageData = null;
         try (Java2DFrameConverter converter = new Java2DFrameConverter()) {

             try {
                 BufferedImage bufferedImage = converter.convert(frame);
                 Buffer buffer= frame.image[0];


                 ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ImageIO.write(bufferedImage, "jpg", baos);

                 long endTime = System.nanoTime();
                 long duration = (endTime - startTime);
                 double durationInMilliseconds = duration / 1_000_000.0;

                 System.out.println("getByteData took: " + durationInMilliseconds + " milliseconds");
                 baos.flush();
                 imageData = baos.toByteArray();
                 baos.close();


                 return imageData;
             }catch (Exception e){
                 e.printStackTrace();
             }

         }catch (Exception e){
             e.printStackTrace();
         }

        return imageData;
    }
    public String getMurmurHash(byte[] data) {
        long startTime = System.nanoTime();
        HashFunction hashFunction = Hashing.murmur3_128(); // Use murmur3 128-bit variant

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        double durationInMilliseconds = duration / 1_000_000.0;

        //System.out.println("murmurHash took: " + durationInMilliseconds + " milliseconds");
        return hashFunction.hashBytes(data).toString();
    }

    public String hashFrame(Frame frame) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = (ByteBuffer) frame.image[0];

            // Example: hash every nth pixel
            int step = 10; // Adjust according to your requirements
            byte[] pixelSample = new byte[4]; // Assuming a 4-byte color depth
            for (int i = 0; i < buffer.limit(); i += step * 4) {
                buffer.position(i);
                buffer.get(pixelSample, 0, 4);
                digest.update(pixelSample);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }



}