package com.javamasters.view;

import javax.swing.*;
import java.awt.*;

public class LabeledTextField extends Container {
    public final TextField textField = new TextField();
    public final Label label = new Label();

    public LabeledTextField() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(label);
        add(textField);
    }
}
