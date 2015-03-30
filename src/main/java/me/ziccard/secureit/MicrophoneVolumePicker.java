/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

public class MicrophoneVolumePicker extends View {
  Paint paint = new Paint();

  final int GREEN = 8453888;   // 80FF00
  final int ORANGE = 16744448; // FF8000
  final int RED = 14549506;    // DE0202

  private boolean _isMono = false;

  // margini dal bordo
  final int PADDING_TOP = 30;
  final int PADDING_LEFT = 30;
  final int PADDING_RIGHT = 30;
  final int PADDING_BOTTOM = 30;

  // DRAWING AREAS
  private int _canvasWidth; // in android canvas.getWidth()
  private int _canvasHeight; // in android canvas.getHeight()

  // area disponibile per il disegno
  private int _drawableAreaHeight;
  private int _drawableAreaWidth;

  // noise & cliping value
  final int FULL_SCALE = 120;
  int NOISE_THRESHOLD = 50;
  final int CLIPPING_THRESHOLD = 70;

  // ogetto canvas
  // private var _canvas:Shape;

  // fattore di decellerazione
  final double FACTOR = 0.05;

  // valori visualizzati
  private double _microfoneLeftValue;
  private double _microfoneRightValue;

  // read values
  private double microfoneReadLeftValue;
  private double microfoneReadRightValue;

  public MicrophoneVolumePicker(Context context) {
    super(context);
    this.paint.setTextSize(19f);
  }
  
  public void setNoiseThreshold(double noiseThreshold) {
    this.NOISE_THRESHOLD = (int)noiseThreshold;
  }

  /**
   * Sets the read decibels values (for stereo)
   */
  public void setValues(double leftValue, double rightValue) {
    microfoneReadLeftValue = leftValue;
    microfoneReadRightValue = rightValue;
  }

  @Override
  public void onDraw(Canvas canvas) {

    Log.i("MicrophoneVolumePicker", "Creating view");

    _canvasWidth = canvas.getWidth();
    _canvasHeight = canvas.getHeight();

    Log.i("MicrophoneVolumePicker", "CanvasWidth: " + _canvasWidth
        + " CanvasHeight: " + _canvasHeight);

    _drawableAreaHeight = _canvasHeight - PADDING_TOP - PADDING_BOTTOM;
    _drawableAreaWidth = _canvasWidth - PADDING_LEFT - PADDING_RIGHT;

    if (_isMono)
      _microfoneLeftValue = _microfoneRightValue;
    
    _microfoneLeftValue = microfoneReadLeftValue;
    _microfoneRightValue = microfoneReadRightValue;

    // DECELLAROZIONE PER SMOOTHING DEI DATI IN INGRESSO
    _microfoneLeftValue = ((microfoneReadLeftValue < _microfoneLeftValue) ?
    // SE VALORE RILEVATO E' MINORE APPLICA DECELLERAZIONE
    ((microfoneReadLeftValue * FACTOR) + (_microfoneLeftValue * (1 - FACTOR)))
        :
        // ALTRIMENTI VAI AL VALORE APPENA RILEVATO
        microfoneReadLeftValue);
    _microfoneRightValue = ((microfoneReadRightValue < _microfoneRightValue) ? ((microfoneReadRightValue * FACTOR) + (_microfoneRightValue * (1 - FACTOR)))
        : microfoneReadRightValue);

    // QUI EVENTUALI PROCEDURE DI QUANTIZZAZIONE E NORMALIZZAZIONE
    // NELL'INTERVALLO 0-120 del segnale

    int colomnWidth = 40;
    // DRAW LEFT CHANNEL
    int originX = PADDING_LEFT + _drawableAreaWidth / 2 - 50;
    drawVolumeRect(canvas, originX, colomnWidth, _microfoneLeftValue);

    // DRAWING RIGHT CHANNEL
    originX = PADDING_LEFT + _drawableAreaWidth / 2 + 50;
    drawVolumeRect(canvas, originX, colomnWidth, _microfoneRightValue);
    
    // draw plot
    drawPlot(canvas, colomnWidth);
 
  }

  private void drawVolumeRect(Canvas canvas,
      int originX,
      int width,
      double value) {
    
    double oneDbHeight = _drawableAreaHeight/((double)FULL_SCALE);

    int top = 0;
    int bottom = 0;
    // draws red rectangle: when sampled audio is greater than clipping threshold
    if (value > CLIPPING_THRESHOLD) {
      paint.setColor(Color.RED);
      // determines the starting y-point for red area 
      bottom = PADDING_TOP + ((int)(oneDbHeight*(FULL_SCALE - CLIPPING_THRESHOLD)));
      // determines the ending y-point for red area
      top = PADDING_TOP + ((int)(oneDbHeight*(FULL_SCALE - value))); 

      canvas.drawRect(new Rect(originX, top, originX + width, bottom), paint);

    }
    // draws orange rectangle: when sampled audio is greater than noise threshold
    if (value > NOISE_THRESHOLD) {
      paint.setColor(Color.rgb(0xFF, 0x80, 0x00));
      // determines the starting y-point for orange area 
      bottom = PADDING_TOP + ((int)(oneDbHeight*(FULL_SCALE - NOISE_THRESHOLD)));
      // determines the ending y-point for orange area:
      // if the sampled value is lower than clipping threshold then the value is the 
      // ending point otherwise the starting point of the red area is the ending point 
      top = PADDING_TOP + ((int)(oneDbHeight*(FULL_SCALE - Math.min(value, CLIPPING_THRESHOLD))));

      canvas.drawRect(new Rect(originX, top, originX + width, bottom), paint);
    }

    // draws green rectangle: there must be something under noise threshold
    paint.setColor(Color.GREEN);
    bottom = PADDING_TOP + ((int)(oneDbHeight*FULL_SCALE));
    top = PADDING_TOP + ((int)(oneDbHeight*(FULL_SCALE - Math.min(value, NOISE_THRESHOLD))));

    canvas.drawRect(new Rect(originX, top, originX + width, bottom), paint);

  }
  
  private void drawPlot(Canvas canvas, int width) {
    float step = 10*(_drawableAreaHeight/((float)FULL_SCALE));
    float lineLevel = PADDING_TOP + _drawableAreaHeight;
    float left = PADDING_LEFT + _drawableAreaWidth/2 - 60;
    float right = PADDING_LEFT + _drawableAreaWidth/2 + 100;
    for (int i=0; i< FULL_SCALE/10 + 1; i++) {
      paint.setColor(Color.BLACK);
      if (i*10 == NOISE_THRESHOLD || i*10 == CLIPPING_THRESHOLD) {
        paint.setStrokeWidth(3.0f);
        canvas.drawLine(left, lineLevel, right, lineLevel, paint);
        paint.setStrokeWidth(0);
      } else {
        canvas.drawLine(left, lineLevel, right, lineLevel, paint);
      }
      if (i%2 == 0) {
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(Integer.toString(i*10), left - 10, lineLevel, paint);
      } else {
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(Integer.toString(i*10), right + 10, lineLevel, paint);
      }
      lineLevel = lineLevel - step;
    }
  }

}