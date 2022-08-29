package org.gtp.cocacolaproject.presenter.state;

import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImpl;
import org.gtp.cocacolaproject.presenter.StateImplListener;
import org.gtp.cocacolaproject.presenter.Transition;

public class ShowOrderState extends StateImpl {

    private final State state = State.SHOW_ORDER;

    /**
     * show location and wait for hunter to notify the location is correct before enabling tensorFlow
     *
     * @param listener listener to report progress
     */
    public ShowOrderState(StateImplListener listener) {
        super(listener);

        onProgress(Transition.INITIAL);
    }

    @Override
    public void start() {

        onProgress(Transition.RUNNING);
        onProgress(Transition.SUCCESSFUL);
    }

    @Override
    public void abort() {

    }

    @Override
    public void progress(Transition transition, String message) {
        mListener.onProgress(state, transition);
    }
}
