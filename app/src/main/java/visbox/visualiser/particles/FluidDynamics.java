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
import java.util.Arrays;

import org.lwjgl.BufferUtils;

import visbox.Analyser.AnalyserConfig;
import visbox.ShaderManager;
import visbox.VBMain;
import visbox.logger.Logger;
import visbox.ui.GLFWUI;
import visbox.ui.IO;

public class FluidDynamics extends ParticleVisualiser {
    
    private int program;
    private int vAO;
    private int instanceVBO;
    FloatBuffer instanceBuffer;
    
    private int numParticles = 2000;
    private float pSize = 5.0f;
    private float mass = 1.0f;
    private float restDensity = 0.00010f;
    private float pressureMultiplier = 40000f;
    private float nearPressureMultiplier = 80000f;
    private float viscosityStrength = 0.5f;
    private float smoothingRadius = 30f;
    private float collisionDamping = 0.8f;
    private float gravity = -500f; //-150f
    private int iterationsPerFrame = 5;
    private float frameDT = 1.0f/60.0f;

    private float mouseInteractRadius = 200f;
    private float mouseInteractStrength = 800f;
    
    public float[] pX, pY;
    public float[] vX, vY;
    public float[] aX, aY;
    public float[] pPredX, pPredY;
    public float[] density, nearDensity;
    public float[] pressure, nearPressure;
    
    public int worldW;
    public int worldH;
    public int gridW;
    public int gridH;
    public float cellSize = smoothingRadius;
    public SpatialMap[] spatialLookup;
    public int[] spatialLookupIndices;
    
    private float poly6C;
    private float spikyPow3C;
    private float spikyPow2C;
    private float spikyPow3DerivC;
    private float spikyPow2DerivC;
    private float smoothingR2 = smoothingRadius*smoothingRadius;
    private float mouseInteractRadius2 = mouseInteractRadius*mouseInteractRadius;
    
    private boolean debug = false;
    
    public FluidDynamics() {
        super("FluidDynamics", 10);
        pX = new float[numParticles];
        pY = new float[numParticles];
        vX = new float[numParticles];
        vY = new float[numParticles];
        aX = new float[numParticles];
        aY = new float[numParticles];
        pPredX = new float[numParticles];
        pPredY = new float[numParticles];
        density = new float[numParticles];
        nearDensity = new float[numParticles];
        pressure = new float[numParticles];
        nearPressure = new float[numParticles];
        
        spatialLookup = new SpatialMap[numParticles];
        spatialLookupIndices = new int[numParticles];
        
        recomputeKernelConstants();
    }
    
    private void debug() {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        float avg = 0.0f;
        for (int i=0; i<numParticles; i++) {
            if (density[i]<min) min = density[i];
            if (density[i]>max) max = density[i];
            avg += density[i];
        }
        avg /= numParticles;
        
        Logger.debug("Min density: "+min);
        Logger.debug("Max density: "+max);
        Logger.debug("Avg density: "+avg);
        
        min = Float.POSITIVE_INFINITY;
        max = Float.NEGATIVE_INFINITY;
        avg = 0.0f;
        for (int i=0; i<numParticles; i++) {
            if (pressure[i]<min) min = pressure[i];
            if (pressure[i]>max) max = pressure[i];
            avg += pressure[i];
        }
        avg /= numParticles;
        
        Logger.debug("Min pressure: "+min);
        Logger.debug("Max pressure: "+max);
        Logger.debug("Avg pressure: "+avg);
        
        /*min = Float.POSITIVE_INFINITY;
        max = Float.NEGATIVE_INFINITY;
        avg = 0.0f;
        for (int i=0; i<numParticles; i++) {
        if (aX[i]<min) min = aX[i];
        if (aX[i]>max) max = aX[i];
        avg += aX[i];
        }
        avg /= numParticles;
        
        Logger.debug("Min aX: "+min);
        Logger.debug("Max aX: "+max);
        Logger.debug("Avg aX: "+avg);*/
        
        Logger.ln();
    }
    
    private void recomputeKernelConstants() {
        float h  = smoothingRadius;
        float pi = (float) Math.PI;
        
        poly6C          = (float) (315.0 / (64.0 * pi * Math.pow(h, 9)));
        spikyPow3C      = (float) (15.0  / (pi * Math.pow(h, 6)));
        spikyPow3DerivC = (float) (45.0  / (pi * Math.pow(h, 6)));
        spikyPow2C      = (float) (15.0  / (2.0 * pi * Math.pow(h, 5)));
        spikyPow2DerivC = (float) (15.0  / (pi * Math.pow(h, 5)));
    }
    
