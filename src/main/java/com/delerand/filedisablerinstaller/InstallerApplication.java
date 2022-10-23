package com.delerand.filedisablerinstaller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InstallerApplication extends Application {

    public static void setExeInstallPath(String exeInstallPath) {
        InstallerApplication.exeInstallPath = exeInstallPath;
    }
    public static void setRegInstallPath(String regInstallPath) {
        InstallerApplication.regInstallPath = regInstallPath;
    }

    public static String exeInstallPath;
    public static String regInstallPath;
    public static InstallerApplication app = new InstallerApplication();

    public static void install() {
        InputStream is = null;
        try {
            //unpack the exe file
            List<Path> result = app.getPathsFromResourceJAR("data");
            for (Path path : result) {

                String filePathInJAR = path.toString();
                if (filePathInJAR.startsWith("/")) {
                    filePathInJAR = filePathInJAR.substring(1, filePathInJAR.length());
                }
                is = app.getFileFromResourceAsStream(filePathInJAR);
            }

            File directory = new File(regInstallPath);
            if (!directory.isDirectory()) {
                directory.mkdir();
            }

            File out = new File(exeInstallPath);
            Files.copy(is, out.toPath(), StandardCopyOption.REPLACE_EXISTING);

            //trying to build the reg file
            if (shellBuilder() && writeToRegistry()) {
                showInformation("You need to confirm the registry entry for successful installation!");
            }
            else {
                throw new IOException();
            }
        }
        catch (IOException e) {
            showError("Installation error!");
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    private InputStream getFileFromResourceAsStream(String fileName) {

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }

    }

    private List<Path> getPathsFromResourceJAR(String folder)
            throws URISyntaxException, IOException {

        List<Path> result;

        String jarPath = getClass().getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
                .getPath();

        URI uri = URI.create("jar:file:" + jarPath);
        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            result = Files.walk(fs.getPath(folder))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
        return result;
    }

    //build the reg file
    private static boolean shellBuilder() {
        try {
            File reg = new File(regInstallPath + "/shell.reg");
            FileWriter writer = new FileWriter(reg, false);
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            List<String> strings = new ArrayList<>();
            strings.add("Windows Registry Editor Version 5.00");
            strings.add("");
            strings.add("[HKEY_CLASSES_ROOT\\*\\shell\\Disable/Enable]");
            strings.add("\"MUIVerb\"=\"Disable / Enable\"");
            strings.add("\"Icon\"=\"explorer.exe\"");
            strings.add("");
            strings.add("[HKEY_CLASSES_ROOT\\*\\shell\\Disable/Enable\\command]");
            strings.add("@=\"" + exeInstallPath.replace("\\", "\\\\") + " \\\"%1\\\"\"");

            for(String s: strings){
                bufferedWriter.write(s + "\n");
            }

            bufferedWriter.flush();
            bufferedWriter.close();
            return true;
        }
        catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //start the reg file
    public static boolean writeToRegistry() {
        try {
            //regInstallPath = regInstallPath.replaceAll("\\s+", "^ ");
            //String PathRemovedWhitespaces = "\"" + regInstallPath + "\"";
            //System.out.println(PathRemovedWhitespaces);
            Process process = Runtime.getRuntime().exec("cmd /c start " + regInstallPath + "/shell.reg", null, new File(regInstallPath + "\\"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    //error alert
    protected static void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(error);

        alert.showAndWait();
    }

    //information alert
    protected static void showInformation(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(msg);
        alert.showAndWait();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(InstallerApplication.class.getResource("installer.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 420, 340);
        stage.setResizable(false);
        stage.setTitle("File disabler by Delerand");
        stage.setScene(scene);
        stage.show();

        TextField textField = new TextField("Path");
        textField.setMinWidth(120);
    }

    public static void main(String[] args) {
        launch();
    }
}