package visbox.visualiser.particles;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

import java.awt.Graphics2D;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.lwjgl.BufferUtils;

import visbox.Analyser.AnalyserConfig;
import visbox.ShaderManager;
import visbox.VBMain;
import visbox.logger.Logger;
import visbox.ui.GLFWUI;

public class FluidDynamics extends ParticleVisualiser {
    
    private int program;
    private int vAO;
    private int instanceVBO;
    
    private int numParticles = 800;
    private float pSize = 10.0f;
    private float mass = 1.0f;
    private float restDensity = 90000.0f;
    private float stiffness = 1.0f;
    private float viscosity = 0.1f;
    private float smoothingRadius = 300f;
    private float gravity = 0f; //-9.81
    
    public float[] pX, pY;
    public float[] vX, vY;
    public float[] aX, aY;
    public float[] density;
    public float[] pressure;
    
    public int worldW;
    public int worldH;
    public int gridW;
    public int gridH;
    public float cellSize = smoothingRadius;
    public SpatialMap[] spatialLookup;
    public int[] spatialLookupIndices;
    
    public FluidDynamics() {
        super("FluidDynamics", 10);
        pX = new float[numParticles];
        pY = new float[numParticles];
        vX = new float[numParticles];
        vY = new float[numParticles];
        aX = new float[numParticles];
        aY = new float[numParticles];
        density = new float[numParticles];
        pressure = new float[numParticles];
        
        spatialLookup = new SpatialMap[numParticles];
        spatialLookupIndices = new int[numParticles];
    }
    
    @Override
    public void activate(AnalyserConfig a) {
        super.activate(a);
        ShaderManager sM = VBMain.getShaderManager();
        
        program = sM.createProgram("fluiddyn.vert", "fluiddyn.frag");
        sM.setCurrentProgram(program);
        
        // Create VAO
        vAO = glGenVertexArrays();
        sM.bindVAO(vAO);
        
        // Create VBO
        int quadVBO = glGenBuffers();
        sM.bindVBO(quadVBO);
        glBufferData(GL_ARRAY_BUFFER, sM.getGenericQuad(), GL_STATIC_DRAW);
        
        // Add to VAO
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2*Float.BYTES, 0);
        
        // Create instance data VBO
        instanceVBO = glGenBuffers();
        sM.bindVBO(instanceVBO);
        glBufferData(GL_ARRAY_BUFFER, (long) numParticles*3*Float.BYTES, GL_STREAM_DRAW);
        
