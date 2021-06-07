/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;

import android.view.MenuItem;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityImageDetailBinding;
import de.welthungerhilfe.cgm.scanner.ui.views.ZoomImageView;

public class ImageDetailActivity extends BaseActivity {

    ActivityImageDetailBinding activityImageDetailBinding;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        activityImageDetailBinding = DataBindingUtil.setContentView(this,R.layout.activity_image_detail);


        setupToolbar();

        byte[] bytes = getIntent().getByteArrayExtra(AppConstants.EXTRA_QR_BITMAP);
        String url = getIntent().getStringExtra(AppConstants.EXTRA_QR_URL);
        activityImageDetailBinding.zoomView.setZoomEnable(true);
        if (bytes != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            activityImageDetailBinding.zoomView.setImageBitmap(bmp);
        } else if (url != null && !url.equals("")) {
            Glide.with(this).load(url).fitCenter().into(activityImageDetailBinding.zoomView);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(activityImageDetailBinding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.title_image_view);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void onBackPressed() {
        finish();
    }
}
