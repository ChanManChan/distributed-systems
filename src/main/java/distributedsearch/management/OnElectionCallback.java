package distributedsearch.management;

public interface OnElectionCallback {
    void onElectedToBeLeader();

    void onWorker();
}
