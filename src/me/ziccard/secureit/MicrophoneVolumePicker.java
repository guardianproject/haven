package me.ziccard.secureit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;

public class MicrophoneVolumePicker extends View {
    Paint paint = new Paint();
    
    final int GREEN = 8453888;
    final int ORANGE = 16744448;
    final int RED = 14549506;

    private boolean _isMono = false;
        
    //margini dal bordo
    final int PADDING_TOP = 30;
    final int PADDING_LEFT = 30;
    final int PADDING_RIGHT = 30;
    final int PADDING_BOTTOM = 30;
    
    
    // DRAWING AREAS
    private int _canvasWidth;  //in android canvas.getWidth()
    private int _canvasHeight;  //in android canvas.getHeight()
        
        //area disponibile per il disegno
    private int _drawableAreaHeight;
    private int _drawableAreaWidth;
        
        
    // noise & cliping value
    final int FULL_SCALE = 120;
    final int NOISE_TRESHOLD = 50;
    final int CLIPPING_TRESHOLD = 70;
    
    // ogetto canvas 
    // private var _canvas:Shape;
    
    
    // fattore di decellerazione
    final double FACTOR = 0.05; 
    
    // valori visualizzati
    private double _microfoneLeftValue;
    private double _microfoneRightValue;
    
    //read values
    private double microfoneReadLeftValue;
    private double microfoneReadRightValue;
    
    public MicrophoneVolumePicker(Context context) {
        super(context);            
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
    	
    	Log.i("MicrophoneVolumePicker","Creating view");
    	
    	_canvasWidth = canvas.getWidth();
        _canvasHeight = canvas.getHeight();
        
        Log.i("MicrophoneVolumePicker","CanvasWidth: "+_canvasWidth + " CanvasHeight: "+_canvasHeight);
         
        _drawableAreaHeight = _canvasHeight-PADDING_TOP-PADDING_BOTTOM;
        _drawableAreaWidth = _canvasWidth-PADDING_LEFT-PADDING_RIGHT;
    
        if(_isMono) _microfoneLeftValue = _microfoneRightValue;

	    // DECELLAROZIONE PER SMOOTHING DEI DATI IN INGRESSO
	    _microfoneLeftValue = 
	    		((microfoneReadLeftValue < _microfoneLeftValue) ?   
	    		//SE VALORE RILEVATO E' MINORE APPLICA DECELLERAZIONE
	    		((microfoneReadLeftValue * FACTOR) + (_microfoneLeftValue * (1 - FACTOR))) :  
	            // ALTRIMENTI VAI AL VALORE APPENA RILEVATO
	    		microfoneReadLeftValue);
	    _microfoneRightValue = 
	    		((microfoneReadRightValue<_microfoneRightValue) ?
	    		((microfoneReadRightValue * FACTOR) + (_microfoneRightValue * (1 - FACTOR))):
	    		microfoneReadRightValue); 

    //QUI EVENTUALI PROCEDURE DI QUANTIZZAZIONE E NORMALIZZAZIONE NELL'INTERVALLO 0-120 del segnale
    
    //DRAW LEFT CHANNEL 
    // TODO: riscrivere gli if sotto forma di bound in Math.min e Math.max
    int originX = PADDING_LEFT+_drawableAreaWidth/2 - 50; 
    int originY;      
    if(_microfoneLeftValue > CLIPPING_TRESHOLD){
        //Se è maggiore della soglia di clipping
        paint.setColor(Color.RED);
        // ATTENZIONE BASATO SU ASSI X,Y = (0,0) CENTRATI IN ALTO A SINISTRA. POTREBBE RICHIEDERE l'inversione 
        // dell'asse Y in accordo con l'origine in android 
        originY = PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE-CLIPPING_TRESHOLD);
        
        canvas.drawRect(new Rect(originX,
                                 originY,
                                 originX + 40, 
                                 // 40 l'effettiva larghezza del rettangolo
                                 originY -(int)((_microfoneLeftValue-CLIPPING_TRESHOLD)*(_drawableAreaHeight/FULL_SCALE))), 
                        paint);

    }
    if(_microfoneLeftValue > NOISE_TRESHOLD){
         //Se è maggiore della soglia di noise
        // TODO: cambiare sto colore giallo in un arancio decente!
        paint.setColor(Color.YELLOW); // in android paint.setColor(Color.YELLOW);
        originY = PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE-NOISE_TRESHOLD);


