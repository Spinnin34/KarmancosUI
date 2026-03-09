package p.karmancos.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimatedGui extends BaseGui {

    private final Plugin plugin;
    private final Map<Integer, List<GuiItem>> frames;
    private final Map<Integer, Integer> frameDelays;
    private BukkitTask animationTask;
    private int currentFrame = 0;
    private long period = 20;
    private boolean loop = true;
    private boolean paused = false;
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
        frames.put(index, items);
    }

    public void addFrame(int index, List<GuiItem> items, int delayTicks) {
        frames.put(index, items);
        frameDelays.put(index, delayTicks);
    }

    /**
     * Set delay for a specific frame
     */
    public void setFrameDelay(int index, int delay) {
        frameDelays.put(index, delay);
    }

    /**
     * Set the base period for animation
     */
    public void setPeriod(long period) {
        this.period = period;
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
        if (frame >= 0 && frame < frames.size()) {
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
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }

        paused = false;
        currentFrame = 0;

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
        currentFrame++;
        if (currentFrame >= frames.size()) {
            if (loop) {
                currentFrame = 0;
            } else {
                currentFrame = frames.size() - 1;
                stopAnimation();
                if (onAnimationComplete != null) {
                    onAnimationComplete.run();
                }
            }
        }
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
            for (int i = 0; i < frameItems.size() && i < inventory.getSize(); i++) {
                GuiItem item = frameItems.get(i);
                if (item != null) {
                    setItem(i, item);
                }
            }
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        stopAnimation();
        frames.clear();
        frameDelays.clear();
    }
}

