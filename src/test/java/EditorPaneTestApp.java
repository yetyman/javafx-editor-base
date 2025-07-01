import javafx.application.Application;
import javafx.beans.binding.DoubleExpression;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yetyman.controls.GridHelper;
import org.yetyman.editor.*;

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

                for (Node anchor : anchors) {
                    Anchor a1 = (Anchor)anchor.getProperties().getOrDefault("ANCHOR", null);
                    if(a1 != null) {

                        for (Node anchor2 : anchors) {
                            Anchor a2 = (Anchor)anchor2.getProperties().getOrDefault("ANCHOR", null);
                            if(a2 != null) {

                                if(a1 != a2) {
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
}
