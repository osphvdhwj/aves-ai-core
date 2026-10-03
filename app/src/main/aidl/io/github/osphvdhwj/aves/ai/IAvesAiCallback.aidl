package io.github.osphvdhwj.aves.ai;

oneway interface IAvesAiCallback {
    void onProgress(long requestId, int percent);
    void onResult(long requestId, in Bundle result);
    void onError(long requestId, int code, String message);
}
