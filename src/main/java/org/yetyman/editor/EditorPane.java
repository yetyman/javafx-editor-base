package org.yetyman.editor;

import binding.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.concurrent.atomic.AtomicReference;

public class EditorPane extends Pane {
    protected final ResizableCanvas bgCanvas = new ResizableCanvas();
    public final PaneOfManyPlanes transformationPane = new PaneOfManyPlanes();
    protected final ResizableCanvas fgCanvas = new ResizableCanvas();

    public final ObservableList<Node> items = transformationPane.getChildren();
    public final ObservableList<Node> anchors = FXCollections.observableArrayList();
    AtomicReference<Point2D> lastOffset = new AtomicReference<>();
    AtomicReference<Anchor> draggedAnchor = new AtomicReference<>();

    public final ObservableList<EditorItem> editorItems = FXCollections.observableArrayList();
    public final SimpleBooleanProperty renderTargetBounds = new SimpleBooleanProperty(false);

    public EditorPane(){
        getChildren().addAll(bgCanvas, transformationPane, fgCanvas);

        bgCanvas.setMouseTransparent(true);
        fgCanvas.setMouseTransparent(true);
        setOnDragDropped(evt->{
            evt.setDropCompleted(true);
        });
        setOnDragOver(evt->{
            evt.acceptTransferModes(TransferMode.ANY);
            Point2D lastMouse = new Point2D(evt.getX(), evt.getY());
            Anchor a = draggedAnchor.get();

            Point2D pt = transformationPane.planeManager.fromTo(Plane.screen, a.anchorPlane.get(), lastMouse.add(a.size.get().multiply(.5)).subtract(lastOffset.get()));
            a.location.set(pt);
        });
        anchors.addListener((ListChangeListener<Node>) c->{
            while(c.next()) {
                for (Node node : c.getRemoved()) {
                    getChildren().remove(node);
                }
                for (Node node : c.getAddedSubList()) {
                    getChildren().add(node);
                }
            }
        });
        transformationPane.planeManager.addOnRefreshListener(pm->{
            requestLayout();
        });
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        bgCanvas.resizeRelocate(0,0,getWidth(),getHeight());
        transformationPane.resizeRelocate(0,0,getWidth(),getHeight());
        fgCanvas.resizeRelocate(0,0,getWidth(),getHeight());

        //items should lay themselves out in the pomp,
        // anchors need to be positioned here.
        for (Node control : anchors) {
            Anchor a = (Anchor)control.getProperties().getOrDefault("ANCHOR", null);
            if (a != null) {
                Point2D pt = transformationPane.planeManager.fromTo(a.anchorPlane.get(), Plane.screen, a.location.get());
                control.resizeRelocate(pt.getX()-a.size.get().getX()/2, pt.getY()-a.size.get().getY()/2, a.size.get().getX(), a.size.get().getY());
            }
        }

        drawBackground(bgCanvas.getGraphicsContext2D());
        drawForeground(fgCanvas.getGraphicsContext2D());
    }

    protected void drawBackground(GraphicsContext gc){
        gc.clearRect(0,0, getWidth(), getHeight());

        if(renderTargetBounds.get()) {
            Bounds targetInScreen = transformationPane.planeManager.fromTo(Plane.target, Plane.screen, transformationPane.planeManager.targetSpace.get());
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeRect(targetInScreen.getMinX(), targetInScreen.getMinY(), targetInScreen.getWidth(), targetInScreen.getHeight());
            gc.setStroke(Color.TRANSPARENT);
            gc.setLineWidth(0);
        }


        for (EditorItem editorItem : editorItems) {
            editorItem.renderBackground(this.transformationPane.planeManager, gc);
        }

    }
    protected void drawForeground(GraphicsContext gc){
        gc.clearRect(0,0, getWidth(), getHeight());

        for (EditorItem editorItem : editorItems) {
            editorItem.renderForeground(this.transformationPane.planeManager, gc);
        }

    }

    public static PlaneSettings setPlane(Node child, Plane plane) {
        return PaneOfManyPlanes.setPlane(child, plane);
    }
    public static PlaneSettings setPlane(Node child, Plane plane, PlaneScale scale) {
        return PaneOfManyPlanes.setPlane(child, plane, scale);
    }

    public static PlaneSettings getPlaneSettings(Node n) {
        return PaneOfManyPlanes.getPlane(n);
    }

    public enum AnchorStyle { round, square };
    public Anchor createAnchor(AnchorStyle anchorStyle) {
        Anchor a = new Anchor();
        Button btn = (Button) a.ui.get();

        btn.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        btn.setPrefSize(a.size.get().getX(), a.size.get().getY());
        btn.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        btn.getProperties().put("ANCHOR", a);

        switch (anchorStyle) {
            case round -> {
                btn.setStyle("-fx-border-radius: 20; -fx-background-radius: 20; -fx-border-style: solid; -fx-border-width: .5;");
            }
            case square -> {
                btn.setStyle("-fx-border-radius: 0; -fx-background-radius: 0;");
            }
        }

        btn.setOnDragDetected(evt->{
            Dragboard d = btn.startDragAndDrop(TransferMode.ANY);
//            WritableImage image = btn.snapshot(new SnapshotParameters(), null);

//            d.setDragView(image, evt.getX()+1, evt.getY()+1);
            lastOffset.set(new Point2D(evt.getX(), evt.getY()));
            draggedAnchor.set(a);

            ClipboardContent content = new ClipboardContent();
            content.putString("Circle source text");
            d.setContent(content);
        });
        btn.setOnMouseDragged((MouseEvent event) -> {
            event.setDragDetect(true);
        });
        btn.setOnDragDone(evt->{
            draggedAnchor.set(null);
        });


        anchors.add(btn);

        return a;
    }
}
