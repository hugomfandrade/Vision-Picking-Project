package org.gtp.cocacolaproject.common;

public class AsynchronousForCycleStatus {

    private final int mTotalOperations;
    private int mCompletedOperations;
    private boolean mAborted;

    public AsynchronousForCycleStatus(int totalOperations) {
        mTotalOperations = totalOperations;
        mCompletedOperations = 0;
        mAborted = false;
    }

    public synchronized boolean isAborted() {
        return mAborted;
    }

    public synchronized void abort() {
        mAborted = true;
    }

    public synchronized void operationCompleted() {
        mCompletedOperations++;
    }

    public synchronized boolean isFinished() {
        return mTotalOperations == mCompletedOperations;
    }

    @Override
    public String toString() {
        return mCompletedOperations + " of " + mTotalOperations;
    }
}