    private float smoothingKernelPoly6(float dst) {
        if (dst >= smoothingRadius) return 0.0f;
        float v = smoothingRadius * smoothingRadius - dst * dst;
        return v * v * v * poly6C;
    }
    
    private float spikyKernelPow3(float dst) {
        if (dst >= smoothingRadius) return 0.0f;
        float v = smoothingRadius - dst;
        return v * v * v * spikyPow3C;
    }
    
    private float spikyKernelPow2(float dst) {
        if (dst >= smoothingRadius) return 0.0f;
        float v = smoothingRadius - dst;
        return v * v * spikyPow2C;
    }
    
    private float derivativeSpikyPow3(float dst) {
        if (dst > smoothingRadius) return 0.0f;
        float v = smoothingRadius - dst;
        return -v * v * spikyPow3DerivC; // scalar dW/dr
    }
    
    private float derivativeSpikyPow2(float dst) {
        if (dst > smoothingRadius) return 0.0f;
        float v = smoothingRadius - dst;
        return -v * spikyPow2DerivC; // scalar dW/dr
    }
    
    // Main density kernel
    private float densityKernel(float dst) {
        return spikyKernelPow2(dst);       // (h - r)^2 * SpikyPow2ScalingFactor
    }
    
    // Short-range / near-density kernel
    private float nearDensityKernel(float dst) {
        return spikyKernelPow3(dst);       // (h - r)^3 * SpikyPow3ScalingFactor
    }
    
    // Derivatives for pressure forces
    private float densityDerivative(float dst) {
        return derivativeSpikyPow2(dst);   // scalar dW/dr
    }
    
    private float nearDensityDerivative(float dst) {
        return derivativeSpikyPow3(dst);   // scalar dW/dr
    }
    
    // Viscosity kernel
    private float viscosityKernel(float dst) {
        return smoothingKernelPoly6(dst);  // Poly6-based smoothing
    }
    
    @Override
    public void activate(AnalyserConfig a) {
        super.activate(a);
        ShaderManager sM = VBMain.getShaderManager();
        
        program = sM.createProgram("fluiddyn/fluiddyn.vert", "fluiddyn/fluiddyn.frag");
        sM.setCurrentProgram(program);
        sM.useProgram(program);
        
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
        glBufferData(GL_ARRAY_BUFFER, (long) numParticles*4*Float.BYTES, GL_STREAM_DRAW);
        
        int stride = 4*Float.BYTES;
        // Add center to VAO
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 0L);
        glVertexAttribDivisor(1, 1);
        
