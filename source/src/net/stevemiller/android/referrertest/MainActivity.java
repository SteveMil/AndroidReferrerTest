package net.stevemiller.android.referrertest;

import java.util.Observable;
import java.util.Observer;

import net.stevemiller.android.referrertest.Logger.LogEntry;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

//******************************************************************************
public class MainActivity extends ListActivity implements Observer
{
    //--------------------------------------------------------------------------
    public MainActivity()
    {
        Logger.log(this, "Activity.Activity()", getAndClearIntent());
    }

    //--------------------------------------------------------------------------
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Logger.log(this, "Activity.onCreate(Bundle)", getAndClearIntent());
        super.onCreate(savedInstanceState);

        // Bind our ListView to the list of LogEntry objects.
        setListAdapter(new LogAdpater(this));

        // Listen for log events so we know when to update our UI.
        Logger.getObservable().addObserver(this);
    }

    //--------------------------------------------------------------------------
    @Override protected void onStart()
    {
        Logger.log(this, "Activity.onStart()", getAndClearIntent());
        super.onStart();
    }

    //--------------------------------------------------------------------------
    @Override protected void onRestart()
    {
        Logger.log(this, "Activity.onRestart()", getAndClearIntent());
        super.onRestart();
    }

    //--------------------------------------------------------------------------
    @Override protected void onResume()
    {
        Logger.log(this, "Activity.onResume()", getAndClearIntent());
        super.onResume();
    }

    //--------------------------------------------------------------------------
    @Override protected void onPause()
    {
        Logger.log(this, "Activity.onPause()", getAndClearIntent());
        super.onPause();
    }

    //--------------------------------------------------------------------------
    @Override protected void onStop()
    {
        Logger.log(this, "Activity.onStop()", getAndClearIntent());
        super.onStop();
    }

    //--------------------------------------------------------------------------
    @Override protected void onDestroy()
    {
        Logger.log(this, "Activity.onDestroy()", getAndClearIntent());

        // Stop listening for log events.
        Logger.getObservable().deleteObserver(this);

        super.onDestroy();
    }

    //--------------------------------------------------------------------------
    @Override protected void onNewIntent(Intent intent)
    {
        Logger.log(this, "Activity.onNewIntent(Intent)", intent);
        super.onNewIntent(intent);
    }

    //--------------------------------------------------------------------------
    @Override public boolean onCreateOptionsMenu(android.view.Menu menu)
    {
        // Inflate our menu items.
        getMenuInflater().inflate(R.menu.main, menu);

        // If we cannot launch the uninstall intent on this platform, then hide
        // the uninstall menu item.
        if (!isIntentAvailable(getUninstallIntent()))
        {
            menu.removeItem(R.id.menuitem_uninstall);
        }

        return super.onCreateOptionsMenu(menu);
    }

    //--------------------------------------------------------------------------
    @Override public boolean onOptionsItemSelected(android.view.MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menuitem_clear:
                Logger.clear(this);
                return true;

            case R.id.menuitem_uninstall:
                startActivity(getUninstallIntent());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //--------------------------------------------------------------------------
    @Override public void update(Observable observable, Object data)
    {
        // Any time the list of LogEntry objects is updated, we also update our UI.
        ((LogAdpater)getListAdapter()).notifyDataSetChanged();
    }

    //--------------------------------------------------------------------------
    private Intent getAndClearIntent()
    {
        // Get our intent and then clear it so we don't log it more than once.
        Intent intent = getIntent();
        if (null != intent)
        {
            setIntent(null);
        }
        return intent;
    }

    //--------------------------------------------------------------------------
    private Intent getUninstallIntent()
    {
        return new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + getPackageName()));
    }

    //--------------------------------------------------------------------------
    private boolean isIntentAvailable(Intent intent)
    {
        return (getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0);
    }

    //**************************************************************************
    private static class LogAdpater extends ArrayAdapter<LogEntry>
    {
        //----------------------------------------------------------------------
        public LogAdpater(Context context)
        {
            super(context, R.layout.log_entry, R.id.text_1, Logger.getLogEntries(context));
        }

        //----------------------------------------------------------------------
        @Override public View getView(int position, View convertView, ViewGroup parent)
        {
            // Let the ArrayAdapter create the view and populate the main text field.
            convertView = super.getView(position, convertView, parent);

            // Get the LogEntry for this cell.
            LogEntry logEntry = ((position >= 0) && (position < getCount())) ? getItem(position) : null;

            // Fill in our second line of text.
            if (null != convertView)
            {
                ((TextView)convertView.findViewById(R.id.text_2)).setText((null != logEntry) ? logEntry.getLine2() : "");
            }

            return convertView;
        }
    }
}
