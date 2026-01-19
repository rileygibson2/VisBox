package visbox;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import visbox.logger.LogColor;
import visbox.logger.LogColorEnum;
import visbox.logger.Logger;

@LogColor(LogColorEnum.CYAN)
public class AudioManager {

    public enum InputMode {
        MIC,
        FILE
    }
    
    private final String resourcePath;
    private Thread playbackThread;
    private volatile boolean playbackRunning;
    private Analyser analyser;
    private AudioFormat baseFormat;
    private InputMode inputMode;
    
    public AudioManager(Analyser analyser) {
        this.resourcePath = "/audio/daybreak.wav";
        this.analyser = analyser;
        this.playbackRunning = false;
        this.inputMode = InputMode.FILE;
    }
    
    public void start() {
        if (playbackThread != null && playbackThread.isAlive()) {
            Logger.info("Already running");
            return;
        }
        
        Runnable r = null;
        switch (inputMode) {
            case MIC: r = this::runMic; break;
            case FILE: r = this::runFile; break;
            default: break;
        }
        if (r==null) {
            Logger.error("No input mode set, cannot start");
            return;
        }
        playbackRunning = true;
        playbackThread = new Thread(r, "AudioManager-Playback");
        playbackThread.start();
    }
    
    public void stop() {
        playbackRunning = false;
        if (playbackThread != null) {
            try {
                playbackThread.join(1000); // wait up to 1s for clean shutdown
            } catch (InterruptedException ignored) {}
        }
        Logger.info("Stopped");
    }
    
    private void runFile() {
        Logger.info("Starting file playback from " + resourcePath);
        
        try (InputStream rawStream = AudioManager.class.getResourceAsStream(resourcePath)) {
            if (rawStream == null) {
                Logger.error("Resource not found: " + resourcePath);
                return;
            }
            
            try (AudioInputStream sourceStream =
                AudioSystem.getAudioInputStream(new BufferedInputStream(rawStream))) {
                    
                    // Ensure we have a PCM_SIGNED format the mixer can handle
                    baseFormat = sourceStream.getFormat();
                    AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false // little-endian
                    );
                    
                    try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(decodedFormat, sourceStream)) {
                        
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                            line.open(decodedFormat);
                            line.start();
                            int channels = baseFormat.getChannels(); // 1 = mono, 2 = stereo
                            int bytesPerFrame = 2 * channels;                // 16-bit * channels
                            byte[] buffer = new byte[4096];          // raw PCM bytes
                            // One float per *frame* (sample across channels)
                            float[] floatSamples = new float[buffer.length / (2 * channels)];
                            analyser.updateSampleRate(baseFormat.getFrameRate());
                            
                            int bytesRead;
                            while (playbackRunning && (bytesRead = pcmStream.read(buffer, 0, buffer.length)) != -1) {
                                int framesRead = bytesRead / bytesPerFrame; // how many sample-frames are in this buffer
                                
                                // Convert interleaved PCM to mono float samples
                                for (int frame = 0; frame < framesRead; frame++) {
                                    int baseIndex = frame * bytesPerFrame;
                                    float sum = 0f;
                                    
                                    for (int ch = 0; ch < channels; ch++) {
                                        int i = baseIndex + ch * 2;
                                        
                                        // little-endian 16-bit signed
                                        int lo = buffer[i] & 0xFF;
                                        int hi = buffer[i + 1] << 8;
                                        short sample = (short) (hi | lo);
                                        
                                        sum += sample / 32768.0f; // normalize to -1..1
                                    }
                                    
                                    floatSamples[frame] = sum / channels; // average channels → mono
                                }
                                
                                // Feed samples into FFT analyzer
                                if (analyser != null && framesRead > 0) {
                                    analyser.addSamples(floatSamples, framesRead);
                                }
                                
                                // Still play audio to speakers
                                line.write(buffer, 0, bytesRead);
                                
                            }
                            
                            line.drain();
                        }
                    }
                }
            } catch (UnsupportedAudioFileException e) {
                Logger.error("Unsupported audio file format: " + e.getMessage());
            } catch (LineUnavailableException e) {
                Logger.error("Audio line unavailable: " + e.getMessage());
            } catch (IOException e) {
                Logger.error("IO error: " + e.getMessage());
            }
            
            
            Logger.info("Playback thread exiting");
        }
        
        private void runMic() {
            Logger.info("Starting mic playback from " + resourcePath);
            
            AudioFormat format = new AudioFormat(
                44100f,
                16,
                1,
                true,
                false
            );
            TargetDataLine mic = null;
            
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    Logger.error("Mic format is not supported: "+format.toString());
                    return;
                }
                
                mic = (TargetDataLine) AudioSystem.getLine(info);
                mic.open(format);
                mic.start();
                
                baseFormat = format;
                analyser.updateSampleRate(format.getSampleRate());
                
                byte[] buffer = new byte[4096];          // raw PCM bytes
                // One float per *frame* (sample across channels)
                float[] floatSamples = new float[buffer.length/2];
                analyser.updateSampleRate(baseFormat.getFrameRate());
                
                while (playbackRunning) {
                    int bytesRead = mic.read(buffer, 0, buffer.length);
                    if (bytesRead<=0) continue;
                    int framesRead = bytesRead/2; // how many sample-frames are in this buffer
                    
                    // Convert interleaved PCM to mono float samples
                    for (int i=0; i<framesRead; i++) {
                        int lo = buffer[2*i] & 0xFF;
                        int hi = buffer[2*i + 1] << 8;
                        short sample = (short) (hi | lo);
                        floatSamples[i] = sample / 32768.0f; // normalize to -1..1
                    }
                    
                    // Feed samples into FFT analyzer
                    if (analyser != null && framesRead > 0) {
                        analyser.addSamples(floatSamples, framesRead);
                    }
                }
            } catch (LineUnavailableException e) {
                Logger.error("Audio line unavailable: " + e.getMessage());
            } finally {
                if (mic!=null) {
                    mic.stop();
                    mic.close();
                }
            }
            Logger.info("Playback thread exiting");
        }
        
        public float getSampleRate() {
            if (baseFormat!=null) return baseFormat.getFrameRate();
            return 0f;
        }
    }