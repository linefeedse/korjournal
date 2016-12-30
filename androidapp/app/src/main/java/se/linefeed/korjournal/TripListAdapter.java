package se.linefeed.korjournal;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import se.linefeed.korjournal.models.TripListModel;

import java.util.ArrayList;

public class TripListAdapter extends BaseAdapter implements View.OnClickListener {
    private Activity activity;
    private ArrayList data;
    private static LayoutInflater inflater=null;
    public Resources res;
    TripListModel tempValues=null;
    int i=0;

    public TripListAdapter(Activity a, ArrayList d, Resources resLocal) {

        activity = a;
        data=d;
        res = resLocal;

        inflater = ( LayoutInflater )activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    public int getCount() {
        if(data.size()<=0)
            return 1;
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    /********* Create a holder Class to contain inflated xml file elements *********/
    public static class ViewHolder{

        public TextView dateText;
        public TextView fromText;
        public TextView toText;
        public TextView kmText;
        public TextView reasonText;

    }

    /****** Depends upon data size called for each row , Create each ListView row *****/
    public View getView(int position, View convertView, ViewGroup parent) {

        View vi = convertView;
        ViewHolder holder;

        if(convertView==null){

            /****** Inflate tabitem.xml file for each row ( Defined below ) *******/
            vi = inflater.inflate(R.layout.tripitem, null);

            /****** View Holder Object to contain tabitem.xml file elements ******/

            holder = new ViewHolder();
            holder.dateText = (TextView) vi.findViewById(R.id.date);
            holder.fromText=(TextView)vi.findViewById(R.id.from);
            holder.toText=(TextView)vi.findViewById(R.id.to);
            holder.kmText=(TextView)vi.findViewById(R.id.km);
            holder.reasonText=(TextView)vi.findViewById(R.id.reason);

            /************  Set holder with LayoutInflater ************/
            vi.setTag( holder );
        }
        else
            holder=(ViewHolder)vi.getTag();

        if(data.size()<=0)
        {
            holder.dateText.setText("Ingen Data");
        }
        else
        {
            /***** Get each Model object from Arraylist ********/
            tempValues=null;
            tempValues = ( TripListModel ) data.get( position );

            /************  Set Model values in Holder elements ***********/

            holder.dateText.setText(tempValues.getWhen());
            holder.fromText.setText(tempValues.getFromAddress());
            holder.toText.setText(tempValues.getToAddress());
            holder.kmText.setText(tempValues.getKmString());
            holder.reasonText.setText(tempValues.getReason());

            /******** Set Item Click Listner for LayoutInflater for each row *******/

            vi.setOnClickListener(new OnItemClickListener( position ));
        }
        return vi;
    }

    @Override
    public void onClick(View v) {
    }

    /********* Called when Item click in ListView ************/
    private class OnItemClickListener  implements View.OnClickListener {
        private int mPosition;

        OnItemClickListener(int position){
            mPosition = position;
        }

        @Override
        public void onClick(View arg0) {

            TripListActivity sct = (TripListActivity)activity;
            sct.onItemClick(mPosition);
        }
    }
}
