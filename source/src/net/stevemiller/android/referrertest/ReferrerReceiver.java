package net.stevemiller.android.referrertest;

import java.net.URLDecoder;
import java.util.Observable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


//******************************************************************************
public class ReferrerReceiver extends BroadcastReceiver
{
    private static final ObservableChanged _observable = new ObservableChanged();

    //--------------------------------------------------------------------------
    public static Observable getObservable()
    {
        return _observable;
    }

    //--------------------------------------------------------------------------
    public static String getReferrer(Context context)
    {
        // Return any persisted referrer value or null if we don't have a referrer.
        return context.getSharedPreferences("referrer", Context.MODE_PRIVATE).getString("referrer", null);
    }

    //--------------------------------------------------------------------------
    public ReferrerReceiver()
    {
        Logger.log(null, "ReferrerReceiver.ReferrerReceiver()");
    }

    //--------------------------------------------------------------------------
    @Override public void onReceive(Context context, Intent intent)
    {
        Logger.log(context, "ReferrerReceiver.onReceive(Context, Intent)", intent);

        try
        {
            // Make sure this is the intent we expect - it always should be.
            if ((null != intent) && (intent.getAction().equals("com.android.vending.INSTALL_REFERRER")))
            {
                // This intent should have a referrer string attached to it.
                String rawReferrer = intent.getStringExtra("referrer");
                if (null != rawReferrer)
                {
                    // The string is usually URL Encoded, so we need to decode it.
                    String referrer = URLDecoder.decode(rawReferrer, "UTF-8");

                    // Log the referrer string.
                    Logger.log(context,
                        "ReferrerReceiver.onReceive(Context, Intent)" +
                        "\nRaw referrer: " + rawReferrer +
                        "\nReferrer: " + referrer);

                    // Persist the referrer string.
                    context.getSharedPreferences("referrer", Context.MODE_PRIVATE).
                        edit().putString("referrer", referrer).commit();

                    // Let any listeners know about the change.
                    _observable.notifyObservers(referrer);
                }
            }
        }
        catch (Exception e)
        {
            Logger.log(context, e.toString());
        }
    }

    //**************************************************************************
    protected static class ObservableChanged extends Observable
    {
        //----------------------------------------------------------------------
        @Override public boolean hasChanged()
        {
            return true;
        }
    }
}