        // Add size to VAO
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 2L * Float.BYTES);
        glVertexAttribDivisor(2, 1);
        
        // Add property to VAO
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glVertexAttribDivisor(3, 1);
        
        sM.setUniformFloat("uLow", 0f);
        sM.setUniformFloat("uHigh", (float) 50f*50f);
        
        sM.setOrthoProjection();
        sM.useProgram(0);
        
        instanceBuffer = BufferUtils.createFloatBuffer(numParticles*4);
        
        setup();
    }
    
    @Override
    public void update() {
        //float frameDT = VBMain.getDeltaTime(); // or fixed
        float dT = frameDT / iterationsPerFrame;
        
        for (int i = 0; i < iterationsPerFrame; i++) {
            stepSimulation(dT);
        }
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
        instanceBuffer.clear();
        
        for (int i=0; i<numParticles; i++) {
            instanceBuffer.put(pX[i]);
            instanceBuffer.put(pY[i]);
            instanceBuffer.put(pSize);
            instanceBuffer.put((vX[i]*vX[i]+vY[i]*vY[i])); // speed squared as property
        }
        instanceBuffer.flip();
        
        VBMain.getShaderManager().bindVBO(instanceVBO);
        glBufferSubData(GL_ARRAY_BUFFER, 0, instanceBuffer);
        VBMain.getShaderManager().bindVBO(0);
    }
    
    private void setup() {
        // Setup particles
        float adj = 0.3f;
        int minX = (int) (GLFWUI.WIDTH*adj);
        int minY = (int) (GLFWUI.HEIGHT*adj);
        int maxX = (int) (GLFWUI.WIDTH*(1-adj));
        int maxY = (int) (GLFWUI.HEIGHT*(1-adj));
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
            int gX = (int) (pPredX[i]/cellSize);
            int gY = (int) (pPredY[i]/cellSize);
            spatialLookup[i] = new SpatialMap(i, hashCell(gX, gY));            
        }
        Arrays.sort(spatialLookup, (a, b) -> Integer.compare(a.hash, b.hash));
        
        int last = Integer.MIN_VALUE;
        
        for (int i=0; i<spatialLookup.length; i++) {
            int h = spatialLookup[i].hash;
            
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
    
    class SpatialMap {
        int pI;
        int hash;
        SpatialMap(int pI, int hash) {this.pI = pI; this.hash = hash;}
    }
    
    private void computeDensityAndPressure(int p) {
        float xI = pPredX[p];
        float yI = pPredY[p];
        
        float rho = 0.0f;
        float rhoNear = 0.0f;
        
        int cX = (int) (xI/cellSize);
        int cY = (int) (yI/cellSize);
        
        for (int gY=-1; gY<=1; gY++) {
            for (int gX=-1; gX<=1; gX++) {
                int nGX = cX+gX;
                int nGY = cY+gY;
                if (nGX<0||nGX>=gridW||nGY<0||nGY>=gridH) continue;
                
                int hash = hashCell(nGX, nGY);
                int i = spatialLookupIndices[hash];
                if (i==Integer.MAX_VALUE) continue;
                
                while (i<spatialLookup.length&&spatialLookup[i].hash==hash) {
                    int j = spatialLookup[i].pI;
                    i++;
                    float dX = pPredX[j] - xI;
                    float dY = pPredY[j] - yI;
                    float d2 = dX*dX + dY*dY;
                    if (d2 > smoothingR2) continue;
                    
                    float dst = (float) Math.sqrt(d2);
                    
                    rho += densityKernel(dst);
                    rhoNear += nearDensityKernel(dst);
                    
                }
            }
        }
        
        density[p] = rho;
        nearDensity[p] = rhoNear;
        
        float pMain = (rho-restDensity) * pressureMultiplier;
        float pNear = rhoNear * nearPressureMultiplier;
        
        //if (pMain < 0.0f) pMain = 0.0f;
        //if (pNear < 0.0f) pNear = 0.0f;
        
        pressure[p] = pMain;
        nearPressure[p] = pNear;
    }
    
    private void applyForces(int p) {
        float xI = pPredX[p];
        float yI = pPredY[p];
        
        float rhoI = density[p];
        float presI = pressure[p];
        float nearPresI = nearPressure[p];
        
        float fX = 0.0f;
        float fY = 0.0f;
        
        float vIx = vX[p];
        float vIy = vY[p];
        float viscX = 0.0f;
        float viscY = 0.0f;
        
        int cX = (int) (xI/cellSize);
        int cY = (int) (yI/cellSize);
        
        for (int gY=-1; gY<=1; gY++) {
            for (int gX=-1; gX<=1; gX++) {
                int nGX = cX+gX;
                int nGY = cY+gY;
                if (nGX<0||nGX>=gridW||nGY<0||nGY>=gridH) continue;
                
                int hash = hashCell(nGX, nGY);
                int i = spatialLookupIndices[hash];
                if (i==Integer.MAX_VALUE) continue;
                
                while (i<spatialLookup.length&&spatialLookup[i].hash==hash) {
                    int j = spatialLookup[i].pI;
                    i++;
                    if (j==p) continue;
                    
                    float dX = pPredX[j] - xI;
                    float dY = pPredY[j] - yI;
                    float d2 = dX*dX + dY*dY;
                    if (d2 > smoothingR2 || d2 < 1e-12f) continue;
                    
                    float dst = (float) Math.sqrt(d2);
                    float invDst = 1.0f / dst;
                    float dirX = dX * invDst;
                    float dirY = dY * invDst;
                    
                    float rhoJ = density[j];
                    float rhoNearJ = nearDensity[j];
                    float presJ = pressure[j];
                    float nearPresJ = nearPressure[j];
                    
                    float sharedPres = 0.5f * (presI + presJ);
                    float sharedNearPres = 0.5f * (nearPresI + nearPresJ);
                    
                    float dW = densityDerivative(dst);      // DerivativeSpikyPow2
                    float dWNear = nearDensityDerivative(dst);  // DerivativeSpikyPow3
                    
                    if (rhoJ > 1e-6f && sharedPres > 0.0f) {
                        float scalar = dW * sharedPres / rhoJ;
                        fX += dirX * scalar;
                        fY += dirY * scalar;
                    }
                    if (rhoNearJ > 1e-6f && sharedNearPres > 0.0f) {
                        float scalarNear = dWNear * sharedNearPres / rhoNearJ;
                        fX += dirX * scalarNear;
                        fY += dirY * scalarNear;
                    }
                    
                    // Viscocity
                    float wVisc = viscosityKernel(dst);  // Poly6
                    float vJx = vX[j];
                    float vJy = vY[j];
                    
                    // Pull towards neighbour velocity
                    viscX += (vJx - vIx) * wVisc;
                    viscY += (vJy - vIy) * wVisc;
                }
            }
        }
        
        aX[p] = 0.0f;
        aY[p] = 0.0f;
        
        if (rhoI > 1e-6f) {
            aX[p] += fX / rhoI;
            aY[p] += fY / rhoI;
            aX[p] += viscosityStrength * viscX / rhoI;
            aY[p] += viscosityStrength * viscY / rhoI;
        }
        else {
            aX[p] += viscosityStrength * viscX;
            aY[p] += viscosityStrength * viscY;
        }
        
        // Gravity
        aY[p] += mass*gravity;
    }
    
    private void applyMouseForce(float dT) {
        if (!IO.mouseDown()) return;
        float mouseVX = (float) (IO.mouseDX/dT);
        float mouseVY = (float) (IO.mouseDY/dT);
        float m2 = mouseVX*mouseVX+mouseVY+mouseVY;
        float mvDirX = 0f;
        float mvDirY = 0f;
        
        if (m2>1e-6f) {
            float inv = 1.0f/(float) Math.sqrt(m2);
            mvDirX = mouseVX*inv;
            mvDirY = mouseVY*inv;
        }
        
        for (int p=0; p<numParticles; p++) {
            float xI = pX[p];
            float yI = pY[p];
            float dX = (float) (xI-IO.mouseX);
            float dY = (float) (yI-IO.mouseY);
            float d2 = dX*dX+dY*dY;
            if (d2>mouseInteractRadius2 || d2<1e-6f) continue;
            
            float dist = (float) Math.sqrt(d2);
            float t = 1.0f-(dist/mouseInteractRadius);
            float falloff = t*t;
            
            //Radial
            float invDist = 1.0f/dist;
            float dirX = dX*invDist;
            float dirY = dY*invDist;
            float radialAccel = mouseInteractStrength*falloff;
            float sign = IO.leftPressed ? -1.0f : 1.0f;
            
            aX[p] += sign*radialAccel*dirX;
            aY[p] += sign*radialAccel*dirY;
            
            // Drag
            aX[p] += (mouseInteractStrength*0.3)*falloff*mvDirX;
            aY[p] += (mouseInteractStrength*0.3)*falloff*mvDirY;
        }
    }
    
    
    private void applyCollisionForces() {
        float radius = pSize*0.5f;
        
        for (int i = 0; i < numParticles; i++) {
            float x = pX[i];
            float y = pY[i];
            float vx = vX[i];
            float vy = vY[i];
            
            // LEFT wall: x >= radius
            if (x < radius) {
                x = radius;                 // snap out of wall
                if (vx < 0.0f) {            // moving into wall?
                    vx = -vx * collisionDamping; // bounce in x
                }
            }
            
            // RIGHT wall: x <= worldW - radius
            if (x > worldW-radius) {
                x = worldW-radius;
                if (vx > 0.0f) {            // moving into wall?
                    vx = -vx * collisionDamping;
                }
            }
            
            // BOTTOM wall: y >= radius
            if (y<radius) {
                y = radius;
                if (vy < 0.0f) {
                    vy = -vy * collisionDamping;
                }
            }
            
            // TOP wall: y <= worldH - radius
            if (y>worldH-radius) {
                y = worldH-radius;
                if (vy > 0.0f) {
                    vy = -vy * collisionDamping;
                }
            }
            
            pX[i] = x;
            pY[i] = y;
            vX[i] = vx;
            vY[i] = vy;
        }
        
    }
    
    private void integrate(float dT) {
        for (int p=0; p<numParticles; p++) {
            vX[p] += aX[p]*dT;
            vY[p] += aY[p]*dT;
            
            pX[p] += vX[p]*dT;
            pY[p] += vY[p]*dT;
        }
    }
    
    private void predictPositions(float dtPrediction) {
        for (int i = 0; i < numParticles; i++) {
            pPredX[i] = pX[i] + vX[i] * dtPrediction;
            pPredY[i] = pY[i] + vY[i] * dtPrediction;
        }
    }
    
    private void stepSimulation(float dT) {
        predictPositions(dT);
        clearSpatialLookup();
        buildSpatialLookup();
        
        for (int p=0; p<numParticles; p++) {
            computeDensityAndPressure(p);
            applyForces(p);
        }
        
        applyMouseForce(dT);
        integrate(dT);
        
        applyCollisionForces();
        
        if (debug && VBMain.isTickIncrement(20)) debug();
    }
    
}
