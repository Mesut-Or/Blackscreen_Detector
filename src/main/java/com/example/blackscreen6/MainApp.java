package com.example.blackscreen6;


import com.sun.mail.smtp.SMTPOutputStream;
import javafx.application.Application;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;






public class MainApp extends Application implements StreamStatusListener {

    private Circle streamStatusIndicator;
    StreamProcessor streamProcessor;



    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) {
        // UI Components for UDP Stream
        TextField udpAddressField = new TextField();
        udpAddressField.setPromptText("Enter UDP Stream Address");
        udpAddressField.setText("udp://@224.2.2.2:1234");

        TextField blackIntensity = new TextField();
        blackIntensity.setPromptText("Enter the desired black intensity");
        blackIntensity.setText("30");

        Button startStreamButton = new Button("Start Stream");
        Button stopStreamButton = new Button("Stop Stream");

        // UI Components for Email
        TextField toAddressField = new TextField();
        toAddressField.setPromptText("Recipient's Email Address");
        TextField subjectField = new TextField();
        subjectField.setPromptText("Email Subject");
        TextField messageField = new TextField();
        messageField.setPromptText("Email Message");
        TextField fromEmailField = new TextField();
        fromEmailField.setPromptText("Your Email Address");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Your Email Password");
        Button sendEmailButton = new Button("Send Email");

        // Indicator for Stream Status
        streamStatusIndicator = new Circle(10, Color.RED); // Default to red (not active)
        Label indicatorLabel = new Label("Stream Status:", streamStatusIndicator);
        indicatorLabel.setContentDisplay(ContentDisplay.RIGHT);

        // VBox Layout
        VBox layout = new VBox(10); // Vertical layout with 10px spacing
        layout.getChildren().addAll(
                new Label("UDP Stream Control"), udpAddressField,blackIntensity, startStreamButton, stopStreamButton,
                indicatorLabel, // Add the stream status indicator
                new Separator(),
                new Label("Send Email"), toAddressField, subjectField, messageField,
                fromEmailField, passwordField, sendEmailButton
        );

        // Set up button actions
        startStreamButton.setOnAction(event -> handleStartStream(udpAddressField.getText(),blackIntensity.getText()));
        stopStreamButton.setOnAction(event -> handleStopStream());
        sendEmailButton.setOnAction(event -> EmailUtil.sendEmail(toAddressField.getText(), subjectField.getText(),
                messageField.getText(), fromEmailField.getText(),
                passwordField.getText()));



        // Set up the primary stage
        primaryStage.setTitle("UDP Stream and Email Sender");
        primaryStage.setScene(new Scene(layout, 400, 350));
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (streamProcessor != null) {
            streamProcessor.shutdown();
        }
    }

    private void handleStartStream(String udpAddress, String blackIntensity) {

        int intValueBlack = Integer.parseInt(blackIntensity);
        streamProcessor = new StreamProcessor(udpAddress,this,intValueBlack);

        // Start processing the UDP stream with the given address
        streamProcessor.startStreamProcessing();



        //if(isWorking) updateStreamStatus(true);

    }



    private void handleStopStream() {
        // Stop processing the UDP stream
        System.out.println("Stopping stream processing");
        streamProcessor.stopStreamProcessing();
        System.out.println("Stream processing is working: " + streamProcessor.isWorking());
        if (streamProcessor.isWorking()) {
            updateStreamStatus(true);
        } else {
            updateStreamStatus(false);
        }


    }

    private void updateStreamStatus(boolean isActive) {
        Platform.runLater(() -> {
            if (isActive) {
                streamStatusIndicator.setFill(Color.GREEN);
            } else {
                streamStatusIndicator.setFill(Color.RED);
            }
        });
    }


    @Override
    public void onStreamStatusChanged(boolean isActive) {
        updateStreamStatus(isActive);
    }
}