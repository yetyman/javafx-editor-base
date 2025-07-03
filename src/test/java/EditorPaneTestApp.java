import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yetyman.editor.*;
import org.yetyman.editor.editorItems.BoxVisitor;

public class EditorPaneTestApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(EditorPaneTestApp.class);
    private Stage primaryStage;
    private EditorPane editor;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        //editor pane and some random anchors for cool shapes
        var controller = createEditorPaneTest();

        //all inclusive anchors and ui item
        createSampleWireFrame();

        //visitor that can hide show on any ui element
        var box = createSampleVisitor();
        var placeHolder1 = createPlaceHolder();
        var placeHolder2 = createPlaceHolder();
        EditorPane.setPlane(placeHolder2, Plane.target, PlaneScale.scale);
        var placeHolder3 = createPlaceHolder();
        EditorPane.getPlaneSettings(placeHolder1).boundary().set(new BoundingBox(-80,-80, 40,40));
        EditorPane.getPlaneSettings(placeHolder2).boundary().set(new BoundingBox(-80,40, 60,40));
        EditorPane.getPlaneSettings(placeHolder3).boundary().set(new BoundingBox(30,10, 70,40));

        placeHolder1.setOnMouseClicked(evt->box.changeUI(placeHolder1));
        placeHolder2.setOnMouseClicked(evt->box.changeUI(placeHolder2));
        placeHolder3.setOnMouseClicked(evt->box.changeUI(placeHolder3));

        controller.setPrefSize(600, 600);
//        controller.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        controller.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        this.primaryStage = primaryStage;
        this.primaryStage.setScene(new Scene(controller));
        primaryStage.show();
    }

    private Node createPlaceHolder() {
        FlowPane box = new FlowPane();
        box.setStyle("-fx-background-color: lightgray;");
        box.getChildren().add(new TextField("hello world"));
        box.getChildren().add(new Label("hello world"));
        box.getChildren().add(new CheckBox("hello world"));
        box.getChildren().add(new RadioButton("hello world"));
        box.getChildren().add(new RadioButton("hello world"));
        editor.items.add(box);
        EditorPane.setPlane(box, Plane.target, PlaneScale.size);
        return box;
    }

    private void createSampleWireFrame() {
        //create a box representation and controls on the editor
        //then add logic for hiding and showing the anchors!
        SampleBox box = new SampleBox();
        editor.editorItems.add(box);
        box.initialize(editor);
    }

    private BoxVisitor createSampleVisitor(){
        BoxVisitor box = new BoxVisitor();
        box.initialize(editor);
        return box;
    }

    private Region createEditorPaneTest() {
        editor = new EditorPane() {
            {
                transformationPane.planeManager.targetSpace.set(new BoundingBox(-100, -100, 200, 200));
            }
            @Override
            protected void drawForeground(GraphicsContext gc) {
                super.drawForeground(gc);

                gc.setLineWidth(.5);
                gc.setStroke(Color.BLUE);

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


}
