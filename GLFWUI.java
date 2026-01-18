package visbox.ui;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import visbox.VBMain;
import visbox.logger.LogColor;
import visbox.logger.LogColorEnum;

@LogColor(LogColorEnum.PURPLE)
public class GLFWUI {
    
    private boolean initialised;
    private long window;
    private int width = 1280;
    private int height = 720;
    
    private float time = 0f;
    
    private double fpsTimer;
    private int frames;
    private double fps;
    
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
        window = glfwCreateWindow(width, height, "VisBox", NULL, NULL);
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
        glfwSetWindowPos(window, (vid.width() - width) / 2, (vid.height() - height) / 2);
        
        // Make context current
        glfwMakeContextCurrent(window);
        
        // Enable vsync
        glfwSwapInterval(1);
        
        // Show window
        glfwShowWindow(window);
        
        // Load OpenGL bindings (VERY IMPORTANT)
        GL.createCapabilities();
        
        // Setup initial viewport
        glViewport(0, 0, width, height);
        
        // Handle resize
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            width = w;
            height = h;
            glViewport(0, 0, w, h);
        });
        
        // Enable blending (default for when we need it later)
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        initialised = true;
    }
    
    public void iterate() {
        // FPS calcs
        fpsTimer += VBMain.getDeltaTime();
        frames++;
        if (fpsTimer>=1.0) {
            fps = frames / fpsTimer;
            glfwSetWindowTitle(window, "VisBox | "+(int) Math.floor(fps)+" FPS");
            fpsTimer = 0.0;
            frames = 0;
        }
        
        glfwPollEvents();
        
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
        
        VBMain.getInstance().getCurrentVisualiser().render();
        
        glfwSwapBuffers(window);
        
        glfwSwapBuffers(window);
    }
    
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
