package org.yetyman.editor;

import com.sun.glass.ui.Clipboard;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlaneManager {

    private static final Logger log = LoggerFactory.getLogger(PaneOfManyPlanes.class);

    public ObjectProperty<Bounds> screenSpace = new SimpleObjectProperty<>(new BoundingBox(0,0,100,100));
    public ObjectProperty<Bounds> normalSpace = new SimpleObjectProperty<>(new BoundingBox(0,0,1,1));
    public ObjectProperty<Bounds> percentSpace = new SimpleObjectProperty<>(new BoundingBox(0,0,1,1));
    public ObjectProperty<Bounds> targetSpace = new SimpleObjectProperty<>(new BoundingBox(1000, 1000, 1000,1000));

    //always fits with just a zoom + translate
    //a transform takes up very little memory, but making them is comparatively more expensive, cache each transform.
    private Transform targetPlaneToScreen = new Affine();
    private Transform normalPlaneToScreen = new Affine();
    private Transform percentPlaneToScreen = new Affine();
    private Transform screenPlaneToNormal = new Affine();
    private Transform screenPlaneToTarget = new Affine();
    private Transform screenPlaneToPercent = new Affine();
    private Transform normalPlaneToTarget = new Affine();
    private Transform normalPlaneToPercent = new Affine();
    private Transform percentPlaneToTarget = new Affine();
    private Transform percentPlaneToNormal = new Affine();
    private Transform targetPlaneToNormal = new Affine();
    private Transform targetPlaneToPercent = new Affine();

    private Point2D screenOriginInTarget = new Point2D(0,0);
    private Point2D screenOriginInNormal = new Point2D(0,0);
    private Point2D screenOriginInPercent = new Point2D(0,0);

    public ObjectProperty<BoundaryFit> targetPlaneFit = new SimpleObjectProperty<>(BoundaryFit.fit_largest);

    private boolean prevent = false;//allows grouped changes
    public final DoubleProperty zoomX = new SimpleDoubleProperty(1);
    public final DoubleProperty zoomY = new SimpleDoubleProperty(1);
    public final DoubleProperty panX = new SimpleDoubleProperty(0);
    public final DoubleProperty panY = new SimpleDoubleProperty(0);
    private List<Consumer<PlaneManager>> onRefreshListeners = new ArrayList<>();

    public PlaneManager(){
        screenSpace.addListener(s-> refresh());
        targetSpace.addListener(s-> refresh());
        targetPlaneFit.addListener(s-> refresh());

        zoomX.addListener(s-> refresh());
        panX.addListener(s-> refresh());
        panY.addListener(s-> refresh());
    }

    private void refresh(){
        if(prevent)
            return;

        Bounds screen = screenSpace.get();
        this.normalSpace.set(new BoundingBox(0,0, FastMath.max(1, screen.getWidth()/screen.getHeight()), FastMath.max(1, screen.getHeight() / screen.getWidth())));
        Bounds normal = this.normalSpace.get();
        Bounds target = targetSpace.get();
        Bounds percent = new BoundingBox(0,0,1,1);//unused lol
        BoundaryFit fit = targetPlaneFit.get();
        double zoomX = this.zoomX.get();
        double zoomY = this.zoomY.get();
        double panX = this.panX.get();
        double panY = this.panY.get();

        double normalAR = normal.getWidth()/normal.getHeight();
        double targetAR = target.getWidth()/target.getHeight();

        double widthMult = 1, heightMult = 1;
        switch (fit) {
            case fit_width -> {
                widthMult = target.getWidth()/normal.getWidth();
                heightMult = widthMult;
            }
            case fit_height -> {
                heightMult = target.getHeight()/normal.getHeight();
                widthMult = heightMult;
            }
            case fit_smallest -> {
                if(targetAR > normalAR) {
                    heightMult = target.getHeight()/normal.getHeight();
                    widthMult = heightMult;
                } else {
                    widthMult = target.getWidth()/normal.getWidth();
                    heightMult = widthMult;
                }
            }
            case fit_largest -> {
                if(targetAR < normalAR) {
                    heightMult = target.getHeight()/normal.getHeight();
                    widthMult = heightMult;
                } else {
                    widthMult = target.getWidth()/normal.getWidth();
                    heightMult = widthMult;
                }
            }
            case stretch -> {
                widthMult = target.getWidth() / normal.getWidth();
                heightMult = target.getHeight() / normal.getHeight();
            }
        }


        //always aspect ratio
        Scale normalToScreen = new Scale(screen.getWidth() / normal.getWidth(), screen.getHeight() / normal.getHeight(),0,0);

        double xAdjust = normalAR > 1 ? (targetAR - normalAR)/2*target.getWidth() : 0;
        double yAdjust = normalAR < 1 ? (1 / targetAR - 1 / normalAR)/2*target.getHeight() : 0;

        Transform normalToTarget = new Scale(widthMult, heightMult, 0, 0);
        normalToTarget = new Translate(target.getMinX()+xAdjust, target.getMinY()+yAdjust).createConcatenation(normalToTarget);

        Scale normalToPercent = new Scale(1/normal.getWidth(), 1/normal.getHeight(), 0,0);

        //happens in normal space
        Scale zoom = new Scale(zoomX, zoomY, normal.getWidth()/2, normal.getHeight()/2);
        Translate translate = new Translate(-panX, -panY);

        try {
            normalPlaneToScreen = translate.createConcatenation(zoom).createConcatenation(normalToScreen);
            targetPlaneToScreen = normalPlaneToScreen.createConcatenation(normalToTarget.createInverse());
            percentPlaneToScreen = normalToScreen.createConcatenation(normalToPercent.createInverse());

            screenPlaneToNormal = normalPlaneToScreen.createInverse();
            screenPlaneToTarget = targetPlaneToScreen.createInverse();
            screenPlaneToPercent = percentPlaneToScreen.createInverse();

            normalPlaneToPercent = screenPlaneToPercent.createConcatenation(normalPlaneToScreen);
            normalPlaneToTarget  = screenPlaneToTarget.createConcatenation(normalPlaneToScreen);
            targetPlaneToPercent = screenPlaneToPercent.createConcatenation(targetPlaneToScreen);
            targetPlaneToNormal  = screenPlaneToNormal.createConcatenation(targetPlaneToScreen);
            percentPlaneToTarget = screenPlaneToTarget.createConcatenation(percentPlaneToScreen);
            percentPlaneToNormal = screenPlaneToNormal.createConcatenation(percentPlaneToScreen);

            screenOriginInTarget = screenPlaneToTarget.transform(0, 0);
            screenOriginInNormal = screenPlaneToNormal.transform(0, 0);
            screenOriginInPercent = screenPlaneToPercent.transform(0, 0);
        } catch (Exception ex) {
            log.error("Could not define transformations between planes", ex);
        }
    }

    public Point2D pxLengthInTargetSpace(double x, double y) {
        Point2D targetB = screenPlaneToTarget.transform(x, y);
        return targetB.subtract(screenOriginInTarget);
    }
    public Point2D pxLengthInNormalSpace(double x, double y) {
        Point2D normalB = screenPlaneToNormal.transform(x, y);
        return normalB.subtract(screenOriginInNormal);
    }
    public Point2D pxLengthInPercentSpace(double x, double y) {
        Point2D percentB = screenPlaneToPercent.transform(x, y);
        return percentB.subtract(screenOriginInPercent);
    }

    //will need to add more overloads of these
    public Point2D inTargetSpace(Point2D screenPt) {
        return screenPlaneToTarget.transform(screenPt);
    }
    public Point2D inNormalSpace(Point2D screenPt) {
        return screenPlaneToNormal.transform(screenPt);
    }
    public Point2D inPercentSpace(Point2D screenPt) {
        return screenPlaneToPercent.transform(screenPt);
    }

    public Point2D targetPtOnScreen(Point2D targetPt) {
        return targetPlaneToScreen.transform(targetPt);
    }
    public Point2D normalPtOnScreen(Point2D normalPt) {
        return normalPlaneToScreen.transform(normalPt);
    }
    public Point2D percentPtOnScreen(Point2D percentPt) {
        return percentPlaneToScreen.transform(percentPt);
    }

    /**
     * relative zoom. for absolute zoom, use setZoom
     * @param scale the scale, in multiplication, of zoom relative to current zoom. zoom *= scale
     */
    public void zoom(double scale) {
        zoom(scale, scale);
    }
    /**
     * relative zoom. for absolute zoom, use setZoom
     * @param scaleX the scale, in multiplication, of zoom relative to current zoom. zoom *= scale
     * @param scaleY the scale, in multiplication, of zoom relative to current zoom. zoom *= scale
     */
    public void zoom(double scaleX, double scaleY) {
        setZoom(zoomX.get()*scaleX, zoomY.get()*scaleY);
    }
    public void setZoom(double scale) {
        setZoom(scale, scale);
    }
    public void setZoom(double scaleX, double scaleY) {
        prevent = true;
        zoomX.set(scaleX);
        zoomY.set(scaleY);
        prevent = false;
        refresh();
    }

    /**
     * relative pan. for absolute pan, use setPan
     * @param moveX the move, in addition, of pan relative to current pan. pan += move
     * @param moveY the move, in addition, of pan relative to current pan. pan += move
     */
    public void pan(double moveX, double moveY) {
        setPan(panX.get()*moveX, panY.get()*moveY);
    }
    public void setPan(double x, double y) {
        prevent = true;
        panX.set(x);
        panY.set(y);
        prevent = false;
        refresh();
    }

    public double getPanX() {
        return panX.get();
    }

    public double getPanY() {
        return panY.get();
    }

    /**
     * relative pan. for absolute pan, use setPan
     * @param moveX the move, in addition, of pan relative to current pan. pan += move
     * @param moveY the move, in addition, of pan relative to current pan. pan += move
     * @param scale the scale, in multiplication, of zoom relative to current zoom. zoom *= scale
     */
    public void pz(double moveX, double moveY, double scale) {
        setPZ(moveX, moveY, scale, scale);
    }
    /**
     * relative pan. for absolute pan, use setPan
     * @param moveX the move, in addition, of pan relative to current pan. pan += move
     * @param moveY the move, in addition, of pan relative to current pan. pan += move
     * @param scaleX the scale, in multiplication, of zoom relative to current zoom. zoom *= scale
     * @param scaleY the scale, in multiplication, of zoom relative to current zoom. zoom *= scale
     */
    public void pz(double moveX, double moveY, double scaleX, double scaleY) {
        setPZ(panX.get()*moveX, panY.get()*moveY,zoomX.get()*scaleX, zoomY.get()*scaleY);
    }
    public void setPZ(double panX, double panY, double scaleX, double scaleY) {
        prevent = true;
        zoomX.set(scaleX);
        zoomY.set(scaleY);
        this.panX.set(panX);
        this.panY.set(panY);
        prevent = false;
        refresh();
    }

    public Consumer<PlaneManager> addOnRefreshListener(Consumer<PlaneManager> onRefresh) {
        onRefreshListeners.add(onRefresh);
        return onRefresh;
    }

    public boolean removeOnRefreshListener(Consumer<PlaneManager> onRefresh) {
        return onRefreshListeners.remove(onRefresh);
    }

    private static final Transform IDENTITY = new Translate(0,0);

    public Point2D fromTo(Plane from, Plane to, Point2D pt) {
        return fromTo(from, to).transform(pt);
    }
    public Bounds fromTo(Plane from, Plane to, Bounds bounds) {
        return fromTo(from, to).transform(bounds);
    }
    public Transform fromTo(Plane from, Plane to) {
         return switch (from) {
            case screen ->
                switch (to) {
                    case screen -> IDENTITY;
                    case normal -> screenPlaneToNormal;
                    case percent -> screenPlaneToPercent;
                    case target -> screenPlaneToTarget;
                };
            case normal ->
                switch (to) {
                    case screen -> normalPlaneToScreen;
                    case normal -> IDENTITY;
                    case percent -> normalPlaneToPercent;
                    case target -> normalPlaneToTarget;
                };
            case percent ->
                switch (to) {
                    case screen -> percentPlaneToScreen;
                    case normal -> percentPlaneToNormal;
                    case percent -> IDENTITY;
                    case target -> percentPlaneToTarget;
                };
             case target ->
                switch (to) {
                    case screen -> targetPlaneToScreen;
                    case normal -> targetPlaneToNormal;
                    case percent -> targetPlaneToPercent;
                    case target -> IDENTITY;
                };
         };
    }
}
