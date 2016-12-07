package com.yunos.alicontacts.dialpad;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.ToneGenerator;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.dialpad.DialpadFooterView.OnDailpadFooterBarViewClickListener;
import com.yunos.alicontacts.quickcall.QuickCallPickerActivity;
import com.yunos.alicontacts.quickcall.QuickCallSetting;
import com.yunos.common.UiTools;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

/*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 begin*/
import android.media.AudioManager;
import android.media.MediaActionSound;
import java.lang.Thread;
import com.bird.contacts.BirdFeatureOption;
/*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 end*/
public class DialpadView extends LinearLayout implements DialpadImageButton.OnPressedListener,
        View.OnLongClickListener {
    private static final String TAG = "DialpadView";
    private static final boolean DEBUG = true;

    /** The length of DTMF tones in milliseconds */
    private static final int TONE_LENGTH_MS = 100;

    /** The DTMF tone volume relative to other sounds in the stream */
    private static final int TONE_RELATIVE_VOLUME = 80;

    /**
     * Stream type used to play the DTMF tones off call, and mapped to the
     * volume control keys
     * tianyuan.ty 20160330 : change type to AudioManager.STREAM_SYSTEM for merge BugID:(8079139).
     */
    private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_SYSTEM;

    private static final String[] BUTTON_TAG = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#"
    };

    private static final String[] EXT_BUTTON_TAG = {
            ",", "+", ";"
    };

    private OnTextChangeListener mOnTextChangeListener;
    View mDigtalField;
    private EditText mEditText;
    private TextView mTextView;
    private boolean mHasTextInfo;
    private int mTextInfoResId;

    //private AutoScaleTextSizeWatcher mAutoScaleTextSizeWatcher;

    private AlertDialog mQuickCallSettingDialog;
    private QuickCallSetting mQuickCallSetting;
    private OnSpeedDialListener mSpeedDialListener;

