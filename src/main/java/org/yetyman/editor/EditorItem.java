package org.yetyman.editor;

import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

public interface EditorItem {
    /**
     * initialize anchors and the main item here.
     * including anchor movement listeners
     *
     * @param pane
     */
    void initialize(EditorPane editor);
    void renderBackground(PlaneManager planes, GraphicsContext gc);
    void renderForeground(PlaneManager planes, GraphicsContext gc);
    List<Node> getNodes();
    List<Anchor> getAnchors();
    void refreshAnchors();
}
