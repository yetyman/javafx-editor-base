package org.yetyman.editor.editorItems;

import binding.Property;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import org.yetyman.editor.*;

import java.util.ArrayList;
import java.util.List;

public class BoxVisitor implements EditorItem {

    private List<Anchor> anchors;
    private List<Node> nodes;
    private Anchor tl;
    private Anchor tr;
    private Anchor br;
    private Anchor bl;
    private Anchor c;
    private Anchor r;
    private final Property<PlaneSettings> plane = new Property<>(null);
    private EditorPane editor;

    @Override
    public void initialize(EditorPane editor) {
        this.editor = editor;
        editor.editorItems.add(this);

        tl = editor.createAnchor(EditorPane.AnchorStyle.square);
        tr = editor.createAnchor(EditorPane.AnchorStyle.square);
        br = editor.createAnchor(EditorPane.AnchorStyle.square);
        bl = editor.createAnchor(EditorPane.AnchorStyle.square);
        c = editor.createAnchor(EditorPane.AnchorStyle.round);
        r = editor.createAnchor(EditorPane.AnchorStyle.round);

        tl.ui.get().setCursor(Cursor.NW_RESIZE);
        tr.ui.get().setCursor(Cursor.NE_RESIZE);
        br.ui.get().setCursor(Cursor.SE_RESIZE);
        bl.ui.get().setCursor(Cursor.SW_RESIZE);
        c.ui.get().setCursor(Cursor.MOVE);
        r.ui.get().setCursor(Cursor.HAND);

        tl.ui.get().setStyle("-fx-base: blue;");
        bl.ui.get().setStyle("-fx-base: red;");
        br.ui.get().setStyle("-fx-base: green;");

        anchors = List.of(tl, tr, br, bl, c, r);

        nodes = new ArrayList<>();
        nodes.add(null);

        tr.location.bindOut(s -> {
            cyclePrevent(() -> {
                Point2D hVec = br.location.get().subtract(bl.location.get());
                Point2D vVec = bl.location.get().subtract(tl.location.get());

                br.location.set(intersect(s, vVec, bl.location.get(), hVec));
                tl.location.set(intersect(s, hVec, bl.location.get(), vVec));
                updateCAndR();
                updateBoxPane();
            });
        });
        bl.location.bindOut(s -> {
            cyclePrevent(() -> {
                Point2D hVec = tr.location.get().subtract(tl.location.get());
                Point2D vVec = br.location.get().subtract(tr.location.get());

                br.location.set(intersect(s, hVec, tr.location.get(), vVec));
                tl.location.set(intersect(s, vVec, tr.location.get(), hVec));
                updateCAndR();
                updateBoxPane();
            });
        });
        br.location.bindOut(s -> {
            cyclePrevent(() -> {
                Point2D hVec = tr.location.get().subtract(tl.location.get());
                Point2D vVec = bl.location.get().subtract(tl.location.get());

                bl.location.set(intersect(s, hVec, tl.location.get(), vVec));
                tr.location.set(intersect(s, vVec, tl.location.get(), hVec));
                updateCAndR();
                updateBoxPane();
            });
        });
        tl.location.bindOut(s -> {
            cyclePrevent(() -> {
                Point2D hVec = br.location.get().subtract(bl.location.get());
                Point2D vVec = br.location.get().subtract(tr.location.get());

                bl.location.set(intersect(s, vVec, br.location.get(), hVec));
                tr.location.set(intersect(s, hVec, br.location.get(), vVec));
                updateCAndR();
                updateBoxPane();
            });
        });
        c.location.bindOut((s, a, b) -> {
            cyclePrevent(() -> {
                Point2D diff = b.subtract(a);
                tr.location.set(tr.location.get().add(diff));
                br.location.set(br.location.get().add(diff));
                bl.location.set(bl.location.get().add(diff));
                tl.location.set(tl.location.get().add(diff));
                updateCAndR();
                updateBoxPane();
            });
        });
        r.location.bindOut((s, a, b) -> {
            cyclePrevent(() -> {
                Point2D hVec = tl.location.get().subtract(tr.location.get());
                Point2D vVec = tl.location.get().subtract(bl.location.get());

                double hLen = hVec.magnitude();
                double vLen = vVec.magnitude();

                Point2D newVVec = r.location.get().subtract(c.location.get()).normalize();
                Point2D newHVec = new Point2D(-newVVec.getY(), newVVec.getX()).normalize();

                hVec = newHVec.multiply(hLen);
                vVec = newVVec.multiply(vLen);

                tr.location.set(c.location.get().add(hVec.multiply(.5)).add(vVec.multiply(.5)));
                br.location.set(c.location.get().add(hVec.multiply(.5)).add(vVec.multiply(-.5)));
                tl.location.set(c.location.get().add(hVec.multiply(-.5)).add(vVec.multiply(.5)));
                bl.location.set(c.location.get().add(hVec.multiply(-.5)).add(vVec.multiply(-.5)));

                updateCAndR();
                updateBoxPane();
            });
        });
        plane.bindOut((s,a,b)->{
            cyclePrevent(() -> {
                if(b!=null) {
                    for (Anchor anchor : anchors) {
                        anchor.ui.get().setVisible(true);
                    }
                    //minorly biased here fitting the anchors to the box, rather than the box to anchors.
                    Bounds bounds = plane.get().boundary().get();
                    double rotation = plane.get().node().getRotate();

                    Point2D tlPt = new Point2D(bounds.getMinX(), bounds.getMinY());
                    Point2D trPt = new Point2D(bounds.getMaxX(), bounds.getMinY());
                    Point2D brPt = new Point2D(bounds.getMaxX(), bounds.getMaxY());
                    Point2D blPt = new Point2D(bounds.getMinX(), bounds.getMaxY());
                    Point2D cPt = new Point2D(bounds.getCenterX(), bounds.getCenterY());

                    Rotate rRot = new Rotate(rotation, cPt.getX(), cPt.getY());

                    tlPt = rRot.transform(tlPt);
                    trPt = rRot.transform(trPt);
                    brPt = rRot.transform(brPt);
                    blPt = rRot.transform(blPt);

                    tr.location.set(trPt);
                    br.location.set(brPt);
                    tl.location.set(tlPt);
                    bl.location.set(blPt);

                    updateCAndR();
                    updateBoxPane();
                } else {
                    for (Anchor anchor : anchors) {
                        anchor.ui.get().setVisible(false);
                    }
                }
            });
        }).forcePush();
    }

