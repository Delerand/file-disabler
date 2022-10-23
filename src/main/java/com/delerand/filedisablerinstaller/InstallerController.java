package com.delerand.filedisablerinstaller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class InstallerController {
    @FXML
    public void onInstallButtonClick() {
        InstallerApplication.setExeInstallPath(path.getText() +  "\\file-disabler.exe");
        InstallerApplication.setRegInstallPath(path.getText());
        InstallerApplication.install();
    }
    @FXML
    public Label enterPath;

    @FXML
    public TextField path;
}