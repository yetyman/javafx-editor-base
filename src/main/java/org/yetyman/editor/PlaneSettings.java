package org.yetyman.editor;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;

public record PlaneSettings(Node node, Plane plane, PlaneScale scale, SimpleObjectProperty<Bounds> boundary){};