//    @GuardedBy ("mToneGeneratorLock")
    private ToneGenerator mToneGenerator;
    private final Object mToneGeneratorLock = new Object();
    private boolean mDTMFToneEnabled;
    private Vibrator mVibrator;

    private DialpadFooterView mDialpadFooterView;

    private DialpadFragment mFragment;
    /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 begin*/
    private MediaActionSound mDialpadSound;
    /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 end*/
    public DialpadView(Context context) {
        super(context);
        mContext = context;
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 begin*/
		android.util.Log.i("lcf_diapad","BIRD_DOOV_SOUND==>" + BirdFeatureOption.BIRD_DOOV_SOUND);
        if (BirdFeatureOption.BIRD_DOOV_SOUND) {
            initDialpadSound();
        }
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 end*/
    }

    public DialpadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 begin*/
		android.util.Log.i("lcf_diapad","BIRD_DOOV_SOUND==>" + BirdFeatureOption.BIRD_DOOV_SOUND);
        if (BirdFeatureOption.BIRD_DOOV_SOUND) {
            initDialpadSound();
        }
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 end*/
    }

    public DialpadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 begin*/
		android.util.Log.i("lcf_diapad","BIRD_DOOV_SOUND==>"+BirdFeatureOption.BIRD_DOOV_SOUND);
        if (BirdFeatureOption.BIRD_DOOV_SOUND) {
            initDialpadSound();
        }
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 end*/
    }
    public static interface OnSpeedDialListener {
        void onSpeedDial(String number, int defaultSim);
    }

    public void setOnSpeedDialListener(OnSpeedDialListener l) {
        mSpeedDialListener = l;
    }

    public static interface OnTextChangeListener {
        void onTextChangeListener(String number);
    }

    public void setEditTextView(EditText editText) {
        if (mEditText == editText) {
            // avoid to put more and more DialerKeyListener instances to InputFilter[] below.
            return;
        }
        mEditText = editText;
        // If the uri of intent Intent.ACTION_DIAL contains invalid chars, e.g. chinese chars,
        // android:inputType="phone" in the xml can NOT prevent the invalid chars from showing.
        // So we have to add a filter here to remove invalid chars.
        // But we don't want to break existing filters, e.g. length filter etc.
        // Here we put the extra filter to the end of existing filters.
        UiTools.addDialerListenerToEditText(mEditText);
    }

    public void setOnTextChangeListener(OnTextChangeListener l) {
        mOnTextChangeListener = l;
    }

    public String getPhoneNumber() {
        if (mEditText == null) {
            return "";
        }
        return mEditText.getText().toString();
    }

    public void setParentFragment(DialpadFragment fragment) {
        mFragment = fragment;
    }

    /*
     * Bug:63235, by Ali.Xulun
     * Enhancements:
     * 1. fix number==null cause NullPointerException issue
     * 2. Because EditText will change String's content in some case, we get the value from it.
     * 3. If length is 0, don't need to set selection.
     */
    public void setPhoneNumber(String number) {
        if (mEditText != null && number != null) {
            mEditText.setText(number);
            final String trueNumber = mEditText.getText().toString();

            if (trueNumber.length() == 0) {
                updateEditorField(true);
            } else {
                mEditText.setSelection(trueNumber.length());
            }
        }
    }

    public void setInfoText(boolean hasInfo, int resId) {
        mHasTextInfo = hasInfo;
        mTextInfoResId = resId;
        if (mTextView != null) {
            updateEditorField(TextUtils.isEmpty(getPhoneNumber()));
        }
    }

    public void createDailpadView() {
        mTextView = (TextView) this.findViewById(R.id.dialpad_txt);
        mDigtalField = this.findViewById(R.id.dialpad_digital);
        setupKeypad(mDigtalField);
        mDialpadFooterView = (DialpadFooterView) this.findViewById(R.id.dialpad_btn_group);

        // For Bug 5252086, remove gesture from dialpad
        //setDigitalViewGesture();
    }

    public void setOnDailpadFooterBarViewClickListener(OnDailpadFooterBarViewClickListener listener) {
        if (mDialpadFooterView != null) {
            mDialpadFooterView.setOnDailpadFooterBarViewClickListener(listener);
        }
    }

