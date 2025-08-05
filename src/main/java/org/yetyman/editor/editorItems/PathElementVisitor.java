package org.yetyman.editor.editorItems;

import binding.Property;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.*;
import org.yetyman.editor.*;

import java.util.List;

public class PathElementVisitor implements EditorItem {
    private EditorPane editor;
    private Anchor startCP;
    private Anchor endCP;
    private Anchor startOppCP;
    private Anchor endOppCP;

    private Anchor removeAnchor;

    private Anchor arcWidthCP;
    private Anchor arcHeightCP;
    private Anchor arcRotationCP;
    private Anchor arcSweepToggleCP;
    private Anchor arcLargeArcToggleCP;
    private List<Anchor> all;

    private final PathVisitor parent;
    final Property<Path> path = new Property<>(null);
    final Property<PathElement> element = new Property<>(null);
    int index;

    public PathElementVisitor(PathVisitor pathVisitor) {
        parent = pathVisitor;
    }

    @Override
    public void initialize(EditorPane editor) {
        this.editor = editor;
        editor.editorItems.add(this);

        startCP = editor.createAnchor(EditorPane.AnchorStyle.square);
        endCP = editor.createAnchor(EditorPane.AnchorStyle.square);

        startOppCP = editor.createAnchor(EditorPane.AnchorStyle.square);
        endOppCP = editor.createAnchor(EditorPane.AnchorStyle.square);

        //leaving this hell for another day
        //https://svg-tutorial.com/editor/arc
        arcWidthCP = editor.createAnchor(EditorPane.AnchorStyle.square);
        arcHeightCP = editor.createAnchor(EditorPane.AnchorStyle.square);
        arcRotationCP = editor.createAnchor(EditorPane.AnchorStyle.square);
        arcSweepToggleCP = editor.createAnchor(EditorPane.AnchorStyle.square);
        arcLargeArcToggleCP = editor.createAnchor(EditorPane.AnchorStyle.square);

        all = List.of(startCP, endCP, startOppCP, endOppCP, arcWidthCP, arcHeightCP, arcRotationCP, arcSweepToggleCP, arcLargeArcToggleCP);

        all.forEach(anchor -> anchor.ui.get().setVisible(false));

        element.bindOut(e->{
            cyclePrevent(this::refreshAnchors);
        });

        startCP.location.bindOut(p->{
            cyclePrevent(()->{
                adjustStartAnchor(element.get(), parent.calculateStartAnchor(element.get(), index).subtract(p));
            });
        });
        startOppCP.location.bindOut(p->{
            cyclePrevent(()->{
                adjustEndAnchor(parent.prevBefore(index), parent.calculateEndAnchor(parent.prevBefore(index), index-1).subtract(p));
            });
        });

        endCP.location.bindOut(p->{
            cyclePrevent(()->{
                adjustEndAnchor(element.get(), parent.calculateEndAnchor(element.get(), index).subtract(p));
            });
        });

    }

    @Override
    public void refreshAnchors() {
        cyclePrevent(()->{
            index = path.get().getElements().indexOf(element.get());

            this.startCP.ui.get().setVisible(hasStartAnchor(element.get()));
            this.endCP.ui.get().setVisible(hasEndAnchor(element.get()));

            PathElement prev = parent.prevBefore(index);
            PathElement next = parent.nextAfter(index);

            this.startOppCP.ui.get().setVisible(prev != null && hasEndAnchor(prev));
            this.endOppCP.ui.get().setVisible(next != null && hasStartAnchor(next));

            refreshStartAnchor();
            refreshEndAnchor();
            refreshStartOppAnchor();
            refreshEndOppAnchor();
        });
    }

    private void refreshEndOppAnchor() {
        PathElement next = parent.nextAfter(index);
        if (next != null && this.endOppCP.ui.get().isVisible())
            this.endOppCP.location.set(parent.calculateStartAnchor(next, index+1));
    }

    private void refreshStartOppAnchor() {
        PathElement prev = parent.prevBefore(index);
        if (prev != null && this.startOppCP.ui.get().isVisible())
            this.startOppCP.location.set(parent.calculateEndAnchor(prev, index - 1));
    }

    private void refreshEndAnchor() {
        if (this.endCP.ui.get().isVisible())
            this.endCP.location.set(parent.calculateEndAnchor(element.get(), index));
    }

    private void refreshStartAnchor() {
        if (this.startCP.ui.get().isVisible())
            this.startCP.location.set(parent.calculateStartAnchor(element.get(), index));
    }



    private boolean hasStartAnchor(PathElement pathElement) {
        return switch (pathElement) {
            case LineTo ignored -> false;
            case VLineTo ignored -> false;
            case HLineTo ignored -> false;
            case MoveTo ignored -> false;
            case ClosePath ignored -> false;
            case QuadCurveTo ignored -> true;
            case CubicCurveTo ignored -> true;
            case ArcTo ignored -> false;
            case null, default -> throw new RuntimeException("Unknown path element");
        };
    }
    private boolean hasEndAnchor(PathElement pathElement) {
        return switch (pathElement) {
            case LineTo ignored -> false;
            case VLineTo ignored -> false;
            case HLineTo ignored -> false;
            case MoveTo ignored -> false;
            case ClosePath ignored -> false;
            case QuadCurveTo ignored -> false;
            case CubicCurveTo ignored -> true;
            case ArcTo ignored -> false;
            case null, default -> throw new RuntimeException("Unknown path element");
        };
    }
    private void adjustEndAnchor(PathElement element, Point2D delta) {
        switch (element) {
            case CubicCurveTo cct -> {
                cct.setControlX2(cct.getControlX2()+delta.getX());
                cct.setControlY2(cct.getControlY2()+delta.getY());
            }
            case null, default -> throw new RuntimeException("Unknown path element");
        };
    }
    private void adjustStartAnchor(PathElement element, Point2D delta) {
        switch (element) {
            case CubicCurveTo cct -> {
                cct.setControlX1(cct.getControlX1()+delta.getX());
                cct.setControlY1(cct.getControlY1()+delta.getY());
            }
            case QuadCurveTo cct -> {
                cct.setControlX(cct.getControlX()+delta.getX());
                cct.setControlY(cct.getControlY()+delta.getY());
            }
            case null, default -> throw new RuntimeException("Unknown path element");
        };
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

    }

    @Override
    public void renderForeground(PlaneManager planes, GraphicsContext gc) {
        Point2D start = parent.getStart(element.get(), index);
        Point2D end = parent.calculateEnd(element.get(), index);
        if(startCP.ui.get().isVisible()) {
            strokeLine(gc, start, startCP.location.get());
        }
        if(startOppCP.ui.get().isVisible()) {
            strokeLine(gc, start, startOppCP.location.get());
        }
        if(endCP.ui.get().isVisible()) {
            strokeLine(gc, end, endCP.location.get());
        }
        if(endOppCP.ui.get().isVisible()) {
            strokeLine(gc, end, endOppCP.location.get());
        }
    }

    private void strokeLine(GraphicsContext gc, Point2D a, Point2D b) {
        a = editor.transformationPane.planeManager.fromTo(Plane.target, Plane.screen, a);
        b = editor.transformationPane.planeManager.fromTo(Plane.target, Plane.screen, b);
        gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
    }

    @Override
    public List<Node> getNodes() {
        return List.of(path.get());
    }

    @Override
    public List<Anchor> getAnchors() {
        return all;
    }

    public void changeUI(Path p, PathElement n) {
        //we don't currently need to do anything with the previous node. no bindings yet
        path.set(p);
        element.set(n);
    }
}
