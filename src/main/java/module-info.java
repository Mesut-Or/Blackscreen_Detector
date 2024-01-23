module com.example.blackscreen6 {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.bytedeco.javacv;
    requires java.mail;
    requires  java.desktop;
    requires org.bouncycastle.provider;
    requires com.google.common;


    opens com.example.blackscreen6 to javafx.fxml;
    exports com.example.blackscreen6;
}