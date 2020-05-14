package co.payload.android;

import android.os.AsyncTask;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.*;

import co.payload.arm.ARMRequest;
import co.payload.arm.ARMObject;

public class Payload {
    private final static String TAG = Payload.class.getSimpleName();

    public interface Success<T> {
        void callback(T resp);
    }

    public interface Error {
        void callback(Throwable err);
    }

    public static class Promise {
        Req req;

        public Promise(Req req) {
            this.req = req;
        }

        public Promise then(final Success<?> callback) {
            this.req.success = callback;
            return this;
        }

        public Promise error(final Error callback) {
            this.req.error = callback;
            return this;
        }

    }

    private static class Req {
        ARMObject obj      = null;
        ARMRequest request = null;
        Class cls          = null;
        String action      = null;
        Success success    = null;
        Error error        = null;
        Object resp        = null;
        Throwable exc      = null;
        Object arg         = null;
    }

    static public Promise all(final ARMRequest request) {
        Req req = new Req();
        req.request = request;
        req.action = "all";
        new Payload.Run().execute(req);

        return new Promise(req);
    }


    static public Promise get(final Class cls, String id) {
        Req req = new Req();
        req.cls = cls;
        req.action = "get";
        req.arg = id;
        new Payload.Run().execute(req);

        return new Promise(req);
    }

    static public Promise create(final ARMObject obj) {
        Req req = new Req();
        req.obj = obj;
        req.action = "create";
        new Payload.Run().execute(req);

        return new Promise(req);
    }

    static public Promise update(final ARMObject obj, Map.Entry<String,Object>... args) {
        Req req = new Req();
        req.obj = obj;
        req.action = "update";
        req.arg = args;
        new Payload.Run().execute(req);

        return new Promise(req);
    }

    static public Promise delete(final ARMObject obj) {
        Req req = new Req();
        req.obj = obj;
        req.action = "delete";
        new Payload.Run().execute(req);

        return new Promise(req);
    }

    private static class Run extends AsyncTask<Req, Void, Req> {
        @Override
        protected Req doInBackground(Req... requests) {
            Req req = requests[0];

            try {
                Method action = null;
                Object data   = null;

                if (req.cls != null) {
                    action = Stream.of(req.cls.getMethods())
                        .filter((m) -> m.getName().equals(req.action))
                        .findFirst()
                        .get();
                    //action = req.obj.getClass().getMethod(req.action);
                    if (req.arg != null)
                        data = action.invoke(req.cls, req.arg);
                    else
                        data = action.invoke(req.obj);
                } else if (req.obj != null) {
                    action = Stream.of(req.obj.getClass().getMethods())
                        .filter((m) -> m.getName().equals(req.action))
                        .findFirst()
                        .get();
                    //action = req.obj.getClass().getMethod(req.action);
                    if (req.arg != null)
                        data = action.invoke(req.obj, req.arg);
                    else
                        data = action.invoke(req.obj);
                } else {
                    action = Stream.of(req.request.getClass().getMethods())
                        .filter((m) -> m.getName().equals(req.action))
                        .findFirst()
                        .get();

                    //action = req.request.getClass().getMethod(req.action);
                    if (req.arg != null)
                        data = action.invoke(req.request, req.arg);
                    else
                        data = action.invoke(req.request);
                }

                final Object resp = data;
                req.resp = resp;
                return req;
            } catch ( Exception exc ) {
                req.exc = exc.getCause();
                return req;
            }
        }

        @Override
        protected void onPostExecute(Req req) {
            if ( req.exc != null ) {
                if ( req.error != null )
                    req.error.callback(req.exc);
                else
                    Log.e(TAG, "exception", req.exc);
            } else if( ARMObject.class.isAssignableFrom(req.resp.getClass()) ) {
                req.success.callback((ARMObject) req.resp);
            } else {
                req.success.callback((List<ARMObject>) req.resp);
            }
        }

    }
}
