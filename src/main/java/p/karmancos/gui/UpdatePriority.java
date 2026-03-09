package p.karmancos.gui;

public enum UpdatePriority {
    LOW(100),
    NORMAL(50),
    HIGH(10),
    IMMEDIATE(0);

    private final int delayTicks;

    UpdatePriority(int delayTicks) {
        this.delayTicks = delayTicks;
    }

    public int getDelayTicks() {
        return delayTicks;
    }
}

