package com.example.appointable;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;
import java.util.Set;

public class EventDotDecorator implements DayViewDecorator {

    private final Set<CalendarDay> dates;
    private final int color;

    public EventDotDecorator(Set<CalendarDay> dates) {
        this.dates = new HashSet<>(dates);
        this.color = 0xFFFF9D00; // purple dot (change if you want)
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new DotSpanDecorator(color));
    }

    // ---- INNER SPAN CLASS ----
    private static class DotSpanDecorator implements LineBackgroundSpan {

        private final Paint paint;

        DotSpanDecorator(int color) {
            paint = new Paint();
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
        }

        @Override
        public void drawBackground(
                Canvas canvas,
                Paint textPaint,
                int left,
                int right,
                int top,
                int baseline,
                int bottom,
                CharSequence text,
                int start,
                int end,
                int lineNumber
        ) {
            float radius = 6;
            float cx = (left + right) / 2f;
            float cy = bottom + radius * 1.5f;

            canvas.drawCircle(cx, cy, radius, paint);
        }
    }
}