        canvas.drawRect(new Rect(originX, 
                                originY,
                                originX+40,
                                originY-Math.min((int)(CLIPPING_TRESHOLD-NOISE_TRESHOLD),(int)(_microfoneLeftValue-NOISE_TRESHOLD))
                                *(_drawableAreaHeight/FULL_SCALE)),
                        paint); 
    }

    // sicuramente ha una parte di volume inferiore a noise
    paint.setColor(Color.GREEN);
    originY = PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE);

    canvas.drawRect(new Rect(originX, 
                            originY,
                            originX+40,
                            originY-Math.min((int)NOISE_TRESHOLD,(int)(_microfoneLeftValue))*(_drawableAreaHeight/FULL_SCALE)),
                    paint); 


    // DRAWING RIGHT CHANNEL
    originX = PADDING_LEFT+_drawableAreaWidth/2 + 50; 
    if(_microfoneRightValue > CLIPPING_TRESHOLD){
        //Se è maggiore della soglia di clipping
        paint.setColor(Color.RED);
        // ATTENZIONE BASATO SU ASSI X,Y = (0,0) CENTRATI IN ALTO A SINISTRA. POTREBBE RICHIEDERE l'inversione 
        // dell'asse Y in accordo con l'origine in android 
        originY = PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE-CLIPPING_TRESHOLD);

        canvas.drawRect(new Rect(originX,
                                 originY,
                                 originX - 40, 
                                 // 40 l'effettiva larghezza del rettangolo
                                 originY - (int)((_microfoneRightValue-CLIPPING_TRESHOLD)*(_drawableAreaHeight/FULL_SCALE))), 
                        paint);

    }
    if(_microfoneRightValue > NOISE_TRESHOLD){
         //Se è maggiore della soglia di noise
        // TODO: cambiare sto colore giallo in un arancio decente!
        paint.setColor(Color.YELLOW); // in android paint.setColor(Color.YELLOW);
        originY = PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE-NOISE_TRESHOLD);


        canvas.drawRect(new Rect(originX, 
                                originY,
                                originX-40,
                                originY-Math.min((int)(CLIPPING_TRESHOLD-NOISE_TRESHOLD),(int)(_microfoneRightValue-NOISE_TRESHOLD))
                                *(_drawableAreaHeight/FULL_SCALE)),
                        paint); 
    }

    // sicuramente ha una parte di volume inferiore a noise
    paint.setColor(Color.GREEN);
    originY = PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE);

    canvas.drawRect(new Rect(originX, 
                            originY,
                            originX-40,
                            originY-Math.min((int)NOISE_TRESHOLD,(int)(_microfoneRightValue))*(_drawableAreaHeight/FULL_SCALE)),
                    paint); 

    }

}


