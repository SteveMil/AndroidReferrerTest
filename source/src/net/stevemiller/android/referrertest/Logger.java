package net.stevemiller.android.referrertest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

//******************************************************************************
public abstract class Logger
{
    private static final String               TAG         = "ReferrerTest";
    private static final ObservableChanged    _observable = new ObservableChanged();
    private static final LinkedList<LogEntry> _logEntries = new LinkedList<LogEntry>();
    private static final LinkedList<LogEntry> _logToWrite = new LinkedList<LogEntry>();
    private static       File                 _file;
    private static       boolean              _readLogComplete;

    //--------------------------------------------------------------------------
    public static void log(Context context, String string)
    {
        // Build a new log entry and add it to our main list.
        LogEntry logEntry = new LogEntry(string);
        _logEntries.add(logEntry);

        // Also add it to the list of log we want to write to disk.
        _logToWrite.add(logEntry);

        // Read in any prior log we might have from a previous run.
        // If we cannot read yet, then no reason to try to write.
        if (readLog(context))
        {
            FileOutputStream     fileOutputStream     = null;
            BufferedOutputStream bufferedOutputStream = null;
            ObjectOutputStream   objectOutputStream   = null;

            try
            {
                // Check if we are appending.
                boolean append = (_file.length() > 0);

                // Open our log file for append.
                fileOutputStream     = new FileOutputStream(_file, append);
                bufferedOutputStream = new BufferedOutputStream(fileOutputStream, 1024);
                objectOutputStream   = append ? new ObjectOutputStreamAppender(bufferedOutputStream) : new ObjectOutputStream(bufferedOutputStream);

                // Write all queued log entries to our file.
                while (!_logToWrite.isEmpty())
                {
                    objectOutputStream.writeObject(_logToWrite.removeFirst());
                }

                // Flush to disk.
                objectOutputStream.flush();
            }
            catch (Exception e)
            {
                Log.e(TAG, e.toString());
            }
            finally
            {
                // Close our file.
                if (null != objectOutputStream)   try { objectOutputStream.close();   } catch (Exception e) { Log.e(TAG, e.toString()); }
                if (null != bufferedOutputStream) try { bufferedOutputStream.close(); } catch (Exception e) { Log.e(TAG, e.toString()); }
                if (null != fileOutputStream)     try { fileOutputStream.close();     } catch (Exception e) { Log.e(TAG, e.toString()); }
            }
        }

        // If anyone cares that the list changed, let them know now.
        _observable.notifyObservers();
    }

    //--------------------------------------------------------------------------
    public static void log(Context context, String string, Intent intent)
    {
        if (null == intent)
        {
            log(context, string);
        }
        else
        {
            StringBuilder strbld = new StringBuilder(2048);
            strbld.append(string);
            dumpIntent(strbld, intent);
            log(context, strbld.toString());
        }
    }

    //--------------------------------------------------------------------------
    public static void dumpIntent(StringBuilder strbld, Intent intent)
    {
        strbld.append("\n-=- INTENT -=-");

        if (null == intent)
        {
            strbld.append("\nIntent: null");
            return;
        }

        strbld.append("\ntoString: "           + intent.toString());
        strbld.append("\ndescribeContents: "   + intent.describeContents());
        strbld.append("\ngetAction: "          + intent.getAction());
        strbld.append("\ngetData: "            + intent.getData());
        strbld.append("\ngetDataString: "      + intent.getDataString());
        strbld.append("\ngetFlags: "           + String.format("0x%08X",  intent.getFlags()));
        strbld.append("\ngetScheme: "          + intent.getScheme());
        strbld.append("\ngetType: "            + intent.getType());
        strbld.append("\ngetComponent: "       + intent.getComponent());
        strbld.append("\nhasFileDescriptors: " + intent.hasFileDescriptors());

        if (null != intent.getComponent())
        {
            strbld.append("\nComponentName.describeContents: "     + intent.getComponent().describeContents());
            strbld.append("\nComponentName.flattenToShortString: " + intent.getComponent().flattenToShortString());
            strbld.append("\nComponentName.flattenToString: "      + intent.getComponent().flattenToString());
            strbld.append("\nComponentName.getClassName: "         + intent.getComponent().getClassName());
            strbld.append("\nComponentName.getPackageName: "       + intent.getComponent().getPackageName());
            strbld.append("\nComponentName.getShortClassName: "    + intent.getComponent().getShortClassName());
            strbld.append("\nComponentName.toShortString: "        + intent.getComponent().toShortString());
            strbld.append("\nComponentName.toString: "             + intent.getComponent().toString());
        }

        Set<String> categories = intent.getCategories();
        if (null != categories)
        {
            for (String category : categories)
            {
                strbld.append("\nCategory: " + category);
            }
        }

        Bundle bundle = intent.getExtras();
        if (null != bundle)
        {
            dumpBundle(bundle, strbld);
        }
    }

