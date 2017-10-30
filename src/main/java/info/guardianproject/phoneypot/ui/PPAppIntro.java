package info.guardianproject.phoneypot.ui;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;


import info.guardianproject.phoneypot.R;

/**
 * Created by n8fr8 on 5/8/17.
 */

public class PPAppIntro extends AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFadeAnimation();

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance(getString(R.string.intro1_title), getString(R.string.intro1_desc),
                R.drawable.web_hi_res_512, getResources().getColor(R.color.colorPrimaryDark)));

        /**
        SliderPage sliderPage = new SliderPage();
        sliderPage.setTitle(getString(R.string.intro2_title));
     //   sliderPage.setDescription("This is a demo of the AppIntro library.");
        sliderPage.setBgColor(getResources().getColor(R.color.colorPrimaryDark));
        addSlide(AppIntroFragment.newInstance(sliderPage));**/
        CustomSlideBigText cs1 = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
        cs1.setTitle(getString(R.string.intro2_title));
        addSlide(cs1);

        CustomSlideBigText cs2 = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
        cs2.setTitle(getString(R.string.intro3_desc));
        cs2.showButton(getString(R.string.action_configure), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PPAppIntro.this,MicrophoneConfigureActivity.class));
            }
        });
        addSlide(cs2);

        CustomSlideBigText cs3 = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
        cs3.setTitle(getString(R.string.intro4_desc));
        addSlide(cs3);

        CustomSlideNotify cs4 = CustomSlideNotify.newInstance(R.layout.custom_slide_notify);
        addSlide(cs4);

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro5_title), getString(R.string.intro5_desc),
                R.drawable.web_hi_res_512, getResources().getColor(R.color.colorPrimaryDark)));

        setDoneText(getString(R.string.onboarding_action_end));

        // Hide Skip/Done button.
        showSkipButton(false);
        // setProgressButtonEnabled(false);

    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.

        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }
}