/*

// microfono registra in mono
       private var _isMono:Boolean = false;
        
        //margini dal bordo
        private const PADDING_TOP:int = 30;
        private const PADDING_LEFT:int = 30;
        private const PADDING_RIGHT:int = 30;
        private const PADDING_BOTTOM:int = 30;
        
        
        // DRAWING AREAS
        
        
        private var _canvasWidth:int;  //in android canvas.getWidth()
        private var _canvasHeight:int;  //in android canvas.getHeight()
            
            //area disponibile per il disegno
        private var _drawableAreaHeight:int;
        private var _drawableAreaWidth:int;
            
            
        // noise & cliping value
        private const FULL_SCALE:int = 120;
        private const NOISE_TRESHOLD:int = 70;
        private const CLIPPING_TRESHOLD:int = 100;
        
        // ogetto canvas 
        private var _canvas:Shape;
        
        
        // fattore di decellerazione
        private const FACTOR:Number = 0.05; 
        
        // valori visualizzati
        private var _microfoneLeftValue:int;
        private var _microfoneRightValue:int;
        
        public function DocumentClass() {
            _canvas = new Shape();
            addChild(_canvas);
            // ispirato http://www.healthyhearing.com/uploads/images/new/dB%20Volume%20Meter.jpg
            
            // inizializzo variabile per il disegno
             _canvasWidth = 320;  //in android canvas.getWidth()
             _canvasHeight = 480;  //in android canvas.getHeight()
            
            //area disponibile per il disegno
             _drawableAreaHeight = _canvasHeight-PADDING_TOP-PADDING_BOTTOM;
             _drawableAreaWidth = _canvasWidth-PADDING_LEFT-PADDING_RIGHT;
            
            
            
            
            // DISEGNA LE LABEL CON I VALORI
            drawLabel();
            this.addEventListener(Event.ENTER_FRAME, onDraw);
        }
        
        private function drawLabel():void{
            
            var canvas:Shape = _canvas;      // oggetto fittizio per similitudine
            for(var i:int = 0; i<=FULL_SCALE/10;i++){
                var labelTextField:TextField = new TextField();
                labelTextField.width = 30;
                var myFormat:TextFormat = new TextFormat();
                myFormat.align = TextFormatAlign.CENTER;
                labelTextField.x = PADDING_LEFT+_drawableAreaWidth/2 - 90;
                labelTextField.y = PADDING_TOP+(_drawableAreaHeight/FULL_SCALE)*(10*i)-8;
                labelTextField.defaultTextFormat = myFormat;
                canvas.stage.addChild(labelTextField);
                labelTextField.text = ""+(FULL_SCALE/10-i)*10;
                
                // RIGHT LABEL
                labelTextField = new TextField();
                labelTextField.width = 30;
                labelTextField.defaultTextFormat = myFormat;
                labelTextField.text = ""+((FULL_SCALE/10)-i)*10;
                
                labelTextField.x = PADDING_LEFT+_drawableAreaWidth/2 + 55;
                labelTextField.y = PADDING_TOP+(_drawableAreaHeight/FULL_SCALE)*(10*i)-8;
                canvas.stage.addChild(labelTextField);
            }
                
        }
        protected function onDraw(e:Event):void{
            // giusto per similudine facciamo finta che la canvas
            // passata nell'onDraw di Android sia questa
            var canvas:Shape = _canvas;             
            
            // valori in input
            var microfoneReadLeftValue:int = FULL_SCALE*Math.random();
            var microfoneReadRightValue:int = FULL_SCALE*Math.random();
            if(_isMono) _microfoneLeftValue = _microfoneRightValue;
            
            
             _microfoneLeftValue = (microfoneReadLeftValue<_microfoneLeftValue) ?   // SE VALORE RILEVATO E' MINORE
                                        // APPLICA DECELLERAZIONE
                                        (microfoneReadLeftValue * FACTOR) + (_microfoneLeftValue * (1 - FACTOR)) :  
                                        // ALTRIMENTI VAI AL VALORE APPENA RILEVATO
                                        microfoneReadLeftValue;
             _microfoneRightValue = (microfoneReadRightValue<_microfoneRightValue) ?
                                        (microfoneReadRightValue * FACTOR) + (_microfoneRightValue * (1 - FACTOR)):
                                        microfoneReadRightValue; 
            
            
            //QUI EVENTUALI PROCEDURE DI QUANTIZZAZIONE E NORMALIZZAZIONE NELL'INTERVALLO 0-120 del segnale
            
            
            //SMOOTHING DEI VALORI
            
        
            canvas.graphics.clear()  //in android non dovrebbe essere necessario questo comando
            
            
            if(_microfoneLeftValue > CLIPPING_TRESHOLD){
                //Se è maggiore della soglia di clipping
                canvas.graphics.beginFill(0xff0000); // in android paint.setColor(Color.RED);
                canvas.graphics.drawRect(PADDING_LEFT+_drawableAreaWidth/2 - 50, 
                                         PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE-CLIPPING_TRESHOLD),
                                         40,
                                         -((_microfoneLeftValue-CLIPPING_TRESHOLD)*(_drawableAreaHeight/FULL_SCALE))); 
            }
            if(_microfoneLeftValue > NOISE_TRESHOLD){
                //Se è maggiore della soglia di noise
                canvas.graphics.beginFill(0xff7700); // in android paint.setColor(Color.YELLOW);
                canvas.graphics.drawRect(PADDING_LEFT+_drawableAreaWidth/2 - 50, 
                                         PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE-NOISE_TRESHOLD),
                                         40,
                                         -Math.min((CLIPPING_TRESHOLD-NOISE_TRESHOLD),(_microfoneLeftValue-NOISE_TRESHOLD))
                                                  *(_drawableAreaHeight/FULL_SCALE)); 
            }
            // sicuramente ha una parte di volume inferiore a noise
                canvas.graphics.beginFill(0x22ff22); // in android paint.setColor(Color.GREEN);
                canvas.graphics.drawRect(PADDING_LEFT+_drawableAreaWidth/2 - 50, 
                                         PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE),  // semanticamente corretto, tie!
                                         40,
                                         -Math.min(NOISE_TRESHOLD,(_microfoneLeftValue))*(_drawableAreaHeight/FULL_SCALE)); 
                                         
                                         
            // CANALE DESTRO
            
            if(_microfoneRightValue > CLIPPING_TRESHOLD){
                //Se è maggiore della soglia di clipping
                canvas.graphics.beginFill(0xff0000); // in android paint.setColor(Color.RED);
                canvas.graphics.drawRect(PADDING_LEFT+_drawableAreaWidth/2 + 50, 
                                         PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE-CLIPPING_TRESHOLD),
                                         -40,
                                         -((_microfoneRightValue-CLIPPING_TRESHOLD)*(_drawableAreaHeight/FULL_SCALE))); 
            }
            if(_microfoneRightValue > NOISE_TRESHOLD){
                //Se è maggiore della soglia di noise
                canvas.graphics.beginFill(0xff7700); // in android paint.setColor(Color.YELLOW);
                canvas.graphics.drawRect(PADDING_LEFT+_drawableAreaWidth/2 + 50, 
                                         PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE-NOISE_TRESHOLD),
                                         -40,
                                         -Math.min((CLIPPING_TRESHOLD-NOISE_TRESHOLD),(_microfoneRightValue-NOISE_TRESHOLD))
                                                  *(_drawableAreaHeight/FULL_SCALE)); 
            }
            // sicuramente ha una parte di volume inferiore a noise
                canvas.graphics.beginFill(0x22ff22); // in android paint.setColor(Color.GREEN);
                canvas.graphics.drawRect(PADDING_LEFT+_drawableAreaWidth/2 + 50, 
                                         PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(FULL_SCALE),  // semanticamente corretto, tie!
                                         -40,
                                         -Math.min(NOISE_TRESHOLD,(_microfoneRightValue))*(_drawableAreaHeight/FULL_SCALE)); 
                                         
                                         
                
                // DISEGNA UN PO' DI COSE SOPRA. NON SONO SICURO DELL'EQUIVALENTE IN ANDROID.
                // ANDREBBE IN OGNI CASO CHIAMATA UNA SOLA VOLTA E NON AD OGNI onDRAW COME FACCIO QUI
                drawInfos(); 
        }
        
        private function drawInfos():void{
            var canvas:Shape = _canvas;      // oggetto fittizio per similitudine
            for(var i:int = 0; i<=FULL_SCALE/10;i++){
                canvas.graphics.lineStyle(1,0x777777);
                canvas.graphics.moveTo(PADDING_LEFT+_drawableAreaWidth/2 - 60,
                                        PADDING_TOP+(_drawableAreaHeight/FULL_SCALE)*(10*i));
                canvas.graphics.lineTo(PADDING_LEFT+_drawableAreaWidth/2 + 60,
                                        PADDING_TOP+_drawableAreaHeight/FULL_SCALE*(10*i));
            
            }
        }
    }


*/