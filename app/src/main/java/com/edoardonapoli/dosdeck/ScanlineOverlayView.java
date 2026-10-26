package com.edoardonapoli.dosdeck;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

final class ScanlineOverlayView extends View {
    private final Paint paint = new Paint();

    ScanlineOverlayView(Context context) {
        super(context);
        paint.setColor(Color.argb(22, 0, 0, 0));
        paint.setStrokeWidth(1f);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        setFocusable(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int y = 0; y < getHeight(); y += 5) {
            canvas.drawLine(0, y, getWidth(), y, paint);
        }
    }
}
