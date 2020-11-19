package com.webgeoservices.sample.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.webgeoservices.sample.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;

public class PlaceDataAdapter extends ArrayAdapter<PlaceData> {

    public PlaceDataAdapter(Context context, ArrayList<PlaceData> data) {
        super(context, 0, data);
    }

    @Override
    public void add(PlaceData object) {
        super.add(object);
        this.sort(new Comparator<PlaceData>() {
            @Override
            public int compare(PlaceData o1, PlaceData o2) {
                return Long.compare(o2.getDate(), o1.getDate());
            }
        });
    }
    @Override
    public void notifyDataSetChanged() {
        //do your sorting here

        super.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        convertView = LayoutInflater.from(getContext()).inflate( R.layout.item_location, parent, false);

        PlaceData place = getItem(position);

        if (place.getType() == PlaceData.dataType.location) {
            TextView tvCoord = (TextView) convertView.findViewById( R.id.coordinate );
            tvCoord.setText( place.getLatitude() + "," + place.getLongitude() );
            TextView tvDate = (TextView) convertView.findViewById( R.id.date );
            tvDate.setText( displayDateFormat.format( place.getDate() ) );
        } else if (place.getType() == PlaceData.dataType.POI) {
            convertView = LayoutInflater.from(getContext()).inflate( R.layout.item_poi, parent, false);
            TextView tvCoord = (TextView) convertView.findViewById( R.id.coordinate );
            tvCoord.setText( place.getLatitude() + "," + place.getLongitude() );
            TextView tvDate = (TextView) convertView.findViewById( R.id.date );
            tvDate.setText( displayDateFormat.format( place.getDate() ) );

            String poiDetails = "City = " + place.getCity() + "\n" + "ZipCode = " + place.getZipCode() + "\n" + "Distance = " + place.getDistance().toString() + "\n";
            if(place.getMovingDuration() != null)
                poiDetails += "Duration = " + place.getMovingDuration();
            TextView tvdetails = (TextView) convertView.findViewById( R.id.details );
            tvdetails.setText( poiDetails );
        } else if (place.getType() == PlaceData.dataType.visit) {
            convertView = LayoutInflater.from(getContext()).inflate( R.layout.item_visit, parent, false);
            TextView tvCoord = (TextView) convertView.findViewById( R.id.coordinate );
            tvCoord.setText( place.getLatitude() + "," + place.getLongitude() );

            String endVisitInformation = "Visit is Ongoing";

            if (place.getDuration() != 0) {
                endVisitInformation = "End time = " + displayDateFormat.format(place.getDepartureDate()) + "\n" + "Duration = ";
                long timeSec = place.getDuration() / 1000;
                int hours = (int) timeSec / 3600;
                if(hours != 0)
                    endVisitInformation += String.format( "%02d", hours ) + " hours ";

                int temp = (int) timeSec - hours * 3600;
                int mins = temp / 60;
                if(mins != 0)
                    endVisitInformation += String.format( "%02d", mins ) + " mins ";

                temp = temp - mins * 60;
                int secs = temp;
                if(mins != 0)
                    endVisitInformation += String.format( "%02d", secs ) + " secs";

            }

            String visitDetails = "Start time = " + displayDateFormat.format(place.getArrivalDate()) + "\n" + endVisitInformation;
            TextView tvdetails = (TextView) convertView.findViewById( R.id.details );
            tvdetails.setText( visitDetails );
        }

        return convertView;
    }
}
