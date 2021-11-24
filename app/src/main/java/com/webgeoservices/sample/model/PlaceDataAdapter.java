package com.webgeoservices.sample.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.webgeoservices.sample.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class PlaceDataAdapter extends ArrayAdapter<PlaceData> {

    public PlaceDataAdapter(Context context, ArrayList<PlaceData> data) {
        super(context, 0, data);
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

            String poiDetails = "City = " + place.getCity() + "\n" + "ZipCode = " + place.getZipCode() + "\n" + "Distance = ";
            if(place.getTravelingDistance() != null) {
                poiDetails += place.getTravelingDistance() + "\n";
            } else {
                poiDetails += place.getDistance().toString() + "\n";
            }
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

                int secs = temp - mins * 60;
                if(secs != 0)
                    endVisitInformation += String.format( "%02d", secs ) + " secs";

            }

            String visitDetails = "Start time = " + displayDateFormat.format(place.getArrivalDate()) + "\n" + endVisitInformation;
            TextView tvDetails = (TextView) convertView.findViewById( R.id.details );
            tvDetails.setText( visitDetails );
        } else if (place.getType() == PlaceData.dataType.regionLog) {
            convertView = LayoutInflater.from(getContext()).inflate( R.layout.item_region, parent, false);
            TextView tvCoord = (TextView) convertView.findViewById( R.id.coordinate );
            tvCoord.setText( String.format("%.5f", place.getLatitude()) + "," + String.format("%.5f", place.getLongitude()) );
            TextView tvDate = (TextView) convertView.findViewById( R.id.date );
            tvDate.setText( displayDateFormat.format( place.getDate() ) );

            String regionDetails = "Identifier = " + place.getRegionIdentifier() + "\n";
            if(!place.getIdStore().isEmpty()) {
                regionDetails += "Id store = " + place.getIdStore() + "\n";
            }
            regionDetails += "Radius = " + place.getRadius() + "\n";
            if(place.isDidEnter()) {
                regionDetails += "Transition : Enter \n";
            } else {
                regionDetails += "Transition : Exit \n";
            }

            if(place.isCurrentPositionInside()) {
                regionDetails += "Current Position Inside : Enter ";
            } else {
                regionDetails += "Current Position Inside :  Exit ";
            }

            if(place.getDuration() != 0){
                regionDetails += "\n" + "Duration = " + place.getDuration() + "\n";
            }

            if(!place.getTravelingDistance().isEmpty()){
                regionDetails += "Distance = " + place.getTravelingDistance();
            }



            TextView tvdetails = (TextView) convertView.findViewById( R.id.details );
            tvdetails.setText( regionDetails );

        }

        return convertView;
    }


}
