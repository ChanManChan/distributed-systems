package distributed.search;

public interface OnRequestCallback {
    byte[] handleRequest(byte[] requestPayload);

    String getEndpoint();
}
