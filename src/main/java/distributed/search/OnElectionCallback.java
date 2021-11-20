package distributed.search;

public interface OnElectionCallback {
    void onElectedToBeLeader();

    void onWorker();
}