    private void updateBoxPane() {
        Point2D hVec = tl.location.get().subtract(tr.location.get());
        Point2D vVec = tl.location.get().subtract(bl.location.get());

        double hLen = hVec.magnitude();
        double vLen = vVec.magnitude();
        Bounds b = new BoundingBox(c.location.get().getX() - hLen / 2, c.location.get().getY() - vLen / 2, hLen, vLen);

        double angle = tl.location.get().angle(tr.location.get(), tl.location.get().add(new Point2D(1, 0)));
        if (tr.location.get().getY() < tl.location.get().getY())
            angle = -angle;
        plane.get().node().setRotate(angle);
        plane.get().boundary().set(b);
    }

    private Point2D intersect(Point2D p1, Point2D n1, Point2D p2, Point2D n2) {
        Point2D p1End = p1.add(n1); // another point in line p1->n1
        Point2D p2End = p2.add(n2); // another point in line p2->n2

        double m1 = (p1End.getY() - p1.getY()) / (p1End.getX() - p1.getX()); // slope of line p1->n1
        double m2 = (p2End.getY() - p2.getY()) / (p2End.getX() - p2.getX()); // slope of line p2->n2

        double b1 = p1.getY() - m1 * p1.getX(); // y-intercept of line p1->n1
        double b2 = p2.getY() - m2 * p2.getX(); // y-intercept of line p2->n2

        double px = (b2 - b1) / (m1 - m2); // collision x
        double py = m1 * px + b1; // collision y

        if (Double.isInfinite(m1)) {
            //vertical line
            return new Point2D(p1.getX(), m2 * p1.getX() + p2.getY());
        } else if (Double.isInfinite(m2)) {
            //vertical line
            return new Point2D(p2.getX(), m1 * p2.getX() + p1.getY());
        }
        return new Point2D(px, py); // return statement
    }

    private void updateCAndR() {
        c.location.set(tr.location.get().midpoint(bl.location.get()));

        Point2D twenty = editor.transformationPane.planeManager.pxLengthInTargetSpace(0, 20);
        Point2D vec = tl.location.get().subtract(bl.location.get());
        double length = vec.magnitude() / 2 + twenty.getY();

        r.location.set(c.location.get().add(vec.normalize().multiply(length)));
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
        for (int i = 0; i < 4; i++) {
            Anchor a1 = anchors.get(i);
            Anchor a2 = anchors.get((i + 1) % 4);

            if(!a1.ui.get().isVisible())
                continue;

            Point2D pt1 = planes.fromTo(a1.anchorPlane.get(), Plane.screen, a1.location.get());
            Point2D pt2 = planes.fromTo(a2.anchorPlane.get(), Plane.screen, a2.location.get());
            gc.strokeLine(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY());
        }

        Anchor a1 = anchors.get(4);
        Anchor a2 = anchors.get(5);

        if(!a1.ui.get().isVisible())
            return;

        Point2D pt1 = planes.fromTo(a1.anchorPlane.get(), Plane.screen, a1.location.get());
        Point2D pt2 = planes.fromTo(a2.anchorPlane.get(), Plane.screen, a2.location.get());
        gc.strokeLine(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY());
    }

    @Override
    public List<Node> getNodes() {
        return nodes;
    }

    @Override
    public List<Anchor> getAnchors() {
        return anchors;
    }

    public void changeUI(Node n) {
        //we don't currently need to do anything with the previous node. no bindings yet
        nodes.set(0, n);
        this.plane.set(EditorPane.getPlaneSettings(n));//plane binding will update locations
    }
}