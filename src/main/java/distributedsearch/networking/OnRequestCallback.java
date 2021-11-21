package distributedsearch.networking;

public interface OnRequestCallback {
    byte[] handleRequest(byte[] requestPayload);

    String getEndpoint();
}
