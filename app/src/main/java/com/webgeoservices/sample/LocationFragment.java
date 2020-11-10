package com.webgeoservices.sample;

import android.os.Bundle;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.webgeoservices.woosmapgeofencing.PositionsManager;
import com.webgeoservices.woosmapgeofencing.database.WoosmapDb;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LocationFragment extends Fragment {

    TextView mLocationInfo;
    PositionsManager mPositionsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPositionsManager = new PositionsManager(getContext(), WoosmapDb.getInstance(getContext(), true));

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.location, container, false);
        mLocationInfo = view.findViewById(R.id.location);
        mLocationInfo.setMovementMethod(new ScrollingMovementMethod());

        mLocationInfo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Layout layout = ((TextView) v).getLayout();
                int y = (int)event.getY();
                if (layout!=null){
                    int line = layout.getLineForVertical(y);
                    int start = mLocationInfo.getLayout().getLineStart(line);
                    int end = mLocationInfo.getLayout().getLineEnd(line);
                    String text = mLocationInfo.getText().toString().substring(start, end);
                    if(text.contains("Location")) {
                        Pattern patte = Pattern.compile("  Location : ([-0-9.]*),([-0-9.]*)");
                        Matcher matcher = patte.matcher(text);
                        Double lat = 0.0;
                        Double lng = 0.0;
                        while (matcher.find()) {
                            lat = Double.valueOf(matcher.group(1));
                            lng = Double.valueOf(matcher.group(2));

                        }
                        mPositionsManager.searchAPI(lat,lng);
                    }

                }
                return true;
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }



}
