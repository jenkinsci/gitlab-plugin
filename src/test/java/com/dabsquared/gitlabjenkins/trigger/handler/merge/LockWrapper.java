package com.dabsquared.gitlabjenkins.trigger.handler.merge;

/**
 * A wrapper offering some easy-to-use methods for blocking.
 * Primarily intended for testing multiple builds - the original inspiration was a "multi" version of {@link hudson.util.OneShotEvent}.
 *
 * @author benjie.gatt
 */
public final class LockWrapper {

    private final Object lock;
    
    public LockWrapper() {
        this.lock = this;
    }
    
    /**
     * Non-blocking method that signals this event.
     */
    public void signal() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }
    
    /**
     * Blocks until the event becomes the signaled state.
     *
     * If the specified amount of time elapses, this method returns null even if the value isn't offered.
     */
    public void block(long timeout) throws InterruptedException {
        synchronized (lock) {
            lock.wait(timeout);
        }
    }
}
