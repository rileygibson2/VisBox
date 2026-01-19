package visbox.ui;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import visbox.VBMain;
import visbox.logger.LogColor;
import visbox.logger.LogColorEnum;
import visbox.logger.Logger;
import visbox.visualiser.Visualiser;

@LogColor(LogColorEnum.PURPLE)
public class GLFWUI {
    
    private boolean initialised;
    private long window;
    public static int WIDTH = 1280;
    public static int HEIGHT = 720;
    
    private float time = 0f;
    
    private double fpsTimer;
    private int frames;
    private double fps;

    private String titleData;
    
    public GLFWUI() {
        this.initialised = false;
        this.fpsTimer = 0.0;
        this.frames = 0;
    }
    
    public void init() {
        // print GLFW errors to stderr
        GLFWErrorCallback.createPrint(System.err).set();
        
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // Window hints (OpenGL version + behavior)
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        
        // Create window
        window = glfwCreateWindow(WIDTH, HEIGHT, "VisBox", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        
        // ESC closes window
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(win, true);
            }
        });
        
        // Center the window on the main monitor
        long monitor = glfwGetPrimaryMonitor();
        var vid = glfwGetVideoMode(monitor);
        glfwSetWindowPos(window, (vid.width() - WIDTH) / 2, (vid.height() - HEIGHT) / 2);
        
        // Make context current
        glfwMakeContextCurrent(window);
        
        // Enable vsync
        glfwSwapInterval(1);
        
        // Show window
        glfwShowWindow(window);
        
        // Load OpenGL bindings (VERY IMPORTANT)
        GL.createCapabilities();
        
        // Setup initial viewport
        glViewport(0, 0, WIDTH, HEIGHT);
        
        // Handle resize
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            WIDTH = w;
            HEIGHT = h;
            glViewport(0, 0, w, h);
            Visualiser v = VBMain.getInstance().getCurrentVisualiser();
            if (v!=null) v.resize(w, h);
        });
        
        initialised = true;
    }
    
    public void iterate() {
        // FPS calcs
        fpsTimer += VBMain.getDeltaTime();
        frames++;
        if (fpsTimer>=1.0) {
            fps = frames / fpsTimer;
            String s = "VisBox | "+(int) Math.floor(fps)+" FPS";
            if (titleData!=null) s += " - "+titleData;

            glfwSetWindowTitle(window, s);
            fpsTimer = 0.0;
            frames = 0;
        }
        
        glfwPollEvents();

        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
        VBMain.getInstance().getCurrentVisualiser().render();
        
        glfwSwapBuffers(window);
    }

    public void setTitleData(String data) {titleData = data;}
    
    public double getApproxFPS() {return fps;}
    
    public boolean windowAlive() {
        return initialised && !glfwWindowShouldClose(window);
    }
    
    public void cleanup() {
        Logger.info("Cleaning up");
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