        int stride = 3*Float.BYTES;
        // Add center to VAO
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 0L);
        glVertexAttribDivisor(1, 1);
        
        // Add size to VAO
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 2L * Float.BYTES);
        glVertexAttribDivisor(2, 1);
        
        sM.setOrthoProjection();
        
        setup();
    }
    
    @Override
    public void update() {
        stepSimulation();
    }
    
    @Override
    public void render() {
        ShaderManager sM = VBMain.getShaderManager();
        uploadInstanceData();
        
        sM.bindFBO(0);
        sM.useProgram(program);
        sM.setBlend(false);
        sM.bindVAO(vAO);
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, numParticles);
        
        sM.bindVAO(0);
        sM.useProgram(0);
        sM.setBlend(false);
    }
    
    @Override
    public void render(Graphics2D g, int w, int h) {}
    
    private void uploadInstanceData() {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(numParticles*3);
        
        for (int i=0; i<numParticles; i++) {
            buffer.put(pX[i]);
            buffer.put(pY[i]);
            buffer.put(pSize);
        }
        buffer.flip();
        
        VBMain.getShaderManager().bindVBO(instanceVBO);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        VBMain.getShaderManager().bindVBO(0);
    }
    
    private void setup() {
        // Setup particles
        int adj = 300;
        int minX = adj;
        int minY = adj;
        int maxX = GLFWUI.WIDTH-adj;
        int maxY = GLFWUI.HEIGHT-adj;
        int w = maxX-minX;
        int h = maxY-minY;
        float aspect = w/h;
        int cols = (int) Math.ceil(Math.sqrt(numParticles*aspect));
        int rows = (int) Math.ceil((float) numParticles/cols);
        
        for (int i=0; i<numParticles; i++) {
            int r = i/cols;
            int c = i%cols;
            
            pX[i] = minX+(c+0.5f)*(w/cols);
            pY[i] = minY+(r+0.5f)*(h/rows);
            vY[i] = 1.0f;
        }
        
        // Setup grid
        worldW = GLFWUI.WIDTH;
        worldH = GLFWUI.HEIGHT;
        gridW = (int) Math.ceil(worldW/cellSize);
        gridH = (int) Math.ceil(worldH/cellSize);
        
        Logger.debug(worldW+", "+worldH);
        Logger.debug(gridW+", "+gridH);
    }
    
    private void clearSpatialLookup() {
        for (int i=0; i<numParticles; i++) {
            spatialLookup[i] = null;
            spatialLookupIndices[i] = Integer.MAX_VALUE;
        }
    }
    
    private void buildSpatialLookup() {
        for (int i=0; i<numParticles; i++) {
            int gX = (int) (pX[i]/cellSize);
            int gY = (int) (pY[i]/cellSize);
            spatialLookup[i] = new SpatialMap(i, hashCell(gX, gY));            
        }
        Arrays.sort(spatialLookup, (a, b) -> Integer.compare(a.h, b.h));
        
        int last = Integer.MIN_VALUE;
        
        for (int i=0; i<spatialLookup.length; i++) {
            int h = spatialLookup[i].h;
            
            if (h>last) {
                spatialLookupIndices[h] = i;
                last = h;
            }
        }
    }
    
    private int hashCell(int cX, int cY) {
        int h = cX*73856093+cY*19349663;
        return (h&0x7fffffff)%numParticles;
    }
    
    private List<Integer> getParticlesInCell(int cX, int cY) {
        int h = hashCell(cX, cY);
        int i = spatialLookupIndices[h];
        List<Integer> particles = new ArrayList<>();
        if (i==Integer.MAX_VALUE) return particles;
        
        int z = spatialLookup[i].h;
        while (z==h&&i<spatialLookup.length) {
            particles.add(spatialLookup[i].pI);
            z = spatialLookup[i].h;
            i++;
        }
        
        return particles;
    }
    
    class SpatialMap {
        int pI;
        int h;
        SpatialMap(int pI, int h) {this.pI = pI; this.h = h;}
    }
    
    private void computeDensityAndPressure() {
        float smoothingR2 = smoothingRadius*smoothingRadius;
        
        for (int p=0; p<numParticles; p++) {
            float xI = pX[p];
            float yI = pY[p];
            
            float rho = 0.0f;
            
            int gX = (int) (xI/cellSize);
            int gY = (int) (yI/cellSize);
            
            for (int dY=-1; dY<=1; dY++) {
                for (int dX=-1; dX<=1; dX++) {
                    int nGX = gX+dX;
                    int nGY = gY+dY;
                    if (nGX<0||nGX>=gridW||nGY<0||nGY>=gridH) continue;
                    List<Integer> particles = getParticlesInCell(nGX, nGY);
                    
                    for (int i=0; i<particles.size(); i++) {
                        int j = particles.get(i);
                        float rX = xI-pX[j];
                        float rY = yI-pY[j];
                        float r2 = rX*rX+rY*rY;
                        if (r2>=smoothingR2) continue;
                        
                        float r = (float) Math.sqrt(r2);
                        float w = densityKernel(r, smoothingRadius);
                        
                        rho += mass*w;
                    }
                }
            }
            density[p] = rho;
            pressure[p] = stiffness*(density[p]-restDensity);
            //if (pressure[p]<0.0f) pressure[p] = 0.0f;
        }
    }
    
    private float densityKernel(float r, float h) { //Poly6
        if (r>=h) return 0.0f;
        float c = (float) (315.0f/(64.0f*Math.PI*Math.pow(h, 9)));
        float x = (h*h-r*r);
        return c*(x*x*x);
    }
    
    private void calculateForces() {
        float smoothingR2 = smoothingRadius*smoothingRadius;
        
        for (int p=0; p<numParticles; p++) {
            float xI = pX[p];
            float yI = pY[p];
            float rhoI = density[p];
            float presI = pressure[p];
            
            float fX = 0.0f;
            float fY = 0.0f;
            
            int gX = (int) (xI/cellSize);
            int gY = (int) (yI/cellSize);
            
            for (int dY=-1; dY<=1; dY++) {
                for (int dX=-1; dX<=1; dX++) {
                    int nGX = gX+dX;
                    int nGY = gY+dY;
                    if (nGX<0||nGX>=gridW||nGY<0||nGY>=gridH) continue;
                    List<Integer> particles = getParticlesInCell(nGX, nGY);
                    
                    for (int i=0; i<particles.size(); i++) {
                        int j = particles.get(i);
                        if (j==p) continue;
                        
                        float rX = xI-pX[j];
                        float rY = yI-pY[j];
                        float r2 = rX*rX+rY*rY;
                        if (r2>=smoothingR2||r2<1e-12f) continue;
                        
                        float r = (float) Math.sqrt(r2);
                        float rhoJ = density[j];
                        float presJ = pressure[j];
                        
                        Vec2 gradW = gradientKernel(rX, rY, r, smoothingRadius);
                        
                        float presFactor = -mass*mass*(presI+presJ)/(2.0f*rhoJ);
                        
                        fX += presFactor*gradW.x;
                        fY += presFactor*gradW.y;
                    }
                }
            }
            
            fY += mass*gravity;
            
            aX[p] = fX/mass;
            aY[p] = fY/mass;
        }
        
        
        float minRho = Float.POSITIVE_INFINITY;
        float maxRho = Float.NEGATIVE_INFINITY;
        float sumRho = 0;
        
        for (int i = 0; i < numParticles; i++) {
            float rho = density[i];
            minRho = Math.min(minRho, rho);
            maxRho = Math.max(maxRho, rho);
            sumRho += rho;
        }
        float avgRho = sumRho / numParticles;
        
        Logger.debug("rho min=" + minRho + " max=" + maxRho + " avg=" + avgRho);
    }
    
    class Vec2 {
        float x, y;
        Vec2(float x, float y) {this.x = x; this.y = y;}
    }
    
    private Vec2 gradientKernel(float rX, float rY, float r, float h) {
        if (r<=0.0f||r>=h) return new Vec2(0.0f, 0.0f);
        float c = -45.0f/(float) (Math.PI*h*h*h*h*h*h);
        float factor = c*(h-r)*(h-r)/r;
        return new Vec2(rX*factor, rY*factor);
    }
    
    private void applyCollisionForces() {
        float radius = pSize * 0.5f;
        
        // tune these:
        float k = 500.0f;   // wall stiffness
        float c = 20.0f;    // wall damping
        
        for (int i = 0; i < numParticles; i++) {
            float x = pX[i];
            float y = pY[i];
            float vx = vX[i];
            float vy = vY[i];
            
            float fx = 0.0f;
            float fy = 0.0f;
            
            // LEFT wall (x >= radius)
            float penLeft = radius - x;
            if (penLeft > 0.0f) {
                // normal = (1, 0)
                fx += k * penLeft - c * vx;   // damp vx into wall
            }
            
            // RIGHT wall (x <= worldW - radius)
            float penRight = (x + radius) - worldW;
            if (penRight > 0.0f) {
                // normal = (-1, 0)
                fx += -k * penRight - c * vx;
            }
            
            // BOTTOM wall (y >= radius)
            float penBottom = radius - y;
            if (penBottom > 0.0f) {
                // normal = (0, 1)
                fy += k * penBottom - c * vy;
            }
            
            // TOP wall (y <= worldH - radius)
            float penTop = (y + radius) - worldH;
            if (penTop > 0.0f) {
                // normal = (0, -1)
                fy += -k * penTop - c * vy;
            }
            
            // add to acceleration: a += F/m
            aX[i] += fx / mass;
            aY[i] += fy / mass;
        }
    }
    
    
    private void integrate(float dT) {
        for (int i=0; i<numParticles; i++) {
            vX[i] += aX[i]*dT;
            vY[i] += aY[i]*dT;
            
            pX[i] += vX[i]*dT;
            pY[i] += vY[i]*dT;
        }
    }
    
    private void stepSimulation() {
        //if (VBMain.getGlobalTick()>1) return;
        float dT = 1.0f / 120.0f;
        clearSpatialLookup();
        buildSpatialLookup();
        computeDensityAndPressure();
        
        //Logger.debugArray(density);
        //Logger.debugArray(pressure);
        
        
        calculateForces();
        applyCollisionForces();
        
        //Logger.debugArray(aX);
        //Logger.debugArray(aY);
        Logger.ln();
        
        integrate(dT);
    }
    
}
