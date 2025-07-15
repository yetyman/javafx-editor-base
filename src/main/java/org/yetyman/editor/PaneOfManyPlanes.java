package org.yetyman.editor;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class PaneOfManyPlanes extends Pane {
    private static final Logger log = LoggerFactory.getLogger(PaneOfManyPlanes.class);

    public final PlaneManager planeManager = new PlaneManager();

    public PaneOfManyPlanes(){
        planeManager.addOnRefreshListener(s-> {
            hideOffscreenNodes();
            requestLayout();
        });

        addEventHandler(ScrollEvent.ANY, evt->{
            Point2D focus = new Point2D(evt.getX(), evt.getY()); //planeManager.inNormalSpace(new Point2D(evt.getX(), evt.getY()));
            double zoom = (evt.getDeltaX() + evt.getDeltaY())/100d + 1d;

            planeManager.zoom(zoom, zoom, focus.getX(), focus.getY());
        });

        AtomicReference<Point2D> pt = new AtomicReference<>();
        addEventHandler(MouseEvent.MOUSE_PRESSED, evt->{
            pt.set(new Point2D(evt.getX(), evt.getY()));
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, evt->{
            Point2D pt2 = new Point2D(evt.getX(), evt.getY());
            Point2D delta = pt2.subtract(pt.get());
            pt.set(pt2);

            planeManager.pan(delta.getX(), delta.getY());
        });
    }

    public Bounds getBounds(Plane plane) {
        return switch (plane) {
            case screen -> planeManager.screenSpace.get();
            case normal -> planeManager.normalSpace.get();
            case percent -> planeManager.percentSpace.get();
            case target -> planeManager.targetSpace.get();
        };
    }


    @Override
    public void resize(double width, double height) {
        super.resize(width, height);

        planeManager.screenSpace.set(new BoundingBox(0,0, width, height));
    }


    public static PlaneSettings getPlane(Node child) {
        return (PlaneSettings) child.getProperties().get("PLANE");
    }

    public static PlaneSettings setPlane(Node child, Plane plane) {
        return setPlane(child, plane, PlaneScale.size);
    }
    public static PlaneSettings setPlane(Node child, Plane plane, PlaneScale scale) {
        if(plane == null || scale == null)
            throw new RuntimeException("plane and scale cannot be null");

        PlaneSettings settings = new PlaneSettings(child, plane, scale, new SimpleObjectProperty<>(new BoundingBox(10,10,100,100)));
        child.getProperties().put("PLANE", settings);

        //set up request layout event
        if(child.getParent() instanceof PaneOfManyPlanes pomp)
            pomp.requestLayout();
        settings.boundary().addListener(s->{
            if(child.getParent() instanceof PaneOfManyPlanes pomp)
                pomp.requestLayout();
        });

        //remove these settings when removed from this parent
        AtomicReference<ChangeListener<Parent>> listener = new AtomicReference<>(null);
        ChangeListener<Parent> parentChange = (s, a, b)->{
            if(b == null) {
                child.getProperties().remove("PLANE");
                child.parentProperty().removeListener(listener.get());
            }
        };
        listener.set(parentChange);
        child.parentProperty().addListener(parentChange);

        return settings;
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        Map<Plane, Bounds> scale = new HashMap<>();

        for (Node child : new ArrayList<>(getChildren())) {
            if(!child.isManaged())
                continue;

            PlaneSettings settings = (PlaneSettings) child.getProperties().get("PLANE");

            if(settings == null || settings.boundary().get() == null)
                continue;

            Bounds scaleUnit = scale.computeIfAbsent(settings.plane(), p->{
                Bounds unit = new BoundingBox(0,0,1,1);
                Bounds screenUnit = planeManager.fromTo(settings.plane(), Plane.screen, unit);
                return screenUnit;
            });

            if(settings.scale() == PlaneScale.scale) {
                Bounds screenB = planeManager.fromTo(settings.plane(), Plane.screen, settings.boundary().get());
                Bounds planeB = settings.boundary().get();

                //apply the effect of the matrix rather than the whole matrix directly
                if (child.getTransforms().isEmpty()) {
                    child.getTransforms().add(new Scale(scaleUnit.getWidth(), scaleUnit.getHeight()));
                } else {
                    child.getTransforms().set(0, new Scale(scaleUnit.getWidth(), scaleUnit.getHeight()));
                }

                child.resizeRelocate(screenB.getMinX(), screenB.getMinY(), planeB.getWidth(), planeB.getHeight());
            } else {
                //layout within boundary
                Bounds b = planeManager.fromTo(settings.plane(), Plane.screen, settings.boundary().get());
                child.resizeRelocate(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
            }
        }
    }

    private void hideOffscreenNodes() {
        for (Node child : new ArrayList<>(getChildren())) {
            if (!child.isManaged())
                continue;
            PlaneSettings settings = (PlaneSettings) child.getProperties().get("PLANE");

            if(settings == null || settings.boundary().get() == null)
                continue;

            Bounds b = planeManager.fromTo(settings.plane(), Plane.screen, settings.boundary().get());
            Bounds screen = planeManager.screenSpace.get();
            child.setVisible(screen.intersects(b));
        }
    }
}
