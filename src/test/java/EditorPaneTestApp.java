import binding.IBinding;
import javafx.application.Application;
import javafx.beans.binding.DoubleExpression;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yetyman.controls.GridHelper;
import org.yetyman.editor.*;

import java.util.List;

import static javafx.scene.layout.Region.USE_PREF_SIZE;

public class EditorPaneTestApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(EditorPaneTestApp.class);
    private Stage primaryStage;
    private EditorPane editor;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        var controller = createEditorPaneTest();
        createSampleWireFrame();

        controller.setPrefSize(600, 600);
//        controller.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        controller.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        this.primaryStage = primaryStage;
        this.primaryStage.setScene(new Scene(controller));
        primaryStage.show();
    }

    private void createSampleWireFrame() {
        //create a box representation and controls on the editor
        //then add logic for hiding and showing the anchors!
        sampleBox box = new sampleBox();
        editor.editorItems.add(box);
        box.initialize(editor);
    }

    private Region createEditorPaneTest() {
        editor = new EditorPane() {
            {
                transformationPane.planeManager.targetSpace.set(new BoundingBox(-100, -100, 200, 200));
            }
            @Override
            protected void drawForeground(GraphicsContext gc) {
                super.drawForeground(gc);

                gc.setLineWidth(1.5);
                gc.setStroke(Color.BLACK);

                for (int i = 0; i < 8; i++) {
                    Node anchor = anchors.get(i);
                    Anchor a1 = (Anchor) anchor.getProperties().getOrDefault("ANCHOR", null);
                    if (a1 != null) {

                        for (int j = 0; j < 8; j++) {
                            Node anchor2 = anchors.get(j);
                            Anchor a2 = (Anchor) anchor2.getProperties().getOrDefault("ANCHOR", null);
                            if (a2 != null) {

                                if (a1 != a2) {
                                    Point2D pt1 = transformationPane.planeManager.fromTo(a1.anchorPlane.get(), Plane.screen, a1.location.get());
                                    Point2D pt2 = transformationPane.planeManager.fromTo(a2.anchorPlane.get(), Plane.screen, a2.location.get());
                                    gc.strokeLine(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY());
                                }
                            }
                        }
                    }
                }
            }
        };

        Anchor a1 = editor.createAnchor(EditorPane.AnchorStyle.round);
        Anchor a2 = editor.createAnchor(EditorPane.AnchorStyle.round);
        Anchor a3 = editor.createAnchor(EditorPane.AnchorStyle.round);
        Anchor a4 = editor.createAnchor(EditorPane.AnchorStyle.round);
        Anchor a5 = editor.createAnchor(EditorPane.AnchorStyle.round);
        Anchor a6 = editor.createAnchor(EditorPane.AnchorStyle.round);
        Anchor a7 = editor.createAnchor(EditorPane.AnchorStyle.round);
        Anchor a8 = editor.createAnchor(EditorPane.AnchorStyle.round);

        a1.location.set(new Point2D(-40, -25));
        a2.location.set(new Point2D(-40,  25));
        a3.location.set(new Point2D(-25, 40));
        a4.location.set(new Point2D(25,  40));
        a5.location.set(new Point2D(40, 25));
        a6.location.set(new Point2D(40,  -25));
        a7.location.set(new Point2D(25, -40));
        a8.location.set(new Point2D(-25,  -40));

        Label l = new Label("I like butts");
        editor.items.add(l);
        PlaneSettings settings = EditorPane.setPlane(l, Plane.target, PlaneScale.scale);

        a1.location.bindOut(s->{
            settings.boundary().set(new BoundingBox(s.getX()-30, s.getY()-8, 60,16));
        }).push();

        return editor;
    }

    public class sampleBox implements EditorItem {

        private List<Anchor> anchors;
        private List<Node> nodes;
        private Anchor tl;
        private Anchor tr;
        private Anchor br;
        private Anchor bl;
        private Anchor c;
        private Anchor r;
        private PlaneSettings plane;

        @Override
        public void initialize(EditorPane pane) {
            FlowPane box = new FlowPane();
            box.setStyle("-fx-background-color: red;");
            box.getChildren().add(new TextField("hello world"));
            box.getChildren().add(new Label("hello world"));
            box.getChildren().add(new CheckBox("hello world"));
            box.getChildren().add(new RadioButton("hello world"));
            box.getChildren().add(new RadioButton("hello world"));
            editor.items.add(box);
            plane = EditorPane.setPlane(box, Plane.target, PlaneScale.size);

            tl = pane.createAnchor(EditorPane.AnchorStyle.square);
            tr = pane.createAnchor(EditorPane.AnchorStyle.square);
            br = pane.createAnchor(EditorPane.AnchorStyle.square);
            bl = pane.createAnchor(EditorPane.AnchorStyle.square);
            c = pane.createAnchor(EditorPane.AnchorStyle.round);
            r = pane.createAnchor(EditorPane.AnchorStyle.round);

            tl.ui.get().setStyle("-fx-base: blue;");
            bl.ui.get().setStyle("-fx-base: red;");
            br.ui.get().setStyle("-fx-base: green;");

            tl.location.set(new Point2D(-50,-50));
            br.location.set(new Point2D(50,50));
            tr.location.set(new Point2D(50,-50));
            bl.location.set(new Point2D(-50,50));

            anchors = List.of(tl, tr, br, bl, c, r);
            nodes = List.of(box);

            tr.location.bindOut(s->{
                cyclePrevent(()->{
                    Point2D hVec = br.location.get().subtract(bl.location.get());
                    Point2D vVec = bl.location.get().subtract(tl.location.get());

                    br.location.set(intersect(s, vVec, bl.location.get(), hVec));
                    tl.location.set(intersect(s, hVec, bl.location.get(), vVec));
                    updateCAndR();
                    updateBoxPane();
                });
            });
            bl.location.bindOut(s->{
                cyclePrevent(()->{
                    Point2D hVec = tr.location.get().subtract(tl.location.get());
                    Point2D vVec = br.location.get().subtract(tr.location.get());

                    br.location.set(intersect(s, hVec, tr.location.get(), vVec));
                    tl.location.set(intersect(s, vVec, tr.location.get(), hVec));
                    updateCAndR();
                    updateBoxPane();
                });
            });
            var brBind = br.location.bindOut(s->{
                cyclePrevent(()->{
                    Point2D hVec = tr.location.get().subtract(tl.location.get());
                    Point2D vVec = bl.location.get().subtract(tl.location.get());

                    bl.location.set(intersect(s, hVec, tl.location.get(), vVec));
                    tr.location.set(intersect(s, vVec, tl.location.get(), hVec));
                    updateCAndR();
                    updateBoxPane();
                });
            });
            var tlBind = tl.location.bindOut(s->{
                cyclePrevent(()->{
                    Point2D hVec = br.location.get().subtract(bl.location.get());
                    Point2D vVec = br.location.get().subtract(tr.location.get());

                    bl.location.set(intersect(s, vVec, br.location.get(), hVec));
                    tr.location.set(intersect(s, hVec, br.location.get(), vVec));
                    updateCAndR();
                    updateBoxPane();
                });
            });
            c.location.bindOut((s,a,b)->{
                cyclePrevent(()->{
                    Point2D diff = b.subtract(a);
                    tr.location.set(tr.location.get().add(diff));
                    br.location.set(br.location.get().add(diff));
                    bl.location.set(bl.location.get().add(diff));
                    tl.location.set(tl.location.get().add(diff));
                    updateCAndR();
                    updateBoxPane();
                });
            });
            r.location.bindOut((s,a,b)->{
               cyclePrevent(()->{
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

            brBind.forcePush();
            tlBind.forcePush();
        }

        private void updateBoxPane() {
            Point2D hVec = tl.location.get().subtract(tr.location.get());
            Point2D vVec = tl.location.get().subtract(bl.location.get());

            double hLen = hVec.magnitude();
            double vLen = vVec.magnitude();
            Bounds b = new BoundingBox(c.location.get().getX()-hLen/2,c.location.get().getY()-vLen/2, hLen, vLen);

            double angle = tl.location.get().angle(tr.location.get(), tl.location.get().add(new Point2D(1,0)));
            if(tr.location.get().getY() < tl.location.get().getY())
                angle = -angle;
            plane.node().setRotate(angle);
            plane.boundary().set(b);
        }

        private Point2D intersect(Point2D p1, Point2D n1, Point2D p2, Point2D n2)
        {
            Point2D p1End = p1.add(n1); // another point in line p1->n1
            Point2D p2End = p2.add(n2); // another point in line p2->n2

            double m1 = (p1End.getY() - p1.getY()) / (p1End.getX() - p1.getX()); // slope of line p1->n1
            double m2 = (p2End.getY() - p2.getY()) / (p2End.getX() - p2.getX()); // slope of line p2->n2

            double b1 = p1.getY() - m1 * p1.getX(); // y-intercept of line p1->n1
            double b2 = p2.getY() - m2 * p2.getX(); // y-intercept of line p2->n2

            double px = (b2 - b1) / (m1 - m2); // collision x
            double py = m1 * px + b1; // collision y

            if(Double.isInfinite(m1)) {
                //vertical line
                return new Point2D(p1.getX(), m2 * p1.getX() + p2.getY());
            }
            else if(Double.isInfinite(m2)) {
                //vertical line
                return new Point2D(p2.getX(), m1 * p2.getX() + p1.getY());
            }
            return new Point2D(px, py); // return statement
        }

        private void updateCAndR() {
            c.location.set(tr.location.get().midpoint(bl.location.get()));

            Point2D twenty = editor.transformationPane.planeManager.pxLengthInTargetSpace(0, 20);
            Point2D vec = tl.location.get().subtract(bl.location.get());
            double length = vec.magnitude()/2 + twenty.getY();

            r.location.set(c.location.get().add(vec.normalize().multiply(length)));
        }

        private boolean cyclePrevent = false;
        public void cyclePrevent(Runnable run) {
            if(cyclePrevent)
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
                Anchor a2 = anchors.get((i+1)%4);

                Point2D pt1 = planes.fromTo(a1.anchorPlane.get(), Plane.screen, a1.location.get());
                Point2D pt2 = planes.fromTo(a2.anchorPlane.get(), Plane.screen, a2.location.get());
                gc.strokeLine(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY());
            }

            Anchor a1 = anchors.get(4);
            Anchor a2 = anchors.get(5);

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
    }
}
