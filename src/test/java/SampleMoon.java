import binding.Property;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import org.yetyman.editor.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SampleMoon implements EditorItem {

    private List<Anchor> anchors;
    private List<Node> nodes;
    private Anchor circle;
    private Anchor sub;
    private final Property<PlaneSettings> plane = new Property<>(null);
    private EditorPane editor;

    @Override
    public void initialize(EditorPane editor) {
        this.editor = editor;
        editor.editorItems.add(this);

        Circle full = new Circle(20,20, 20);
        Circle shadow = new Circle(40,20, 20);

        final Shape[] moon = {Shape.subtract(full, shadow)};
        moon[0].setStroke(Color.BLUE);
        moon[0].setStrokeWidth(2);
        moon[0].setFill(Color.TRANSPARENT);
        moon[0].setSmooth(true);
        moon[0].setStrokeLineCap(StrokeLineCap.ROUND);
        moon[0].setStrokeLineJoin(StrokeLineJoin.ROUND);
        plane.set(EditorPane.setPlane(moon[0], Plane.target, PlaneScale.scale));
        plane.get().boundary().set(new BoundingBox(30,-80, 40,40));

        editor.items.add(moon[0]);
        int index = editor.items.indexOf(moon[0]);

        circle = editor.createAnchor(EditorPane.AnchorStyle.square);
        sub = editor.createAnchor(EditorPane.AnchorStyle.square);

        circle.ui.get().setCursor(Cursor.NW_RESIZE);
        sub.ui.get().setCursor(Cursor.NE_RESIZE);

        circle.ui.get().setStyle("-fx-base: grey;");
        sub.ui.get().setStyle("-fx-base: #222;");

        circle.location.set(new Point2D(plane.get().boundary().get().getCenterX(), plane.get().boundary().get().getCenterY()));

        anchors = List.of(circle, sub);

        nodes = new ArrayList<>();
        nodes.add(null);

        circle.location.bindOut(s -> {
            cyclePrevent(() -> {
                plane.get().boundary().set(new BoundingBox( s.getX()-full.getRadius(), s.getY()-full.getRadius(), full.getRadius()*2, full.getRadius()*2));
                sub.location.set(circle.location.get().add(shadow.getCenterX()-full.getCenterX(), 0));
            });
        }).forcePush();

        sub.location.bindOut(s -> {
            cyclePrevent(() -> {
                shadow.setCenterX(sub.location.get().getX()-circle.location.get().getX()+20);

                moon[0] = Shape.subtract(full, shadow);
                moon[0].setStroke(Color.BLUE);
                moon[0].setStrokeWidth(2);
                moon[0].setFill(Color.TRANSPARENT);
                moon[0].setSmooth(true);
                moon[0].setStrokeLineCap(StrokeLineCap.ROUND);
                moon[0].setStrokeLineJoin(StrokeLineJoin.ROUND);

                plane.set(EditorPane.setPlane(moon[0], Plane.target, PlaneScale.scale));
                plane.get().boundary().set(new BoundingBox( circle.location.get().getX()-full.getRadius(), circle.location.get().getY()-full.getRadius(), full.getRadius()*2, full.getRadius()*2));
                editor.items.set(index, moon[0]);
            });
        });
    }

    private boolean cyclePrevent = false;

    public void cyclePrevent(Runnable run) {
        if (cyclePrevent)
            return;
        cyclePrevent = true;
        run.run();
        cyclePrevent = false;
    }

    @Override
    public void renderBackground(PlaneManager planes, GraphicsContext gc) {
//        Bounds b = planes.fromTo(Plane.target, Plane.screen, plane.get().boundary().get());
//        gc.strokeRect(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
    }

    @Override
    public void renderForeground(PlaneManager planes, GraphicsContext gc) {

    }

    @Override
    public List<Node> getNodes() {
        return nodes;
    }

    @Override
    public List<Anchor> getAnchors() {
        return anchors;
    }
}
