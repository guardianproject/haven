package info.guardianproject.phoneypot.ui;

import com.github.paolorotolo.appintro.AppIntro;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

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
                R.drawable.intro1, getResources().getColor(R.color.colorPrimaryDark)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro2_title), getString(R.string.intro2_desc),
                R.drawable.intro2, getResources().getColor(R.color.colorPrimaryDark)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro3_title), getString(R.string.intro3_desc),
                R.drawable.intro4, getResources().getColor(R.color.colorPrimaryDark)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro4_title), getString(R.string.intro4_desc),
                R.drawable.intro3, getResources().getColor(R.color.colorPrimaryDark)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro5_title), getString(R.string.intro5_desc),
                R.drawable.intro1, getResources().getColor(R.color.colorPrimaryDark)));

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