//    public void setAutoScaleTextSizeWatcher(AutoScaleTextSizeWatcher watcher) {
//        this.mAutoScaleTextSizeWatcher = watcher;
//    }

    public void setFooterBarSimMode(boolean mutiSimEnable,
            boolean sim1Enable, boolean sim2Enable) {
        if (mDialpadFooterView != null) {
            mDialpadFooterView.setFooterBarSimMode(mutiSimEnable, sim1Enable, sim2Enable);
        }
    }

    private void updateEditorField(boolean showText) {
        if (mHasTextInfo && showText) {
            mTextView.setVisibility(View.VISIBLE);
            mTextView.setText(mTextInfoResId);
            return;
        }
        mTextView.setVisibility(View.GONE);
    }

    protected void notifyListener(String s) {
        if (mOnTextChangeListener != null) {
            mOnTextChangeListener.onTextChangeListener(s);
        }
    }

    public void deleteButtonLongClicked() {
        if (mEditText == null) {
            Log.w(TAG, "deleteButtonLongClicked: mEditText is null.");
            return;
        }
        mEditText.setText("");
        mEditText.setCursorVisible(false);
        updateEditorField(true);
    }

    public void deleteButtonClicked() {
        if (mEditText == null) {
            Log.w(TAG, "deleteButtonClicked: mEditText is null.");
            return;
        }
        int len = mEditText.length();
        int selEnd = mEditText.getSelectionEnd();
        // no content or the cursor is before all content
        if ((len == 0) || (selEnd == 0)) {
            return;
        }

        int selStart = mEditText.getSelectionStart();
        Editable edit = mEditText.getEditableText();
        if (selStart != selEnd) {
            edit.delete(selStart, selEnd);
        } else {
            edit.delete(selStart - 1, selStart);
        }

        // the edit position is not in the end, display the cursor
        if (selEnd != len) {
            mEditText.setCursorVisible(true);
        }
        // all numbers are deleted, display the prompt
        if (mEditText.length() == 0) {
            updateEditorField(true);
        }
    }

    private void digtalButtonClicked(String s) {
        if (mEditText == null) {
            Log.w(TAG, "digtalButtonClicked: mEditText is null.");
            return;
        }
        int len = mEditText.length();
        int selStart = mEditText.getSelectionStart();
        int selEnd = mEditText.getSelectionEnd();
        Editable edit = mEditText.getEditableText();
        if (selStart != selEnd) {
            edit.replace(selStart, selEnd, s);
        } else {
            edit.insert(selStart, s);
        }

        updateEditorField(false);
        // the edit position is not in the end, display the cursor
        if (selEnd != len) {
            mEditText.setCursorVisible(true);
        } else {
            mEditText.setCursorVisible(false);
        }
    }

    /************ BEGIN. Add dialpad related codes, added by fangjun.lin ***********/
    private void setupKeypad(View fragmentView) {
        int[] buttonIds = new int[] {
                R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven,
                R.id.eight, R.id.nine, R.id.zero, R.id.star, R.id.pound
        };
        for (int id : buttonIds) {
            DialpadImageButton btn = (DialpadImageButton) fragmentView.findViewById(id);
            btn.setOnPressedListener(this);
            btn.setOnLongClickListener(this);
        }
    }

    /* YUNOS BEGIN PB */
    //##email:caixiang.zcx@alibaba-inc.com
    //##BugID:(5883538) ##date:2015/05/11
    //##description:add edit contact function when set the quick call
    private void pickEditContact(final int keycode) {
        Intent intent = new Intent(mContext, QuickCallPickerActivity.class);
        intent.putExtra(QuickCallSetting.PICMETHOD, 0);
        intent.putExtra(QuickCallSetting.EXTRAPOS, keycode);
        mContext.startActivity(intent);
    }
    /* YUNOS END PB */

    private void pickContact(final int keycode) {
        Intent intent = new Intent(mContext, QuickCallPickerActivity.class);
        intent.putExtra(QuickCallSetting.PICMETHOD, 1);
        intent.putExtra(QuickCallSetting.EXTRAPOS, keycode);
        mContext.startActivity(intent);
    }

    private void showQuickCallSettingDialog(final int keyCode) {
        /* YUNOS BEGIN PB */
        //##email:caixiang.zcx@alibaba-inc.com
        //##BugID:(5883538) ##date:2015/05/11
        //##description:add edit contact function when set the quick call
        String [] chooseItems = { getResources().getString(R.string.pick_exit_contacts),
                getResources().getString(R.string.pick_edit_contacts) };
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(true);
        String msg = getResources().getString(R.string.quick_call_setting_tip, keyCode);
        builder.setTitle(msg);
        builder.setItems(chooseItems,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if(which == 0) {
                    pickEditContact(keyCode + KeyEvent.KEYCODE_0);
                }else {
                    pickContact(keyCode + KeyEvent.KEYCODE_0);
                }
                setPhoneNumber("");
           }
        });
        builder.setNegativeButton(null);
        /*builder.setPositiveButton(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                pickContact(keyCode + KeyEvent.KEYCODE_0);
                setPhoneNumber("");
            }
        });*/
        /* YUNOS END PB */
        mQuickCallSettingDialog = builder.create();
        mQuickCallSettingDialog.show();
    }

    @Override
    public void onPressed(View view, boolean pressed) {
        if (DEBUG)
            Log.d(TAG, "onPressed(). view: " + view + ", pressed: " + pressed);
        if (pressed) {
            switch (view.getId()) {
                case R.id.one: {
                    keyPressed(KeyEvent.KEYCODE_1);
                    break;
                }
                case R.id.two: {
                    keyPressed(KeyEvent.KEYCODE_2);
                    break;
                }
                case R.id.three: {
                    keyPressed(KeyEvent.KEYCODE_3);
                    break;
                }
                case R.id.four: {
                    keyPressed(KeyEvent.KEYCODE_4);
                    break;
                }
                case R.id.five: {
                    keyPressed(KeyEvent.KEYCODE_5);
                    break;
                }
                case R.id.six: {
                    keyPressed(KeyEvent.KEYCODE_6);
                    break;
                }
                case R.id.seven: {
                    keyPressed(KeyEvent.KEYCODE_7);
                    break;
                }
                case R.id.eight: {
                    keyPressed(KeyEvent.KEYCODE_8);
                    break;
                }
                case R.id.nine: {
                    keyPressed(KeyEvent.KEYCODE_9);
                    break;
                }
                case R.id.zero: {
                    keyPressed(KeyEvent.KEYCODE_0);
                    break;
                }
                case R.id.pound: {
                    keyPressed(KeyEvent.KEYCODE_POUND);
                    break;
                }
                case R.id.star: {
                    keyPressed(KeyEvent.KEYCODE_STAR);
                    break;
                }
                default: {
                    Log.wtf(TAG, "Unexpected onTouch(ACTION_DOWN) event from: " + view);
                    break;
                }
            }
        }
    }

    private void keyPressed(int keyCode) {
        if (DEBUG)
            Log.d(TAG, "keyPressed(). keyCode: " + keyCode);
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 begin*/
		android.util.Log.i("lcf_diapad","BIRD_DOOV_SOUND==>" + BirdFeatureOption.BIRD_DOOV_SOUND);
		if (BirdFeatureOption.BIRD_DOOV_SOUND) {
      		switch (keyCode) {
    	  	case KeyEvent.KEYCODE_1:
      	 		   playTone(ToneGenerator.TONE_DTMF_1, TONE_LENGTH_MS);
      		 	   break;
     		 case KeyEvent.KEYCODE_2:
     		     playTone(ToneGenerator.TONE_DTMF_2, TONE_LENGTH_MS);
    	 	     break;
      		case KeyEvent.KEYCODE_3:
     		     playTone(ToneGenerator.TONE_DTMF_3, TONE_LENGTH_MS);
    	 	     break;
    	 	 case KeyEvent.KEYCODE_4:
    	 	     playTone(ToneGenerator.TONE_DTMF_4, TONE_LENGTH_MS);
    	 	     break;
     		 case KeyEvent.KEYCODE_5:
     		     playTone(ToneGenerator.TONE_DTMF_5, TONE_LENGTH_MS);
     		     break;
     		 case KeyEvent.KEYCODE_6:
     		     playTone(ToneGenerator.TONE_DTMF_6, TONE_LENGTH_MS);
      		    break;
      		case KeyEvent.KEYCODE_7:
      		    playTone(ToneGenerator.TONE_DTMF_7, TONE_LENGTH_MS);
      		    break;
     		 case KeyEvent.KEYCODE_8:
     		     playTone(ToneGenerator.TONE_DTMF_8, TONE_LENGTH_MS);
     		     break;
     		 case KeyEvent.KEYCODE_9:
     		     playTone(ToneGenerator.TONE_DTMF_9, TONE_LENGTH_MS);
     		     break;
    		  case KeyEvent.KEYCODE_0:
    		      playTone(ToneGenerator.TONE_DTMF_0, TONE_LENGTH_MS);
     		     break;
     		 case KeyEvent.KEYCODE_POUND:
      		    playTone(ToneGenerator.TONE_DTMF_P, TONE_LENGTH_MS);
      		    break;
     		 case KeyEvent.KEYCODE_STAR:
     		     playTone(ToneGenerator.TONE_DTMF_S, TONE_LENGTH_MS);
      		    break;
     		 default:
      		    break;
			}
        }
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 end*/	
        this.digtalButtonClicked(BUTTON_TAG[keyCode - KeyEvent.KEYCODE_0]);

        vibrate(TONE_LENGTH_MS);
    }

    /**
     * Plays the specified tone for TONE_LENGTH_MS milliseconds. The tone is
     * played locally, using the audio stream for phone calls. Tones are played
     * only if the "Audible touch tones" user preference is checked, and are NOT
     * played if the device is in silent mode. The tone length can be -1,
     * meaning "keep playing the tone." If the caller does so, it should call
     * stopTone() afterward.
     *
     * @param tone a tone code from {@link ToneGenerator}
     * @param durationMs tone length.
     */
    private void playTone(int tone, int durationMs) {
		android.util.Log.i("lcf_diapad","playTone tone==>"+tone+" durationMs==>"+durationMs);
        // if local tone playback is disabled, just return.
        mDTMFToneEnabled = Settings.System.getInt(this.getContext().getContentResolver(),
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;

        if (!mDTMFToneEnabled) {
            return;
        }

        // Also do nothing if the phone is in silent mode.
        // We need to re-check the ringer mode for *every* playTone()
        // call, rather than keeping a local flag that's updated in
        // onResume(), since it's possible to toggle silent mode without
        // leaving the current activity (via the ENDCALL-longpress menu.)
        AudioManager audioManager = (AudioManager) getContext().getSystemService(
                Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
                || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
            // mHaptic.vibrateFromDialpad(ringerMode);
            return;
        }
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 begin*/
		android.util.Log.i("lcf_diapad","playTone BIRD_DOOV_SOUND ==>"+BirdFeatureOption.BIRD_DOOV_SOUND);
        if(BirdFeatureOption.BIRD_DOOV_SOUND){
            initDialpadSound();
            mDialpadSound.play(tone+4);
            return;
        }
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 end*/
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                try {
                    mToneGenerator = new ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME);
                } catch (RuntimeException e) {
                    Log.w("DialpadView", "Exception caught while creating local tone generator: " + e);
                    mToneGenerator = null;
                    return;
                }
            }

            // Start the new tone (will stop any playing tone)
            mToneGenerator.startTone(tone, durationMs);
        }

    }

    /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 begin*/
    private void initDialpadSound(){
        if(mDialpadSound == null){
            mDialpadSound = new MediaActionSound();
            new Thread(){
                public void run() {
                  mDialpadSound.load(MediaActionSound.DIALPAD_0);
                  mDialpadSound.load(MediaActionSound.DIALPAD_1);
                  mDialpadSound.load(MediaActionSound.DIALPAD_2);
                  mDialpadSound.load(MediaActionSound.DIALPAD_3);
                  mDialpadSound.load(MediaActionSound.DIALPAD_4);
                  mDialpadSound.load(MediaActionSound.DIALPAD_5);
                  mDialpadSound.load(MediaActionSound.DIALPAD_6);
                  mDialpadSound.load(MediaActionSound.DIALPAD_7);
                  mDialpadSound.load(MediaActionSound.DIALPAD_8);
                  mDialpadSound.load(MediaActionSound.DIALPAD_9);
                  mDialpadSound.load(MediaActionSound.DIALPAD_POUND);
                  mDialpadSound.load(MediaActionSound.DIALPAD_STAR);
                }
            }.start();
      }
    }
    /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 end*/
    /**
     * Stop the tone if it is played.
     */
    private void stopTone() {
        // if local tone playback is disabled, just return.
        if (!mDTMFToneEnabled) {
            return;
        }
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 begin*/
        if(BirdFeatureOption.BIRD_DOOV_SOUND){
            return;
        }
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 end*/
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                Log.w(TAG, "stopTone: mToneGenerator == null");
                return;
            }
            mToneGenerator.stopTone();
        }
    }

    public void releaseToneGenerator() {
        stopTone();
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 begin*/
        if(BirdFeatureOption.BIRD_DOOV_SOUND){
          if (mDialpadSound != null) {
              mDialpadSound.release();
              mDialpadSound = null;
          }
        }
        /*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 end*/
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }
    }

    private void vibrate(int duration) {
        boolean vibratorEnabled = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1;
        if (!vibratorEnabled) {
            return;
        }

        if (mVibrator == null) {
            mVibrator = (Vibrator) getContext().getSystemService(Service.VIBRATOR_SERVICE);
        }

        mVibrator.vibrate(duration);
    }

    @Override
    public boolean onLongClick(View v) {
        int id = v.getId();
        int keyCode = -1;
        boolean event = false;
        switch (id) {
            case R.id.one:
                keyCode = KeyEvent.KEYCODE_1;
                break;
            case R.id.two:
                keyCode = KeyEvent.KEYCODE_2;
                break;
            case R.id.three:
                keyCode = KeyEvent.KEYCODE_3;
                break;
            case R.id.four:
                keyCode = KeyEvent.KEYCODE_4;
                break;
            case R.id.five:
                keyCode = KeyEvent.KEYCODE_5;
                break;
            case R.id.six:
                keyCode = KeyEvent.KEYCODE_6;
                break;
            case R.id.seven:
                keyCode = KeyEvent.KEYCODE_7;
                break;
            case R.id.eight:
                keyCode = KeyEvent.KEYCODE_8;
                break;
            case R.id.nine:
                keyCode = KeyEvent.KEYCODE_9;
                break;
            case R.id.zero:
                digtalButtonClicked(EXT_BUTTON_TAG[1]);
                event = true;
                break;
            case R.id.star:
                digtalButtonClicked(EXT_BUTTON_TAG[0]);
                event = true;
                break;
            case R.id.pound:
                digtalButtonClicked(EXT_BUTTON_TAG[2]);
                event = true;
                break;
            default:
                break;
        }
        if (keyCode != -1 && getPhoneNumber().length() == 0) {
            if (mQuickCallSetting == null) {
                mQuickCallSetting = QuickCallSetting.getQuickCallInstance(mContext);
            }
            String number = mQuickCallSetting.getPhoneNumber(keyCode);
            if (TextUtils.isEmpty(number)) {
                showQuickCallSettingDialog(keyCode - KeyEvent.KEYCODE_0);
            } else {
                int sim = mQuickCallSetting.getDefaultQuickDialSim(keyCode);
                if (mSpeedDialListener != null) {
                    mSpeedDialListener.onSpeedDial(number, sim);
                }
            }
            UsageReporter.onClick(mFragment.getActivity(), null,
                    UsageReporter.ContactsSettingsPage.QUICK_CALL_LONG_PRESS);
            return true;
        }
        return event;
    }

    // private void replacePreviousDigitIfPossible(String s) {
    // if (mEditText == null) {
    // Log.w(TAG, "replacePreviousDigitIfPossible: mEditText is null.");
    // return;
    // }
    // final Editable editable = mEditText.getText();
    // final int currentPosition = mEditText.getSelectionStart();
    // if (currentPosition > 0) {
    // mEditText.setSelection(currentPosition);
    // editable.replace(currentPosition - 1, currentPosition, s);
    // }
    // }
    /************ END.Add dialpad related codes, added by fangjun.lin ***********/

    /*private void setDigitalViewGesture() {
        mDigitalGestureDetector = new GestureDetector(this.getContext(),
                new DialpadOnGestureListener());
        this.mDigtalField.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mDigitalGestureDetector.onTouchEvent(event);
                return false;
            }

        });
    }

    class DialpadOnGestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (velocityX < VELOCITY_X_SLOP) {
                if (mFragment != null) {
                    mFragment.scrollViewPager();
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }*/
}
