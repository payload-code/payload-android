package co.payload.android;

import org.json.*;

import android.os.AsyncTask;
import android.util.Log;
import android.content.Intent;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Field;
import java.util.ArrayList;

import co.payload.arm.ARMRequest;
import co.payload.arm.ARMObject;
import co.payload.arm.ARMObject;
import co.payload.pl;
import co.payload.Exceptions;
import co.payload.Utils;

public class Payload {
    public final static String TAG = Payload.class.getSimpleName();

    public interface Response<T> {
        void callback(T resp);
    }

    public interface Error {
        void callback(Throwable err);
    }

    public static class BasePromise<T> {
        public Req req;

        public BasePromise(Req req) {
            this.req = req;
        }

        public T then(final Response<?> callback) {
            this.req.success_cbs.add(callback);
            return (T)this;
        }

        public T error(final Error callback) {
            this.req.error_cbs.add(callback);
            return (T)this;
        }

        public void success(Object obj) {
            this.req.success(obj);
        }

        public void failure(Throwable err) {
            this.req.failure(err);
        }

    }

    public static class Promise extends BasePromise<Promise> {
        public Promise(Req req) {
            super(req);
        }
    }

    public static class Req {
        ARMObject obj      = null;
        ARMRequest request = null;
        Class cls          = null;
        String action      = null;
        List<Response> success_cbs = null;
        List<Error> error_cbs      = null;
        Object resp        = null;
        Throwable exc      = null;
        Object arg         = null;

        Req() {
            this.success_cbs = new ArrayList<Response>();
            this.error_cbs   = new ArrayList<Error>();
        }

        public void success(Object obj) {
            for ( int i = 0; i < this.success_cbs.size(); i++ )
                this.success_cbs.get(i).callback(obj);
        }

        public void failure(Throwable err) {
            for ( int i = 0; i < this.error_cbs.size(); i++ )
                this.error_cbs.get(i).callback(err);
        }

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

    static public Checkout checkout(final ARMObject obj) {
        return new Checkout(obj);
    }

    static public Form submit(View view, ARMObject obj) {
        Req req = new Req();
        req.action = "create";
        return new Form(view, obj).submit();
    }

    private static class Run extends AsyncTask<Req, Void, Req> {
        @Override
        protected Req doInBackground(Req... requests) {
            Req req = requests[0];
            req.resp = null;
            req.exc = null;

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
                } else if ( req.request != null ) {
                    action = Stream.of(req.request.getClass().getMethods())
                        .filter((m) -> m.getName().equals(req.action))
                        .findFirst()
                        .get();

                    //action = req.request.getClass().getMethod(req.action);
                    if (req.arg != null)
                        data = action.invoke(req.request, req.arg);
                    else
                        data = action.invoke(req.request);
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
                if ( req.error_cbs.size() > 0 )
                    req.failure(req.exc);
                else
                    Log.e(TAG, "exception", req.exc);
            } else if( ARMObject.class.isAssignableFrom(req.resp.getClass()) ) {
                req.success((ARMObject) req.resp);
            } else {
                req.success((List<ARMObject>) req.resp);
            }
        }

    }

    public static class PaymentPromise<T> extends BasePromise<T> {
        List<Response> processed_cbs;
        List<Response> declined_cbs;

        public PaymentPromise(Req req) {
            super(req);

            this.processed_cbs = new ArrayList<Response>();
            this.declined_cbs = new ArrayList<Response>();

            this.then((pl.Payment pmt) -> {
                if (pmt.getStr("status").equals("processed")) {
                    for ( int i = 0; i < this.processed_cbs.size(); i++ )
                        this.processed_cbs.get(i).callback(pmt);
                }

                if (pmt.getStr("status").equals("declined"))  {
                    for ( int i = 0; i < this.declined_cbs.size(); i++ )
                        this.declined_cbs.get(i).callback(pmt);
                }
            });
        }

        public T processed(final Response<?> callback) {
            this.processed_cbs.add(callback);
            return (T)this;
        }

        public T declined(final Response<?> callback) {
            this.declined_cbs.add(callback);
            return (T)this;
        }

    }

    public static class Checkout extends PaymentPromise<Checkout> {
        public Form form;
        private CheckoutModal checkout_dialog;

        public Checkout(final ARMObject obj) {
            super(new Req());

            req.obj = obj;
            req.action = "create";

            Activity activity = getActivity();
            checkout_dialog = new CheckoutModal(this, (pl.Payment)this.req.obj);
            checkout_dialog.show(activity.getFragmentManager(), "dialog");

        }

