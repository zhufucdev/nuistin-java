package com.javamasters.view.component;

import javax.swing.*;
import java.awt.*;

public class Labeled <T extends Component> extends Container {
    public final T field;
    public final Label label = new Label();

    public Labeled(T field) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.field = field;
        add(label);
        add(field);
    }
}
