package com.example.alexsmedstimer;

public class AlarmWidgetLargeProvider extends AlarmWidgetProvider {
    @Override
    protected int getWidgetLayout() {
        return R.layout.widget_alarm_large;
    }

    @Override
    protected int getWidgetVariant() {
        return VARIANT_LARGE;
    }
}
