package es.quirk.bladereminder.fragments;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import es.quirk.bladereminder.R;
import es.quirk.bladereminder.ShaveEntry;
import es.quirk.bladereminder.Utils;
import timber.log.Timber;
import butterknife.ButterKnife;
import butterknife.Bind;
import es.quirk.bladereminder.database.DataSource;
import java.util.Date;
import java.text.ParseException;

/**
 * Use the {@link StatisticsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StatisticsFragment extends Fragment {

    @Bind(R.id.stats_highest_uses) TextView mHighestUses;
    @Bind(R.id.stats_highest_uses_date) TextView mHighestUsesDate;
    @Bind(R.id.stats_average_uses) TextView mAverageUses;
    @Bind(R.id.oldest_date) TextView mOldestDate;

    private DataSource mDataSource;
    private final DateFormat mLocalFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG);
    private final DateFormat mDateFormat = Utils.createDateFormatYYYYMMDD();

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StatisticsFragment.
     */
    public static StatisticsFragment newInstance() {
        StatisticsFragment fragment = new StatisticsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public StatisticsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*if (getArguments() != null) {
        }*/
    }

    @Override
    public void onDestroyView() {
        super.onDetach();
        ButterKnife.unbind(this);
    }

    private void fillstats() {
        // fill in crapstats from the database!!
        String oldestDate = mDataSource.getOldestUse();
        if (oldestDate.isEmpty())
            oldestDate = getString(R.string.never);
        mOldestDate.setText(oldestDate);

        String average = "";
        try {
            average = Utils.niceFormat(mDataSource.getAverage());
        } catch (NoSuchFieldException | ArithmeticException ignore) {
            average = getString(R.string.not_enough_entries);
        }
        mAverageUses.setText(average);

        ShaveEntry highest = mDataSource.getHighestUse();
        mHighestUses.setText(Integer.toString(highest.getCount()));

        mHighestUsesDate.setText(highest.getDate());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_statistics, container, false);
        ButterKnife.bind(this, rootView);
        mDataSource = new DataSource(rootView.getContext());
        fillstats();
        return rootView;
    }

}
