/*
 * Requests for Android
 * Copyright (C) 2016-2019 CodeBasePK
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pk.codebase.requests;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HTTPRequest {

    private static final String TAG = HTTPRequest.class.getName();

    private final String CONTENT_TYPE_JSON = "application/json";
    private final String CONTENT_TYPE_FORM = String.format(
            "multipart/form-data; boundary=%s", FormData.BOUNDARY);
    private final String CONTENT_TYPE_URL_ENCODED = "application/x-www-form-urlencoded";

    private OnErrorListener mOnErrorListener;
    private OnFileUploadProgressListener mOnFileUploadProgressListener;
    private OnResponseListener mOnResponseListener;

    private ExecutorService mThread;
    private Handler mHandler;
    private Map<String, String> mHeaders;

    public HTTPRequest() {
        mHandler = new Handler(Looper.getMainLooper());
        mThread = Executors.newSingleThreadExecutor();
        mHeaders = new HashMap<>();
    }

    public interface OnErrorListener {
        void onError(HTTPError error);
    }

    public interface OnFileUploadProgressListener {
        void onFileUploadProgress(File file, long uploaded, long total);
    }

    public interface OnResponseListener {
        void onResponse(HTTPResponse response);
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public void setOnFileUploadProgressListener(OnFileUploadProgressListener listener) {
        mOnFileUploadProgressListener = listener;
    }

    public void setOnResponseListener(OnResponseListener listener) {
        mOnResponseListener = listener;
    }

    private void request(String method, String url, Object payload, Map<String, String> headers,
                         HTTPOptions options) {
        HTTP http = new HTTP();
        http.setUploadProgressListener(this::emitOnFileUploadProgress);
        Map<String, String> actualHeaders = headers;
        if (actualHeaders == null) {
            actualHeaders = new HashMap<>();
        }

        if (!method.equals("GET")) {
            if (payload instanceof FormData) {
                actualHeaders.put("Content-Type", CONTENT_TYPE_FORM);
            } else if (!actualHeaders.containsKey("Content-Type") ||
                    !actualHeaders.containsKey("content-type")) {
                actualHeaders.put("Content-Type", CONTENT_TYPE_JSON);
            }
        }
        try {
            HTTPResponse response = http.request(method, url, payload, actualHeaders,
                    options.connectTimeout, options.readTimeout);
            emitOnResponse(response);
        } catch (HTTPError error) {
            emitOnError(error);
        } catch (Exception e) {
            emitOnError(new HTTPError(HTTPError.UNKNOWN, HTTPError.STAGE_UNKNOWN, e));
        }
    }

    private void actuallyGet(String url, Map<String, String> headers, HTTPOptions options) {
        mThread.submit(() -> request("GET", url, null, headers, options));
    }

    public void get(String url) {
        actuallyGet(url, null, new HTTPOptions());
    }

    public void get(String url, Map<String, String> headers) {
        actuallyGet(url, headers, new HTTPOptions());
    }

    public void get(String url, HTTPOptions options) {
        actuallyGet(url, null, options);
    }

    public void get(String url, Map<String, String> headers, HTTPOptions options) {
        actuallyGet(url, headers, options);
    }

    private void actuallyPost(String url, Object payload, Map<String, String> headers,
                              HTTPOptions options) {
        if (!headers.containsKey("Content-Type") || !headers.containsKey("content-type")) {
            headers.put("Content-Type", CONTENT_TYPE_JSON);
        }
        mThread.submit(() -> request("POST", url, payload, headers, options));
    }

    public void post(String url, String payload) {
        actuallyPost(url, payload, new HashMap<>(), new HTTPOptions());
    }

    public void post(String url, String payload, Map<String, String> headers) {
        actuallyPost(url, payload, headers, new HTTPOptions());
    }

    public void post(String url, FormData payload, Map<String, String> headers) {
        actuallyPost(url, payload, headers, new HTTPOptions());
    }

     public void post(String url, JSONObject payload, Map<String, String> headers) {
         actuallyPost(url, payload, headers, new HTTPOptions());
    }

     public void post(String url, JSONArray payload, Map<String, String> headers) {
        actuallyPost(url, payload, headers, new HTTPOptions());
    }

     public void post(String url, Object pojo, Map<String, String> headers, HTTPOptions options) {
         actuallyPost(url, pojo, headers, options);
    }

    private void actuallyDelete(String url, Object payload, Map<String, String> headers,
                                HTTPOptions options) {
        mThread.submit(() -> request("DELETE", url, payload, headers, options));
    }

    public void delete(String url, Object payload, Map<String, String> headers,
                       HTTPOptions options) {
        actuallyDelete(url, payload, headers, options);
    }

    private void actuallyPut(String url, Object payload, Map<String, String> headers,
                                HTTPOptions options) {
        mThread.submit(() -> request("PUT", url, payload, headers, options));
    }

    public void put(String url, Object payload, Map<String, String> headers, HTTPOptions options) {
        actuallyPut(url, payload, headers, options);
    }

    private void actuallyPatch(String url, Object payload, Map<String, String> headers,
                             HTTPOptions options) {
        mThread.submit(() -> request("PATCH", url, payload, headers, options));
    }

    public void patch(String url, Object payload, Map<String, String> headers,
                      HTTPOptions options) {
        actuallyPatch(url, payload, headers, options);
    }

    private void emitOnResponse(HTTPResponse response) {
        if (mOnResponseListener != null) {
            mHandler.post(() -> mOnResponseListener.onResponse(response));
        }
    }

    private void emitOnFileUploadProgress(File file, long loaded, long total) {
        if (mOnFileUploadProgressListener != null) {
            mHandler.post(() -> mOnFileUploadProgressListener.onFileUploadProgress(
                    file, loaded, total));
        }
    }

    private void emitOnError(HTTPError error) {
        if (mOnErrorListener != null) {
            mHandler.post(() -> mOnErrorListener.onError(error));
        }
    }
}
