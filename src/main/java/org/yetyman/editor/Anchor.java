package org.yetyman.editor;

import binding.OneWayBinding;
import binding.Property;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;

//the draggable items at the corners of objects that can be manipulated on screen.
public class Anchor {
    public final Property<Node> ui = new Property<>(new Button());
    public final Property<Plane> anchorPlane = new Property<>(Plane.target);
    //location in target space
    public final Property<Point2D> location = new Property<>(Point2D.ZERO);
    //size in screen space
    public final Property<Point2D> size = new Property<>(new Point2D(15,15));

    public Anchor(){
        ui.bindOut((s,a,b)->{
            refresh();
        });
        location.bindOut((s,a,b)->{
            refresh();
        });
        size.bindOut((s,a,b)->{
            refresh();
        });
        anchorPlane.bindOut((s,a,b)->{
            refresh();
        });
    }

    private void refresh() {
//        Point2D pt = location.get();
//        Point2D size = this.size.get();
//        Node ui = this.ui.get();
//
//        if(pt!= null && size != null && ui != null) {
//            Point2D tl = pt.subtract(size.getX() / 2, size.getY() / 2);
//
//            ui.resizeRelocate(tl.getX(), tl.getY(), size.getX(), size.getY());
//        }
        if(ui.get()!=null && ui.get().getParent()!=null)
            ui.get().getParent().requestLayout();
    }
}
