package com.example.alexsmedstimer;

public class AlarmWidgetMiniProvider extends AlarmWidgetProvider {
    @Override
    protected int getWidgetLayout() {
        return R.layout.widget_alarm_mini;
    }

    @Override
    protected int getWidgetVariant() {
        return VARIANT_MINI;
    }
}
