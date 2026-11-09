package com.sigebox.central.ui.theme;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

/**
 * Control − valor + de la central (DESIGN v5 §5.14); reemplaza al {@code Spinner} de JavaFX. Se llega con Tab y se
 * cambia con las flechas o con las teclas + y −; en cada límite su botón se apaga.
 */
public final class Stepper extends HBox {

    private final int min;
    private final int max;
    private final IntegerProperty value = new SimpleIntegerProperty();
    private final Button minus = new Button();
    private final Button plus = new Button();
    private final Label valueLabel = new Label();
    private String name = "";

    public Stepper(int min, int max, int initial) {
        if (min > max) {
            throw new IllegalArgumentException("rango vacío: " + min + "–" + max);
        }
        this.min = min;
        this.max = max;
        getStyleClass().add("stepper");
        setAlignment(Pos.CENTER);
        setSpacing(4);
        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.SPINNER);
        for (var b : new Button[] {minus, plus}) {
            b.getStyleClass().add("stepper-button");
            b.setFocusTraversable(false);
        }
        minus.setGraphic(Icons.icon("minus", 16));
        plus.setGraphic(Icons.icon("plus", 16));
        minus.setAccessibleText("Menos");
        plus.setAccessibleText("Más");
        minus.setOnAction(e -> decrement());
        plus.setOnAction(e -> increment());
        valueLabel.getStyleClass().add("stepper-value");
        valueLabel.setMinWidth(36);
        valueLabel.setAlignment(Pos.CENTER);
        getChildren().addAll(minus, valueLabel, plus);
        value.addListener((o, a, b) -> sync());
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case UP, RIGHT, PLUS, ADD, EQUALS -> increment();
                case DOWN, LEFT, MINUS, SUBTRACT -> decrement();
                default -> {
                    return;
                }
            }
            e.consume();
        });
        value.set(clamp(initial));
        sync();
    }

    /** El valor; cambia solo dentro del rango. */
    public IntegerProperty valueProperty() {
        return value;
    }

    public int getValue() {
        return value.get();
    }

    public void setValue(int v) {
        value.set(clamp(v));
    }

    public void increment() {
        setValue(getValue() + 1);
    }

    public void decrement() {
        setValue(getValue() - 1);
    }

    /** Nombre que dice el lector de pantalla junto al valor ("Rounds del evento: 4"). */
    public void setName(String name) {
        this.name = name == null ? "" : name;
        sync();
    }

    public Button minusButton() {
        return minus;
    }

    public Button plusButton() {
        return plus;
    }

    public Label valueLabel() {
        return valueLabel;
    }

    private int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }

    private void sync() {
        int v = value.get();
        valueLabel.setText(String.valueOf(v));
        minus.setDisable(v <= min);
        plus.setDisable(v >= max);
        setAccessibleText(name.isEmpty() ? String.valueOf(v) : name + ": " + v);
    }
}
