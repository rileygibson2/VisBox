package visbox.ui;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.*;

public class IO {
   
    private static long window;

    public static double mouseX;
    public static double mouseY;
    private static double prevMouseX;
    private static double prevMouseY;
    public static double mouseDY;
    public static double mouseDX;

    public static boolean leftPressed;
    public static boolean rightPressed;

    public static void setWindow(long w) {window = w;}

    public static void setupCallbacks() {
        // ESC closes window
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(win, true);
            }
        });

        /*glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button==GLFW_MOUSE_BUTTON_LEFT && action==GLFW_PRESS) {
                leftPressed = true;
            }
            if (button==GLFW_MOUSE_BUTTON_RIGHT && action==GLFW_PRESS) {
                rightPressed = true;
            }
        });*/
    }

    public static void update() {
        prevMouseX = mouseX;
        prevMouseY = mouseY;

        double[] xPos = new double[1];
        double[] yPos = new double[1];

        glfwGetCursorPos(window, xPos, yPos);

        mouseX = xPos[0];
        mouseY = GLFWUI.HEIGHT-yPos[0];
        mouseDX = mouseX-prevMouseX;
        mouseDY = mouseY-prevMouseY;
        
        leftPressed = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT)==GLFW_PRESS;
        rightPressed = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT)==GLFW_PRESS;
    }

    public static boolean mouseDown() {return leftPressed||rightPressed;}

}