    //--------------------------------------------------------------------------
    public static void dumpBundle(Bundle bundle, StringBuilder strbld)
    {
        strbld.append("\n-=- BUNDLE -=-");

        if (null == bundle)
        {
            strbld.append("\nBundle: null");
            return;
        }

        strbld.append("\ntoString: "           + bundle.toString());
        strbld.append("\ndescribeContents: "   + bundle.describeContents());
        strbld.append("\nsize: "               + bundle.size());
        strbld.append("\nhasFileDescriptors: " + bundle.hasFileDescriptors());

        Set<String> keys = bundle.keySet();
        if (null != keys)
        {
            for (String key : keys)
            {
                Object object = bundle.get(key);
                strbld.append("\nKey: " + key + ", Value(" + ((null != object) ? object.getClass().getSimpleName() : "Object") + "): " + object);
            }
        }
    }

    //--------------------------------------------------------------------------
    public static void clear(Context context)
    {
        boolean notify = (_logEntries.size() > 0);

        // Delete the log file on disk.
        if (buildPath(context) && _file.exists())
        {
            _file.delete();
        }

        // Clear all current log.
        _logEntries.clear();
        _logToWrite.clear();

        // If the list changed and anyone cares that the list changed, let them know now.
        if (notify)
        {
            _observable.notifyObservers();
        }
    }

    //--------------------------------------------------------------------------
    public static List<LogEntry> getLogEntries(Context context)
    {
        readLog(context);
        return _logEntries;
    }

    //--------------------------------------------------------------------------
    public static Observable getObservable()
    {
        return _observable;
    }

    //--------------------------------------------------------------------------
    private static boolean readLog(Context context)
    {
        boolean notify = false;

        // Attempt to build a path to our log file.  This requires a fully
        // initialized Context, so it can fail at first.
        if (!_readLogComplete && buildPath(context))
        {
            FileInputStream     fileInputStream     = null;
            BufferedInputStream bufferedInputStream = null;
            ObjectInputStream   objectInputStream   = null;

            try
            {
                // Attempt to open our log file from a previous run.
                fileInputStream     = new FileInputStream(_file);
                bufferedInputStream = new BufferedInputStream(fileInputStream, 8192);
                objectInputStream   = new ObjectInputStream(bufferedInputStream);

                // Read in all the LogEntry objects.
                LinkedList<LogEntry> readEntries = new LinkedList<LogEntry>();
                while ((fileInputStream.available() > 0) || (bufferedInputStream.available() > 0))
                {
                    try
                    {
                        readEntries.addLast((LogEntry)objectInputStream.readObject());
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, e.toString());
                        break;
                    }
                }

                if (readEntries.size() > 0)
                {
                    // Insert the read entries into the head of our log.
                    _logEntries.addAll(0, readEntries);

                    notify = true;
                }

                // Flag us done reading in any prior log.
                _readLogComplete = true;
            }
            catch (FileNotFoundException e)
            {
                // Flag us done reading in any prior log if there was no prior log.
                _readLogComplete = true;
            }
            catch (Exception e)
            {
                Log.e(TAG, e.toString());
            }
            finally
            {
                // Close our file.
                if (null != objectInputStream)   try { objectInputStream.close();   } catch (Exception e) { Log.e(TAG, e.toString()); }
                if (null != bufferedInputStream) try { bufferedInputStream.close(); } catch (Exception e) { Log.e(TAG, e.toString()); }
                if (null != fileInputStream)     try { fileInputStream.close();     } catch (Exception e) { Log.e(TAG, e.toString()); }
            }
        }

        // If the list changed and anyone cares that the list changed, let them know now.
        if (notify)
        {
            _observable.notifyObservers();
        }

        return _readLogComplete;
    }

    //--------------------------------------------------------------------------
    private static boolean buildPath(Context context)
    {
        if ((null == _file) && (null != context))
        {
            try
            {
                _file = new File(context.getFilesDir().getAbsolutePath() + File.pathSeparator + "log.obj");
            }
            catch (Exception e)
            {
                // It is normal to end up here if the Context is not initialized.
            }
        }
        return (null != _file);
    }

    //**************************************************************************
    public static class LogEntry implements Serializable
    {
        private static final long serialVersionUID = 1L;

        protected long   _timestamp;
        protected int    _pid;
        protected int    _tid;
        protected String _log;

        //----------------------------------------------------------------------
        public LogEntry(String log)
        {
            _timestamp = System.currentTimeMillis();
            _pid       = Process.myPid();
            _tid       = Process.myTid();
            _log       = log;
        }

        //----------------------------------------------------------------------
        @Override public String toString()
        {
            return _log;
        }

        //----------------------------------------------------------------------
        public String getLine2()
        {
            return String.format(Locale.ENGLISH, "%tF  %tT.%tL,  P:%d,  T:%d", _timestamp, _timestamp, _timestamp, _pid, _tid);
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

    //**************************************************************************
    protected static class ObjectOutputStreamAppender extends ObjectOutputStream
    {
        //----------------------------------------------------------------------
        public ObjectOutputStreamAppender(OutputStream output) throws IOException
        {
            super(output);
        }

        //----------------------------------------------------------------------
        @Override protected void writeStreamHeader()
        {
        }
    }
}
