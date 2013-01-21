package net.stevemiller.android.referrertest;

import android.app.Application;

//******************************************************************************
public class MainApplication extends Application
{
    //--------------------------------------------------------------------------
    public MainApplication()
    {
        Logger.log(this, "Application.Application()");
    }

    //--------------------------------------------------------------------------
    @Override public void onCreate()
    {
        Logger.log(this, "Application.onCreate()");
        super.onCreate();
    }
}
