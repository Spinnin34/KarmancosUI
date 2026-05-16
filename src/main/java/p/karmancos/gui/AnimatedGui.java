package p.karmancos.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

public class AnimatedGui extends BaseGui {

    private final Plugin plugin;
    private final Map<Integer, List<GuiItem>> frames;
    private final Map<Integer, Integer> frameDelays;
    private BukkitTask animationTask;
    private int currentFrame = 0;
    private long period = 20;
    private boolean loop = true;
    private boolean paused = false;
    private int lastRenderedFrameSize = 0;
    private Runnable onAnimationComplete;

    public AnimatedGui(Plugin plugin, int rows, Component title) {
        super(rows, title);
        this.plugin = plugin;
        this.frames = new HashMap<>();
        this.frameDelays = new HashMap<>();
    }

    public AnimatedGui(Plugin plugin, int rows, String title) {
        super(rows, title);
        this.plugin = plugin;
        this.frames = new HashMap<>();
        this.frameDelays = new HashMap<>();
    }

    public void addFrame(int index, List<GuiItem> items) {
        frames.put(index, items == null ? List.of() : new ArrayList<>(items));
    }

    public void addFrame(int index, List<GuiItem> items, int delayTicks) {
        frames.put(index, items == null ? List.of() : new ArrayList<>(items));
        frameDelays.put(index, Math.max(0, delayTicks));
    }

    /**
     * Set delay for a specific frame
     */
    public void setFrameDelay(int index, int delay) {
        frameDelays.put(index, Math.max(0, delay));
    }

    /**
     * Set the base period for animation
     */
    public void setPeriod(long period) {
        this.period = Math.max(1, period);
    }

    /**
     * Set whether animation should loop
     */
    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    /**
     * Set callback for when animation completes (non-looping only)
     */
    public void setOnAnimationComplete(Runnable callback) {
        this.onAnimationComplete = callback;
    }

    /**
     * Get total number of frames
     */
    public int getFrameCount() {
        return frames.size();
    }

    /**
     * Get current frame index
     */
    public int getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Jump to a specific frame
     */
    public void goToFrame(int frame) {
        if (frames.containsKey(frame)) {
            currentFrame = frame;
            updateFrame();
        }
    }

    /**
     * Pause the animation
     */
    public void pause() {
        paused = true;
    }

    /**
     * Resume the animation
     */
    public void resume() {
        paused = false;
    }

    /**
     * Toggle pause state
     */
    public void togglePause() {
        paused = !paused;
    }

    /**
     * Check if animation is paused
     */
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void open(Player player) {
        super.open(player);
        if (!paused) {
            startAnimation();
        }
    }

    /**
     * Start the animation
     */
    public void startAnimation() {
        if (frames.isEmpty()) {
            return;
        }
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }

        paused = false;
        currentFrame = firstFrameKey();

        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (viewers.isEmpty()) {
                stopAnimation();
                return;
            }

            if (paused) return;

            updateFrame();

            int delay = frameDelays.getOrDefault(currentFrame, 0);
            if (delay > 0) {
                paused = true;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    paused = false;
                    advanceFrame();
                }, delay);
            } else {
                advanceFrame();
            }
        }, 0L, period);
    }

    private void advanceFrame() {
        if (frames.isEmpty()) {
            stopAnimation();
            return;
        }

        List<Integer> keys = sortedFrameKeys();
        int currentIndex = keys.indexOf(currentFrame);
        int nextIndex = currentIndex + 1;

        if (currentIndex == -1 || nextIndex >= keys.size()) {
            if (loop && !keys.isEmpty()) {
                currentFrame = keys.getFirst();
                return;
            }
            currentFrame = keys.getLast();
            stopAnimation();
            if (onAnimationComplete != null) {
                onAnimationComplete.run();
            }
            return;
        }

        currentFrame = keys.get(nextIndex);
    }

    public void stopAnimation() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
        paused = false;
    }

    /**
     * Restart the animation from the beginning
     */
    public void restart() {
        stopAnimation();
        startAnimation();
    }

    private void updateFrame() {
        List<GuiItem> frameItems = frames.get(currentFrame);
        if (frameItems != null) {
            for (int i = 0; i < lastRenderedFrameSize && i < inventory.getSize(); i++) {
                removeItem(i);
            }
            for (int i = 0; i < frameItems.size() && i < inventory.getSize(); i++) {
                GuiItem item = frameItems.get(i);
                if (item != null) {
                    setItem(i, item);
                }
            }
            lastRenderedFrameSize = Math.min(frameItems.size(), inventory.getSize());
        }
    }

    @Override
    protected void prepareForBedrockForm(Player player) {
        if (!frames.isEmpty() && !frames.containsKey(currentFrame)) {
            currentFrame = firstFrameKey();
        }
        updateFrame();
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        stopAnimation();
    }

    public void clearFrames() {
        frames.clear();
        frameDelays.clear();
        lastRenderedFrameSize = 0;
    }

    private int firstFrameKey() {
        return sortedFrameKeys().getFirst();
    }

    private List<Integer> sortedFrameKeys() {
        List<Integer> keys = new ArrayList<>(frames.keySet());
        Collections.sort(keys);
        return keys;
    }
}