        public void initForm() {
            this.form = new Form(this.checkout_dialog.getView(), this.req.obj);

            this.form
                .then((pl.Payment payment) -> {
                    Checkout.this.success(payment);
                }).processed((pl.Payment payment) -> {
                    Checkout.this.checkout_dialog.dismiss();
                }).error((Throwable err) -> {
                    Checkout.this.failure(err);
                });
        }

        public static Activity getActivity() {
            try {
                Class activityThreadClass = Class.forName("android.app.ActivityThread");
                Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
                Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
                activitiesField.setAccessible(true);

                Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
                if (activities == null)
                    return null;

                for (Object activityRecord : activities.values()) {
                    Class activityRecordClass = activityRecord.getClass();
                    Field pausedField = activityRecordClass.getDeclaredField("paused");
                    pausedField.setAccessible(true);
                    if (!pausedField.getBoolean(activityRecord)) {
                        Field activityField = activityRecordClass.getDeclaredField("activity");
                        activityField.setAccessible(true);
                        Activity activity = (Activity) activityField.get(activityRecord);
                        return activity;
                    }
                }

                return null;
            } catch (Exception exc) {
                return null;
            }
        }
    }

    public static class Form extends PaymentPromise<Form> {
        private View view;

        private ARMObject obj;

        public Form(View view, ARMObject obj) {
            super(new Req());
            this.view = view;
            this.obj = obj;
            this.req.obj = obj;
            this.req.action = "create";

            this.error((Throwable err) -> {
                if (err instanceof Exceptions.InvalidAttributes)
                    handleInvalidAttributes((Exceptions.InvalidAttributes)err);
            });
        }

        public Form submit() {
            if (this.obj instanceof pl.Payment)
                this.req.obj = new pl.Payment(){{}};
            else if (this.obj instanceof pl.Card)
                this.req.obj = new pl.Card(){{}};
            else if (this.obj instanceof pl.BankAccount)
                this.req.obj = new pl.BankAccount(){{}};
            else
                this.req.obj = new pl.PaymentMethod(){{}};

            try {
                ((ARMObject)this.req.obj).setJson(new JSONObject(this.obj.obj.toString()));
            } catch (JSONException e) {}

            this.populateObj();
            this.clearErrors();

            new Payload.Run().execute(this.req);
            return this;
        }


        public void populateObj() {
            List<View> inputs = this.getViewsByTagPrefix((ViewGroup)this.view, "pl:");

            for (int i = 0; i < inputs.size(); i++) {
                if (!(inputs.get(i) instanceof EditText))
                    continue;
                EditText input = (EditText)inputs.get(i);
                this.req.obj.set(Input.mapAttr(
                    this.getObjectType(),
                    input.getTag().toString().substring(3)),
                    input.getText().toString()
                );
            }

        }

        public String getObjectType() {
            return this.obj instanceof pl.Payment?"payment":"payment_method";
        }

        private void clearErrors() {
            List<View> inputs = this.getViewsByTagPrefix((ViewGroup)this.view, "pl:");

            for (int i = 0; i < inputs.size(); i++) {
                if (!(inputs.get(i) instanceof EditText))
                    continue;

                EditText input = (EditText)inputs.get(i);

                input.setError(null);
            }

        }

        private void handleInvalidAttributes(Exceptions.InvalidAttributes err) {

            try {
                JSONObject details = err.data.getJSONObject("details");
                Map<String, Object> attrs = Utils.flattenJSON(details);
                List<View> inputs = this.getViewsByTagPrefix((ViewGroup)this.view, "pl:");

                for (int i = 0; i < inputs.size(); i++) {
                    if (!(inputs.get(i) instanceof EditText))
                        continue;

                    EditText input = (EditText)inputs.get(i);
                    String attr_name = Input.mapAttr(this.getObjectType(), input.getTag().toString().substring(3));
                    if (!attrs.containsKey(attr_name))
                        continue;

                    input.setError((String)attrs.get(attr_name));
                }
            } catch(JSONException e) {}
        }

        private ArrayList<View> getViewsByTagPrefix(ViewGroup root, String prefix){
            ArrayList<View> views = new ArrayList<View>();
            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = root.getChildAt(i);
                if (child instanceof ViewGroup) {
                    views.addAll(getViewsByTagPrefix((ViewGroup) child, prefix));
                }

                final Object tagObj = child.getTag();
                if (tagObj != null && tagObj.toString().startsWith(prefix)) {
                    views.add(child);
                }

            }
            return views;
        }
    }
}
