package com.example.alexsmedstimer;

public class AlarmWidgetCompactProvider extends AlarmWidgetProvider {
    @Override
    protected int getWidgetLayout() {
        return R.layout.widget_alarm_compact;
    }

    @Override
    protected int getWidgetVariant() {
        return VARIANT_COMPACT;
    }
}
