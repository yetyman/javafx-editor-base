package org.yetyman.editor;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;

public record PlaneSettings(Plane plane, PlaneScale scale, SimpleObjectProperty<Bounds> boundary){};