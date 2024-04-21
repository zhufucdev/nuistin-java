package com.javamasters.view.component;

import javax.swing.*;
import java.awt.*;

public class Labeled extends Container {
    public final Component field;
    public final Label label = new Label();

    public Labeled(Component field) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.field = field;
        add(label);
        add(field);
    }
}
