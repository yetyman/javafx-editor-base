package org.yetyman.editor.editorItems;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.shape.*;
import org.apache.commons.math3.util.FastMath;
import org.yetyman.editor.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PathVisitor implements EditorItem {
    public final SimpleObjectProperty<Path> path = new SimpleObjectProperty<>(null);
    private final Map<PathElement, PathElementPosition> mappedElements = new ConcurrentHashMap<>();
    private final PathElementVisitor pathElementVisitor = new PathElementVisitor(this);

    public ObservableList<Anchor> anchors = FXCollections.observableArrayList();
    private Anchor addAnchor;
    private EditorPane editor;

    @Override
    public void initialize(EditorPane editor) {
        this.editor = editor;
        addAnchor = editor.createAnchor(EditorPane.AnchorStyle.square);//hovering an anchor will show the add anchor next to it.

        ((Button)addAnchor.ui.get()).setText("+");
        ((Button)addAnchor.ui.get()).setPadding(Insets.EMPTY);

        path.addListener(this::bindToElements);
        pathElementVisitor.initialize(editor);
    }

    @Override
    public void renderBackground(PlaneManager planes, GraphicsContext gc) {
    }

    @Override
    public void renderForeground(PlaneManager planes, GraphicsContext gc) {
        //for each element, if it is invisible (move to) then render a light blue line showing it.
        if(addAnchor.ui.get().isVisible()) {
            Point2D start = getStart(pathElementVisitor.element.get(), pathElementVisitor.index);
            strokeLine(gc, start, addAnchor.location.get());
        }
    }

    private void strokeLine(GraphicsContext gc, Point2D a, Point2D b) {
        a = editor.transformationPane.planeManager.fromTo(Plane.target, Plane.screen, a);
        b = editor.transformationPane.planeManager.fromTo(Plane.target, Plane.screen, b);
        gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
    }
    @Override
    public List<Node> getNodes() {
        return List.of();
    }

    @Override
    public List<Anchor> getAnchors() {
        return List.of();
    }

    @Override
    public void refreshAnchors() {
        cyclePrevent(()-> {

        });
    }

    public void changeUI(Path p) {
        //we don't currently need to do anything with the previous node. no bindings yet
        path.set(p);

        pathElementVisitor.changeUI(p, p.getElements().getFirst());
    }

    private void bindToElements(ObservableValue<? extends Path> s, Path a, Path b) {
        if (a != null) {
            path.get().getElements().removeListener(elementListener);
        }
        if (b != null) {
            path.get().getElements().addListener(elementListener);
            refreshPositions();
        }
    }

    private final ListChangeListener<PathElement> elementListener = c -> {
        int earliestChangeIndex = path.get().getElements().size();
        while(c.next()) {
            earliestChangeIndex = FastMath.min(earliestChangeIndex, c.getFrom());
            earliestChangeIndex = FastMath.min(earliestChangeIndex, c.getTo());

            for (PathElement pathElement : c.getRemoved()) {
                PathElementPosition pep = mappedElements.remove(pathElement);
                if(pep != null)
                    editor.removeAnchor(pep.endAnchor);
            }
            for (PathElement pathElement : c.getAddedSubList()) {
                PathElementPosition pep = new PathElementPosition(pathElement, 0, null, null);
                Anchor anchor = editor.createAnchor(EditorPane.AnchorStyle.round);
                //TODO: setup anchor binding
                anchor.location.bindOut(pt->{
                    //TODO: move actual end position on drag. already have some logic.
                    setEnd(pep.pathElement, pt);
                    refreshFocusedPathElement(pep.pathElement);
                    refreshAddAnchor();
                });

                anchor.ui.get().setOnMouseMoved(evt->{
                    refreshAddAnchor();
                });
                pep.endAnchor = anchor;

                mappedElements.put(pathElement, pep);
            }
        }

        for(; earliestChangeIndex < path.get().getElements().size(); earliestChangeIndex++)
        {
            PathElement pe = path.get().getElements().get(earliestChangeIndex);
            PathElementPosition pep = mappedElements.get(pe);

            pep.index = earliestChangeIndex;
            pep.startPosition = null;
        }

        refreshPositions();//will populate the peps
    };

    private void setEnd(PathElement pathElement, Point2D pt) {
        Point2D start = getStart(pathElement, mappedElements.get(pathElement).index);
        if(!pathElement.isAbsolute())
            pt = pt.subtract(start);

        switch (pathElement) {
            case MoveTo mt -> {
                mt.setX(pt.getX());
                mt.setY(pt.getY());
            }
            case LineTo mt -> {
                mt.setX(pt.getX());
                mt.setY(pt.getY());
            }
            case VLineTo mt -> {
                mt.setY(pt.getY());
            }
            case HLineTo mt -> {
                mt.setX(pt.getX());
            }
            case QuadCurveTo mt -> {
                mt.setX(pt.getX());
                mt.setY(pt.getY());
            }
            case CubicCurveTo mt -> {
                //move the 2nd control point along with it
                mt.setControlX2(mt.getControlX2()+(pt.getX() - mt.getX()));
                mt.setControlY2(mt.getControlY2()+(pt.getY() - mt.getY()));
                mt.setX(pt.getX());
                mt.setY(pt.getY());
            }
            case ArcTo mt -> {
                mt.setX(pt.getX());
                mt.setY(pt.getY());
            }
            default -> {
            }
        }
    }

    private void refreshFocusedPathElement(PathElement pathElement) {
        pathElementVisitor.changeUI(path.get(), pathElement);
        pathElementVisitor.refreshAnchors();
    }

    private static class PathElementPosition {
        public final PathElement pathElement;
        public Anchor endAnchor;
        public int index;
        public Point2D startPosition;

        private PathElementPosition(PathElement pathElement, int index, Point2D startPosition, Anchor endAnchor) {
            this.pathElement = pathElement;
            this.index = index;
            this.startPosition = startPosition;
            this.endAnchor = endAnchor;
        }
    }

    private void refreshPositions() {
        calculateEnd(path.get().getElements().getLast(), path.get().getElements().size());
    }


    PathElement prevBefore(int index) {
        if(index > 0)
            return path.get().getElements().get(index-1);
        else
            return null;
    }

    PathElement nextAfter(int index) {
        if(index+1 < path.get().getElements().size())
            return path.get().getElements().get(index+1);
        else
            return null;
    }

    Point2D firstPosition(Path path) {
        Point2D pt = Point2D.ZERO;
        for (PathElement e : path.getElements()) {
            if(e instanceof MoveTo mt) {
                Point2D d = endXY(mt);
                if (mt.isAbsolute())
                    pt = d;
                else
                    pt = pt.add(d);
            } else
                break;
        }
        return pt;
    }

    Point2D getStart(PathElement element, int index) {
        if (mappedElements.get(element).startPosition == null) {
            calculateStart(element, index);
        }
        if(index == 0)
            return Point2D.ZERO;
        else
            return mappedElements.get(element).startPosition;
    }
    Point2D calculateStart(PathElement pathElement, int index) {
        ListIterator<PathElement> iter = path.get().getElements().listIterator(index);

        return calculateStart(pathElement, index, iter);
    }

    private Point2D calculateStart(PathElement ptrElement, int index, ListIterator<PathElement> iter) {
        if(ptrElement == null)
            return Point2D.ZERO;

        if(!ptrElement.isAbsolute() && iter.hasPrevious()) {
            PathElement pre = iter.previous();
            Point2D thisStart = calculateEnd(pre, index-1);

            PathElementPosition pep = mappedElements.get(ptrElement);
            pep.startPosition = thisStart;
            pep.index = index;

            return thisStart;
        } else
            return Point2D.ZERO;
    }

    Point2D calculateEnd(PathElement pathElement, int index) {
        Point2D pos;
        if(pathElement instanceof ClosePath) {
            pos = firstPosition(path.get());
        } else if(pathElement.isAbsolute()) {
            pos = endXY(pathElement);
        } else {
            pos = getStart(pathElement, index).add(endXY(pathElement));
        }

        PathElementPosition pep = mappedElements.get(pathElement);
        pep.endAnchor.location.set(pos);

        return pos;
    }
    Point2D calculateStartAnchor(PathElement pathElement, int index) {
        Point2D change = startAnchorXY(pathElement);
        return pathElement.isAbsolute() ? change : getStart(pathElement, index).add(change);
    }
    Point2D calculateEndAnchor(PathElement pathElement, int index) {
        Point2D change = endAnchorXY(pathElement);
        return pathElement.isAbsolute() ? change : getStart(pathElement, index).add(change);
    }

    Point2D endXY(PathElement pathElement) {
        return switch (pathElement) {
            case LineTo lt -> new Point2D(lt.getX(), lt.getY());
            case VLineTo vlt -> new Point2D(0, vlt.getY());
            case HLineTo hlt -> new Point2D(hlt.getX(), 0);
            case QuadCurveTo qct -> new Point2D(qct.getX(), qct.getY());
            case CubicCurveTo cct -> new Point2D(cct.getX(), cct.getY());
            case MoveTo mt -> new Point2D(mt.getX(), mt.getY());
            case ArcTo at -> new Point2D(at.getX(), at.getY());
            case ClosePath ignored -> Point2D.ZERO;
            case null, default -> throw new RuntimeException("Unknown path element");
        };
    }

    Point2D startAnchorXY(PathElement pathElement) {
        if(pathElement instanceof CubicCurveTo cct)
            return new Point2D(cct.getControlX1(), cct.getControlY1());
        if(pathElement instanceof QuadCurveTo cct)
            return new Point2D(cct.getControlX(), cct.getControlY());
        else
            return null;
    }
    Point2D endAnchorXY(PathElement pathElement) {
        if(pathElement instanceof CubicCurveTo cct)
            return new Point2D(cct.getControlX2(), cct.getControlY2());
        else
            return null;
    }

    private boolean cyclePrevent = false;
    public void cyclePrevent(Runnable run) {
        if (cyclePrevent)
            return;
        cyclePrevent = true;
        run.run();
        cyclePrevent = false;
    }

    private void refreshAddAnchor() {
        Point2D pxLength = editor.transformationPane.planeManager.pxLengthInTargetSpace(this.addAnchor.size.get().getX(),this.addAnchor.size.get().getY());
        Point2D start = getStart(pathElementVisitor.element.get(), pathElementVisitor.index);

        Point2D vecBA = Point2D.ZERO;
        for (Anchor anchor : pathElementVisitor.getAnchors()) {
            if(anchor.ui.get().isVisible()) {
                Point2D vec = start.subtract(anchor.location.get());
                if(vec.getY() > 0)
                    vec = vec.multiply(-1);

                vecBA = vecBA.add(vec);
            }
        }
        Point2D addStartVec = new Point2D(-vecBA.getY(), vecBA.getX()).normalize().multiply(pxLength.magnitude()*1.5);
        this.addAnchor.location.set(start.add(addStartVec));
    }


}
