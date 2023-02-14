package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.FragmentStatisticsBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.hardware.io.FileSystem;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class StatisticsDialogfragment extends DialogFragment {

    FragmentStatisticsBinding fragmentStatisticsBinding;
    Long currentTime;
    String currentDate;
    PersonRepository personRepository;
    int totalChildren, male, female, ageBig, ageSmall;
    private static final String SUPPORT_APP = "com.google.android.gm";
    private static final String SUPPORT_MIME = "application/zip";
    String body;
    boolean belongs_to_rst=false;
    SessionManager sessionManager;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
       fragmentStatisticsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_statistics, container, false);
       currentDate = getCurrentDate();
       sessionManager = new SessionManager(getActivity());
       Log.i("StatisticsDialogfragment","this is current date "+currentDate);
       currentTime = System.currentTimeMillis();
       Log.i("StatisticsDialogfragment","this is current time "+currentTime);
        Log.i("StatisticsDialogfragment","this is current date in millisecond "+getMilliFromDate(currentDate));
        Log.i("StatisticsDialogfragment","this is current time in date "+getTimeStamp(currentTime));
        Log.i("StatisticsDialogfragment","this is current time in date "+getTimeStamp(getMilliFromDate(currentDate)));
        personRepository = PersonRepository.getInstance(getActivity());
        Log.i("StatisticsDialogfragment","this is current count "+personRepository.getPersonStat(getMilliFromDate(currentDate),belongs_to_rst).size());
        if(sessionManager.getSelectedMode()== AppConstants.RST_MODE){
            belongs_to_rst = true;
        }else {
            belongs_to_rst = false;
        }
        countData(personRepository.getPersonStat(getMilliFromDate(currentDate),belongs_to_rst));
        fragmentStatisticsBinding.btShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String subject = "CGM-Scanner-statistics";

                Intent sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                sendIntent.setType(SUPPORT_MIME);
                sendIntent.setPackage(SUPPORT_APP);
                sendIntent.putExtra(Intent.EXTRA_TEXT,body);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

                ArrayList<Uri> uris = new ArrayList<Uri>();


                sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

                try {
                    getActivity().startActivity(sendIntent);
                } catch (Exception e) {
                    sendIntent.setPackage(null);
                    getActivity().startActivity(sendIntent);
                }
            }
        });
        return fragmentStatisticsBinding.getRoot();
    }

    public static String getCurrentDate() {
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        String formattedDate = df.format(c.getTime());
        return formattedDate;
    }

    public long getMilliFromDate(String dateFormat) {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        try {
            date = formatter.parse(dateFormat);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println("Today is " + date);
        return date.getTime();
    }

    public String getTimeStamp(long timeinMillies) {
        String date = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // modify format
        date = formatter.format(new Date(timeinMillies));


        return date;
    }

    public void countData(List<Person> personList){
        totalChildren = personList.size();
        for(Person person: personList){
            if(person.getSex().equals("male")){
                male++;
            }else {
                female++;
            }
           float age = (float) ((System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24/ 365.0);
            Log.i("stat","this is age "+age);
            if(age >= 2){
                ageBig++;
            }
            else {
                ageSmall++;
            }

        }
        updateUi();
    }
    public void updateUi(){
        fragmentStatisticsBinding.tvDate.setText("Date:- "+getCurrentDate());
        fragmentStatisticsBinding.tvTotalChildren.setText(""+totalChildren);
        fragmentStatisticsBinding.tvMale.setText(""+male);
        fragmentStatisticsBinding.tvFemale.setText(""+female);
        fragmentStatisticsBinding.tvBigAge.setText(""+ageBig);
        fragmentStatisticsBinding.tvSmallAge.setText(""+ageSmall);
        body = "Statistics for "+getCurrentDate()+"\n\nTotal Children = "+totalChildren+"\nMale = "+male+ "\nFemale = "+female+"\n< 2 years = "+ageSmall+"\n> 2 years = "+ageBig;
    }
}
