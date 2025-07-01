import javafx.application.Application;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.value.ObservableValue;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yetyman.controls.GridHelper;
import org.yetyman.editor.PaneOfManyPlanes;
import org.yetyman.editor.Plane;
import org.yetyman.editor.PlaneScale;
import org.yetyman.editor.PlaneSettings;

import static javafx.scene.layout.Region.USE_PREF_SIZE;

public class PaneOfManyPlanesTestApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(PaneOfManyPlanesTestApp.class);
    private Stage primaryStage;

    //putting all the bindings in fields so that they don't GC immediately
    private DoubleExpression screenHeight;
    private DoubleExpression targetHeightOnScreen;
    private DoubleExpression targetWidthOnScreen;
    private DoubleExpression screenWidth;
    private DoubleBinding divideHeight;
    private DoubleBinding divideWidth;
    private ObservableValue<Bounds> targetOnScreen;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        var controller = createPaneOfManyPlanesTestController();
        controller.setPrefSize(600, 600);
//        controller.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        controller.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        this.primaryStage = primaryStage;
        this.primaryStage.setScene(new Scene(controller));
        primaryStage.show();
    }

    private Region createPaneOfManyPlanesTestController() {
        ScrollBar vBar = new ScrollBar();
        ScrollBar hBar = new ScrollBar();
        PaneOfManyPlanes pomp = new PaneOfManyPlanes();


        vBar.setOrientation(Orientation.VERTICAL);
        hBar.setOrientation(Orientation.HORIZONTAL);

        targetOnScreen = pomp.planeManager.targetSpace.map(t -> pomp.planeManager.fromTo(Plane.target, Plane.screen, t));
        screenHeight = DoubleExpression.doubleExpression(pomp.planeManager.screenSpace.map(Bounds::getHeight));
        targetHeightOnScreen = DoubleExpression.doubleExpression(targetOnScreen.map(Bounds::getHeight));
        screenWidth = DoubleExpression.doubleExpression(pomp.planeManager.screenSpace.map(Bounds::getWidth));
        targetWidthOnScreen = DoubleExpression.doubleExpression(targetOnScreen.map(Bounds::getWidth));

        divideHeight = screenHeight.divide(targetHeightOnScreen);
        divideWidth = screenWidth.divide(targetWidthOnScreen);

        vBar.setMin(0);
        hBar.setMin(0);
        vBar.setMax(1);
        hBar.setMax(1);
        vBar.visibleAmountProperty().bind(divideHeight);
        hBar.visibleAmountProperty().bind(divideWidth);
        vBar.valueProperty().bindBidirectional(pomp.planeManager.panY);
        hBar.valueProperty().bindBidirectional(pomp.planeManager.panX);

        createBorderAndCenterSquare(pomp, Plane.screen, .2);
        createBorderAndCenterSquare(pomp, Plane.target, .15);
        createBorderAndCenterSquare(pomp, Plane.normal, .1);
        createBorderAndCenterSquare(pomp, Plane.percent, .05);

        GridPane gp = new GridPane();
        GridHelper.size(gp, 2,2);
        GridHelper.layout(gp, pomp, vBar, hBar, null);

        pomp.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        pomp.setMinSize(0, 0);

        gp.getColumnConstraints().getFirst().setFillWidth(true);
        gp.getColumnConstraints().getLast().setMaxWidth(USE_PREF_SIZE);

        gp.getRowConstraints().getFirst().setFillHeight(true);
        gp.getRowConstraints().getLast().setMaxHeight(USE_PREF_SIZE);
        return gp;
    }

    private static void createBorderAndCenterSquare(PaneOfManyPlanes pomp, Plane plane, double v) {
        String clr = switch (plane) {
            case screen -> "green";
            case normal -> "yellow";
            case percent -> "red";
            case target -> "blue";
        };

        Pane border = new Pane();
        border.setStyle("-fx-border-color: "+clr+"; -fx-border-style: dashed; -fx-border-width: "+(v*50)+";");
        pomp.getChildren().add(border);
        PlaneSettings settings4 = PaneOfManyPlanes.setPlane(border, plane, PlaneScale.scale);
        settings4.boundary().bind(switch (plane) {
            case screen -> pomp.planeManager.screenSpace;
            case normal -> pomp.planeManager.normalSpace;
            case percent -> pomp.planeManager.percentSpace;
            case target -> pomp.planeManager.targetSpace;
        });

        Pane pane = new Pane();
        pane.getChildren().add(new Label(plane.name()));
        pane.setStyle("-fx-background-color: "+clr+"; -fx-border-style: dashed;");
        pomp.getChildren().add(pane);
        PlaneSettings settings2 = PaneOfManyPlanes.setPlane(pane, plane, PlaneScale.size);

        Bounds bounds = pomp.getBounds(plane);
        settings2.boundary().set(new BoundingBox(bounds.getCenterX()-v*bounds.getWidth()/2, bounds.getCenterY()-v*bounds.getHeight()/2, v*bounds.getWidth(), v*bounds.getHeight()));

    }

}
