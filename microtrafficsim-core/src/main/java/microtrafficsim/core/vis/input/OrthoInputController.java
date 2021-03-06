package microtrafficsim.core.vis.input;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import microtrafficsim.core.vis.view.OrthographicView;
import microtrafficsim.math.Rect2d;
import microtrafficsim.math.Vec2d;
import microtrafficsim.math.Vec3d;

import java.util.HashMap;


/**
 * Input controller for an orthographic view.
 *
 * @author Maximilian Luz
 */
public class OrthoInputController implements MouseListener, KeyController {

    private OrthographicView view;
    private int              prevX;
    private int              prevY;
    private double           zoomFactor;
    private HashMap<Short, KeyCommand> cmdPressed;
    private HashMap<Short, KeyCommand> cmdReleased;


    /**
     * Constructs a new {@code OrthoInputController}.
     *
     * @param view           the view this controller should control.
     * @param zoomMultiplier the multiplier used for zoom-input.
     */
    public OrthoInputController(OrthographicView view, double zoomMultiplier) {
        this.view        = view;
        this.zoomFactor  = zoomMultiplier;
        this.cmdPressed  = new HashMap<>();
        this.cmdReleased = new HashMap<>();
    }

    /**
     * Returns the current zoom-factor.
     *
     * @return the current zoom-factor.
     */
    public double getZoomFactor() {
        return zoomFactor;
    }

    /**
     * Sets the current zoom-factor.
     *
     * @param zoomFactor the new zoom-factor.
     */
    public void setZoomFactor(double zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        prevX = e.getX();
        prevY = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        prevX = e.getX();
        prevY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        double scale = view.getScale();
        double diffX = (x - prevX) * (1.0 / scale);
        double diffY = (y - prevY) * (1.0 / scale);

        Vec3d pos = view.getPosition();
        view.setPosition(pos.x - diffX, pos.y + diffY);

        prevX = x;
        prevY = y;
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        Vec2d screenAnchor = new Vec2d(e.getX() / (double) view.getSize().x, e.getY() / (double) view.getSize().y);
        Vec2d worldAnchor = Rect2d.project(new Rect2d(1, 0, 0, 1), view.getViewportBounds(), screenAnchor);

        // set zoom
        double zoom = view.getZoomLevel();
        zoom += e.getRotation()[0] * e.getRotationScale() * zoomFactor;
        zoom += e.getRotation()[1] * e.getRotationScale() * zoomFactor;
        view.setZoomLevel(zoom);

        // update position
        Vec2d worldAtAnchorNow = Rect2d.project(new Rect2d(1, 0, 0, 1), view.getViewportBounds(), screenAnchor);
        Vec2d delta = Vec2d.sub(worldAtAnchorNow, worldAnchor);

        Vec3d vpos = view.getPosition();
        view.setPosition(vpos.x + delta.x, vpos.y + delta.y);
    }


    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        KeyCommand cmd = cmdPressed.get(e.getKeyCode());
        if (cmd != null) cmd.event(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        KeyCommand cmd = cmdReleased.get(e.getKeyCode());
        if (cmd != null) cmd.event(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}


    @Override
    public KeyCommand addKeyCommand(short event, short vk, KeyCommand command) {
        switch (event) {
        case KeyEvent.EVENT_KEY_PRESSED:  return cmdPressed.put(vk, command);
        case KeyEvent.EVENT_KEY_RELEASED: return cmdReleased.put(vk, command);
        }

        return null;
    }

    @Override
    public KeyCommand removeKeyCommand(short event, short vk) {
        switch (event) {
        case KeyEvent.EVENT_KEY_PRESSED:  return cmdPressed.remove(vk);
        case KeyEvent.EVENT_KEY_RELEASED: return cmdReleased.remove(vk);
        default:                          return null;
        }
    }
}
